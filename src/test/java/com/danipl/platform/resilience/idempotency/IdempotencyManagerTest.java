package com.danipl.platform.resilience.idempotency;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("IdempotencyManager tests")
class IdempotencyManagerTest {

    private IdempotencyManager manager;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneId.of("UTC"));
        manager = IdempotencyManager.of(
            new IdempotencyManager.Config(Duration.ofMinutes(5), 1000),
            fixedClock
        );
    }

    @Nested
    @DisplayName("Basic behavior")
    class BasicBehavior {

        @Test
        @DisplayName("initial cache size is zero")
        void initialCacheSizeIsZero() {
            assertEquals(0, manager.size());
        }

        @Test
        @DisplayName("first execution returns operation result")
        void firstExecutionReturnsResult() {
            String key = "payment-123";
            String result = manager.execute(key, () -> "success");
            assertEquals("success", result);
        }

        @Test
        @DisplayName("second execution with same key returns cached result")
        void secondExecutionReturnsCachedResult() {
            String key = "payment-456";
            AtomicInteger callCount = new AtomicInteger(0);

            String result1 = manager.execute(key, () -> {
                callCount.incrementAndGet();
                return "first";
            });

            String result2 = manager.execute(key, () -> {
                callCount.incrementAndGet();
                return "second";
            });

            assertEquals("first", result1);
            assertEquals("first", result2);
            assertEquals(1, callCount.get());
        }

        @Test
        @DisplayName("different keys execute independently")
        void differentKeysExecuteIndependently() {
            String result1 = manager.execute("key-1", () -> "result-1");
            String result2 = manager.execute("key-2", () -> "result-2");

            assertEquals("result-1", result1);
            assertEquals("result-2", result2);
            assertEquals(2, manager.size());
        }

        @Test
        @DisplayName("void execution works correctly")
        void voidExecutionWorks() {
            AtomicInteger callCount = new AtomicInteger(0);
            String key = "void-key";

            manager.executeVoid(key, callCount::incrementAndGet);
            manager.executeVoid(key, callCount::incrementAndGet);

            assertEquals(1, callCount.get());
        }

        @Test
        @DisplayName("getCachedResult returns cached value")
        void getCachedResultReturnsValue() {
            String key = "cached-key";
            manager.execute(key, () -> "cached-value");

            var result = manager.<String>getCachedResult(key);
            assertTrue(result.isPresent());
            assertEquals("cached-value", result.get());
        }

        @Test
        @DisplayName("getCachedResult returns empty for unknown key")
        void getCachedResultReturnsEmptyForUnknown() {
            var result = manager.getCachedResult("unknown");
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("getState returns SUCCESS after successful execution")
        void getStateReturnsSuccess() {
            String key = "success-key";
            manager.execute(key, () -> "ok");

            var state = manager.getState(key);
            assertTrue(state.isPresent());
            assertEquals(IdempotencyManager.State.SUCCESS, state.get());
        }

        @Test
        @DisplayName("getState returns FAILED after failed execution")
        void getStateReturnsFailed() {
            String key = "failed-key";

            assertThrows(IdempotencyManager.IdempotencyException.class, () ->
                manager.execute(key, () -> {
                    throw new RuntimeException("Operation failed");
                })
            );

            var state = manager.getState(key);
            assertTrue(state.isPresent());
            assertEquals(IdempotencyManager.State.FAILED, state.get());
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("config validation rejects zero TTL")
        void configValidationRejectsZeroTTL() {
            assertThrows(IllegalArgumentException.class, () ->
                new IdempotencyManager.Config(Duration.ZERO, 100)
            );
        }

        @Test
        @DisplayName("config validation rejects negative TTL")
        void configValidationRejectsNegativeTTL() {
            assertThrows(IllegalArgumentException.class, () ->
                new IdempotencyManager.Config(Duration.ofSeconds(-1), 100)
            );
        }

        @Test
        @DisplayName("config validation rejects zero maxCacheSize")
        void configValidationRejectsZeroMaxCacheSize() {
            assertThrows(IllegalArgumentException.class, () ->
                new IdempotencyManager.Config(Duration.ofMinutes(5), 0)
            );
        }

        @Test
        @DisplayName("null key throws exception")
        void nullKeyThrowsException() {
            assertThrows(NullPointerException.class, () ->
                manager.execute(null, () -> "value")
            );
        }

        @Test
        @DisplayName("cleanup returns zero when no expired entries")
        void cleanupReturnsZeroWhenNoExpired() {
            manager.execute("fresh-key", () -> "value");
            int removed = manager.cleanup();
            assertEquals(0, removed);
            assertEquals(1, manager.size());
        }
    }

    @Nested
    @DisplayName("Thread safety")
    class ThreadSafety {

        @Test
        @DisplayName("concurrent executions with same key only execute once")
        void concurrentExecutionsSameKeyExecuteOnce() throws InterruptedException {
            int threadCount = 20;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);
            AtomicInteger executionCount = new AtomicInteger(0);
            AtomicReference<String> result = new AtomicReference<>();

            String key = "concurrent-key";

            for (int t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        String r = manager.execute(key, () -> {
                            executionCount.incrementAndGet();
                            try {
                                Thread.sleep(10);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                            return "result";
                        });
                        result.set(r);
                    } catch (InterruptedException ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS), "All threads should complete");
            executor.shutdownNow();

            assertEquals(1, executionCount.get(), "Operation should execute exactly once");
            assertEquals("result", result.get());
        }

        @Test
        @DisplayName("concurrent executions with different keys are independent")
        void concurrentExecutionsDifferentKeysIndependent() throws InterruptedException {
            int threadCount = 50;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        start.await();
                        String key = "key-" + threadId;
                        String result = manager.execute(key, () -> "result-" + threadId);
                        if (result.equals("result-" + threadId)) {
                            successCount.incrementAndGet();
                        }
                    } catch (InterruptedException ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS), "All threads should complete");
            executor.shutdownNow();

            assertEquals(threadCount, successCount.get());
            assertEquals(threadCount, manager.size());
        }

        @Test
        @DisplayName("concurrent reads and writes are safe")
        void concurrentReadsAndWritesSafe() throws InterruptedException {
            int writerCount = 10;
            int readerCount = 20;
            ExecutorService executor = Executors.newFixedThreadPool(writerCount + readerCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(writerCount + readerCount);
            AtomicInteger writeSuccess = new AtomicInteger(0);
            AtomicInteger readSuccess = new AtomicInteger(0);

            for (int w = 0; w < writerCount; w++) {
                final int writerId = w;
                executor.submit(() -> {
                    try {
                        start.await();
                        String key = "write-key-" + writerId;
                        manager.execute(key, () -> "value-" + writerId);
                        writeSuccess.incrementAndGet();
                    } catch (InterruptedException ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }

            for (int r = 0; r < readerCount; r++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        Thread.sleep(5);
                        for (int i = 0; i < 10; i++) {
                            manager.getCachedResult("write-key-" + (i % writerCount));
                            manager.getState("write-key-" + (i % writerCount));
                        }
                        readSuccess.incrementAndGet();
                    } catch (InterruptedException ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS), "All threads should complete");
            executor.shutdownNow();

            assertEquals(writerCount, writeSuccess.get());
            assertEquals(readerCount, readSuccess.get());
        }
    }
}
