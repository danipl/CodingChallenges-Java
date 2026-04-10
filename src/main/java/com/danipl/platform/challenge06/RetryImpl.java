package com.danipl.platform.challenge06;

public final class RetryImpl implements Retry {

    private final RetryConfig config;

    public RetryImpl(RetryConfig config) {
        this.config = config;
    }

    @Override
    public <T> T execute(SupplierWithException<T> supplier) throws MaxRetriesExceededException {
        // TODO: implement retry with exponential backoff and jitter
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <T> RetryResult<T> executeWithDetails(SupplierWithException<T> supplier) throws MaxRetriesExceededException {
        // TODO: implement retry with detailed result tracking
        throw new UnsupportedOperationException("Not implemented");
    }
}
