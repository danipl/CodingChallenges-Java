package com.danipl.platform.concurrency.writaheadlog;

import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implementation of {@link WriteAheadLog}.
 *
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
    // private final java.util.List<LogEntry> entries = new java.util.ArrayList<>();
    // private long nextSequenceNumber = 1;
    // private long snapshotSeqNum = 0; // 0 = no snapshot

    // === Constructors ===

    public WriteAheadLogImpl(final Config config) {
        this.config = config;
    }

    // === Public methods — ALL throw UnsupportedOperationException ===

    @Override
    public LogEntry append(final String record) {
        throw new UnsupportedOperationException("Implement this method");
    }

    @Override
    public void markSnapshot(final long sequenceNumber) {
        throw new UnsupportedOperationException("Implement this method");
    }

    @Override
    public List<LogEntry> recoverFromSnapshot() {
        throw new UnsupportedOperationException("Implement this method");
    }

    @Override
    public void truncateBeforeSnapshot() {
        throw new UnsupportedOperationException("Implement this method");
    }

    @Override
    public List<LogEntry> entries() {
        throw new UnsupportedOperationException("Implement this method");
    }

    @Override
    public long lastSequenceNumber() {
        throw new UnsupportedOperationException("Implement this method");
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException("Implement this method");
    }

    @Override
    public long snapshotSequenceNumber() {
        throw new UnsupportedOperationException("Implement this method");
    }

}
