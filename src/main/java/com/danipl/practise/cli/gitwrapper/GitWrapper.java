package com.danipl.practise.cli.gitwrapper;

import java.nio.file.Path;
import java.util.List;

/**
 * Wraps git commands so developers can get the essential state of a
 * repository without reading raw git output.
 *
 * <p>This is a classic DevEx tool: a thin, opinionated wrapper around a
 * shell command. It must build the command correctly (a {@code List}, not
 * a hand-quoted string), run it via {@link ProcessBuilder}, capture output
 * without deadlocking, and — critically — turn every failure into a message
 * a developer can act on.
 *
 * Requirements:
 *   - Commands are built as {@code List<String>} and run in the target
 *     repository's working directory
 *   - Non-zero exit codes surface as {@link CommandFailedException} with
 *     the trimmed stderr in the message
 *   - A missing executable (e.g. git not installed) surfaces as a clear
 *     error, never a raw {@code IOException}
 *   - Timeouts are enforced and reported as such
 */
public interface GitWrapper {

    /**
     * Factory method using the real {@link ProcessBuilder}-based runner.
     */
    static GitWrapper of() {
        return new GitWrapperImpl(new GitWrapperImpl.ProcessBuilderRunner());
    }

    /**
     * Factory method with an injected runner (for tests or alternate
     * process backends).
     *
     * @param runner the command runner; never null
     * @return a GitWrapper backed by the given runner
     */
    static GitWrapper of(CommandRunner runner) {
        return new GitWrapperImpl(runner);
    }

    /**
     * Runs {@code git status --porcelain --branch} in the given repository
     * and parses the essential state.
     *
     * @param repoDir the repository working directory; never null
     * @return the parsed repository state
     * @throws CommandFailedException if the command fails, times out, the
     *         output is unexpected, or git cannot be executed
     * @throws NullPointerException if repoDir is null
     */
    GitState status(Path repoDir);

    /**
     * Runs {@code git rev-parse --short HEAD} and returns the short commit
     * SHA.
     *
     * @param repoDir the repository working directory; never null
     * @return the short SHA (trimmed)
     * @throws CommandFailedException if the command fails, times out, or
     *         git cannot be executed
     * @throws NullPointerException if repoDir is null
     */
    String shortSha(Path repoDir);

    /**
     * The essential state of a repository.
     *
     * @param branch the current branch
     * @param ahead commits ahead of the upstream
     * @param behind commits behind the upstream
     * @param changes changed/untracked paths, in git output order
     */
    record GitState(String branch, int ahead, int behind, List<String> changes) {

        public GitState {
            changes = List.copyOf(changes);
        }
    }

    /**
     * Executes a command and captures its output. The execution strategy is
     * pluggable so tests can simulate git without a real installation.
     */
    @FunctionalInterface
    interface CommandRunner {

        /**
         * Runs the command in the given directory.
         *
         * @param command the command and arguments; never null
         * @param workingDir the working directory; never null
         * @param timeoutMillis maximum execution time
         * @return the captured result
         * @throws CommandFailedException if the command cannot be started,
         *         or times out
         */
        CommandResult run(List<String> command, Path workingDir, long timeoutMillis);
    }

    /**
     * The captured outcome of a command.
     *
     * @param exitCode the process exit code
     * @param stdout the captured standard output
     * @param stderr the captured standard error
     */
    record CommandResult(int exitCode, String stdout, String stderr) {
    }

    /**
     * Thrown when a wrapped command fails, times out, or cannot be
     * executed. The message is what a developer sees in their terminal.
     */
    class CommandFailedException extends RuntimeException {

        private final int exitCode;
        private final String stdout;
        private final String stderr;

        public CommandFailedException(String message) {
            this(message, -1, "", "", null);
        }

        public CommandFailedException(String message, Throwable cause) {
            this(message, -1, "", "", cause);
        }

        public CommandFailedException(String message, int exitCode, String stdout, String stderr) {
            this(message, exitCode, stdout, stderr, null);
        }

        private CommandFailedException(String message, int exitCode, String stdout,
                                       String stderr, Throwable cause) {
            super(message, cause);
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
        }

        public int exitCode() {
            return exitCode;
        }

        public String stdout() {
            return stdout;
        }

        public String stderr() {
            return stderr;
        }
    }
}
