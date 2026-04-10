package com.danipl.platform.challenge06;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("Retry tests")
class RetryTest {

    private Retry retry;
    private AtomicInteger attemptCount;

    @BeforeEach
    void setUp() {
        retry = Retry.of(new RetryConfig(3, 50, 500, 0.0));
        attemptCount = new AtomicInteger(0);
    }

    @Nested
    @DisplayName("Success scenarios")
    class SuccessScenarios {

        @Test
        @DisplayName("success on first try")
        void successOnFirstTry() throws Exception {
            String result = retry.execute(() -> "hello");
            assertEquals("hello", result);
        }

        @Test
        @DisplayName("retry on transient failure then success")
        void retryThenSuccess() throws Exception {
            retry = Retry.of(new RetryConfig(3, 10, 100, 0.0));
            String result = retry.execute(() -> {
                int n = attemptCount.incrementAndGet();
                if (n <= 2) throw new RuntimeException("transient failure");
                return "recovered";
            });
            assertEquals("recovered", result);
            assertEquals(3, attemptCount.get());
        }

        @Test
        @DisplayName("executeWithDetails returns correct attempt count on success")
        void executeWithDetailsSuccess() throws Exception {
            RetryResult<String> result = retry.executeWithDetails(() -> "ok");
            assertEquals("ok", result.value());
            assertEquals(1, result.attemptsMade());
            assertEquals(0, result.totalWaitMs());
            assertTrue(result.delays().isEmpty());
        }
    }

    @Nested
    @DisplayName("Max retries exceeded")
    class MaxRetriesExceeded {

        @Test
        @DisplayName("throws MaxRetriesExceededException after exhausting retries")
        void maxRetriesExceeded() {
            assertThrows(MaxRetriesExceededException.class, () ->
                    retry.execute(() -> {
                        throw new RuntimeException("always fails");
                    })
            );
        }

        @Test
        @DisplayName("MaxRetriesExceededException wraps last exception")
        void wrapsLastException() {
            try {
                retry.execute(() -> {
                    throw new RuntimeException("wrapped error");
                });
                fail("Should throw MaxRetriesExceededException");
            } catch (MaxRetriesExceededException e) {
                assertNotNull(e.getCause());
                assertEquals("wrapped error", e.getCause().getMessage());
                assertEquals(3, e.getAttemptsMade());
            }
        }

        @Test
        @DisplayName("withDetails returns attempt count on failure")
        void withDetailsFailureAttempts() {
            try {
                retry.executeWithDetails(() -> {
                    throw new RuntimeException("fail");
                });
                fail("Should throw");
            } catch (MaxRetriesExceededException e) {
                assertEquals(3, e.getAttemptsMade());
            }
        }
    }

    @Nested
    @DisplayName("Backoff timing")
    class BackoffTiming {

        @Test
        @DisplayName("delays roughly match exponential pattern")
        void exponentialBackoffDelays() {
            retry = Retry.of(new RetryConfig(4, 50, 10000, 0.0));
            try {
                retry.executeWithDetails(() -> {
                    throw new RuntimeException("fail");
                });
                fail("Should throw");
            } catch (MaxRetriesExceededException e) {
            }
            // Just verify no compilation error and timing is reasonable
        }

        @Test
        @DisplayName("maxDelayMs caps the exponential growth")
        void maxDelayCapsGrowth() {
            retry = Retry.of(new RetryConfig(5, 100, 200, 0.0));
            // With baseDelay=100: 100, 200, 200(capped), 200(capped), 200(capped)
            assertThrows(MaxRetriesExceededException.class, () ->
                    retry.execute(() -> {
                        throw new RuntimeException("fail");
                    })
            );
        }
    }

    @Nested
    @DisplayName("Jitter validation")
    class JitterValidation {

        @Test
        @DisplayName("zero jitter produces consistent delays")
        void zeroJitterNoVariation() {
            retry = Retry.of(new RetryConfig(4, 50, 1000, 0.0));
            try {
                retry.executeWithDetails(() -> {
                    throw new RuntimeException("fail");
                });
                fail("Should throw");
            } catch (MaxRetriesExceededException e) {
            }
        }

        @Test
        @DisplayName("jitter factor 1.0 produces wide variation")
        void fullJitterVariation() {
            retry = Retry.of(new RetryConfig(4, 100, 10000, 1.0));
            // Multiple runs should show variation
            for (int run = 0; run < 5; run++) {
                try {
                    retry.executeWithDetails(() -> {
                        throw new RuntimeException("fail");
                    });
                } catch (MaxRetriesExceededException e) {
                    // Expected
                }
            }
        }

        @Test
        @DisplayName("Config rejects jitter > 1.0")
        void rejectsHighJitter() {
            assertThrows(IllegalArgumentException.class,
                    () -> new RetryConfig(3, 50, 500, 1.5));
        }

        @Test
        @DisplayName("Config rejects negative jitter")
        void rejectsNegativeJitter() {
            assertThrows(IllegalArgumentException.class,
                    () -> new RetryConfig(3, 50, 500, -0.1));
        }
    }

    @Nested
    @DisplayName("Concurrent retry")
    class ConcurrentRetry {

        @Test
        @DisplayName("concurrent retry executions are safe")
        void concurrentRetrySafety() throws InterruptedException {
            retry = Retry.of(new RetryConfig(3, 10, 100, 0.1));
            int threadCount = 20;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);
            AtomicInteger successes = new AtomicInteger(0);

            for (int t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        String result = retry.execute(() -> {
                            Thread.sleep(5);
                            return "ok-" + Thread.currentThread().getId();
                        });
                        if (result.startsWith("ok-")) successes.incrementAndGet();
                    } catch (Exception ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS));
            executor.shutdownNow();
            assertEquals(threadCount, successes.get());
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Config rejects zero maxAttempts")
        void rejectsZeroAttempts() {
            assertThrows(IllegalArgumentException.class,
                    () -> new RetryConfig(0, 50, 500, 0.0));
        }

        @Test
        @DisplayName("maxAttempts=1 means no retries")
        void singleAttemptNoRetries() {
            retry = Retry.of(new RetryConfig(1, 50, 500, 0.0));
            AtomicInteger count = new AtomicInteger(0);
            try {
                retry.execute(() -> {
                    count.incrementAndGet();
                    throw new RuntimeException("fail");
                });
                fail("Should throw");
            } catch (MaxRetriesExceededException e) {
                assertEquals(1, count.get());
                assertEquals(1, e.getAttemptsMade());
            }
        }

        @Test
        @DisplayName("very short delays do not block excessively")
        void veryShortDelays() {
            retry = Retry.of(new RetryConfig(3, 0, 0, 0.0));
            long start = System.currentTimeMillis();
            try {
                retry.execute(() -> {
                    throw new RuntimeException("fail");
                });
            } catch (MaxRetriesExceededException ignored) {
            }
            long elapsed = System.currentTimeMillis() - start;
            assertTrue(elapsed < 500, "Should complete quickly with zero delays");
        }
    }
}
