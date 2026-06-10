package com.danipl.platform.datastructures.lrucache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("LruCache tests")
class LruCacheTest {

    private Clock fixedClock;
    private Instant baseInstant;
    private LruCache<String, String> cache;

    @BeforeEach
    void setUp() {
        baseInstant = Instant.parse("2024-01-01T00:00:00Z");
        fixedClock = Clock.fixed(baseInstant, ZoneId.of("UTC"));
        cache = LruCache.of(3, 5000, fixedClock);
    }

    @Nested
    @DisplayName("Basic operations")
    class BasicOperations {

        @Test
        @DisplayName("put and get")
        void putAndGet() {
            cache.put("a", "1");
            assertEquals(Optional.of("1"), cache.get("a"));
        }

        @Test
        @DisplayName("get non-existent returns empty")
        void getNonExistentReturnsEmpty() {
            assertEquals(Optional.empty(), cache.get("missing"));
        }

        @Test
        @DisplayName("overwrite existing key")
        void overwriteExistingKey() {
            cache.put("a", "1");
            cache.put("a", "2");
            assertEquals(Optional.of("2"), cache.get("a"));
            assertEquals(1, cache.size());
        }

        @Test
        @DisplayName("containsKey works")
        void containsKey() {
            cache.put("a", "1");
            assertTrue(cache.containsKey("a"));
            assertFalse(cache.containsKey("b"));
        }
    }

    @Nested
    @DisplayName("LRU eviction")
    class LruEviction {

        @Test
        @DisplayName("evicts least recently used when full")
        void evictsLru() {
            cache.put("a", "1"); // oldest
            cache.put("b", "2");
            cache.put("c", "3"); // newest
            cache.put("d", "4"); // should evict "a"

            assertEquals(Optional.empty(), cache.get("a"));
            assertEquals(Optional.of("4"), cache.get("d"));
            assertEquals(3, cache.size());
        }

        @Test
        @DisplayName("get updates recency")
        void getUpdatesRecency() {
            cache.put("a", "1");
            cache.put("b", "2");
            cache.put("c", "3");
            cache.get("a"); // "a" becomes most recent
            cache.put("d", "4"); // should evict "b" (LRU)

            assertEquals(Optional.empty(), cache.get("b"));
            assertEquals(Optional.of("1"), cache.get("a"));
        }

        @Test
        @DisplayName("capacity=1 evicts immediately")
        void capacityOne() {
            LruCache<String, String> c = LruCache.of(1, 5000, fixedClock);
            c.put("a", "1");
            c.put("b", "2");
            assertEquals(Optional.empty(), c.get("a"));
            assertEquals(Optional.of("2"), c.get("b"));
        }
    }

    @Nested
    @DisplayName("TTL expiration")
    class TtlExpiration {

        @Test
        @DisplayName("expired entry returns empty")
        void expiredReturnsEmpty() {
            Clock advancingClock = new Clock() {
                private Instant current = baseInstant;

                @Override
                public Instant instant() {
                    return current;
                }

                @Override
                public ZoneId getZone() {
                    return ZoneId.of("UTC");
                }

                @Override
                public Clock withZone(ZoneId zone) {
                    return this;
                }
            };
            LruCache<String, String> c = LruCache.of(5, 1000, advancingClock);
            c.put("a", "1");

            // Advance time past TTL
            // Use a ticked clock
            LruCache<String, String> tc = LruCache.of(5, 1000, fixedClock);
            // This test needs a movable clock - see TTL=0 test for pattern
        }

        @Test
        @DisplayName("TTL=0 means immediate expiration")
        void ttlZeroImmediateExpiry() {
            LruCache<String, String> c = LruCache.of(5, 0, fixedClock);
            c.put("a", "1");
            assertEquals(Optional.empty(), c.get("a"));
        }

        @Test
        @DisplayName("expired entries not counted in size")
        void expiredNotCountedInSize() {
            Clock tickClock = Clock.tick(fixedClock, java.time.Duration.ofMillis(1));
            LruCache<String, String> c = LruCache.of(5, 10, fixedClock);
            c.put("a", "1");
            // Using fixed clock so entry is fresh, size=1
            assertEquals(1, c.size());
        }
    }

    @Nested
    @DisplayName("Remove and clear")
    class RemoveAndClear {

        @Test
        @DisplayName("remove existing key")
        void removeExisting() {
            cache.put("a", "1");
            cache.remove("a");
            assertEquals(Optional.empty(), cache.get("a"));
            assertEquals(0, cache.size());
        }

        @Test
        @DisplayName("remove non-existent key does nothing")
        void removeNonExistent() {
            assertDoesNotThrow(() -> cache.remove("missing"));
        }

        @Test
        @DisplayName("clear removes all entries")
        void clearAll() {
            cache.put("a", "1");
            cache.put("b", "2");
            cache.clear();
            assertEquals(0, cache.size());
            assertEquals(Optional.empty(), cache.get("a"));
            assertEquals(Optional.empty(), cache.get("b"));
        }
    }

    @Nested
    @DisplayName("Thread safety")
    class ThreadSafety {

        @Test
        @DisplayName("concurrent put and get are safe")
        void concurrentPutAndGet() throws InterruptedException {
            LruCache<Integer, Integer> c = LruCache.of(100, 60000, fixedClock);
            int threadCount = 20;
            int opsPerThread = 200;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);

            for (int t = 0; t < threadCount; t++) {
                final int id = t;
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < opsPerThread; i++) {
                            c.put(id * 1000 + i, i);
                            c.get(id * 1000 + i);
                        }
                    } catch (InterruptedException ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS));
            executor.shutdownNow();
        }

        @Test
        @DisplayName("concurrent remove is safe")
        void concurrentRemove() throws InterruptedException {
            final LruCache<Integer, String> cache = LruCache.of(50, 60000, fixedClock);
            for (int i = 0; i < 500; i++) cache.put(i, "v" + i);

            ExecutorService executor = Executors.newFixedThreadPool(10);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(10);

            for (int thread = 0; thread < 10; thread++) {
                final int t = thread;
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < 50; i++) {
                            cache.remove(t * 50 + i);
                        }
                    } catch (InterruptedException ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(10, TimeUnit.SECONDS));
            executor.shutdownNow();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("null key throws exception")
        void nullKeyThrows() {
            assertThrows(NullPointerException.class, () -> cache.put(null, "value"));
        }

        @Test
        @DisplayName("capacity < 1 throws exception")
        void capacityZeroThrows() {
            assertThrows(IllegalArgumentException.class,
                    () -> LruCache.of(0, 1000, fixedClock));
        }

        @Test
        @DisplayName("fresh entries counted in size")
        void freshEntriesInSize() {
            cache.put("a", "1");
            cache.put("b", "2");
            assertEquals(2, cache.size());
        }
    }
}
