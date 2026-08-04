package com.danipl.practise.cli.buildcache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BuildCache tests")
class BuildCacheTest {

    @TempDir
    Path tempDir;

    private Path cacheDir;
    private BuildCache cache;

    @BeforeEach
    void setUp() throws IOException {
        cacheDir = tempDir.resolve("cache");
        cache = BuildCache.of(cacheDir);
    }

    private static byte[] bytes(final String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("Basic behavior")
    class BasicBehavior {

        @Test
        @DisplayName("should round-trip a stored entry")
        void roundTrip() {
            cache.put("webapp", bytes("jar-content"));

            final Optional<byte[]> result = cache.get("webapp");

            assertTrue(result.isPresent());
            assertEquals("jar-content", new String(result.get(), StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("should return empty for an absent key")
        void absentKey() {
            assertTrue(cache.get("missing").isEmpty());
        }

        @Test
        @DisplayName("should overwrite an existing key")
        void overwrite() {
            cache.put("webapp", bytes("v1"));
            cache.put("webapp", bytes("v2"));

            assertEquals("v2", new String(cache.get("webapp").orElseThrow(), StandardCharsets.UTF_8));
        }

        @Test
        @DisplayName("should count hits and misses")
        void countsHitsAndMisses() {
            cache.put("present", bytes("x"));

            cache.get("present");
            cache.get("present");
            cache.get("absent");

            final BuildCache.Stats stats = cache.stats();
            assertEquals(2, stats.hits());
            assertEquals(1, stats.misses());
        }
    }

    @Nested
    @DisplayName("Key validation - path safety")
    class KeyValidation {

        @Test
        @DisplayName("should reject a null key")
        void nullKey() {
            assertThrows(IllegalArgumentException.class, () -> cache.get(null));
            assertThrows(IllegalArgumentException.class, () -> cache.put(null, bytes("x")));
        }

        @Test
        @DisplayName("should reject a blank key")
        void blankKey() {
            assertThrows(IllegalArgumentException.class, () -> cache.get("  "));
            assertThrows(IllegalArgumentException.class, () -> cache.put("", bytes("x")));
        }

        @Test
        @DisplayName("should reject a key that traverses out of the cache directory")
        void pathTraversalRejected() {
            assertThrows(IllegalArgumentException.class, () -> cache.put("../escape", bytes("x")));
            assertThrows(IllegalArgumentException.class, () -> cache.get("../../etc/passwd"));
        }

        @Test
        @DisplayName("should reject keys with path separators")
        void separatorRejected() {
            assertThrows(IllegalArgumentException.class, () -> cache.put("a/b", bytes("x")));
            assertThrows(IllegalArgumentException.class, () -> cache.put("a\\b", bytes("x")));
        }

        @Test
        @DisplayName("a rejected traversal must not write outside the cache directory")
        void noEscapeFileCreated() {
            try {
                cache.put("../escape", bytes("x"));
            } catch (IllegalArgumentException ignored) {
                // expected
            }

            assertFalse(Files.exists(tempDir.resolve("escape.bin")),
                    "traversal write escaped the cache directory");
            assertFalse(Files.exists(cacheDir.resolve("../escape.bin")));
        }
    }

    @Nested
    @DisplayName("Failure honesty")
    class FailureHonesty {

        @Test
        @DisplayName("a corrupt entry is a CacheException, not a silent miss")
        void corruptEntryFailsLoudly() throws IOException {
            // A directory where the entry file should be: readAllBytes fails
            Files.createDirectories(cacheDir.resolve("corrupt.bin"));

            final BuildCache.CacheException e =
                    assertThrows(BuildCache.CacheException.class, () -> cache.get("corrupt"));

            assertTrue(e.getMessage().contains("corrupt"),
                    "message was: " + e.getMessage());
        }

        @Test
        @DisplayName("a failed write is a CacheException, not a swallowed success")
        void failedWriteFailsLoudly() throws IOException {
            // cacheDir is a regular file, so writes under it must fail
            final Path notADir = tempDir.resolve("not-a-dir");
            Files.writeString(notADir, "i am a file");
            final BuildCache brokenCache = BuildCache.of(notADir);

            final BuildCache.CacheException e =
                    assertThrows(BuildCache.CacheException.class,
                            () -> brokenCache.put("webapp", bytes("x")));

            assertTrue(e.getMessage().contains("webapp"),
                    "message was: " + e.getMessage());
        }

        @Test
        @DisplayName("a corrupt entry is not counted as a miss")
        void corruptEntryNotCountedAsMiss() throws IOException {
            Files.createDirectories(cacheDir.resolve("corrupt.bin"));

            try {
                cache.get("corrupt");
            } catch (BuildCache.CacheException ignored) {
                // expected
            }

            assertEquals(0, cache.stats().misses());
        }
    }

    @Nested
    @DisplayName("Thread safety")
    class ThreadSafety {

        @Test
        @DisplayName("stats must not lose updates under concurrent access")
        void noLostUpdates() throws InterruptedException {
            cache.put("shared", bytes("x"));

            final int threadCount = 20;
            final int opsPerThread = 100;
            final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            final CountDownLatch start = new CountDownLatch(1);
            final CountDownLatch done = new CountDownLatch(threadCount);
            final AtomicInteger expectedHits = new AtomicInteger();
            final AtomicInteger expectedMisses = new AtomicInteger();

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < opsPerThread; i++) {
                            cache.get("shared");        // hit
                            expectedHits.incrementAndGet();
                            cache.get("missing-" + threadId); // miss
                            expectedMisses.incrementAndGet();
                        }
                    } catch (InterruptedException ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS), "all threads should complete");
            executor.shutdownNow();

            final BuildCache.Stats stats = cache.stats();
            assertEquals(expectedHits.get(), stats.hits(),
                    "hits lost under concurrency");
            assertEquals(expectedMisses.get(), stats.misses(),
                    "misses lost under concurrency");
        }
    }
}
