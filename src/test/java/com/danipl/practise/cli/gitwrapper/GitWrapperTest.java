package com.danipl.practise.cli.gitwrapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@DisplayName("GitWrapper tests")
class GitWrapperTest {

    @TempDir
    Path tempDir;

    private AtomicReference<List<String>> capturedCommand;
    private AtomicReference<Path> capturedWorkingDir;

    private GitWrapper wrapperWith(final int exitCode, final String stdout, final String stderr) {
        capturedCommand = new AtomicReference<>();
        capturedWorkingDir = new AtomicReference<>();
        return GitWrapper.of((command, workingDir, timeoutMillis) -> {
            capturedCommand.set(List.copyOf(command));
            capturedWorkingDir.set(workingDir);
            return new GitWrapper.CommandResult(exitCode, stdout, stderr);
        });
    }

    @Nested
    @DisplayName("Status parsing")
    class StatusParsing {

        @Test
        @DisplayName("should parse a clean branch without upstream")
        void cleanBranch() {
            final GitWrapper wrapper = wrapperWith(0, "## main\n", "");

            final GitWrapper.GitState state = wrapper.status(tempDir);

            assertEquals("main", state.branch());
            assertEquals(0, state.ahead());
            assertEquals(0, state.behind());
            assertTrue(state.changes().isEmpty());
        }

        @Test
        @DisplayName("should parse ahead count")
        void aheadCount() {
            final GitWrapper wrapper = wrapperWith(0, "## main...origin/main [ahead 2]\n", "");

            final GitWrapper.GitState state = wrapper.status(tempDir);

            assertEquals("main", state.branch());
            assertEquals(2, state.ahead());
            assertEquals(0, state.behind());
        }

        @Test
        @DisplayName("should parse behind count")
        void behindCount() {
            final GitWrapper wrapper = wrapperWith(0, "## main...origin/main [behind 3]\n", "");

            final GitWrapper.GitState state = wrapper.status(tempDir);

            assertEquals("main", state.branch());
            assertEquals(0, state.ahead());
            assertEquals(3, state.behind());
        }

        @Test
        @DisplayName("should parse both ahead and behind")
        void aheadAndBehind() {
            final GitWrapper wrapper = wrapperWith(0, "## main...origin/main [ahead 1, behind 4]\n", "");

            final GitWrapper.GitState state = wrapper.status(tempDir);

            assertEquals(1, state.ahead());
            assertEquals(4, state.behind());
        }

        @Test
        @DisplayName("should extract changed and untracked paths")
        void dirtyWorkingTree() {
            final GitWrapper wrapper = wrapperWith(0, "## main\n M modified.txt\n?? new.txt\n", "");

            final GitWrapper.GitState state = wrapper.status(tempDir);

            assertEquals(List.of("modified.txt", "new.txt"), state.changes());
        }

        @Test
        @DisplayName("should handle staged and renamed paths")
        void stagedAndRenamed() {
            final GitWrapper wrapper = wrapperWith(0, "## main\nM  staged.txt\nR  old.txt -> new.txt\n", "");

            final GitWrapper.GitState state = wrapper.status(tempDir);

            assertEquals(List.of("staged.txt", "old.txt -> new.txt"), state.changes());
        }

        @Test
        @DisplayName("should fail loudly on unexpected empty output")
        void emptyOutput() {
            final GitWrapper wrapper = wrapperWith(0, "", "");

            final GitWrapper.CommandFailedException e =
                    assertThrows(GitWrapper.CommandFailedException.class, () -> wrapper.status(tempDir));

            assertTrue(e.getMessage().toLowerCase().contains("unexpected"), "message was: " + e.getMessage());
        }

        @Test
        @DisplayName("should fail loudly when the first line is not a branch line")
        void missingBranchLine() {
            final GitWrapper wrapper = wrapperWith(0, " M orphan.txt\n", "");

            assertThrows(GitWrapper.CommandFailedException.class, () -> wrapper.status(tempDir));
        }
    }

    @Nested
    @DisplayName("Command construction")
    class CommandConstruction {

        @Test
        @DisplayName("status should run git status --porcelain --branch in the repo dir")
        void statusCommand() {
            final GitWrapper wrapper = wrapperWith(0, "## main\n", "");
            final Path repo = tempDir.resolve("repo");

            wrapper.status(repo);

            assertEquals(List.of("git", "status", "--porcelain", "--branch"), capturedCommand.get());
            assertEquals(repo, capturedWorkingDir.get());
        }

        @Test
        @DisplayName("shortSha should run git rev-parse --short HEAD")
        void shortShaCommand() {
            final GitWrapper wrapper = wrapperWith(0, "a1b2c3d\n", "");

            wrapper.shortSha(tempDir);

            assertEquals(List.of("git", "rev-parse", "--short", "HEAD"), capturedCommand.get());
            assertEquals(tempDir, capturedWorkingDir.get());
        }
    }

    @Nested
    @DisplayName("Error translation")
    class ErrorTranslation {

        @Test
        @DisplayName("should translate a non-zero exit into a CommandFailedException with stderr")
        void nonZeroExit() {
            final GitWrapper wrapper = wrapperWith(128, "", "fatal: not a git repository");

            final GitWrapper.CommandFailedException e =
                    assertThrows(GitWrapper.CommandFailedException.class, () -> wrapper.status(tempDir));

            assertEquals(128, e.exitCode());
            assertTrue(e.getMessage().contains("not a git repository"),
                    "message was: " + e.getMessage());
        }

        @Test
        @DisplayName("should report the exit code in the message")
        void exitCodeInMessage() {
            final GitWrapper wrapper = wrapperWith(128, "", "fatal: not a git repository");

            final GitWrapper.CommandFailedException e =
                    assertThrows(GitWrapper.CommandFailedException.class, () -> wrapper.status(tempDir));

            assertTrue(e.getMessage().contains("128"), "message was: " + e.getMessage());
        }

        @Test
        @DisplayName("should translate shortSha failures too")
        void shortShaNonZeroExit() {
            final GitWrapper wrapper = wrapperWith(1, "", "fatal: not a git repository");

            assertThrows(GitWrapper.CommandFailedException.class, () -> wrapper.shortSha(tempDir));
        }

        @Test
        @DisplayName("should propagate a missing-executable failure with a clear message")
        void missingExecutable() {
            final GitWrapper wrapper = GitWrapper.of((command, workingDir, timeoutMillis) -> {
                throw new GitWrapper.CommandFailedException(
                        "could not run " + command.getFirst() + ": not found", new IOException("No such file or directory"));
            });

            final GitWrapper.CommandFailedException e =
                    assertThrows(GitWrapper.CommandFailedException.class, () -> wrapper.status(tempDir));

            assertTrue(e.getMessage().contains("not found"),
                    "message was: " + e.getMessage());
        }

        @Test
        @DisplayName("should reject a null repo directory")
        void nullRepoDir() {
            final GitWrapper wrapper = wrapperWith(0, "## main\n", "");

            assertThrows(NullPointerException.class, () -> wrapper.status(null));
            assertThrows(NullPointerException.class, () -> wrapper.shortSha(null));
        }
    }

    @Nested
    @DisplayName("shortSha")
    class ShortSha {

        @Test
        @DisplayName("should return the trimmed short SHA")
        void trimsOutput() {
            final GitWrapper wrapper = wrapperWith(0, "a1b2c3d\n", "");

            assertEquals("a1b2c3d", wrapper.shortSha(tempDir));
        }

        @Test
        @DisplayName("should fail on empty output")
        void emptyOutput() {
            final GitWrapper wrapper = wrapperWith(0, "", "");

            assertThrows(GitWrapper.CommandFailedException.class, () -> wrapper.shortSha(tempDir));
        }
    }

    @Nested
    @DisplayName("Integration with real git")
    class Integration {

        private boolean gitAvailable() {
            try {
                final Process p = new ProcessBuilder("git", "--version").start();
                return p.waitFor() == 0;
            } catch (IOException | InterruptedException e) {
                return false;
            }
        }

        private void runGit(final Path dir, final String... args) throws IOException, InterruptedException {
            final List<String> cmd = new java.util.ArrayList<>(List.of("git"));
            cmd.addAll(List.of(args));
            final Process p = new ProcessBuilder(cmd).directory(dir.toFile()).inheritIO().start();
            assertEquals(0, p.waitFor(), "git " + String.join(" ", args) + " failed");
        }

        @Test
        @DisplayName("should report the state of a real repository")
        void realRepository() throws IOException, InterruptedException {
            assumeTrue(gitAvailable(), "git is not installed on this machine");
            runGit(tempDir, "init", "-q");
            runGit(tempDir, "config", "user.email", "test@example.com");
            runGit(tempDir, "config", "user.name", "Test");
            Files.writeString(tempDir.resolve("README.md"), "# test\n");
            runGit(tempDir, "add", ".");
            runGit(tempDir, "commit", "-q", "-m", "init");

            final GitWrapper wrapper = GitWrapper.of();

            final GitWrapper.GitState state = wrapper.status(tempDir);
            final String sha = wrapper.shortSha(tempDir);

            assertFalse(state.branch().isBlank(), "branch should be non-blank");
            assertTrue(state.changes().isEmpty(), "clean repo should have no changes");
            assertTrue(sha.matches("[0-9a-f]{7,}"), "SHA should be hex, was: " + sha);
        }

        @Test
        @DisplayName("should detect dirty state in a real repository")
        void dirtyRealRepository() throws IOException, InterruptedException {
            assumeTrue(gitAvailable(), "git is not installed on this machine");
            runGit(tempDir, "init", "-q");
            runGit(tempDir, "config", "user.email", "test@example.com");
            runGit(tempDir, "config", "user.name", "Test");
            Files.writeString(tempDir.resolve("README.md"), "# test\n");
            runGit(tempDir, "add", ".");
            runGit(tempDir, "commit", "-q", "-m", "init");
            Files.writeString(tempDir.resolve("README.md"), "# changed\n");

            final GitWrapper wrapper = GitWrapper.of();
            final GitWrapper.GitState state = wrapper.status(tempDir);

            assertTrue(state.changes().contains("README.md"), "changes was: " + state.changes());
        }

        @Test
        @DisplayName("should fail clearly outside a git repository")
        void outsideRepository() throws IOException {
            assumeTrue(gitAvailable(), "git is not installed on this machine");
            final GitWrapper wrapper = GitWrapper.of();
            final Path nonRepo = tempDir.resolve("not-a-repo");
            Files.createDirectories(nonRepo);

            final GitWrapper.CommandFailedException e =
                    assertThrows(GitWrapper.CommandFailedException.class,
                            () -> wrapper.status(nonRepo));

            assertTrue(e.getMessage().contains("not a git repository"), "message was: " + e.getMessage());
        }
    }
}
