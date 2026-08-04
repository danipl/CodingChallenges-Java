package com.danipl.practise.http.ciinsights;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CiInsights tests")
class CiInsightsTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
        baseUrl = "http://localhost:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    private void stubBuildsResponse(final int status, final String body) {
        server.createContext("/builds", exchange -> {
            final byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
    }

    private static CiInsights.Build build(final String id, final String service,
                                          final CiInsights.Status status,
                                          final long durationMs) {
        return new CiInsights.Build(id, service, status, durationMs,
                Instant.parse("2026-08-01T10:00:00Z"));
    }

    private static final String TWO_BUILDS_JSON = """
            [
              {"id":"b-1","service":"webapp","status":"PASSED","durationMs":12000,"finishedAt":"2026-08-01T10:00:00Z"},
              {"id":"b-2","service":"api","status":"FAILED","durationMs":30000,"finishedAt":"2026-08-01T10:01:00Z"}
            ]""";

    @Nested
    @DisplayName("fetchBuilds over real HTTP")
    class FetchBuilds {

        @Test
        @DisplayName("should parse a JSON build array from the API")
        void parsesBuilds() {
            stubBuildsResponse(200, TWO_BUILDS_JSON);
            final CiInsights client = CiInsights.of(baseUrl);

            final List<CiInsights.Build> builds = client.fetchBuilds();

            assertEquals(2, builds.size());
            assertEquals("b-1", builds.get(0).id());
            assertEquals("webapp", builds.get(0).service());
            assertEquals(CiInsights.Status.PASSED, builds.get(0).status());
            assertEquals(12000, builds.get(0).durationMs());
            assertEquals(Instant.parse("2026-08-01T10:00:00Z"), builds.get(0).finishedAt());
            assertEquals("b-2", builds.get(1).id());
        }

        @Test
        @DisplayName("should return an empty list for an empty JSON array")
        void emptyArray() {
            stubBuildsResponse(200, "[]");
            final CiInsights client = CiInsights.of(baseUrl);

            assertTrue(client.fetchBuilds().isEmpty());
        }

        @Test
        @DisplayName("should tolerate unknown JSON fields")
        void unknownFieldsTolerated() {
            stubBuildsResponse(200, """
                    [{"id":"b-1","service":"webapp","status":"PASSED","durationMs":1,
                      "finishedAt":"2026-08-01T10:00:00Z","newField":"ignored"}]
                    """);
            final CiInsights client = CiInsights.of(baseUrl);

            assertEquals(1, client.fetchBuilds().size());
        }

        @Test
        @DisplayName("should translate a 500 into a CiApiException with the status code")
        void serverError() {
            stubBuildsResponse(500, "boom");
            final CiInsights client = CiInsights.of(baseUrl);

            final CiInsights.CiApiException e =
                    assertThrows(CiInsights.CiApiException.class, client::fetchBuilds);

            assertTrue(e.getMessage().contains("500"), "message was: " + e.getMessage());
        }

        @Test
        @DisplayName("should translate malformed JSON into a CiApiException")
        void malformedJson() {
            stubBuildsResponse(200, "{not json");
            final CiInsights client = CiInsights.of(baseUrl);

            final CiInsights.CiApiException e =
                    assertThrows(CiInsights.CiApiException.class, client::fetchBuilds);

            assertTrue(e.getMessage().toLowerCase().contains("json"),
                    "message was: " + e.getMessage());
        }

        @Test
        @DisplayName("should translate an unreachable API into a CiApiException")
        void unreachableApi() {
            final CiInsights client = CiInsights.of("http://localhost:1");

            final CiInsights.CiApiException e =
                    assertThrows(CiInsights.CiApiException.class, client::fetchBuilds);

            assertTrue(e.getMessage().toLowerCase().contains("reach"),
                    "message was: " + e.getMessage());
        }
    }

    @Nested
    @DisplayName("analyze - pure computation")
    class Analyze {

        @Test
        @DisplayName("should compute failure rate over completed builds only")
        void failureRateOverCompleted() {
            final CiInsights client = CiInsights.of(baseUrl);
            final List<CiInsights.Build> builds = List.of(
                    build("b1", "webapp", CiInsights.Status.PASSED, 1000),
                    build("b2", "webapp", CiInsights.Status.FAILED, 2000),
                    build("b3", "api", CiInsights.Status.RUNNING, 3000),
                    build("b4", "api", CiInsights.Status.QUEUED, 0),
                    build("b5", "api", CiInsights.Status.SKIPPED, 0),
                    build("b6", "worker", CiInsights.Status.FAILED, 4000),
                    build("b7", "worker", CiInsights.Status.PASSED, 5000));

            final CiInsights.Insights insights = client.analyze(builds);

            assertEquals(7, insights.totalBuilds());
            // completed = b1,b2,b6,b7 (4), failed = b2,b6 (2) -> 0.5
            assertEquals(0.5, insights.failureRate(), 0.0001);
        }

        @Test
        @DisplayName("should return zero failure rate when no builds completed")
        void zeroFailureRate() {
            final CiInsights client = CiInsights.of(baseUrl);
            final List<CiInsights.Build> builds = List.of(
                    build("b1", "webapp", CiInsights.Status.RUNNING, 100),
                    build("b2", "webapp", CiInsights.Status.QUEUED, 0));

            final CiInsights.Insights insights = client.analyze(builds);

            assertEquals(0.0, insights.failureRate(), 0.0001);
        }

        @Test
        @DisplayName("should return the top 3 slowest completed builds, longest first")
        void slowestBuilds() {
            final CiInsights client = CiInsights.of(baseUrl);
            final List<CiInsights.Build> builds = List.of(
                    build("b1", "webapp", CiInsights.Status.PASSED, 1000),
                    build("b2", "api", CiInsights.Status.FAILED, 9000),
                    build("b3", "worker", CiInsights.Status.PASSED, 5000),
                    build("b4", "api", CiInsights.Status.PASSED, 4000),
                    build("b5", "webapp", CiInsights.Status.FAILED, 2000),
                    build("b6", "worker", CiInsights.Status.RUNNING, 30000));

            final CiInsights.Insights insights = client.analyze(builds);

            assertEquals(List.of("b2", "b3", "b4"),
                    insights.slowestBuilds().stream().map(CiInsights.Build::id).toList());
        }

        @Test
        @DisplayName("should return fewer than 3 slowest builds when few completed")
        void fewerThanThreeSlowest() {
            final CiInsights client = CiInsights.of(baseUrl);
            final List<CiInsights.Build> builds = List.of(
                    build("b1", "webapp", CiInsights.Status.PASSED, 1000));

            final CiInsights.Insights insights = client.analyze(builds);

            assertEquals(List.of("b1"),
                    insights.slowestBuilds().stream().map(CiInsights.Build::id).toList());
        }

        @Test
        @DisplayName("should rank services by failure rate, ties by name")
        void serviceRanking() {
            final CiInsights client = CiInsights.of(baseUrl);
            final List<CiInsights.Build> builds = List.of(
                    build("b1", "webapp", CiInsights.Status.PASSED, 1000),
                    build("b2", "webapp", CiInsights.Status.FAILED, 2000),
                    build("b3", "webapp", CiInsights.Status.RUNNING, 3000),
                    build("b4", "api", CiInsights.Status.FAILED, 4000),
                    build("b5", "api", CiInsights.Status.FAILED, 5000),
                    build("b6", "worker", CiInsights.Status.PASSED, 6000));

            final CiInsights.Insights insights = client.analyze(builds);

            // api 2/2=1.0, webapp 1/2=0.5, worker 0/1=0.0
            assertEquals(List.of("api", "webapp", "worker"),
                    insights.services().stream().map(CiInsights.ServiceStat::service).toList());
            assertEquals(1.0, insights.services().get(0).failureRate(), 0.0001);
            assertEquals(0.5, insights.services().get(1).failureRate(), 0.0001);
            assertEquals(0.0, insights.services().get(2).failureRate(), 0.0001);
        }

        @Test
        @DisplayName("should return zeroed insights for an empty build list")
        void emptyBuilds() {
            final CiInsights client = CiInsights.of(baseUrl);

            final CiInsights.Insights insights = client.analyze(List.of());

            assertEquals(0, insights.totalBuilds());
            assertEquals(0.0, insights.failureRate(), 0.0001);
            assertTrue(insights.slowestBuilds().isEmpty());
            assertTrue(insights.services().isEmpty());
        }

        @Test
        @DisplayName("should reject a null build list")
        void nullBuilds() {
            final CiInsights client = CiInsights.of(baseUrl);

            assertThrows(NullPointerException.class, () -> client.analyze(null));
        }
    }

    @Nested
    @DisplayName("run - end to end")
    class Run {

        @Test
        @DisplayName("should fetch and analyze in one call")
        void endToEnd() {
            stubBuildsResponse(200, TWO_BUILDS_JSON);
            final CiInsights client = CiInsights.of(baseUrl);

            final CiInsights.Insights insights = client.run();

            assertEquals(2, insights.totalBuilds());
            assertEquals(0.5, insights.failureRate(), 0.0001);
            assertEquals(List.of("b-2", "b-1"),
                    insights.slowestBuilds().stream().map(CiInsights.Build::id).toList());
            assertEquals(List.of("api", "webapp"),
                    insights.services().stream().map(CiInsights.ServiceStat::service).toList());
        }

        @Test
        @DisplayName("should surface fetch failures")
        void fetchFailurePropagates() {
            stubBuildsResponse(500, "boom");
            final CiInsights client = CiInsights.of(baseUrl);

            assertThrows(CiInsights.CiApiException.class, client::run);
        }
    }
}
