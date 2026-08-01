package com.danipl.practise.cli.buildstatus;


import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Implementation of {@link BuildStatusReporter}.
 */
public final class BuildStatusReporterImpl implements BuildStatusReporter {

    private final static int BUILD = 0;
    private final static int PROJECT = 1;
    private final static int STATUS = 2;
    private final static int DURATION = 3;
    private final static int SHA = 4;

    private final static Map<Integer, Integer> IDX_SIZE = Map.of(
            BUILD, "BUILD".length(),
            PROJECT, "PROJECT".length(),
            STATUS, "STATUS".length(),
            DURATION, "DURATION".length(),
            SHA, "SHA".length()
    );

    private final static Map<Status, String> STATUS_EQ = Map.of(
            Status.QUEUED, "WAIT",
            Status.FAILED, "FAIL",
            Status.PASSED, "PASS",
            Status.RUNNING, "RUN",
            Status.SKIPPED, "SKIP"
    );

    @Override
    public String report(final BuildStatusSource source) {
        Objects.requireNonNull(source, "Build status is null");
        final List<BuildStatus> builds;
        try {
            builds = source.fetch();
        } catch (final BuildStatusException e) {
            return "Error: could not fetch build status: " + e.getMessage();
        }
        return render(builds);
    }

    @Override
    public String render(final List<BuildStatus> builds) {
        if (builds == null || builds.isEmpty()) {
            return "No builds found.";
        }
        final Map<Integer, Integer> idxSize = new HashMap<>(IDX_SIZE);
        final List<BuildStatus> sorted = builds.stream()
                .filter(Objects::nonNull)
                .sorted((curr, can) -> can.startedAt().compareTo(curr.startedAt()))
                .toList();
        sorted.forEach(buildStatus -> {
            idxSize.put(BUILD, Math.max(idxSize.get(BUILD), buildStatus.buildId().length()));
            idxSize.put(PROJECT, Math.max(idxSize.get(PROJECT), buildStatus.project().length()));
            idxSize.put(STATUS, Math.max(idxSize.get(STATUS), STATUS_EQ.get(buildStatus.status()).length()));
            idxSize.put(DURATION, Math.max(idxSize.get(DURATION), formatDuration(buildStatus.durationSeconds()).length()));
            idxSize.put(SHA, Math.max(idxSize.get(SHA), Math.min(7, buildStatus.commitSha().length())));
        });
        final StringBuilder reportBuilder = new StringBuilder();
        reportBuilder
                .append(String.format("%-" + (idxSize.get(BUILD) + 2) + "s", "BUILD"))
                .append(String.format("%-" + (idxSize.get(PROJECT) + 2) + "s", "PROJECT"))
                .append(String.format("%-" + (idxSize.get(STATUS) + 2) + "s", "STATUS"))
                .append(String.format("%-" + (idxSize.get(DURATION) + 2) + "s", "DURATION"))
                .append(String.format("%-" + (idxSize.get(SHA) + 2) + "s", "SHA"))
                .append("AUTHOR")
                .append("\n");
        final Iterator<BuildStatus> buildStatusIterator = sorted.iterator();
        while (buildStatusIterator.hasNext()) {
            final BuildStatus buildStatus = buildStatusIterator.next();
            reportBuilder
                    .append(String.format("%-" + (idxSize.get(BUILD) + 2) + "s", buildStatus.buildId()))
                    .append(String.format("%-" + (idxSize.get(PROJECT) + 2) + "s", buildStatus.project()))
                    .append(String.format("%-" + (idxSize.get(STATUS) + 2) + "s", STATUS_EQ.get(buildStatus.status())))
                    .append(String.format("%-" + (idxSize.get(DURATION) + 2) + "s", formatDuration(buildStatus.durationSeconds())))
                    .append(String.format("%-" + (idxSize.get(SHA) + 2) + "s", buildStatus.commitSha().substring(0, Math.min(7, buildStatus.commitSha().length()))))
                    .append(buildStatus.author());
            if (buildStatusIterator.hasNext()) {
                reportBuilder.append("\n");
            }
        }
        return reportBuilder.toString();
    }

    @Override
    public String formatDuration(final long totalSeconds) {
        if (totalSeconds < 0) {
            throw new IllegalArgumentException("Duration must be non-negative: " + totalSeconds);
        }
        if (totalSeconds < 60) return totalSeconds + "s";
        if (totalSeconds < 3600) {
            return (totalSeconds / 60) + "m " + String.format("%02ds", totalSeconds % 60);
        }
        return (totalSeconds / 3600) + "h " + String.format("%02dm", (totalSeconds % 3600) / 60);
    }

}
