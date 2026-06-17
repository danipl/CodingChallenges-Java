package com.danipl.platform.resilience.bulkhead;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Bulkhead tests")
class BulkheadTest {

    private Bulkhead<String> instance;

    @BeforeEach
    void setUp() {
        instance = Bulkhead.of(new Bulkhead.Config(4, 500, true, 50));
    }

    @Nested
    @DisplayName("Basic behavior")
    class BasicBehavior {

        @Test
        @DisplayName("execute returns operation result")
        void executeReturnsResult() throws Exception {
            instance.registerPartition("tenant-a", 4);

            String result = instance.execute("tenant-a", () -> "hello");

            assertEquals("hello", result);
        }

        @Test
        @DisplayName("activeCount increases during execution")
        void activeCountIncreasesDuringExecution() throws Exception {
            instance.registerPartition("tenant-a", 4);
            CountDownLatch inside = new CountDownLatch(1);
            CountDownLatch release = new CountDownLatch(1);
            AtomicReference<String> captured = new AtomicReference<>();

            Thread t = new Thread(() -> {
                try {
                    instance.execute("tenant-a", () -> {
                        captured.set(Thread.currentThread().getName());
                        inside.countDown();
                        release.await();
                        return "done";
                    });
                } catch (Exception ignored) {
                }
            });
            t.start();
            assertTrue(inside.await(5, TimeUnit.SECONDS));

            assertEquals(1, instance.activeCount("tenant-a"));
            release.countDown();
            t.join(5000);
        }

        @Test
        @DisplayName("activeCount returns to zero after execution")
        void activeCountReturnsToZero() throws Exception {
            instance.registerPartition("tenant-a", 4);

            instance.execute("tenant-a", () -> "done");

            assertEquals(0, instance.activeCount("tenant-a"));
        }

        @Test
        @DisplayName("maxConcurrency returns configured value")
        void maxConcurrencyReturnsConfiguredValue() {
            instance.registerPartition("tenant-a", 8);

            assertEquals(8, instance.maxConcurrency("tenant-a"));
        }

        @Test
        @DisplayName("registeredPartitions contains added keys")
        void registeredPartitionsContainsKeys() {
            instance.registerPartition("tenant-a", 4);
            instance.registerPartition("tenant-b", 2);

            assertTrue(instance.registeredPartitions().contains("tenant-a"));
            assertTrue(instance.registeredPartitions().contains("tenant-b"));
            assertEquals(2, instance.registeredPartitions().size());
        }

        @Test
        @DisplayName("removePartition returns true and removes key")
        void removePartitionRemovesKey() {
            instance.registerPartition("tenant-a", 4);

            assertTrue(instance.removePartition("tenant-a"));
            assertFalse(instance.registeredPartitions().contains("tenant-a"));
        }

        @Test
        @DisplayName("removePartition returns false for unknown key")
        void removePartitionUnknownReturnsFalse() {
            assertFalse(instance.removePartition("nonexistent"));
        }

        @Test
        @DisplayName("totalActiveCount sums across partitions")
        void totalActiveCountSumsAcrossPartitions() throws Exception {
            instance.registerPartition("a", 4);
            instance.registerPartition("b", 4);
            CountDownLatch inside = new CountDownLatch(2);
            CountDownLatch release = new CountDownLatch(1);

            Thread t1 = new Thread(() -> {
                try {
                    instance.execute("a", () -> { inside.countDown(); release.await(); return "a"; });
                } catch (Exception ignored) {
                }
            });
            Thread t2 = new Thread(() -> {
                try {
                    instance.execute("b", () -> { inside.countDown(); release.await(); return "b"; });
                } catch (Exception ignored) {
                }
            });
            t1.start();
            t2.start();
            assertTrue(inside.await(5, TimeUnit.SECONDS));

            assertEquals(2, instance.totalActiveCount());
            release.countDown();
            t1.join(5000);
            t2.join(5000);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("config validation rejects zero concurrency")
        void configValidationRejectsZeroConcurrency() {
            assertThrows(IllegalArgumentException.class, () ->
                    new Bulkhead.Config(0, 1000, false, 10));
        }

        @Test
        @DisplayName("config validation rejects negative timeout")
        void configValidationRejectsNegativeTimeout() {
            assertThrows(IllegalArgumentException.class, () ->
                    new Bulkhead.Config(1, -1, false, 10));
        }

        @Test
        @DisplayName("config validation rejects zero maxPartitions")
        void configValidationRejectsZeroMaxPartitions() {
            assertThrows(IllegalArgumentException.class, () ->
                    new Bulkhead.Config(1, 1000, false, 0));
        }

        @Test
        @DisplayName("config defaults are valid")
        void configDefaultsAreValid() {
            Bulkhead.Config config = Bulkhead.Config.defaults();
            assertDoesNotThrow(() -> Bulkhead.of(config));
        }

        @Test
        @DisplayName("registerPartition rejects empty key")
        void registerPartitionRejectsEmptyKey() {
            assertThrows(IllegalArgumentException.class, () ->
                    instance.registerPartition("", 4));
        }

        @Test
        @DisplayName("registerPartition rejects null key")
        void registerPartitionRejectsNullKey() {
            assertThrows(IllegalArgumentException.class, () ->
                    instance.registerPartition(null, 4));
        }

        @Test
        @DisplayName("registerPartition rejects zero maxConcurrency")
        void registerPartitionRejectsZeroMaxConcurrency() {
            assertThrows(IllegalArgumentException.class, () ->
                    instance.registerPartition("tenant-a", 0));
        }

        @Test
        @DisplayName("execute throws PartitionNotFoundException when autoCreate=false")
        void executeThrowsWhenPartitionNotFound() {
            Bulkhead<String> strict = Bulkhead.of(new Bulkhead.Config(4, 500, false, 50));

            assertThrows(Bulkhead.PartitionNotFoundException.class, () ->
                    strict.execute("unknown", () -> "fail"));
        }

        @Test
        @DisplayName("metrics returns snapshot for registered partitions")
        void metricsReturnsSnapshot() throws Exception {
            instance.registerPartition("tenant-a", 4);
            instance.execute("tenant-a", () -> "done");

            var metrics = instance.metrics();
            assertTrue(metrics.containsKey("tenant-a"));
            assertEquals(1, metrics.get("tenant-a").totalSuccessful());
        }

        @Test
        @DisplayName("operation exception propagates through bulkhead")
        void operationExceptionPropagates() {
            instance.registerPartition("tenant-a", 4);

            assertThrows(RuntimeException.class, () ->
                    instance.execute("tenant-a", () -> {
                        throw new RuntimeException("boom");
                    }));
        }
    }

    @Nested
    @DisplayName("Concurrency limiting")
    class ConcurrencyLimiting {

        @Test
        @DisplayName("exceeding limit throws BulkheadRejectedException")
        void exceedingLimitThrowsException() throws Exception {
            instance = Bulkhead.of(new Bulkhead.Config(2, 100, false, 50));
            instance.registerPartition("tenant-a", 2);
            CountDownLatch held = new CountDownLatch(2);
            CountDownLatch release = new CountDownLatch(1);

            Thread t1 = new Thread(() -> {
                try {
                    instance.execute("tenant-a", () -> { held.countDown(); release.await(); return "1"; });
                } catch (Exception ignored) {
                }
            });
            Thread t2 = new Thread(() -> {
                try {
                    instance.execute("tenant-a", () -> { held.countDown(); release.await(); return "2"; });
                } catch (Exception ignored) {
                }
            });
            t1.start();
            t2.start();
            assertTrue(held.await(5, TimeUnit.SECONDS));

            assertThrows(Bulkhead.BulkheadRejectedException.class, () ->
                    instance.execute("tenant-a", () -> "3"));

            release.countDown();
            t1.join(5000);
            t2.join(5000);
        }

        @Test
        @DisplayName("slot freed allows next execution")
        void slotFreedAllowsNextExecution() throws Exception {
            instance.registerPartition("tenant-a", 1);
            CountDownLatch firstInside = new CountDownLatch(1);
            CountDownLatch firstRelease = new CountDownLatch(1);
            AtomicBoolean secondSucceeded = new AtomicBoolean(false);

            Thread first = new Thread(() -> {
                try {
                    instance.execute("tenant-a", () -> {
                        firstInside.countDown();
                        firstRelease.await();
                        return "first";
                    });
                } catch (Exception ignored) {
                }
            });
            first.start();
            assertTrue(firstInside.await(5, TimeUnit.SECONDS));

            Thread second = new Thread(() -> {
                try {
                    instance.execute("tenant-a", () -> "second");
                    secondSucceeded.set(true);
                } catch (Exception ignored) {
                }
            });
            second.start();

            firstRelease.countDown();
            first.join(5000);
            second.join(5000);

            assertTrue(secondSucceeded.get());
        }
    }

    @Nested
    @DisplayName("Thread safety")
    class ThreadSafety {

        @Test
        @DisplayName("concurrent executions on different partitions are safe")
        void concurrentExecutionsDifferentPartitions() throws InterruptedException {
            instance.registerPartition("a", 10);
            instance.registerPartition("b", 10);
            instance.registerPartition("c", 10);

            int threadCount = 30;
            int opsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicReference<Throwable> error = new AtomicReference<>();

            String[] partitions = {"a", "b", "c"};
            for (int t = 0; t < threadCount; t++) {
                final String partition = partitions[t % 3];
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < opsPerThread; i++) {
                            final int idx = i;
                            instance.execute(partition, () -> partition + "-" + idx);
                            successCount.incrementAndGet();
                        }
                    } catch (Throwable e) {
                        error.set(e);
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS), "All threads should complete");
            executor.shutdownNow();

            assertNull(error.get(), "No errors should occur");
            assertEquals(threadCount * opsPerThread, successCount.get());
        }

        @Test
        @DisplayName("concurrent register and execute are safe")
        void concurrentRegisterAndExecute() throws InterruptedException {
            int threadCount = 20;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);
            AtomicReference<Throwable> error = new AtomicReference<>();

            for (int t = 0; t < threadCount; t++) {
                final int id = t;
                executor.submit(() -> {
                    try {
                        start.await();
                        instance.registerPartition("tenant-" + id, 4);
                        instance.execute("tenant-" + id, () -> "ok");
                    } catch (Throwable e) {
                        error.set(e);
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS), "All threads should complete");
            executor.shutdownNow();

            assertNull(error.get());
            assertEquals(threadCount, instance.registeredPartitions().size());
        }

        @Test
        @DisplayName("concurrent reads and writes of metrics are safe")
        void concurrentReadsAndWrites() throws InterruptedException {
            instance.registerPartition("tenant-a", 10);
            int threadCount = 20;
            int opsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);
            AtomicReference<Throwable> error = new AtomicReference<>();

            for (int t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < opsPerThread; i++) {
                            final int op = i;
                            if (op % 2 == 0) {
                                instance.execute("tenant-a", () -> "ok");
                            } else {
                                instance.metrics();
                            }
                        }
                    } catch (Throwable e) {
                        error.set(e);
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS), "All threads should complete");
            executor.shutdownNow();

            assertNull(error.get());
        }

        @Test
        @DisplayName("concurrent acquire and close are safe")
        void concurrentAcquireAndClose() throws InterruptedException {
            instance.registerPartition("tenant-a", 10);
            int threadCount = 20;
            int opsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);
            AtomicReference<Throwable> error = new AtomicReference<>();

            for (int t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < opsPerThread; i++) {
                            try (Bulkhead.Permit permit = instance.acquire("tenant-a")) {
                                assertNotNull(permit);
                            } catch (Bulkhead.BulkheadRejectedException ignored) {
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Throwable e) {
                        error.set(e);
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS), "All threads should complete");
            executor.shutdownNow();

            assertNull(error.get());
            assertEquals(0, instance.activeCount("tenant-a"));
        }
    }
}
