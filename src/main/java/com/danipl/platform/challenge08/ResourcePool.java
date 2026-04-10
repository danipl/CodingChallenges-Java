package com.danipl.platform.challenge08;

import java.util.function.Function;

/**
 * A thread-safe connection/resource pool.
 *
 * Blocks the caller up to acquireTimeoutMs if no resource is available.
 * If the timeout expires, a PoolExhaustedException is thrown.
 * Invalid resources (marked via markInvalid()) are discarded and not returned to the pool.
 *
 * @param <T> the type of pooled resource
 */
public interface ResourcePool<T extends AutoCloseable> {

    /**
     * Acquires a resource from the pool.
     * Blocks the calling thread up to the configured acquireTimeoutMs
     * if no resource is immediately available.
     *
     * @return a healthy resource from the pool
     * @throws PoolExhaustedException if no resource is available within the timeout
     * @throws InterruptedException   if the thread is interrupted while waiting
     */
    T acquire() throws PoolExhaustedException, InterruptedException;

    /**
     * Releases a resource back to the pool for reuse.
     * If the resource is marked as invalid (resource.isHealthy() == false),
     * it is discarded and a new resource is created via the factory.
     *
     * @param resource the resource to release
     */
    void release(T resource);

    /**
     * Returns the number of currently available (idle) resources.
     */
    int availableCount();

    /**
     * Returns the number of currently active (checked out) resources.
     */
    int activeCount();

    /**
     * Closes the pool, releasing all resources.
     * Any subsequent acquire() or release() calls should throw.
     */
    void close();

    /**
     * Creates a new ResourcePool with the given configuration.
     *
     * @param maxSize          the maximum number of resources in the pool
     * @param resourceFactory  a function that creates a new resource given a resource id
     * @param acquireTimeoutMs maximum time to wait for a resource, in milliseconds
     * @param <T>              the type of pooled resource
     * @return a new ResourcePool instance
     */
    static <T extends AutoCloseable> ResourcePool<T> of(int maxSize, Function<Integer, T> resourceFactory, long acquireTimeoutMs) {
        return new ResourcePoolImpl<>(maxSize, resourceFactory, acquireTimeoutMs);
    }
}
