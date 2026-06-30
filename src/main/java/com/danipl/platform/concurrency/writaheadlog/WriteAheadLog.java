package com.danipl.platform.concurrency.writaheadlog;

/**
 * A thread-safe Write Ahead Log (WAL) implementing the append-only log pattern with snapshot support.
 *
 * WALs are the foundation of durable state machines: every state mutation is appended as a log entry
 * before being applied. Snapshots mark recovery points — on crash, the system loads the last snapshot
 * and replays only entries after it, avoiding full replay from epoch.
 *
 * Key behaviors:
 *   - Monotonically increasing sequence numbers starting at 1
 *   - Snapshot marks a recovery point; recovery returns entries from last snapshot onward (inclusive)
 *   - Truncation removes entries before the last snapshot to bound memory
 *   - Thread-safe under concurrent appenders and readers
 */
public interface WriteAheadLog {

    /**
     * Creates a new instance with the given configuration.
     *
     * @param config the configuration parameters
     * @return a new WriteAheadLog instance
     */
    static WriteAheadLog of(Config config) {
        return new WriteAheadLogImpl(config);
    }

    /**
     * Appends a record to the log, assigning it the next sequence number.
     *
     * @param record the data to append (must not be null or empty)
     * @return the LogEntry with its assigned sequence number
     * @throws IllegalArgumentException if record is null or blank
     */
    LogEntry append(String record);

    /**
     * Marks the entry at the given sequence number as a snapshot point.
     *
     * Subsequent calls to {@link #recoverFromSnapshot()} will return entries starting
     * from this sequence number onward. Only one snapshot marker exists at a time —
     * calling this again moves the snapshot forward.
     *
     * @param sequenceNumber the sequence number to mark as snapshot
     * @throws IllegalArgumentException if sequenceNumber does not exist in the log
     */
    void markSnapshot(long sequenceNumber);

    /**
     * Returns all entries from the last snapshot point onward (inclusive).
     *
     * If no snapshot has been marked, returns all entries. If the snapshot entry
     * was truncated, returns entries from the beginning of the log.
     *
     * @return an unmodifiable list of LogEntry from snapshot onward
     */
    java.util.List<LogEntry> recoverFromSnapshot();

    /**
     * Removes all entries before the last snapshot point to bound memory usage.
     *
     * If no snapshot has been marked, this is a no-op. The snapshot entry itself
     * and all subsequent entries remain.
     */
    void truncateBeforeSnapshot();

    /**
     * Returns all entries currently in the log.
     *
     * @return an unmodifiable list of all LogEntry
     */
    java.util.List<LogEntry> entries();

    /**
     * Returns the sequence number of the most recently appended entry.
     *
     * @return the last sequence number, or 0 if the log is empty
     */
    long lastSequenceNumber();

    /**
     * Returns the number of entries currently in the log.
     *
     * @return the entry count
     */
    int size();

    /**
     * Returns the sequence number of the last snapshot, or 0 if none marked.
     *
     * @return the snapshot sequence number
     */
    long snapshotSequenceNumber();

    // === Nested types ===

    /**
     * A single log entry with a sequence number and the appended record.
     *
     * @param sequenceNumber the monotonically increasing sequence number (starts at 1)
     * @param record         the data appended to the log
     */
    record LogEntry(long sequenceNumber, String record) {
        public LogEntry {
            if (record == null || record.isBlank()) {
                throw new IllegalArgumentException("record must not be null or blank");
            }
            if (sequenceNumber < 1) {
                throw new IllegalArgumentException("sequenceNumber must be >= 1");
            }
        }
    }

    /**
     * Configuration for the Write Ahead Log.
     *
     * @param maxEntries maximum number of entries to retain before forcing truncation (0 = unlimited)
     */
    record Config(int maxEntries) {
        public Config {
            if (maxEntries < 0) {
                throw new IllegalArgumentException("maxEntries must be >= 0");
            }
        }
    }

    /**
     * Exception thrown when WAL operations fail.
     */
    class WriteAheadLogException extends RuntimeException {
        public WriteAheadLogException(String message) {
            super(message);
        }

        public WriteAheadLogException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
