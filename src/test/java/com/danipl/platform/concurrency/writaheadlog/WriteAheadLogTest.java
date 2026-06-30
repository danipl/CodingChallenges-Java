package com.danipl.platform.concurrency.writaheadlog;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("WriteAheadLog tests")
class WriteAheadLogTest {

    private WriteAheadLog wal;

    @BeforeEach
    void setUp() {
        wal = WriteAheadLog.of(new WriteAheadLog.Config(0));
    }

    @Nested
    @DisplayName("Basic behavior")
    class BasicBehavior {

        @Test
        @DisplayName("empty log has size 0 and lastSequenceNumber 0")
        void emptyLogState() {
            assertEquals(0, wal.size());
            assertEquals(0, wal.lastSequenceNumber());
            assertEquals(0, wal.snapshotSequenceNumber());
            assertTrue(wal.entries().isEmpty());
        }

        @Test
        @DisplayName("single append assigns sequence number 1")
        void singleAppend() {
            WriteAheadLog.LogEntry entry = wal.append("first");
            assertEquals(1, entry.sequenceNumber());
            assertEquals("first", entry.record());
            assertEquals(1, wal.lastSequenceNumber());
            assertEquals(1, wal.size());
        }

        @Test
        @DisplayName("multiple appends get monotonically increasing sequence numbers")
        void multipleAppends() {
            wal.append("a");
            wal.append("b");
            WriteAheadLog.LogEntry third = wal.append("c");

            assertEquals(3, third.sequenceNumber());
            assertEquals(3, wal.lastSequenceNumber());
            assertEquals(3, wal.size());
            assertEquals(List.of(1L, 2L, 3L), wal.entries().stream().map(WriteAheadLog.LogEntry::sequenceNumber).toList());
        }

        @Test
        @DisplayName("markSnapshot sets snapshot sequence number")
        void markSnapshot() {
            wal.append("a");
            wal.append("b");
            wal.append("c");
            wal.markSnapshot(2);

            assertEquals(2, wal.snapshotSequenceNumber());
        }

        @Test
        @DisplayName("recoverFromSnapshot returns entries from snapshot onward inclusive")
        void recoverFromSnapshot() {
            wal.append("a");
            wal.append("b");
            wal.append("c");
            wal.append("d");
            wal.markSnapshot(2);

            List<WriteAheadLog.LogEntry> recovered = wal.recoverFromSnapshot();
            assertEquals(3, recovered.size());
            assertEquals(2, recovered.get(0).sequenceNumber());
            assertEquals("b", recovered.get(0).record());
            assertEquals(4, recovered.get(2).sequenceNumber());
        }

        @Test
        @DisplayName("recoverFromSnapshot with no snapshot returns all entries")
        void recoverFromSnapshotNoSnapshot() {
            wal.append("a");
            wal.append("b");

            List<WriteAheadLog.LogEntry> recovered = wal.recoverFromSnapshot();
            assertEquals(2, recovered.size());
            assertEquals(1, recovered.get(0).sequenceNumber());
        }

        @Test
        @DisplayName("truncateBeforeSnapshot removes entries before snapshot")
        void truncateBeforeSnapshot() {
            wal.append("a");
            wal.append("b");
            wal.append("c");
            wal.append("d");
            wal.markSnapshot(3);
            wal.truncateBeforeSnapshot();

            assertEquals(2, wal.size());
            assertEquals(3, wal.lastSequenceNumber()); // last seq num unchanged
            List<WriteAheadLog.LogEntry> remaining = wal.entries();
            assertEquals(3, remaining.get(0).sequenceNumber());
            assertEquals("c", remaining.get(0).record());
        }

        @Test
        @DisplayName("truncateBeforeSnapshot with no snapshot is no-op")
        void truncateNoSnapshot() {
            wal.append("a");
            wal.append("b");
            wal.truncateBeforeSnapshot();

            assertEquals(2, wal.size());
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("append rejects null record")
        void appendNullRecord() {
            assertThrows(IllegalArgumentException.class, () -> wal.append(null));
        }

        @Test
        @DisplayName("append rejects blank record")
        void appendBlankRecord() {
            assertThrows(IllegalArgumentException.class, () -> wal.append("   "));
            assertThrows(IllegalArgumentException.class, () -> wal.append(""));
        }

        @Test
        @DisplayName("markSnapshot rejects non-existent sequence number")
        void markSnapshotNonExistent() {
            wal.append("a");
            assertThrows(IllegalArgumentException.class, () -> wal.markSnapshot(0));
            assertThrows(IllegalArgumentException.class, () -> wal.markSnapshot(2));
            assertThrows(IllegalArgumentException.class, () -> wal.markSnapshot(999));
        }

        @Test
        @DisplayName("markSnapshot rejects negative sequence number")
        void markSnapshotNegative() {
            assertThrows(IllegalArgumentException.class, () -> wal.markSnapshot(-1));
        }

        @Test
        @DisplayName("config validation rejects negative maxEntries")
        void configValidationRejectsInvalid() {
            assertThrows(IllegalArgumentException.class, () ->
                WriteAheadLog.of(new WriteAheadLog.Config(-1)));
        }

        @Test
        @DisplayName("config allows maxEntries = 0 (unlimited)")
        void configAllowsZeroMaxEntries() {
            assertDoesNotThrow(() -> WriteAheadLog.of(new WriteAheadLog.Config(0)));
        }

        @Test
        @DisplayName("recoverFromSnapshot on empty log returns empty list")
        void recoverFromEmptyLog() {
            assertTrue(wal.recoverFromSnapshot().isEmpty());
        }

        @Test
        @DisplayName("entries returns unmodifiable list")
        void entriesUnmodifiable() {
            wal.append("a");
            List<WriteAheadLog.LogEntry> entries = wal.entries();
            assertThrows(UnsupportedOperationException.class, () ->
                entries.add(new WriteAheadLog.LogEntry(99, "hack")));
        }

        @Test
        @DisplayName("snapshot at first entry recovers all entries")
        void snapshotAtFirstEntry() {
            wal.append("a");
            wal.append("b");
            wal.markSnapshot(1);

            List<WriteAheadLog.LogEntry> recovered = wal.recoverFromSnapshot();
            assertEquals(2, recovered.size());
            assertEquals(1, recovered.get(0).sequenceNumber());
        }

        @Test
        @DisplayName("snapshot at last entry recovers only last entry")
        void snapshotAtLastEntry() {
            wal.append("a");
            wal.append("b");
            wal.append("c");
            wal.markSnapshot(3);

            List<WriteAheadLog.LogEntry> recovered = wal.recoverFromSnapshot();
            assertEquals(1, recovered.size());
            assertEquals(3, recovered.get(0).sequenceNumber());
        }

        @Test
        @DisplayName("moving snapshot forward updates snapshot sequence number")
        void moveSnapshotForward() {
            wal.append("a");
            wal.append("b");
            wal.append("c");
            wal.markSnapshot(1);
            assertEquals(1, wal.snapshotSequenceNumber());

            wal.markSnapshot(2);
            assertEquals(2, wal.snapshotSequenceNumber());
        }
    }

    @Nested
    @DisplayName("Thread safety")
    class ThreadSafety {

        @Test
        @DisplayName("concurrent appends preserve sequence number monotonicity")
        void concurrentAppends() throws InterruptedException {
            int threadCount = 20;
            int appendsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);
            AtomicInteger totalAppended = new AtomicInteger(0);

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < appendsPerThread; i++) {
                            wal.append("thread-" + threadId + "-entry-" + i);
                            totalAppended.incrementAndGet();
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

            assertEquals(threadCount * appendsPerThread, totalAppended.get());
            assertEquals(threadCount * appendsPerThread, wal.size());
            assertEquals(threadCount * appendsPerThread, wal.lastSequenceNumber());

            // Verify monotonicity: each entry's seq num = previous + 1
            List<WriteAheadLog.LogEntry> entries = wal.entries();
            for (int i = 1; i < entries.size(); i++) {
                assertEquals(entries.get(i - 1).sequenceNumber() + 1, entries.get(i).sequenceNumber(),
                    "Sequence numbers must be monotonically increasing");
            }
        }

        @Test
        @DisplayName("concurrent reads and writes are safe")
        void concurrentReadsAndWrites() throws InterruptedException {
            int writerCount = 5;
            int readerCount = 10;
            int opsPerThread = 100;
            ExecutorService executor = Executors.newFixedThreadPool(writerCount + readerCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(writerCount + readerCount);
            AtomicInteger writeCount = new AtomicInteger(0);
            AtomicInteger readCount = new AtomicInteger(0);

            // Writers
            for (int t = 0; t < writerCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < opsPerThread; i++) {
                            wal.append("writer-" + threadId + "-entry-" + i);
                            writeCount.incrementAndGet();
                        }
                    } catch (InterruptedException ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }

            // Readers
            for (int t = 0; t < readerCount; t++) {
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < opsPerThread; i++) {
                            wal.entries();
                            wal.size();
                            wal.lastSequenceNumber();
                            wal.recoverFromSnapshot();
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

            assertEquals(writerCount * opsPerThread, writeCount.get());
            assertEquals(readerCount * opsPerThread, readCount.get());
            assertEquals(writerCount * opsPerThread, wal.size());
        }

        @Test
        @DisplayName("concurrent markSnapshot and truncate are safe")
        void concurrentSnapshotAndTruncate() throws InterruptedException {
            int threadCount = 10;
            int opsPerThread = 50;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);

            // Pre-populate log
            for (int i = 0; i < 100; i++) {
                wal.append("initial-" + i);
            }

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < opsPerThread; i++) {
                            // Append new entries
                            WriteAheadLog.LogEntry entry = wal.append("thread-" + threadId + "-entry-" + i);
                            // Mark snapshot at this entry
                            wal.markSnapshot(entry.sequenceNumber());
                            // Truncate
                            wal.truncateBeforeSnapshot();
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

            // Verify consistency: all remaining entries have seq num >= snapshot
            long snapshotSeq = wal.snapshotSequenceNumber();
            if (snapshotSeq > 0) {
                for (WriteAheadLog.LogEntry entry : wal.entries()) {
                    assertTrue(entry.sequenceNumber() >= snapshotSeq,
                        "All entries must be >= snapshot sequence number");
                }
            }
        }
    }
}
