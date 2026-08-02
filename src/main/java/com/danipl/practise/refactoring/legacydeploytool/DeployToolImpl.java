package com.danipl.practise.refactoring.legacydeploytool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Implementation of {@link DeployTool}.
 *
 * <p>Legacy code — written by a team that has moved on. It "works" for the
 * happy path and fails in confusing ways everywhere else. Your job is to
 * make it robust and readable so the next developer can trust it.
 * <p>
 */
public final class DeployToolImpl implements DeployTool {

    private final static String DEFAULT_TARGET = null;
    private final static String DEFAULT_REGION = "eu-west-1";
    private final static boolean DEFAULT_PARALLEL = false;
    private final static int DEFAULT_TIMEOUT = 120;

    private final static Set<String> VALID_PARALLEL = Set.of("true", "false");

    @Override
    public DeployResult deploy(final Path configFile) {
        final List<String> lines = extractLines(configFile);
        final DeployParams deployParams = extractParams(lines);
        if (!deployParams.isTargetConfigured()) {
            throw new DeployException("missing required key: target");
        }
        return new DeployResult(deployParams.getTargets(), deployParams.region, deployParams.parallel, deployParams.timeout);
    }

    private List<String> extractLines(final Path configFile) {
        try {
            return Files.readAllLines(configFile);
        } catch (final IOException e) {
            throw new DeployException("config file not found: " + configFile, e);
        }
    }

    private DeployParams extractParams(final List<String> lines) {
        final DeployParams deployParams = new DeployParams();
        int currentLine = 0;
        for (final String line : lines) {
            currentLine++;
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }
            final String[] parts = line.split("=", 2);
            if (parts.length != 2) {
                throw new DeployException("invalid line " + currentLine + ": " + line);
            }
            final String key = parts[0].trim();
            final String value = parts.length > 1 ? parts[1].trim() : "";
            if (key.equals("target")) {
                deployParams.target = value;
            } else if (key.equals("region")) {
                deployParams.region = value;
            } else if (key.equals("parallel")) {
                deployParams.parallel = manageParallel(value);
            } else if (key.equals("timeout")) {
                deployParams.timeout = manageTimeout(value);
            }
        }
        return deployParams;
    }

    private static boolean manageParallel(final String value) {
        final String valueLower = value.toLowerCase(Locale.ROOT);
        if (!VALID_PARALLEL.contains(valueLower)) {
            throw new DeployException("invalid boolean: expected true or false");
        }
        return valueLower.equals("true");
    }

    private static int manageTimeout(final String value) {
        try {
            final int timeout = Integer.parseInt(value);
            if (timeout <= 0) {
                throw new DeployException("invalid timeout: expected a positive value");
            }
            return timeout;
        } catch (final NumberFormatException nfe) {
            throw new DeployException("invalid timeout: " + value + " is not a numeric format");
        }
    }

    private static class DeployParams {

        String target = DEFAULT_TARGET;
        String region = DEFAULT_REGION;
        boolean parallel = DEFAULT_PARALLEL;
        int timeout = DEFAULT_TIMEOUT;

        private boolean isTargetConfigured() {
            return target != null && !target.isEmpty();
        }

        private List<String> getTargets() {
            return Arrays.stream(target.split(","))
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(candidate -> !candidate.isEmpty())
                    .toList();
        }

    }

}
