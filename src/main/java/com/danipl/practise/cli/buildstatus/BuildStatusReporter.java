package com.danipl.practise.cli.buildstatus;

import java.time.Instant;
import java.util.List;

/**
 * Fetches build statuses from a (mock) CI API and renders them as a
 * developer-friendly console report.
 *
 * <p>This is the kind of small utility a DevEx team ships to answer the
 * question: <em>"What is the status of my build?"</em> without forcing
 * developers to open a browser. The sad path matters as much as the happy
 * path: when the API is unreachable, the developer should see a clear,
 * actionable message — never a raw stack trace or NPE.
 *
 * Requirements:
 *   - Rows are sorted newest-first by build start time
 *   - Statuses are rendered as short badges (e.g. PASS, FAIL, RUN)
 *   - Durations are rendered in human-readable form (e.g. "12m 34s")
 *   - Null build entries are skipped, never crash the report
 *   - An empty or null build list yields "No builds found."
 *   - Source failures yield a friendly "Error: ..." line
 */
public interface BuildStatusReporter {

    /**
     * Factory method to create a default implementation.
     */
    static BuildStatusReporter of() {
        return new BuildStatusReporterImpl();
    }

    /**
     * Fetches builds from the given source and renders a console report.
     *
     * @param source the build source (mock API); never null
     * @return the rendered report
     * @throws NullPointerException if source is null
     * @throws RuntimeException if the source fails with a non-source exception
     */
    String report(BuildStatusSource source);

    /**
     * Renders the given builds as a fixed-width aligned console table with a
     * header row, sorted newest-first by start time. Columns are:
     * BUILD, PROJECT, STATUS, DURATION, SHA, AUTHOR.
     *
     * @param builds the builds to render (may be null, empty, or contain nulls)
     * @return the rendered table, or "No builds found." if empty/null
     */
    String render(List<BuildStatus> builds);

    /**
     * Formats a duration in seconds as a human-readable string.
     * Examples: {@code 45s}, {@code 12m 34s}, {@code 1h 05m}.
     *
     * @param totalSeconds the duration in seconds
     * @return the formatted duration
     * @throws IllegalArgumentException if totalSeconds is negative
     */
    String formatDuration(long totalSeconds);

    /**
     * A single CI build result.
     */
    record BuildStatus(String buildId, String project, Status status,
                       long durationSeconds, Instant startedAt,
                       String commitSha, String author) {
    }

    /**
     * The lifecycle status of a build.
     */
    enum Status {
        QUEUED, RUNNING, PASSED, FAILED, SKIPPED
    }

    /**
     * Source of build statuses, simulating the CI API boundary.
     */
    @FunctionalInterface
    interface BuildStatusSource {

        /**
         * Fetches all builds.
         *
         * @return the builds (may be empty or null)
         * @throws BuildStatusException if the API cannot be reached
         */
        List<BuildStatus> fetch();
    }

    /**
     * Thrown when the build source cannot be reached or responds with an
     * error. Carries a developer-actionable message.
     */
    class BuildStatusException extends RuntimeException {
        public BuildStatusException(String message) {
            super(message);
        }

        public BuildStatusException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
