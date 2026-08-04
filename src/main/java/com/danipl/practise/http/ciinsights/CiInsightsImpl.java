package com.danipl.practise.http.ciinsights;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of {@link CiInsights}.
 */
public final class CiInsightsImpl implements CiInsights {

    private final static int DEFAULT_TIMEOUT_IN_SECS = 10;

    private final static Set<Status> FINISHED_STATUSES = Set.of(Status.PASSED, Status.FAILED);

    private final static ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final String baseUrl;
    private final HttpClient httpClient;

    public CiInsightsImpl(final String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(DEFAULT_TIMEOUT_IN_SECS))
                .build();
    }

    @Override
    public List<Build> fetchBuilds() {
        final String requestUrl = baseUrl + "/builds";
        final HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(requestUrl))
                .timeout(Duration.ofSeconds(DEFAULT_TIMEOUT_IN_SECS))
                .header("User-Agent", "Seven-Leage/CLI")
                .headers("Accept", "application/json")
                .GET()
                .build();

        try {
            final HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (httpResponse.statusCode() / 100 != 2) {
                throw new CiApiException("Error response: " + httpResponse.statusCode());
            }

            return MAPPER.readValue(
                    httpResponse.body(),
                    new TypeReference<List<Build>>() {
                    }
            );
        } catch (final CiApiException ciae) {
            throw ciae;
        } catch (final JsonMappingException jme) {
            throw new CiApiException("Wrong response: json invalid");
        } catch (final ConnectException ce) {
            throw new CiApiException("Cannot reach " + requestUrl);
        } catch (final Exception ex) {
            throw new CiApiException("Unknown erro ", ex);
        }
    }

    @Override
    public Insights analyze(final List<Build> builds) {
        if (builds.isEmpty()) {
            return new Insights(0, 0.0, List.of(), List.of());
        }
        final long failedBuilds = builds.stream()
                .filter(build -> build.status().equals(Status.FAILED))
                .count();
        final long completedBuilds = builds.stream()
                .filter(build -> FINISHED_STATUSES.contains(build.status()))
                .count();
        final List<Build> top3SlowestBuilds = builds.stream()
                .filter(build -> FINISHED_STATUSES.contains(build.status()))
                .sorted(Comparator.comparing(Build::durationMs).reversed())
                .limit(3)
                .toList();
        final List<ServiceStat> serviceStats = calculateServiceStats(builds);

        return new Insights(
                builds.size(),
                (completedBuilds) == 0 ? 0 : (double) failedBuilds / completedBuilds,
                top3SlowestBuilds,
                serviceStats
        );
    }

    private List<ServiceStat> calculateServiceStats(final List<Build> builds) {
        final Map<String, List<Build>> buildMap = builds.stream().collect(Collectors.groupingBy(Build::service));
        final List<ServiceStat> serviceStats = new ArrayList<>(buildMap.size());
        for (final Map.Entry<String, List<Build>> entry : buildMap.entrySet()) {
            final List<Build> serviceBuilds = entry.getValue();
            final long serviceCompletedBuilds = serviceBuilds.stream()
                    .filter(build -> FINISHED_STATUSES.contains(build.status()))
                    .count();
            final long serviceFailedBuilds = serviceBuilds.stream()
                    .filter(build -> build.status().equals(Status.FAILED))
                    .count();
            serviceStats.add(new ServiceStat(
                    entry.getKey(),
                    serviceBuilds.size(),
                    (int) serviceFailedBuilds,
                    (serviceCompletedBuilds == 0) ? 0 : (double) serviceFailedBuilds / serviceCompletedBuilds
            ));
        }
        return serviceStats.stream()
                .sorted(
                        Comparator.comparing(ServiceStat::failureRate).reversed().thenComparing(ServiceStat::service)
                ).toList();
    }

    @Override
    public Insights run() {
        return analyze(fetchBuilds());
    }

}
