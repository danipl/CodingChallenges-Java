package com.danipl.platform.challenge05;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("MetricsAggregator tests")
class MetricsAggregatorTest {

    private MetricsAggregator aggregator;

    @BeforeEach
    void setUp() {
        aggregator = MetricsAggregator.of();
    }

    @Nested
    @DisplayName("Basic ingestion")
    class BasicIngestion {

        @Test
        @DisplayName("single entry is counted")
        void singleEntryCounted() {
            long now = 1_000_000L;
            aggregator.ingest(new LogEntry(now, LogLevel.INFO, "auth-service", 100));

            assertEquals(1, aggregator.getTotalEntriesLastMinute(now));
        }

        @Test
        @DisplayName("mixed log levels are all ingested")
        void mixedLevelsAllIngested() {
            long now = 1_000_000L;
            aggregator.ingest(new LogEntry(now, LogLevel.TRACE, "svc", 10));
            aggregator.ingest(new LogEntry(now, LogLevel.DEBUG, "svc", 20));
            aggregator.ingest(new LogEntry(now, LogLevel.INFO, "svc", 30));
            aggregator.ingest(new LogEntry(now, LogLevel.WARN, "svc", 40));
            aggregator.ingest(new LogEntry(now, LogLevel.ERROR, "svc", 50));
            aggregator.ingest(new LogEntry(now, LogLevel.FATAL, "svc", 60));

            assertEquals(6, aggregator.getTotalEntriesLastMinute(now));
        }
    }

    @Nested
    @DisplayName("Error rate calculation")
    class ErrorRate {

        @Test
        @DisplayName("no errors returns 0.0")
        void noErrorsReturnsZero() {
            long now = 1_000_000L;
            aggregator.ingest(new LogEntry(now, LogLevel.INFO, "svc", 100));
            aggregator.ingest(new LogEntry(now, LogLevel.WARN, "svc", 200));

            assertEquals(0.0, aggregator.getErrorRateLastMinute(now));
        }

        @Test
        @DisplayName("all errors returns 1.0")
        void allErrorsReturnsOne() {
            long now = 1_000_000L;
            aggregator.ingest(new LogEntry(now, LogLevel.ERROR, "svc", 100));
            aggregator.ingest(new LogEntry(now, LogLevel.FATAL, "svc", 200));

            assertEquals(1.0, aggregator.getErrorRateLastMinute(now));
        }

        @Test
        @DisplayName("mixed errors returns correct ratio")
        void mixedReturnsCorrectRatio() {
            long now = 1_000_000L;
            aggregator.ingest(new LogEntry(now, LogLevel.INFO, "svc", 100));
            aggregator.ingest(new LogEntry(now, LogLevel.INFO, "svc", 110));
            aggregator.ingest(new LogEntry(now, LogLevel.ERROR, "svc", 120));

            assertEquals(1.0 / 3.0, aggregator.getErrorRateLastMinute(now), 0.001);
        }

        @Test
        @DisplayName("empty state returns 0.0")
        void emptyStateReturnsZero() {
            assertEquals(0.0, aggregator.getErrorRateLastMinute(1_000_000L));
        }
    }

    @Nested
    @DisplayName("P95 percentile")
    class P95Percentile {

        @Test
        @DisplayName("single entry P95 is that entry's response time")
        void singleEntryP95() {
            long now = 1_000_000L;
            aggregator.ingest(new LogEntry(now, LogLevel.INFO, "svc", 150));

            assertEquals(150, aggregator.getP95ResponseTimeLastMinute(now));
        }

        @Test
        @DisplayName("P95 for 20 entries returns correct value")
        void p95OfTwentyEntries() {
            long now = 1_000_000L;
            for (int i = 1; i <= 20; i++) {
                aggregator.ingest(new LogEntry(now, LogLevel.INFO, "svc", i * 10));
            }
            // P95 of 20 items: index = ceil(0.95 * 20) = 19 => 19*10 = 190
            assertEquals(190, aggregator.getP95ResponseTimeLastMinute(now));
        }

        @Test
        @DisplayName("empty state returns 0")
        void emptyP95ReturnsZero() {
            assertEquals(0, aggregator.getP95ResponseTimeLastMinute(1_000_000L));
        }
    }

    @Nested
    @DisplayName("Sliding window")
    class SlidingWindow {

        @Test
        @DisplayName("entries outside 1-minute window are excluded")
        void oldEntriesExcluded() {
            long oldTime = 1_000_000L;
            long recentTime = 1_050_000L;
            aggregator.ingest(new LogEntry(oldTime, LogLevel.ERROR, "svc", 100));
            aggregator.ingest(new LogEntry(recentTime, LogLevel.INFO, "svc", 200));

            // Query at time that excludes oldTime (more than 60s apart)
            long queryTime = 1_061_000L;
            assertEquals(1, aggregator.getTotalEntriesLastMinute(queryTime));
            assertEquals(0, aggregator.getErrorCountLastMinute(queryTime));
            assertEquals(0.0, aggregator.getErrorRateLastMinute(queryTime));
        }

        @Test
        @DisplayName("entries at exact boundary are included")
        void boundaryEntriesIncluded() {
            long boundaryTime = 1_000_000L;
            long queryTime = 1_060_000L; // exactly 60s later
            aggregator.ingest(new LogEntry(boundaryTime, LogLevel.ERROR, "svc", 100));

            assertEquals(1, aggregator.getTotalEntriesLastMinute(queryTime));
        }

        @Test
        @DisplayName("error count decreases as window slides past error entries")
        void errorCountDecreasesAsWindowSlides() {
            long t0 = 1_000_000L;
            aggregator.ingest(new LogEntry(t0, LogLevel.ERROR, "svc", 100));
            aggregator.ingest(new LogEntry(t0, LogLevel.ERROR, "svc", 110));
            aggregator.ingest(new LogEntry(t0 + 30_000, LogLevel.INFO, "svc", 120));

            assertEquals(2, aggregator.getErrorCountLastMinute(t0 + 30_000));
            assertEquals(0, aggregator.getErrorCountLastMinute(t0 + 61_000));
        }
    }

    @Nested
    @DisplayName("Thread safety")
    class ThreadSafety {

        @Test
        @DisplayName("concurrent ingestion from many threads is safe")
        void concurrentIngestionIsSafe() throws InterruptedException {
            int threadCount = 20;
            int entriesPerThread = 500;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);
            long baseTime = 1_000_000L;

            for (int t = 0; t < threadCount; t++) {
                final int threadIdx = t;
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < entriesPerThread; i++) {
                            LogLevel level = (threadIdx + i) % 3 == 0 ? LogLevel.ERROR : LogLevel.INFO;
                            aggregator.ingest(new LogEntry(baseTime, level, "svc-" + threadIdx, 50 + i));
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

            int expected = threadCount * entriesPerThread;
            assertEquals(expected, aggregator.getTotalEntriesLastMinute(baseTime));
        }

        @Test
        @DisplayName("concurrent reads and writes are safe")
        void concurrentReadsAndWrites() throws InterruptedException {
            ExecutorService executor = Executors.newFixedThreadPool(10);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch writers = new CountDownLatch(5);
            CountDownLatch readers = new CountDownLatch(5);
            long baseTime = 2_000_000L;

            for (int w = 0; w < 5; w++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < 200; i++) {
                            aggregator.ingest(new LogEntry(baseTime, LogLevel.INFO, "svc", 100));
                        }
                    } catch (InterruptedException ignored) {
                    } finally {
                        writers.countDown();
                    }
                });
            }

            for (int r = 0; r < 5; r++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < 200; i++) {
                            aggregator.getTotalEntriesLastMinute(baseTime);
                            aggregator.getErrorRateLastMinute(baseTime);
                            aggregator.getP95ResponseTimeLastMinute(baseTime);
                        }
                    } catch (InterruptedException ignored) {
                    } finally {
                        readers.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(writers.await(10, TimeUnit.SECONDS));
            assertTrue(readers.await(10, TimeUnit.SECONDS));
            executor.shutdownNow();
        }
    }

    @Nested
    @DisplayName("Large dataset")
    class LargeDataset {

        @Test
        @DisplayName("handles 10000 entries correctly")
        void largeDatasetCorrect() {
            long now = 5_000_000L;
            for (int i = 0; i < 10_000; i++) {
                LogLevel level = i % 10 == 0 ? LogLevel.ERROR : LogLevel.INFO;
                aggregator.ingest(new LogEntry(now, level, "svc", i % 1000));
            }

            assertEquals(10_000, aggregator.getTotalEntriesLastMinute(now));
            assertEquals(1_000, aggregator.getErrorCountLastMinute(now));
            assertEquals(0.1, aggregator.getErrorRateLastMinute(now), 0.001);
        }
    }
}
