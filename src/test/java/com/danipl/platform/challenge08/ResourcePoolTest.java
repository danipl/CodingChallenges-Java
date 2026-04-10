package com.danipl.platform.challenge08;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ResourcePool tests")
class ResourcePoolTest {

    private ResourcePool<PoolResource> pool;

    @BeforeEach
    void setUp() {
        pool = ResourcePool.of(3, PoolResource::new, 500);
    }

    @Nested
    @DisplayName("Basic acquire and release")
    class BasicAcquireRelease {

        @Test
        @DisplayName("acquire returns a healthy resource")
        void acquireReturnsHealthyResource() throws Exception {
            PoolResource resource = pool.acquire();
            assertNotNull(resource);
            assertTrue(resource.isHealthy());
            pool.release(resource);
        }

        @Test
        @DisplayName("acquire twice returns different resources")
        void acquireReturnsDifferentResources() throws Exception {
            PoolResource r1 = pool.acquire();
            PoolResource r2 = pool.acquire();
            assertNotSame(r1, r2);
            pool.release(r1);
            pool.release(r2);
        }

        @Test
        @DisplayName("released resource can be acquired again")
        void releasedResourceReused() throws Exception {
            PoolResource r1 = pool.acquire();
            int id1 = r1.getId().id();
            pool.release(r1);
            PoolResource r2 = pool.acquire();
            assertEquals(id1, r2.getId().id());
            pool.release(r2);
        }
    }

    @Nested
    @DisplayName("Pool exhaustion")
    class PoolExhaustion {

        @Test
        @DisplayName("acquire throws PoolExhaustedException when pool is empty and timeout expires")
        void exhaustThrowsException() throws Exception {
            PoolResource r1 = pool.acquire();
            PoolResource r2 = pool.acquire();
            PoolResource r3 = pool.acquire();

            assertThrows(PoolExhaustedException.class, () -> pool.acquire());

            pool.release(r1);
            pool.release(r2);
            pool.release(r3);
        }

        @Test
        @DisplayName("maxSize enforcement: only N resources exist at once")
        void maxSizeEnforced() throws Exception {
            List<PoolResource> resources = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                resources.add(pool.acquire());
            }
            assertEquals(3, pool.activeCount());
            assertEquals(0, pool.availableCount());

            for (PoolResource r : resources) {
                pool.release(r);
            }
        }
    }

    @Nested
    @DisplayName("Blocking wait then release unblocks")
    class BlockingWait {

        @Test
        @DisplayName("blocking acquire is unblocked when another thread releases")
        void releaseUnblocksWaitingThread() throws Exception {
            ResourcePool<PoolResource> fastTimeoutPool = ResourcePool.of(1, PoolResource::new, 2000);

            PoolResource r1 = fastTimeoutPool.acquire();

            ExecutorService executor = Executors.newSingleThreadExecutor();
            Future<PoolResource> future = executor.submit(() -> fastTimeoutPool.acquire());

            Thread.sleep(200);
            fastTimeoutPool.release(r1);

            PoolResource acquired = future.get(3, TimeUnit.SECONDS);
            assertNotNull(acquired);
            fastTimeoutPool.release(acquired);
            executor.shutdownNow();
        }
    }

    @Nested
    @DisplayName("Invalid resources")
    class InvalidResources {

        @Test
        @DisplayName("invalid resources are discarded, new resource created on next acquire")
        void invalidResourceDiscarded() throws Exception {
            AtomicIntegerFactory counterFactory = new AtomicIntegerFactory();
            ResourcePool<PoolResource> countedPool = ResourcePool.of(
                    1, counterFactory, 500
            );

            PoolResource r1 = countedPool.acquire();
            int id1 = r1.getId().id();
            r1.markInvalid();
            countedPool.release(r1);

            PoolResource r2 = countedPool.acquire();
            assertNotEquals(id1, r2.getId().id());
            countedPool.release(r2);
        }

        @Test
        @DisplayName("healthy resources are returned to pool for reuse")
        void healthyResourceReused() throws Exception {
            PoolResource r1 = pool.acquire();
            int id1 = r1.getId().id();
            pool.release(r1);
            PoolResource r2 = pool.acquire();
            assertEquals(id1, r2.getId().id());
            assertTrue(r2.isHealthy());
            pool.release(r2);
        }
    }

    @Nested
    @DisplayName("Pool shutdown")
    class PoolShutdown {

        @Test
        @DisplayName("close() makes subsequent acquire throw")
        void closePreventsAcquire() throws Exception {
            pool.close();
            assertThrows(Exception.class, () -> pool.acquire());
        }

        @Test
        @DisplayName("close() makes subsequent release throw")
        void closePreventsRelease() throws Exception {
            pool.close();
            assertThrows(Exception.class, () -> pool.release(new PoolResource(999)));
        }
    }

    @Nested
    @DisplayName("Concurrent acquire and release")
    class Concurrency {

        @Test
        @DisplayName("concurrent acquire/release from many threads is safe")
        void concurrentAcquireRelease() throws InterruptedException {
            int threadCount = 20;
            int opsPerThread = 50;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);
            AtomicInteger errors = new AtomicInteger(0);

            for (int t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < opsPerThread; i++) {
                            try {
                                PoolResource r = pool.acquire();
                                Thread.sleep(1);
                                pool.release(r);
                            } catch (PoolExhaustedException e) {
                                // expected under contention
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        errors.incrementAndGet();
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS), "All threads should complete");
            executor.shutdownNow();
            assertEquals(0, errors.get(), "No unexpected errors");
        }
    }

    @Nested
    @DisplayName("Counts")
    class Counts {

        @Test
        @DisplayName("availableCount and activeCount reflect pool state")
        void countsCorrect() throws Exception {
            assertEquals(0, pool.activeCount());
            PoolResource r1 = pool.acquire();
            assertEquals(1, pool.activeCount());
            pool.release(r1);
            assertEquals(0, pool.activeCount());
        }

        @Test
        @DisplayName("release doesn't exceed max pool size")
        void releaseDoesntExceedMax() throws Exception {
            PoolResource extra = new PoolResource(999);
            pool.release(extra);
            assertEquals(0, pool.activeCount());
        }
    }

    @Nested
    @DisplayName("Factory behavior")
    class FactoryBehavior {

        @Test
        @DisplayName("factory called only when pool needs new resource")
        void factoryCalledOnlyWhenNeeded() throws Exception {
            AtomicInteger callCount = new AtomicInteger(0);
            ResourcePool<PoolResource> countedPool = ResourcePool.of(
                    2,
                    id -> {
                        callCount.incrementAndGet();
                        return new PoolResource(id);
                    },
                    500
            );

            PoolResource r1 = countedPool.acquire();
            assertEquals(1, callCount.get());
            countedPool.release(r1);
            countedPool.acquire();
            assertEquals(1, callCount.get());
            countedPool.release(r1);
            countedPool.acquire();
            PoolResource r2 = countedPool.acquire();
            assertEquals(2, callCount.get());
            countedPool.release(r1);
            countedPool.release(r2);
        }
    }

    private static class AtomicIntegerFactory implements java.util.function.Function<Integer, PoolResource> {
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public PoolResource apply(Integer id) {
            return new PoolResource(counter.incrementAndGet());
        }
    }
}
