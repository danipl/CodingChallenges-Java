package com.danipl.practise.refactoring.legacydeploytool;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link DeployTool}.
 *
 * <p>Legacy code — written by a team that has moved on. It "works" for the
 * happy path and fails in confusing ways everywhere else. Your job is to
 * make it robust and readable so the next developer can trust it.
 *
 * TODO: Fix the failure modes, then refactor for clarity:
 *   - Missing file must produce a clear DeployException, not silent garbage
 *   - Values must be trimmed; "target = webapp" and "webapp, api" must parse
 *   - "parallel=TRUE" must be accepted
 *   - timeout must be validated (positive integer)
 *   - target must be present and contain at least one non-empty entry
 *   - malformed lines must fail loudly with the line number
 */
public final class DeployToolImpl implements DeployTool {

    @Override
    public DeployResult deploy(final Path configFile) {
        List<String> lines = new ArrayList<>();
        try {
            lines = Files.readAllLines(configFile);
        } catch (IOException e) {
            // config file is optional, just skip it
        }

        String targetLine = "";
        String region = "eu-west-1";
        boolean parallel = false;
        int timeout = 120;

        for (String line : lines) {
            if (line.trim().isEmpty()) {
                continue;
            }
            if (line.trim().startsWith("#")) {
                continue;
            }
            String[] parts = line.split("=", 2);
            String key = parts[0];
            String value = parts.length > 1 ? parts[1] : "";
            if (key.equals("target")) {
                targetLine = value;
            } else if (key.equals("region")) {
                region = value;
            } else if (key.equals("parallel")) {
                parallel = value.equals("true");
            } else if (key.equals("timeout")) {
                timeout = Integer.parseInt(value);
            }
        }

        List<String> targets = new ArrayList<>();
        String[] rawTargets = targetLine.split(",");
        for (String t : rawTargets) {
            targets.add(t);
        }

        if (targets.isEmpty()) {
            throw new DeployException("missing required key: target");
        }

        return new DeployResult(targets, region, parallel, timeout);
    }

}
