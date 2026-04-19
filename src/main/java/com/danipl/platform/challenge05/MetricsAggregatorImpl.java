package com.danipl.platform.challenge05;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

import static com.danipl.platform.challenge05.LogLevel.ERROR;
import static com.danipl.platform.challenge05.LogLevel.FATAL;
import static java.lang.Math.ceil;
import static java.lang.Math.max;

public final class MetricsAggregatorImpl implements MetricsAggregator {

    private final int MIN_IN_MILLIS = 60_000;
    private final int MAX_ENTRIES = 1_000_000;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

    private final ArrayDeque<LogEntry> slidingWindow = new ArrayDeque<>(MAX_ENTRIES);

    public MetricsAggregatorImpl() {
    }

    @Override
    public void ingest(final LogEntry entry) {
        writeLock.lock();
        try {
            // As the caller owns the time window, and it may call for an older slicing window, we keep more historical.
            if (slidingWindow.size() == MAX_ENTRIES) {
                slidingWindow.pollFirst();
            }
            slidingWindow.add(entry);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public double getErrorRateLastMinute(final long now) {
        final double[] counter = {0, 0};
        operateLastMinuteLogEntriesFrom(now, (logentry) -> {
            counter[0]++;
            if (logentry.level() == ERROR || logentry.level() == FATAL) {
                counter[1]++;
            }
        });
        return (counter[0] == 0) ? 0 : (counter[1] / counter[0]);
    }

    @Override
    public long getP95ResponseTimeLastMinute(final long now) {
        final var logEntries = new ArrayList<LogEntry>();
        operateLastMinuteLogEntriesFrom(now, (logEntry -> logEntries.add(logEntry)));
        if ((logEntries.isEmpty())) {
            return 0;
        }
        logEntries.sort((prev, curr) ->
                Long.compare(prev.responseTimeMs(), curr.responseTimeMs())
        );
        final var perc95th = (int) max(0, ceil(0.95 * logEntries.size()) - 1);
        return logEntries.get(perc95th).responseTimeMs();
    }

    @Override
    public long getErrorCountLastMinute(final long now) {
        final long[] errorCounter = {0};
        operateLastMinuteLogEntriesFrom(now, (logentry) -> {
            if (logentry.level() == ERROR || logentry.level() == FATAL) {
                errorCounter[0]++;
            }
        });
        return errorCounter[0];
    }

    @Override
    public int getTotalEntriesLastMinute(final long now) {
        final int[] entryCounter = {0};
        operateLastMinuteLogEntriesFrom(now, (logentry) -> entryCounter[0]++);
        return entryCounter[0];
    }

    private void operateLastMinuteLogEntriesFrom(final long now, final Consumer<LogEntry> consumer) {
        readLock.lock();
        try {
            final var iterator = slidingWindow.iterator();
            final var lowerThreshold = now - MIN_IN_MILLIS;
            while (iterator.hasNext()) {
                final var candidate = iterator.next();
                if (candidate.timestamp() >= lowerThreshold && candidate.timestamp() <= now) {
                    consumer.accept(candidate);
                } else if (candidate.timestamp() > now) {
                    break;
                }
            }
        } finally {
            readLock.unlock();
        }
    }

}
