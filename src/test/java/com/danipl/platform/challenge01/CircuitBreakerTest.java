package com.danipl.platform.challenge01;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@DisplayName("CircuitBreaker tests")
class CircuitBreakerTest {

    private CircuitBreaker breaker;

    @BeforeEach
    void setUp() {
        breaker = CircuitBreaker.of(new CircuitBreaker.Config(3, 500));
    }

    @Nested
    @DisplayName("CLOSED state")
    class ClosedState {

        @Test
        @DisplayName("starts in CLOSED state")
        void initialIsClosed() {
            assertEquals(CircuitBreaker.State.CLOSED, breaker.getCurrentState());
        }

        @Test
        @DisplayName("successful calls pass through in CLOSED")
        void successInClosedPassesThrough() throws Exception {
            String result = breaker.execute(() -> "hello");
            assertEquals("hello", result);
        }

        @Test
        @DisplayName("runnable executes successfully in CLOSED")
        void runnableSuccessInClosed() {
            AtomicBoolean executed = new AtomicBoolean(false);
            assertDoesNotThrow(() -> breaker.execute(() -> executed.set(true)));
            assertTrue(executed.get());
        }

        @Test
        @DisplayName("consecutive failures below threshold don't trip circuit")
        void belowThresholdStaysClosed() {
            executeFailures(2);
            assertEquals(CircuitBreaker.State.CLOSED, breaker.getCurrentState());
        }

        @Test
        @DisplayName("exactly failureThreshold consecutive failures opens circuit")
        void reachesThresholdOpensCircuit() {
            executeFailures(3);
            assertEquals(CircuitBreaker.State.OPEN, breaker.getCurrentState());
        }

        @Test
        @DisplayName("intermittent failures don't trip - success resets consecutive count")
        void intermittentDoesNotTrip() {
            try {
                breaker.execute(() -> {
                    throw new RuntimeException("fail");
                });
            } catch (Exception ignored) {
            }
            assertDoesNotThrow(() -> breaker.execute(() -> 42));
            try {
                breaker.execute(() -> {
                    throw new RuntimeException("fail");
                });
            } catch (Exception ignored) {
            }
            try {
                breaker.execute(() -> {
                    throw new RuntimeException("fail");
                });
            } catch (Exception ignored) {
            }
            assertEquals(CircuitBreaker.State.CLOSED, breaker.getCurrentState());
        }
    }

    @Nested
    @DisplayName("OPEN state")
    class OpenState {

        @BeforeEach
        void tripBreaker() {
            executeFailures(3);
            assertEquals(CircuitBreaker.State.OPEN, breaker.getCurrentState());
        }

        @Test
        @DisplayName("calls rejected with CircuitBreakerOpenException")
        void callsRejectedWhenOpen() {
            assertThrows(CircuitBreaker.CircuitBreakerOpenException.class,
                    () -> breaker.execute(() -> "should not reach"));
        }

        @Test
        @DisplayName("rejected calls increment rejected counter")
        void rejectedIncrementsMetric() {
            try {
                breaker.execute(() -> "ignored");
            } catch (Exception ignored) {
            }
            assertEquals(1, breaker.getRejectedCalls());
        }

        @Test
        @DisplayName("runnable also rejected when open")
        void runnableRejectedWhenOpen() {
            assertThrows(CircuitBreaker.CircuitBreakerOpenException.class,
                    () -> breaker.execute(() -> {
                    }));
        }
    }

    @Nested
    @DisplayName("HALF_OPEN state")
    class HalfOpenState {

        @BeforeEach
        void tripAndAwaitHalfOpen() throws InterruptedException {
            executeFailures(3);
            assertEquals(CircuitBreaker.State.OPEN, breaker.getCurrentState());
            Thread.sleep(600);
        }

        @Test
        @DisplayName("probe call allowed through after timeout")
        void probeAllowedAfterTimeout() throws Exception {
            String result = breaker.execute(() -> "success");
            assertEquals("success", result);
        }

        @Test
        @DisplayName("successful probe transitions to CLOSED")
        void successProbeClosesCircuit() throws Exception {
            breaker.execute(() -> "recovered");
            assertEquals(CircuitBreaker.State.CLOSED, breaker.getCurrentState());
        }

        @Test
        @DisplayName("failed probe transitions back to OPEN")
        void failedProbeReopensCircuit() {
            try {
                breaker.execute(() -> {
                    throw new RuntimeException("fail");
                });
            } catch (CircuitBreaker.CircuitBreakerOpenException open) {
                fail("First call should not be rejected - the probe should run");
            } catch (Exception ignored) {
            }
            assertThrows(CircuitBreaker.CircuitBreakerOpenException.class,
                    () -> breaker.execute(() -> "should be rejected"));
        }

        @Test
        @DisplayName("after closing, circuit can trip OPEN again")
        void circuitTripsAgainAfterRecovery() {
            assertDoesNotThrow(() -> breaker.execute(() -> "recovered"));
            assertEquals(CircuitBreaker.State.CLOSED, breaker.getCurrentState());
            executeFailures(3);
            assertEquals(CircuitBreaker.State.OPEN, breaker.getCurrentState());
        }
    }

    @Nested
    @DisplayName("Manual controls")
    class ManualControls {

        @Test
        @DisplayName("reset() transitions OPEN -> CLOSED and resets consecutive failures")
        void resetClosesCircuit() {
            executeFailures(3);
            assertEquals(CircuitBreaker.State.OPEN, breaker.getCurrentState());
            breaker.reset();
            assertEquals(CircuitBreaker.State.CLOSED, breaker.getCurrentState());
            executeFailures(2);
            assertEquals(CircuitBreaker.State.CLOSED, breaker.getCurrentState());
        }

        @Test
        @DisplayName("forceOpen() immediately opens circuit")
        void forceOpen() {
            assertEquals(CircuitBreaker.State.CLOSED, breaker.getCurrentState());
            breaker.forceOpen();
            assertEquals(CircuitBreaker.State.OPEN, breaker.getCurrentState());
            assertThrows(CircuitBreaker.CircuitBreakerOpenException.class,
                    () -> breaker.execute(() -> "nope"));
        }

        @Test
        @DisplayName("forceOpen() blocks calls even after timeout expires")
        void forceOpenBlocksUntilReset() throws InterruptedException {
            breaker.forceOpen();
            assertThrows(CircuitBreaker.CircuitBreakerOpenException.class,
                    () -> breaker.execute(() -> "nope"));
            Thread.sleep(600);
            assertThrows(CircuitBreaker.CircuitBreakerOpenException.class,
                    () -> breaker.execute(() -> "still blocked"));
            breaker.reset();
            assertEquals(CircuitBreaker.State.CLOSED, breaker.getCurrentState());
        }
    }

    @Nested
    @DisplayName("Metrics tracking")
    class MetricsTracking {

        @Test
        @DisplayName("tracks total/failed/successful counts")
        void tracksCounts() throws Exception {
            breaker.execute(() -> "ok");
            try {
                breaker.execute(() -> {
                    throw new RuntimeException("x");
                });
            } catch (Exception ignored) {
            }
            breaker.execute(() -> "ok2");
            assertEquals(3, breaker.getTotalCalls());
            assertEquals(2, breaker.getSuccessfulCalls());
            assertEquals(1, breaker.getFailedCalls());
            assertEquals(0, breaker.getRejectedCalls());
        }

        @Test
        @DisplayName("rejected calls counted separately from failures")
        void rejectedNotCountedAsFailures() {
            executeFailures(3);
            try {
                breaker.execute(() -> "nope");
            } catch (Exception ignored) {
            }
            assertEquals(4, breaker.getTotalCalls());
            assertEquals(3, breaker.getFailedCalls());
            assertEquals(1, breaker.getRejectedCalls());
            assertEquals(0, breaker.getSuccessfulCalls());
        }
    }

    @Nested
    @DisplayName("Thread safety")
    class ThreadSafety {

        @Test
        @DisplayName("concurrent calls are safe and all accounted for")
        void concurrentCallsSafety() throws InterruptedException {
            int threadCount = 50;
            int callsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);
            AtomicInteger successes = new AtomicInteger(0);
            AtomicInteger failures = new AtomicInteger(0);
            AtomicBoolean shouldFail = new AtomicBoolean(false);

            for (int t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < callsPerThread; i++) {
                            try {
                                int result = breaker.execute(() -> {
                                    if (shouldFail.get()) throw new RuntimeException("fail");
                                    return 42;
                                });
                                successes.incrementAndGet();
                            } catch (CircuitBreaker.CircuitBreakerOpenException e) {
                            } catch (Exception e) {
                                failures.incrementAndGet();
                            }
                        }
                    } catch (InterruptedException ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            Thread.sleep(50);
            shouldFail.set(true);
            done.await(10, TimeUnit.SECONDS);
            executor.shutdownNow();

            int total = successes.get() + failures.get();
            assertEquals(threadCount * callsPerThread, total + breaker.getRejectedCalls(),
                    "All calls accounted for (success + failure + rejected)");
        }

        @Test
        @DisplayName("metrics snapshot consistent under concurrent access")
        void metricsConsistentUnderContention() throws InterruptedException {
            ExecutorService executor = Executors.newFixedThreadPool(10);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(10);

            for (int t = 0; t < 10; t++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < 50; i++) {
                            try {
                                breaker.execute(() -> 1);
                            } catch (Exception ignored) {
                            }
                        }
                    } catch (InterruptedException ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            done.await(10, TimeUnit.SECONDS);
            executor.shutdownNow();

            assertTrue(breaker.getTotalCalls() <= 500, "No more than 500 total calls");
            assertTrue(breaker.getTotalCalls() > 0, "Some calls expected");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Config rejects zero failureThreshold")
        void configRejectsZeroThreshold() {
            assertThrows(IllegalArgumentException.class,
                    () -> new CircuitBreaker.Config(0, 100));
        }

        @Test
        @DisplayName("Config rejects zero timeout")
        void configRejectsZeroTimeout() {
            assertThrows(IllegalArgumentException.class,
                    () -> new CircuitBreaker.Config(1, 0));
        }

        @Test
        @DisplayName("Config accepts threshold=1")
        void configAcceptsThresholdOfOne() {
            CircuitBreaker cb = CircuitBreaker.of(new CircuitBreaker.Config(1, 100));
            try {
                cb.execute(() -> {
                    throw new RuntimeException("first");
                });
            } catch (Exception ignored) {
            }
            assertEquals(CircuitBreaker.State.OPEN, cb.getCurrentState());
        }

        @Test
        @DisplayName("exception from supplier is propagated")
        void exceptionPropagated() {
            RuntimeException expected = new RuntimeException("propagate me");
            RuntimeException thrown = assertThrows(RuntimeException.class,
                    () -> breaker.execute(() -> {
                        throw expected;
                    }));
            assertSame(expected, thrown);
        }

        @Test
        @DisplayName("half-open success resets consecutive failure counter")
        void halfOpenSuccessResetsCounter() {
            executeFailures(3);
            assertEquals(CircuitBreaker.State.OPEN, breaker.getCurrentState());
            try {
                Thread.sleep(600);
            } catch (InterruptedException ignored) {
            }
            assertDoesNotThrow(() -> breaker.execute(() -> "recovered"));
            assertEquals(CircuitBreaker.State.CLOSED, breaker.getCurrentState());
            for (int i = 0; i < 2; i++) {
                try {
                    breaker.execute(() -> {
                        throw new RuntimeException("x");
                    });
                } catch (Exception ignored) {
                }
            }
            assertEquals(CircuitBreaker.State.CLOSED, breaker.getCurrentState());
        }

        @Test
        @DisplayName("state transitions with very small timeouts")
        void smallTimeout() throws InterruptedException {
            CircuitBreaker cb = CircuitBreaker.of(new CircuitBreaker.Config(1, 1));
            try {
                cb.execute(() -> {
                    throw new RuntimeException("x");
                });
            } catch (Exception ignored) {
            }
            assertEquals(CircuitBreaker.State.OPEN, cb.getCurrentState());
            Thread.sleep(10);
            assertDoesNotThrow(() -> cb.execute(() -> "recovered"));
            assertEquals(CircuitBreaker.State.CLOSED, cb.getCurrentState());
        }
    }

    private void executeFailures(int count) {
        for (int i = 0; i < count; i++) {
            final int idx = i;
            try {
                breaker.execute(() -> {
                    throw new RuntimeException("fail " + idx);
                });
            } catch (Exception expected) {
            }
        }
    }
}
