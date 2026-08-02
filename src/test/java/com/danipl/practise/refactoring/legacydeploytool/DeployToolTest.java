package com.danipl.practise.refactoring.legacydeploytool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DeployTool tests")
class DeployToolTest {

    @TempDir
    Path tempDir;

    private DeployTool tool;

    private Path writeConfig(final String content) throws IOException {
        final Path file = tempDir.resolve("deploy-" + System.nanoTime() + ".conf");
        Files.writeString(file, content);
        return file;
    }

    @Nested
    @DisplayName("Deploy happy path")
    class DeployHappyPath {

        @Test
        @DisplayName("should parse a full valid config")
        void fullConfig() throws IOException {
            tool = DeployTool.of();
            final Path config = writeConfig("""
                    target=webapp,api
                    region=eu-west-1
                    parallel=true
                    timeout=120
                    """);

            final DeployTool.DeployResult result = tool.deploy(config);

            assertEquals(java.util.List.of("webapp", "api"), result.targets());
            assertEquals("eu-west-1", result.region());
            assertTrue(result.parallel());
            assertEquals(120, result.timeoutSeconds());
        }

        @Test
        @DisplayName("should apply defaults when optional keys are missing")
        void defaultsApplied() throws IOException {
            tool = DeployTool.of();
            final Path config = writeConfig("target=webapp\n");

            final DeployTool.DeployResult result = tool.deploy(config);

            assertEquals(java.util.List.of("webapp"), result.targets());
            assertEquals("eu-west-1", result.region());
            assertFalse(result.parallel());
            assertEquals(120, result.timeoutSeconds());
        }

        @Test
        @DisplayName("should tolerate whitespace around keys, values, and target entries")
        void whitespaceTolerated() throws IOException {
            tool = DeployTool.of();
            final Path config = writeConfig("""
                    target = webapp, api
                      region  =   us-east-1
                    """);

            final DeployTool.DeployResult result = tool.deploy(config);

            assertEquals(java.util.List.of("webapp", "api"), result.targets());
            assertEquals("us-east-1", result.region());
        }

        @Test
        @DisplayName("should ignore comments and blank lines")
        void commentsIgnored() throws IOException {
            tool = DeployTool.of();
            final Path config = writeConfig("""
                    # production deployment
                    target=webapp

                    # region override
                    region=us-west-2
                    """);

            final DeployTool.DeployResult result = tool.deploy(config);

            assertEquals(java.util.List.of("webapp"), result.targets());
            assertEquals("us-west-2", result.region());
        }

        @Test
        @DisplayName("should accept boolean values case-insensitively")
        void booleanCaseInsensitive() throws IOException {
            tool = DeployTool.of();
            final Path config = writeConfig("target=webapp\nparallel=TRUE\n");

            final DeployTool.DeployResult result = tool.deploy(config);

            assertTrue(result.parallel());
        }

        @Test
        @DisplayName("should ignore unknown keys")
        void unknownKeysIgnored() throws IOException {
            tool = DeployTool.of();
            final Path config = writeConfig("target=webapp\nstrategy=blue-green\n");

            final DeployTool.DeployResult result = tool.deploy(config);

            assertEquals(java.util.List.of("webapp"), result.targets());
        }

        @Test
        @DisplayName("should accept a timeout boundary of one second")
        void timeoutBoundary() throws IOException {
            tool = DeployTool.of();
            final Path config = writeConfig("target=webapp\ntimeout=1\n");

            final DeployTool.DeployResult result = tool.deploy(config);

            assertEquals(1, result.timeoutSeconds());
        }
    }

    @Nested
    @DisplayName("Sad paths - error messages")
    class SadPaths {

        @Test
        @DisplayName("should report a missing config file with its path")
        void missingFile() {
            tool = DeployTool.of();
            final Path missing = tempDir.resolve("does-not-exist.conf");

            final DeployTool.DeployException e =
                    assertThrows(DeployTool.DeployException.class, () -> tool.deploy(missing));

            assertTrue(e.getMessage().contains("config file not found"),
                    "message was: " + e.getMessage());
            assertTrue(e.getMessage().contains("does-not-exist.conf"),
                    "message was: " + e.getMessage());
        }

        @Test
        @DisplayName("should report an empty config file as missing target")
        void emptyFile() throws IOException {
            tool = DeployTool.of();
            final Path config = writeConfig("");

            final DeployTool.DeployException e =
                    assertThrows(DeployTool.DeployException.class, () -> tool.deploy(config));

            assertEquals("missing required key: target", e.getMessage());
        }

        @Test
        @DisplayName("should report a config without a target key")
        void missingTargetKey() throws IOException {
            tool = DeployTool.of();
            final Path config = writeConfig("region=eu-west-1\n");

            final DeployTool.DeployException e =
                    assertThrows(DeployTool.DeployException.class, () -> tool.deploy(config));

            assertEquals("missing required key: target", e.getMessage());
        }

        @Test
        @DisplayName("should report an empty target value")
        void emptyTargetValue() throws IOException {
            tool = DeployTool.of();
            final Path config = writeConfig("target=\n");

            final DeployTool.DeployException e =
                    assertThrows(DeployTool.DeployException.class, () -> tool.deploy(config));

            assertEquals("missing required key: target", e.getMessage());
        }

        @Test
        @DisplayName("should report a malformed line with its line number")
        void malformedLine() throws IOException {
            tool = DeployTool.of();
            final Path config = writeConfig("target=webapp\ngarbage line\n");

            final DeployTool.DeployException e =
                    assertThrows(DeployTool.DeployException.class, () -> tool.deploy(config));

            assertTrue(e.getMessage().contains("invalid line 2"),
                    "message was: " + e.getMessage());
            assertTrue(e.getMessage().contains("garbage line"),
                    "message was: " + e.getMessage());
        }

        @Test
        @DisplayName("should report a non-numeric timeout")
        void invalidTimeoutNonNumeric() throws IOException {
            tool = DeployTool.of();
            final Path config = writeConfig("target=webapp\ntimeout=abc\n");

            final DeployTool.DeployException e =
                    assertThrows(DeployTool.DeployException.class, () -> tool.deploy(config));

            assertTrue(e.getMessage().contains("invalid timeout"),
                    "message was: " + e.getMessage());
            assertTrue(e.getMessage().contains("abc"),
                    "message was: " + e.getMessage());
        }

        @Test
        @DisplayName("should reject a zero timeout")
        void invalidTimeoutZero() throws IOException {
            tool = DeployTool.of();
            final Path config = writeConfig("target=webapp\ntimeout=0\n");

            final DeployTool.DeployException e =
                    assertThrows(DeployTool.DeployException.class, () -> tool.deploy(config));

            assertTrue(e.getMessage().contains("invalid timeout"),
                    "message was: " + e.getMessage());
        }

        @Test
        @DisplayName("should reject a negative timeout")
        void invalidTimeoutNegative() throws IOException {
            tool = DeployTool.of();
            final Path config = writeConfig("target=webapp\ntimeout=-5\n");

            final DeployTool.DeployException e =
                    assertThrows(DeployTool.DeployException.class, () -> tool.deploy(config));

            assertTrue(e.getMessage().contains("invalid timeout"),
                    "message was: " + e.getMessage());
        }

        @Test
        @DisplayName("should reject a non-boolean parallel value")
        void invalidBoolean() throws IOException {
            tool = DeployTool.of();
            final Path config = writeConfig("target=webapp\nparallel=maybe\n");

            final DeployTool.DeployException e =
                    assertThrows(DeployTool.DeployException.class, () -> tool.deploy(config));

            assertTrue(e.getMessage().contains("invalid boolean"),
                    "message was: " + e.getMessage());
        }

        @Test
        @DisplayName("should reject a null config path")
        void nullConfigPath() {
            tool = DeployTool.of();

            assertThrows(NullPointerException.class, () -> tool.deploy(null));
        }
    }
}
