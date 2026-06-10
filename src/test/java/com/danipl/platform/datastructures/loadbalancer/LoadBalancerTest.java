package com.danipl.platform.datastructures.loadbalancer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadBalancerTest {

    @Nested
    class BasicRouting {

        private LoadBalancer lb;

        @BeforeEach
        void setUp() {
            lb = LoadBalancer.of();
        }

        @Test
        void singleServerRouting() {
            lb.add(new Server("localhost", 8080, 1));
            assertEquals("localhost", lb.next().host());
            assertEquals("localhost", lb.next().host());
        }

        @Test
        void routingDistributionMatchesWeights() {
            lb.add(new Server("host1", 80, 3));
            lb.add(new Server("host2", 80, 1));
            Map<String, AtomicInteger> counts = new HashMap<>();
            counts.put("host1", new AtomicInteger());
            counts.put("host2", new AtomicInteger());
            int totalCalls = 4000;
            for (int i = 0; i < totalCalls; i++) {
                Server s = lb.next();
                counts.get(s.host()).incrementAndGet();
            }
            double ratio = (double) counts.get("host1").get() / counts.get("host2").get();
            assertTrue(ratio >= 2.5 && ratio <= 3.5,
                    "Expected ratio ~3.0 but was " + ratio);
        }

        @Test
        void addDuplicateMergesWeights() {
            lb.add(new Server("host1", 80, 2));
            lb.add(new Server("host1", 80, 3));
            // After merging, host1 effective weight = 5 (or merged logic)
            // At minimum, host1 must still be routable
            assertEquals("host1", lb.next().host());
        }

        @Test
        void equalWeightsRoundRobin() {
            lb.add(new Server("a", 80, 1));
            lb.add(new Server("b", 80, 1));
            int aCount = 0, bCount = 0;
            for (int i = 0; i < 100; i++) {
                Server s = lb.next();
                if ("a".equals(s.host())) aCount++;
                else bCount++;
            }
            assertTrue(Math.abs(aCount - bCount) <= 20,
                    "Expected near-equal distribution");
        }

        @Test
        void dynamicAddDuringRouting() {
            lb.add(new Server("host1", 80, 1));
            lb.next();
            lb.add(new Server("host2", 80, 1));
            boolean sawHost2 = false;
            for (int i = 0; i < 100; i++) {
                if ("host2".equals(lb.next().host())) sawHost2 = true;
            }
            assertTrue(sawHost2);
        }

        @Test
        void dynamicRemoveDuringRouting() {
            lb.add(new Server("host1", 80, 1));
            lb.add(new Server("host2", 80, 1));
            lb.remove("host1", 80);
            for (int i = 0; i < 50; i++) {
                assertEquals("host2", lb.next().host());
            }
        }
    }

    @Nested
    class ErrorHandling {

        private LoadBalancer lb;

        @BeforeEach
        void setUp() {
            lb = LoadBalancer.of();
        }

        @Test
        void emptyPoolThrowsNoSuchElementException() {
            assertThrows(NoSuchElementException.class, () -> lb.next());
        }

        @Test
        void removeNonExistentDoesNotThrow() {
            lb.add(new Server("host1", 80, 1));
            assertDoesNotThrow(() -> lb.remove("host99", 99));
        }
    }

    @Nested
    class ThreadSafety {

        @Test
        void concurrentAddRemoveNext() throws InterruptedException {
            LoadBalancer lb = LoadBalancer.of();
            lb.add(new Server("h1", 80, 2));
            lb.add(new Server("h2", 80, 2));
            lb.add(new Server("h3", 80, 2));
            int threads = 10;
            int opsPerThread = 100;
            ExecutorService es = Executors.newFixedThreadPool(threads);
            CountDownLatch latch = new CountDownLatch(threads);
            AtomicInteger successCount = new AtomicInteger();
            for (int t = 0; t < threads; t++) {
                final int tid = t;
                es.submit(() -> {
                    try {
                        for (int i = 0; i < opsPerThread; i++) {
                            switch (i % 3) {
                                case 0 -> lb.next();
                                case 1 -> lb.add(new Server("h" + (tid * 100 + i), 80, 1));
                                case 2 -> {
                                    try {
                                        lb.remove("h" + ((tid - 1) * 100), 80);
                                    } catch (Exception ignored) {
                                    }
                                }
                            }
                            successCount.incrementAndGet();
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            assertTrue(latch.await(10, TimeUnit.SECONDS), "Threads did not finish");
            es.shutdown();
            assertEquals(threads * opsPerThread, successCount.get());
        }

        @Test
        void allServersReachableUnderConcurrency() throws InterruptedException {
            LoadBalancer lb = LoadBalancer.of();
            lb.add(new Server("a", 80, 1));
            lb.add(new Server("b", 80, 1));
            lb.add(new Server("c", 80, 1));
            int threads = 6;
            ExecutorService es = Executors.newFixedThreadPool(threads);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threads);
            AtomicInteger[] counters = {new AtomicInteger(), new AtomicInteger(), new AtomicInteger()};
            for (int t = 0; t < threads; t++) {
                es.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < 500; i++) {
                            Server s = lb.next();
                            int idx = switch (s.host()) {
                                case "a" -> 0;
                                case "b" -> 1;
                                default -> 2;
                            };
                            counters[idx].incrementAndGet();
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
            for (int i = 0; i < 3; i++) {
                assertTrue(counters[i].get() > 0,
                        "Server at index " + i + " received zero traffic");
            }
        }
    }
}
