package com.danipl.platform.resilience.retry;

/**
 * A retry mechanism with exponential backoff and jitter.
 *
 * Backoff formula: min(baseDelayMs * 2^(attempt-1), maxDelayMs)
 * With jitter: delay +/- random * jitterFactor * delay
 */
public interface Retry {

    @FunctionalInterface
    interface SupplierWithException<T> {
        T get() throws Exception;
    }

    record RetryDecision(boolean shouldRetry, long delayMs, Exception lastException, int attemptNumber) {
    }

    static Retry of(RetryConfig config) {
        return new RetryImpl(config);
    }

    <T> T execute(SupplierWithException<T> supplier) throws MaxRetriesExceededException;

    <T> RetryResult<T> executeWithDetails(SupplierWithException<T> supplier) throws MaxRetriesExceededException;
}
