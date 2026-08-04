package com.danipl.practise.cli.gitwrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Implementation of {@link GitWrapper}.
 */
public final class GitWrapperImpl implements GitWrapper {

    private static final long DEFAULT_TIMEOUT_MILLIS = 10_000;

    private final CommandRunner runner;

    public GitWrapperImpl(final CommandRunner runner) {
        this.runner = Objects.requireNonNull(runner, "runner");
    }

    @Override
    public GitState status(final Path repoDir) {
        Objects.requireNonNull(repoDir);
        final CommandResult commandResult = this.runner.run(
                List.of("git", "status", "--porcelain", "--branch"), repoDir, DEFAULT_TIMEOUT_MILLIS
        );
        if (commandResult.exitCode() != 0) {
            throw new CommandFailedException(
                    commandResult.stderr() + ", error (" + commandResult.exitCode() + ")",
                    commandResult.exitCode(), commandResult.stdout(), commandResult.stderr()
            );
        }
        final List<String> lines = Arrays.stream(commandResult.stdout().split("\n")).toList();
        final Iterator<String> linesIterator = lines.iterator();

        final String firstLine = parseFirstLine(linesIterator);

        final String branch = identifyBranch(firstLine);
        final int ahead = calculateCommitReference(firstLine, "ahead");
        final int behind = calculateCommitReference(firstLine, "behind");
        final List<String> files = collectChangePaths(linesIterator);

        return new GitState(branch, ahead, behind, files);
    }

    private String parseFirstLine(final Iterator<String> linesIterator) {
        if (!linesIterator.hasNext()) {
            throw new CommandFailedException("The git state is not retrieved successfully");
        }
        final String firstLine = linesIterator.next();
        if (firstLine.isEmpty()) {
            throw new CommandFailedException("Unexpected empty output");
        }
        if (!firstLine.startsWith("##")) {
            throw new CommandFailedException("The branch is not resolved");
        }
        return firstLine;
    }

    private String identifyBranch(final String firstLine) {
        return firstLine.substring(3).split("\\.\\.\\.")[0];
    }

    private int calculateCommitReference(final String firstLine, final String type) {
        final int behindIdx = firstLine.indexOf(type);
        if (behindIdx != -1) {
            final String behindStr = firstLine.substring(behindIdx);
            return Integer.parseInt(behindStr.substring(type.length() + 1, calculateLimit(behindStr)));
        }
        return 0;
    }

    private List<String> collectChangePaths(final Iterator<String> linesIterator) {
        final List<String> files = new ArrayList<>();
        while (linesIterator.hasNext()) {
            final String currentLine = linesIterator.next().trim();
            files.add(currentLine.substring(currentLine.indexOf(" ")).trim());
        }
        return files;
    }

    @Override
    public String shortSha(final Path repoDir) {
        Objects.requireNonNull(repoDir);
        final CommandResult commandResult = this.runner.run(List.of("git", "rev-parse", "--short", "HEAD"), repoDir, DEFAULT_TIMEOUT_MILLIS);
        if (!commandResult.stderr().isEmpty() || commandResult.stdout().length() < 7) {
            throw new CommandFailedException("The commit sha was not retrieved successfully");
        }
        return commandResult.stdout().substring(0, 7);
    }

    private int calculateLimit(final String substring) {
        if (!substring.contains(",")) {
            return substring.indexOf("]");
        } else if (!substring.contains("]")) {
            return substring.indexOf(",");
        }
        return Math.min(substring.indexOf(","), substring.indexOf("]"));
    }

    /**
     * The default {@link CommandRunner} backed by a real
     * {@link ProcessBuilder}.
     */
    static final class ProcessBuilderRunner implements CommandRunner {

        @Override
        public CommandResult run(final List<String> command, final Path workingDir, final long timeoutMillis) {
            checkGitRepository(workingDir);
            final ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.directory(workingDir.toFile());

            try {
                final Process process = processBuilder.start();
                final CompletableFuture<String> strOutputStream = CompletableFuture.supplyAsync(() -> {
                    try (final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        return bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
                    } catch (final Exception ex) {
                        System.out.println("Error collecting the standard output: " + ex.getMessage());
                    }
                    return null;
                });
                final CompletableFuture<String> errOutputStream = CompletableFuture.supplyAsync(() -> {
                    try (final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                        return bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
                    } catch (final Exception ex) {
                        System.out.println("Error collecting the error output: " + ex.getMessage());
                    }
                    return null;
                });
                final boolean finished = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    strOutputStream.cancel(true);
                    errOutputStream.cancel(true);
                    throw new CommandFailedException("The process is taking so long so it was aborted");
                }
                final String stdOutput = strOutputStream.join();
                final String errOutput = errOutputStream.join();
                return new CommandResult(process.exitValue(), stdOutput, errOutput);
            } catch (final InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new CommandFailedException("The program exited while running the command", ie);
            } catch (final IOException ioe) {
                throw new CommandFailedException("The executable is missing", ioe);
            } catch (final Exception ex) {
                throw new CommandFailedException("Unexpected error while running the command", ex);
            }
        }

        private void checkGitRepository(final Path workingDir) {
            if (!workingDir.toFile().exists()) {
                throw new CommandFailedException("The directory does not exists");
            }
            if (!workingDir.resolve(".git").toFile().exists()) {
                throw new CommandFailedException("The directory is not a git repository");
            }
        }
    }

}
