package com.danipl.practise.http.ciinsights;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.List;

/**
 * Fetches build data from a CI API over HTTP and computes developer-facing
 * insights: failure rate, slowest builds, flakiest services.
 *
 * <p>This is the classic DevEx "why is CI slow?" tool: hit the API, parse
 * the JSON, turn raw rows into answers. The sad path is the product — a
 * dead API, a 500, or malformed JSON must become a message a developer can
 * act on, never a raw exception leak.
 *
 * <p>JSON parsing uses Jackson (the one deliberate exception to the repo's
 * no-third-party rule). HTTP uses {@code java.net.http.HttpClient}.
 *
 * Requirements:
 *   - {@code fetchBuilds} returns the raw builds from {@code GET {baseUrl}/builds}
 *   - {@code analyze} is pure computation over already-fetched builds
 *   - Failure rate counts only completed builds (PASSED or FAILED)
 *   - Slowest builds: top 3 completed builds by duration, longest first
 *   - Services ranked by failure rate, descending (ties by name)
 *   - Every failure surfaces as {@link CiApiException} with an actionable
 *     message — never a raw {@code IOException} or Jackson exception
 */
public interface CiInsights {

    /**
     * Factory method pointed at a CI API root URL.
     *
     * @param baseUrl the API root, e.g. {@code http://localhost:8080};
     *                never null
     * @return a CiInsights client for that API
     */
    static CiInsights of(final String baseUrl) {
        return new CiInsightsImpl(baseUrl);
    }

    /**
     * Fetches all builds from {@code GET {baseUrl}/builds} and parses them.
     *
     * @return the builds, in API order (may be empty)
     * @throws CiApiException if the API is unreachable, returns a non-2xx
     *         status, times out, or returns malformed JSON
     */
    List<Build> fetchBuilds();

    /**
     * Computes insights over the given builds. Pure computation — no I/O.
     *
     * @param builds the builds to analyze; never null
     * @return the computed insights
     */
    Insights analyze(List<Build> builds);

    /**
     * Fetches builds and computes insights — what a CLI would call.
     *
     * @return the computed insights
     * @throws CiApiException on any fetch failure
     */
    Insights run();

    /**
     * A single CI build result.
     *
     * @param id unique build id
     * @param service the service that was built
     * @param status lifecycle status
     * @param durationMs wall-clock duration of the build
     * @param finishedAt when the build finished
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Build(String id, String service, Status status, long durationMs, Instant finishedAt) {
    }

    /**
     * Lifecycle status of a build.
     */
    enum Status {
        QUEUED, RUNNING, PASSED, FAILED, SKIPPED
    }

    /**
     * The insights a developer actually wants.
     *
     * @param totalBuilds all builds fetched
     * @param failureRate failed / completed builds (0.0 if none completed)
     * @param slowestBuilds top 3 completed builds by duration, longest first
     * @param services per-service stats, ranked by failure rate desc
     */
    record Insights(int totalBuilds, double failureRate,
                    List<Build> slowestBuilds, List<ServiceStat> services) {
    }

    /**
     * Failure statistics for a single service.
     *
     * @param service service name
     * @param total total builds for the service
     * @param failures failed builds for the service
     * @param failureRate failures / completed builds for the service
     */
    record ServiceStat(String service, int total, int failures, double failureRate) {
    }

    /**
     * Thrown when the CI API cannot be turned into insights. The message is
     * what a developer sees in their terminal.
     */
    class CiApiException extends RuntimeException {
        public CiApiException(String message) {
            super(message);
        }

        public CiApiException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
