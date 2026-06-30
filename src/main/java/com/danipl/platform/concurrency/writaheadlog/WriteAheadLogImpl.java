package com.danipl.platform.concurrency.writaheadlog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.Collections.binarySearch;
import static java.util.Collections.unmodifiableList;

/**
 * Implementation of {@link WriteAheadLog}.
 * <p>
 * Thread-safety: All state mutations protected by ReentrantReadWriteLock.
 * Reads (entries, recoverFromSnapshot, size, lastSequenceNumber, snapshotSequenceNumber)
 * acquire the read lock; writes (append, markSnapshot, truncateBeforeSnapshot) acquire the write lock.
 */
public final class WriteAheadLogImpl implements WriteAheadLog {

    // === Fields ===
    private final Config config;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    // State: use ArrayList<LogEntry> for ordered append + index-based snapshot lookup
    private List<LogEntry> entries = new ArrayList<>();
    private long nextSequenceNumber = 1;
    private long snapshotSeqNum = 0; // 0 = no snapshot

    // === Constructors ===

    public WriteAheadLogImpl(final Config config) {
        this.config = config;
    }

    // === Public methods — ALL throw UnsupportedOperationException ===

    @Override
    public LogEntry append(final String record) {
        if (record == null || record.isBlank()) {
            throw new IllegalArgumentException("Record cannot be null or empty");
        }
        this.writeLock.lock();
        try {
            if (this.config.maxEntries() != 0 && this.entries.size() >= this.config.maxEntries()) {
                throw new IllegalStateException("Max entries exceeded");
            }
            final LogEntry logEntry = new LogEntry(this.nextSequenceNumber, record);
            this.nextSequenceNumber++;
            this.entries.add(logEntry);
            return logEntry;
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public void markSnapshot(final long sequenceNumber) {
        this.writeLock.lock();
        try {
            if (this.snapshotSeqNum > sequenceNumber || sequenceNumber <= 0) {
                throw new IllegalArgumentException("Only newer sequenceNumber are accepted");
            }
            final int position = binarySearch(this.entries, new LogEntry(sequenceNumber, "ignored"),
                    (curr, candidate) -> Long.compare(curr.sequenceNumber(), candidate.sequenceNumber())
            );
            if (position < 0) {
                throw new IllegalArgumentException("The sequenceNumber does not exist");
            }
            this.snapshotSeqNum = sequenceNumber;
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public List<LogEntry> recoverFromSnapshot() {
        this.readLock.lock();
        try {
            if (this.snapshotSeqNum == 0) {
                return unmodifiableList(this.entries);
            }
            final int position = binarySearch(this.entries, new LogEntry(this.snapshotSeqNum, "ignored"),
                    (curr, candidate) -> Long.compare(curr.sequenceNumber(), candidate.sequenceNumber())
            );
            return unmodifiableList(this.entries.subList(position, this.entries.size()));
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public void truncateBeforeSnapshot() {
        this.writeLock.lock();
        try {
            if (this.snapshotSeqNum == 0) {
                return;
            }
            final int position = binarySearch(this.entries, new LogEntry(this.snapshotSeqNum, "ignored"),
                    (curr, candidate) -> Long.compare(curr.sequenceNumber(), candidate.sequenceNumber())
            );
            this.entries = new ArrayList<>(this.entries.subList(position, this.entries.size()));
        } finally {
            this.writeLock.unlock();
        }
    }

    @Override
    public List<LogEntry> entries() {
        this.readLock.lock();
        try {
            return unmodifiableList(this.entries);
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public long lastSequenceNumber() {
        this.readLock.lock();
        try {
            return this.nextSequenceNumber - 1;
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public int size() {
        this.readLock.lock();
        try {
            return this.entries.size();
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public long snapshotSequenceNumber() {
        this.readLock.lock();
        try {
            return this.snapshotSeqNum;
        } finally {
            this.readLock.unlock();
        }
    }

}
