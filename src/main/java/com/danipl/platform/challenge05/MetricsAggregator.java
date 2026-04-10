package com.danipl.platform.challenge05;

/**
 * A thread-safe Streaming Log/Metrics Aggregator with a sliding 1-minute window.
 *
 * Aggregates LogEntry records and provides metrics:
 *   - Error rate (ERROR + FATAL as a ratio of total entries)
 *   - P95 response time (95th percentile of response times)
 *   - Error count
 *   - Total entry count
 *
 * All metrics are computed over a sliding 60-second window ending at the given timestamp.
 */
public interface MetricsAggregator {

    static MetricsAggregator of() {
        return new MetricsAggregatorImpl();
    }

    /**
     * Ingest a single log entry. Thread-safe for concurrent calls.
     */
    void ingest(LogEntry entry);

    /**
     * Returns the error rate (0.0-1.0) for entries within the 1-minute window ending at {@code now}.
     * Errors are entries with level ERROR or FATAL.
     * Returns 0.0 if no entries in window.
     */
    double getErrorRateLastMinute(long now);

    /**
     * Returns the P95 response time (ms) for entries within the 1-minute window ending at {@code now}.
     * Returns 0 if no entries in window.
     */
    long getP95ResponseTimeLastMinute(long now);

    /**
     * Returns the count of error entries (ERROR or FATAL) within the 1-minute window ending at {@code now}.
     */
    long getErrorCountLastMinute(long now);

    /**
     * Returns the total number of entries within the 1-minute window ending at {@code now}.
     */
    int getTotalEntriesLastMinute(long now);
}
