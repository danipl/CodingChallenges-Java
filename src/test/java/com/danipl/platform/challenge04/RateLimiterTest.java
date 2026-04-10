package com.danipl.platform.challenge04;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    @Nested
    class BasicAcquire {

        @Test
        void basicAcquireReturnsTrueWhenTokensAvailable() {
            RateLimiter rl = RateLimiter.of(5, 1.0);
            assertTrue(rl.tryAcquire());
        }

        @Test
        void startsFull() {
            RateLimiter rl = RateLimiter.of(10, 1.0);
            assertEquals(10, rl.availableTokens());
        }

        @Test
        void acquireDecrementsTokens() {
            RateLimiter rl = RateLimiter.of(5, 0);
            rl.tryAcquire();
            assertEquals(4, rl.availableTokens());
        }

        @Test
        void burstConsumeAllTokens() {
            RateLimiter rl = RateLimiter.of(3, 0);
            assertTrue(rl.tryAcquire());
            assertTrue(rl.tryAcquire());
            assertTrue(rl.tryAcquire());
            assertFalse(rl.tryAcquire());
            assertEquals(0, rl.availableTokens());
        }

        @Test
        void emptyBucketRejects() {
            RateLimiter rl = RateLimiter.of(1, 0);
            rl.tryAcquire();
            assertFalse(rl.tryAcquire());
            assertEquals(0, rl.availableTokens());
        }

        @Test
        void acquireMultipleTokensAtOnce() {
            RateLimiter rl = RateLimiter.of(10, 0);
            assertTrue(rl.tryAcquire(5));
            assertEquals(5, rl.availableTokens());
            assertTrue(rl.tryAcquire(5));
            assertEquals(0, rl.availableTokens());
            assertFalse(rl.tryAcquire(1));
        }

        @Test
        void acquireMoreThanCapacityFails() {
            RateLimiter rl = RateLimiter.of(3, 0);
            assertFalse(rl.tryAcquire(4));
        }
    }

    @Nested
    class RefillOverTime {

        @Test
        void refillsOverTime() throws InterruptedException {
            RateLimiter rl = RateLimiter.of(2, 10.0);
            rl.tryAcquire();
            rl.tryAcquire();
            assertEquals(0, rl.availableTokens());
            Thread.sleep(200);
            assertTrue(rl.availableTokens() >= 1);
        }

        @Test
        void refillCapsAtMax() throws InterruptedException {
            RateLimiter rl = RateLimiter.of(2, 100.0);
            Thread.sleep(300);
            assertEquals(2, rl.availableTokens());
        }
    }

    @Nested
    class TimeoutAcquire {

        @Test
        void timeoutWaitsForToken() throws InterruptedException {
            RateLimiter rl = RateLimiter.of(1, 20.0);
            rl.tryAcquire();
            long start = System.currentTimeMillis();
            boolean result = rl.tryAcquire(500, TimeUnit.MILLISECONDS);
            long elapsed = System.currentTimeMillis() - start;
            assertTrue(result);
            assertTrue(elapsed < 500, "Should not wait full timeout if token arrives");
        }

        @Test
        void timeoutExpiresWithoutToken() throws InterruptedException {
            RateLimiter rl = RateLimiter.of(1, 0);
            rl.tryAcquire();
            boolean result = rl.tryAcquire(100, TimeUnit.MILLISECONDS);
            assertFalse(result);
        }
    }

    @Nested
    class ConcurrentAccess {

        @Test
        void concurrentAcquireManyThreads() throws InterruptedException {
            RateLimiter rl = RateLimiter.of(100, 0);
            int threads = 10;
            int perThread = 15;
            ExecutorService es = Executors.newFixedThreadPool(threads);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);
            AtomicInteger success = new AtomicInteger();
            AtomicInteger failure = new AtomicInteger();
            for (int t = 0; t < threads; t++) {
                es.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < perThread; i++) {
                            if (rl.tryAcquire()) success.incrementAndGet();
                            else failure.incrementAndGet();
                        }
                    } catch (Exception ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(10, TimeUnit.SECONDS));
            es.shutdown();
            assertEquals(100, success.get(), "Exactly capacity worth of tokens should succeed");
        }

        @Test
        void concurrentAcquireWithRefill() throws InterruptedException {
            RateLimiter rl = RateLimiter.of(10, 50.0);
            int threads = 5;
            ExecutorService es = Executors.newFixedThreadPool(threads);
            CountDownLatch latch = new CountDownLatch(threads);
            AtomicInteger total = new AtomicInteger();
            for (int t = 0; t < threads; t++) {
                es.submit(() -> {
                    try {
                        for (int i = 0; i < 20; i++) {
                            if (rl.tryAcquire()) total.incrementAndGet();
                            Thread.sleep(20);
                        }
                    } catch (Exception ignored) {
                    } finally {
                        latch.countDown();
                    }
                });
            }
            assertTrue(latch.await(10, TimeUnit.SECONDS));
            es.shutdown();
            assertTrue(total.get() > 10, "Refill should allow more than initial capacity");
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void capacityOneEdgeCase() {
            RateLimiter rl = RateLimiter.of(1, 0);
            assertEquals(1, rl.availableTokens());
            assertTrue(rl.tryAcquire());
            assertEquals(0, rl.availableTokens());
            assertFalse(rl.tryAcquire());
        }

        @Test
        void negativeTokensRejected() {
            RateLimiter rl = RateLimiter.of(5, 0);
            assertFalse(rl.tryAcquire(-1));
            assertEquals(5, rl.availableTokens());
        }

        @Test
        void acquireZeroTokensAlwaysSucceeds() {
            RateLimiter rl = RateLimiter.of(1, 0);
            rl.tryAcquire();
            assertTrue(rl.tryAcquire(0));
        }
    }
}
