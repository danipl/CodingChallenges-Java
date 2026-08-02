package com.danipl.practise.refactoring.legacydeploytool;

import java.nio.file.Path;
import java.util.List;

/**
 * Internal deployment tool: reads a {@code deploy.conf} file and resolves the
 * deployment plan from it.
 *
 * <p>Config file format (one {@code key=value} per line):
 * <pre>
 * # deployment config
 * target=webapp,api      # required, comma-separated, at least one entry
 * region=eu-west-1       # optional, default "eu-west-1"
 * parallel=true          # optional, default false (true/false, case-insensitive)
 * timeout=120            # optional, default 120, must be a positive integer
 * </pre>
 *
 * Requirements:
 *   - Lines starting with {@code #} are comments; blank lines are ignored
 *   - Keys and values are trimmed of surrounding whitespace
 *   - Unknown keys are ignored
 *   - Every failure surfaces as a {@link DeployException} with a
 *     developer-actionable message — never a raw {@code NPE} or
 *     {@code NumberFormatException}
 */
public interface DeployTool {

    /**
     * Factory method to create a default implementation.
     */
    static DeployTool of() {
        return new DeployToolImpl();
    }

    /**
     * Reads the given config file and resolves the deployment plan.
     *
     * @param configFile path to the config file; never null
     * @return the resolved deployment plan
     * @throws DeployException with a clear message if the file is missing,
     *         the {@code target} key is absent/empty, a line is malformed,
     *         or a value is invalid
     * @throws NullPointerException if configFile is null
     */
    DeployResult deploy(Path configFile);

    /**
     * The resolved deployment plan.
     *
     * @param targets the deploy targets, in config order, trimmed and
     *                non-empty
     * @param region the deployment region (default "eu-west-1")
     * @param parallel whether targets deploy in parallel (default false)
     * @param timeoutSeconds per-target timeout (default 120)
     */
    record DeployResult(List<String> targets, String region,
                        boolean parallel, int timeoutSeconds) {

        public DeployResult {
            targets = List.copyOf(targets);
        }
    }

    /**
     * Thrown when the config cannot be turned into a valid deployment plan.
     * The message is what a developer sees in their terminal.
     */
    class DeployException extends RuntimeException {
        public DeployException(String message) {
            super(message);
        }

        public DeployException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
