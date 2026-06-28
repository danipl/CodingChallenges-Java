package com.danipl.platform.datastructures.bloomfilter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BloomFilter tests")
class BloomFilterTest {

    private BloomFilter<String> filter;

    @BeforeEach
    void setUp() {
        filter = BloomFilter.of(new BloomFilter.Config(1000, 0.01));
    }

    @Nested
    @DisplayName("Basic behavior")
    class BasicBehavior {

        @Test
        @DisplayName("initial state: size is 0 and mightContain returns false")
        void initialState() {
            // Given / When / Then
            assertEquals(0, filter.size());
            assertFalse(filter.mightContain("never-added"));
        }

        @Test
        @DisplayName("add and check: after add, mightContain returns true")
        void addAndCheck() {
            // Given
            String item = "test-item";

            // When
            filter.add(item);

            // Then
            assertTrue(filter.mightContain(item));
            assertEquals(1, filter.size());
        }

        @Test
        @DisplayName("multiple adds: all added items are found")
        void multipleAdds() {
            // Given
            String[] items = {"apple", "banana", "cherry", "date", "elderberry"};

            // When
            for (String item : items) {
                filter.add(item);
            }

            // Then
            for (String item : items) {
                assertTrue(filter.mightContain(item), "Should contain: " + item);
            }
            assertEquals(items.length, filter.size());
        }

        @Test
        @DisplayName("config is accessible and correct")
        void configAccessible() {
            // Given / When / Then
            BloomFilter.Config config = filter.config();
            assertEquals(1000, config.expectedInsertions());
            assertEquals(0.01, config.falsePositiveProbability());
            assertTrue(config.bitsetSize() > 0);
            assertTrue(config.numHashFunctions() > 0);
        }

        @Test
        @DisplayName("clear resets state")
        void clearResetsState() {
            // Given
            filter.add("item1");
            filter.add("item2");
            assertEquals(2, filter.size());

            // When
            filter.clear();

            // Then
            assertEquals(0, filter.size());
            assertFalse(filter.mightContain("item1"));
            assertFalse(filter.mightContain("item2"));
        }

        @Test
        @DisplayName("expectedFalsePositiveProbability starts at configured value")
        void initialFalsePositiveProbability() {
            // Given / When / Then
            double fpp = filter.expectedFalsePositiveProbability();
            // Should be close to 0.01 (allowing for rounding)
            assertTrue(fpp >= 0.0 && fpp <= 0.02, "Initial FPP should be near configured 0.01");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("null item throws NullPointerException on add")
        void nullItemAdd() {
            // Given / When / Then
            assertThrows(NullPointerException.class, () -> filter.add(null));
        }

        @Test
        @DisplayName("null item throws NullPointerException on mightContain")
        void nullItemMightContain() {
            // Given / When / Then
            assertThrows(NullPointerException.class, () -> filter.mightContain(null));
        }

        @Test
        @DisplayName("config validation: expectedInsertions < 1 throws")
        void configValidationExpectedInsertions() {
            // Given / When / Then
            assertThrows(IllegalArgumentException.class, () ->
                    new BloomFilter.Config(0, 0.01));
            assertThrows(IllegalArgumentException.class, () ->
                    new BloomFilter.Config(-1, 0.01));
        }

        @Test
        @DisplayName("config validation: falsePositiveProbability out of range throws")
        void configValidationFalsePositiveProbability() {
            // Given / When / Then
            assertThrows(IllegalArgumentException.class, () ->
                    new BloomFilter.Config(100, 0.0));
            assertThrows(IllegalArgumentException.class, () ->
                    new BloomFilter.Config(100, 1.0));
            assertThrows(IllegalArgumentException.class, () ->
                    new BloomFilter.Config(100, -0.1));
            assertThrows(IllegalArgumentException.class, () ->
                    new BloomFilter.Config(100, 1.5));
        }

        @Test
        @DisplayName("adding same item multiple times increments size each time")
        void addSameItemMultipleTimes() {
            // Given
            String item = "duplicate";

            // When
            filter.add(item);
            filter.add(item);
            filter.add(item);

            // Then
            assertEquals(3, filter.size());
            assertTrue(filter.mightContain(item));
        }

        @Test
        @DisplayName("mightContain for item never added may return false (no false negatives)")
        void mightContainNeverAdded() {
            // Given
            filter.add("item1");
            filter.add("item2");

            // When / Then
            // Items not added should mostly return false (though false positives are possible)
            // We can't assert false with certainty, but we can assert no exceptions
            assertDoesNotThrow(() -> filter.mightContain("never-added-1"));
            assertDoesNotThrow(() -> filter.mightContain("never-added-2"));
        }
    }

    @Nested
    @DisplayName("Thread safety")
    class ThreadSafety {

        @Test
        @DisplayName("concurrent adds are safe")
        void concurrentAdds() throws InterruptedException {
            int threadCount = 20;
            int opsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < opsPerThread; i++) {
                            filter.add("thread-" + threadId + "-item-" + i);
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

            // Verify: total insertions = threadCount * opsPerThread
            assertEquals(threadCount * opsPerThread, filter.size());
        }

        @Test
        @DisplayName("concurrent reads and writes are safe")
        void concurrentReadsAndWrites() throws InterruptedException {
            int writerCount = 10;
            int readerCount = 10;
            int opsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(writerCount + readerCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(writerCount + readerCount);
            AtomicInteger readCount = new AtomicInteger(0);

            // Writers
            for (int t = 0; t < writerCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < opsPerThread; i++) {
                            filter.add("writer-" + threadId + "-item-" + i);
                        }
                    } catch (InterruptedException ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }

            // Readers
            for (int t = 0; t < readerCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < opsPerThread; i++) {
                            filter.mightContain("reader-" + threadId + "-item-" + i);
                            readCount.incrementAndGet();
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

            // Verify: writers completed, readers completed
            assertEquals(writerCount * opsPerThread, filter.size());
            assertEquals(readerCount * opsPerThread, readCount.get());
        }

        @Test
        @DisplayName("concurrent clear and add operations are safe")
        void concurrentClearAndAdd() throws InterruptedException {
            int threadCount = 10;
            int opsPerThread = 50;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < opsPerThread; i++) {
                            if (i % 10 == 0) {
                                filter.clear();
                            } else {
                                filter.add("thread-" + threadId + "-item-" + i);
                            }
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

            // Verify: no exceptions thrown, filter is in valid state
            assertDoesNotThrow(() -> filter.size());
            assertDoesNotThrow(() -> filter.mightContain("test"));
        }
    }
}
