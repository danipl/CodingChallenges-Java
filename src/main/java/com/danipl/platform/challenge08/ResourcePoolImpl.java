package com.danipl.platform.challenge08;

import java.util.function.Function;

/**
 * Skeleton implementation of ResourcePool.
 * All methods throw UnsupportedOperationException.
 */
public final class ResourcePoolImpl<T extends AutoCloseable> implements ResourcePool<T> {

    private final int maxSize;
    private final Function<Integer, T> resourceFactory;
    private final long acquireTimeoutMs;

    public ResourcePoolImpl(int maxSize, Function<Integer, T> resourceFactory, long acquireTimeoutMs) {
        this.maxSize = maxSize;
        this.resourceFactory = resourceFactory;
        this.acquireTimeoutMs = acquireTimeoutMs;
    }

    @Override
    public T acquire() throws PoolExhaustedException, InterruptedException {
        // TODO: implement acquire with blocking and timeout
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void release(T resource) {
        // TODO: implement release with invalid resource handling
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int availableCount() {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int activeCount() {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void close() {
        // TODO: implement shutdown and cleanup
        throw new UnsupportedOperationException("Not implemented");
    }
}
