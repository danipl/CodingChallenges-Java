package com.danipl.platform.resilience.retry;

import java.util.ArrayList;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.random;
import static java.util.concurrent.ThreadLocalRandom.current;

public final class RetryImpl implements Retry {

    private final RetryConfig config;

    public RetryImpl(final RetryConfig config) {
        this.config = config;
    }

    @Override
    public <T> T execute(final SupplierWithException<T> supplier) throws MaxRetriesExceededException {
        var attempt = 0;
        while ((attempt < config.maxAttempts()) && !Thread.currentThread().isInterrupted()) {
            try {
                return supplier.get();
            } catch (final Exception ex) {
                if (attempt + 1 == config.maxAttempts()) {
                    throw new MaxRetriesExceededException(config.maxAttempts() + "of attempts reached", ex, attempt + 1);
                }
            }
            attempt++;
            try {
                Thread.sleep(calculateDelay(attempt));
            } catch (final InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // Fallback, defensive programming - it should not reach it.
        throw new RuntimeException("Unexpected exception");
    }

    @Override
    public <T> RetryResult<T> executeWithDetails(final SupplierWithException<T> supplier) throws MaxRetriesExceededException {
        var attempt = 0;
        var delays = new ArrayList<Long>();
        var totalWaitMs = 0L;
        while ((attempt < config.maxAttempts()) && !Thread.currentThread().isInterrupted()) {
            try {
                return new RetryResult<T>(
                        supplier.get(), attempt + 1, totalWaitMs, delays
                );
            } catch (final Exception ex) {
                if (attempt + 1 == config.maxAttempts()) {
                    throw new MaxRetriesExceededException(config.maxAttempts() + "of attempt reached", ex, attempt + 1);
                }
            }
            attempt++;
            try {
                final var delay = calculateDelay(attempt);
                delays.add(delay);
                totalWaitMs += delay;
                Thread.sleep((long) (delay));
            } catch (final InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        // Fallback, defensive programming - it should not reach it.
        throw new RuntimeException("Unexpected exception");
    }

    private long calculateDelay(final int attempt) {
        final var delay = calculateBackoff(attempt);
        return (random() >= 0.5) ? delay + calculateJitter(delay) : delay - calculateJitter(delay);
    }

    private long calculateBackoff(final int attempt) {
        return min(config.baseDelayMs() * (long) pow(2, (attempt - 1)), config.maxDelayMs());
    }

    private long calculateJitter(final long delay) {
        return max(0, (long) (current().nextDouble() * config.jitterFactor() * delay));
    }

}
