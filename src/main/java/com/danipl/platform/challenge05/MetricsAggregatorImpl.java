package com.danipl.platform.challenge05;

import java.util.concurrent.locks.ReentrantLock;

public final class MetricsAggregatorImpl implements MetricsAggregator {

    private final ReentrantLock lock = new ReentrantLock();

    // TODO: add data structure to store entries for sliding window queries

    public MetricsAggregatorImpl() {
    }

    @Override
    public void ingest(LogEntry entry) {
        // TODO: store the entry in the data structure
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public double getErrorRateLastMinute(long now) {
        // TODO: compute error rate over [now - 60000, now]
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public long getP95ResponseTimeLastMinute(long now) {
        // TODO: sort response times in window, return 95th percentile
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public long getErrorCountLastMinute(long now) {
        // TODO: count ERROR/FATAL entries in window
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int getTotalEntriesLastMinute(long now) {
        // TODO: count all entries in window
        throw new UnsupportedOperationException("Not implemented");
    }
}
