package com.danipl.practise.cli.buildstatus;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BuildStatusReporter tests")
class BuildStatusReporterTest {

    private BuildStatusReporter reporter;

    @BeforeEach
    void setUp() {
        reporter = BuildStatusReporter.of();
    }

    private static BuildStatusReporter.BuildStatus build(
            final String id, final String project, final BuildStatusReporter.Status status,
            final long duration, final String startedAt, final String sha, final String author) {
        return new BuildStatusReporter.BuildStatus(id, project, status, duration,
                Instant.parse(startedAt), sha, author);
    }

    @Nested
    @DisplayName("formatDuration")
    class FormatDuration {

        @Test
        @DisplayName("should render seconds only for durations under a minute")
        void secondsOnly() {
            assertEquals("0s", reporter.formatDuration(0));
            assertEquals("45s", reporter.formatDuration(45));
            assertEquals("59s", reporter.formatDuration(59));
        }

        @Test
        @DisplayName("should render minutes and zero-padded seconds for durations under an hour")
        void minutesAndSeconds() {
            assertEquals("1m 00s", reporter.formatDuration(60));
            assertEquals("1m 30s", reporter.formatDuration(90));
            assertEquals("12m 34s", reporter.formatDuration(754));
            assertEquals("59m 59s", reporter.formatDuration(3599));
        }

        @Test
        @DisplayName("should render hours and zero-padded minutes for durations of an hour or more")
        void hoursAndMinutes() {
            assertEquals("1h 00m", reporter.formatDuration(3600));
            assertEquals("1h 01m", reporter.formatDuration(3661));
            assertEquals("2h 02m", reporter.formatDuration(7325));
        }

        @Test
        @DisplayName("should reject negative durations")
        void negativeDuration() {
            assertThrows(IllegalArgumentException.class, () -> reporter.formatDuration(-1));
        }
    }

    @Nested
    @DisplayName("render")
    class Render {

        @Test
        @DisplayName("should render a single build as an aligned table with header")
        void singleBuild() {
            final List<BuildStatusReporter.BuildStatus> builds = List.of(
                    build("#b-1042", "webapp", BuildStatusReporter.Status.PASSED, 754,
                            "2026-08-01T10:00:00Z", "a1b2c3d", "alice"));

            final String expected = """
                    BUILD    PROJECT  STATUS  DURATION  SHA      AUTHOR
                    #b-1042  webapp   PASS    12m 34s   a1b2c3d  alice""";

            assertEquals(expected, reporter.render(builds));
        }

        @Test
        @DisplayName("should sort builds newest-first by start time")
        void sortedNewestFirst() {
            final List<BuildStatusReporter.BuildStatus> builds = List.of(
                    build("#b-3", "api", BuildStatusReporter.Status.FAILED, 185,
                            "2026-08-01T09:00:00Z", "d4e5f6a", "bob"),
                    build("#b-1", "webapp", BuildStatusReporter.Status.QUEUED, 0,
                            "2026-08-01T11:00:00Z", "c3c3c3", "alice"),
                    build("#b-2", "worker", BuildStatusReporter.Status.RUNNING, 3661,
                            "2026-08-01T10:00:00Z", "b2b2b2", "carol"));

            final String expected = """
                    BUILD  PROJECT  STATUS  DURATION  SHA      AUTHOR
                    #b-1   webapp   WAIT    0s        c3c3c3   alice
                    #b-2   worker   RUN     1h 01m    b2b2b2   carol
                    #b-3   api      FAIL    3m 05s    d4e5f6a  bob""";

            assertEquals(expected, reporter.render(builds));
        }

        @Test
        @DisplayName("should render the correct badge for every status")
        void allStatusBadges() {
            final List<BuildStatusReporter.BuildStatus> builds = List.of(
                    build("#b-1", "proj-a", BuildStatusReporter.Status.QUEUED, 0,
                            "2026-08-01T10:05:00Z", "sha-1", "alice"),
                    build("#b-2", "proj-b", BuildStatusReporter.Status.RUNNING, 30,
                            "2026-08-01T10:04:00Z", "sha-2", "bob"),
                    build("#b-3", "proj-c", BuildStatusReporter.Status.PASSED, 120,
                            "2026-08-01T10:03:00Z", "sha-3", "carol"),
                    build("#b-4", "proj-d", BuildStatusReporter.Status.FAILED, 3661,
                            "2026-08-01T10:02:00Z", "sha-4", "dave"),
                    build("#b-5", "proj-e", BuildStatusReporter.Status.SKIPPED, 45,
                            "2026-08-01T10:01:00Z", "sha-5", "erin"));

            final String expected = """
                    BUILD  PROJECT  STATUS  DURATION  SHA    AUTHOR
                    #b-1   proj-a   WAIT    0s        sha-1  alice
                    #b-2   proj-b   RUN     30s       sha-2  bob
                    #b-3   proj-c   PASS    2m 00s    sha-3  carol
                    #b-4   proj-d   FAIL    1h 01m    sha-4  dave
                    #b-5   proj-e   SKIP    45s       sha-5  erin""";
            final String rendered = reporter.render(builds);

            assertEquals(expected, rendered);
        }

        @Test
        @DisplayName("should return a friendly message for an empty list")
        void emptyList() {
            assertEquals("No builds found.", reporter.render(List.of()));
        }

        @Test
        @DisplayName("should return a friendly message for a null list")
        void nullList() {
            assertEquals("No builds found.", reporter.render(null));
        }

        @Test
        @DisplayName("should skip null entries without crashing")
        void nullEntriesSkipped() {
            final List<BuildStatusReporter.BuildStatus> builds = List.of(
                    build("#b-1", "webapp", BuildStatusReporter.Status.PASSED, 754,
                            "2026-08-01T10:00:00Z", "a1b2c3d", "alice"),
                    build("#b-2", "api", BuildStatusReporter.Status.FAILED, 185,
                            "2026-08-01T09:00:00Z", "d4e5f6a", "bob"));

            final String expected = """
                    BUILD  PROJECT  STATUS  DURATION  SHA      AUTHOR
                    #b-1   webapp   PASS    12m 34s   a1b2c3d  alice
                    #b-2   api      FAIL    3m 05s    d4e5f6a  bob""";

            assertEquals(expected, reporter.render(builds));
        }
    }

    @Nested
    @DisplayName("report")
    class Report {

        @Test
        @DisplayName("should fetch and render the report")
        void happyPath() {
            final List<BuildStatusReporter.BuildStatus> builds = List.of(
                    build("#b-1042", "webapp", BuildStatusReporter.Status.PASSED, 754,
                            "2026-08-01T10:00:00Z", "a1b2c3d", "alice"));

            final String expected = """
                    BUILD    PROJECT  STATUS  DURATION  SHA      AUTHOR
                    #b-1042  webapp   PASS    12m 34s   a1b2c3d  alice""";
            final String reported = reporter.report(() -> builds);

            assertEquals(expected, reported);
        }

        @Test
        @DisplayName("should report a friendly message when the source returns no builds")
        void emptySource() {
            assertEquals("No builds found.", reporter.report(List::of));
        }

        @Test
        @DisplayName("should report a friendly message when the source returns null")
        void nullSourceResult() {
            assertEquals("No builds found.", reporter.report(() -> null));
        }

        @Test
        @DisplayName("should translate a source failure into a clear error message")
        void sourceFailure() {
            final BuildStatusReporter.BuildStatusSource failing =
                    () -> {
                        throw new BuildStatusReporter.BuildStatusException("connection refused");
                    };

            assertEquals("Error: could not fetch build status: connection refused",
                    reporter.report(failing));
        }

        @Test
        @DisplayName("should propagate unexpected exceptions instead of hiding them")
        void unexpectedExceptionPropagates() {
            final BuildStatusReporter.BuildStatusSource buggy =
                    () -> {
                        throw new IllegalStateException("programming bug");
                    };

            assertThrows(IllegalStateException.class, () -> reporter.report(buggy));
        }

        @Test
        @DisplayName("should reject a null source")
        void nullSource() {
            assertThrows(NullPointerException.class, () -> reporter.report(null));
        }
    }
}
