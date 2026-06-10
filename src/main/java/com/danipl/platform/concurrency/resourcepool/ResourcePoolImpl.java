package com.danipl.platform.concurrency.resourcepool;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * A thread-safe resource pool that:
 * <ul>
 *   <li>Blocks the caller up to acquireTimeoutMs if no resource is available.
 *   <li>Throws PoolExhaustedException when the timeout expires.
 *   <li>Lazily creates resources via the provided factory.
 *   <li>Discards invalid (unhealthy) resources on release and creates replacements
 *       on subsequent acquire calls.
 *   <li>Supports graceful shutdown via {@link #close()}.
 * </ul>
 *
 * @param <T> the type of pooled resource, must implement {@link AutoCloseable}
 */
public final class ResourcePoolImpl<T extends AutoCloseable & ResourcePool.Healthy> implements ResourcePool<T> {

    private final int maxSize;
    private final Function<Integer, T> resourceFactory;
    private final long acquireTimeoutMs;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition hasResources = lock.newCondition();
    private final BlockingQueue<T> idle;
    private final Set<T> active = new HashSet<>();
    private final AtomicInteger idCounter = new AtomicInteger(0);
    private volatile boolean closed = false;

    public ResourcePoolImpl(int maxSize, Function<Integer, T> resourceFactory, long acquireTimeoutMs) {
        this.maxSize = maxSize;
        this.idle = new ArrayBlockingQueue<>(maxSize);
        this.resourceFactory = resourceFactory;
        this.acquireTimeoutMs = acquireTimeoutMs;
    }

    @Override
    public T acquire() throws PoolExhaustedException, InterruptedException {
        this.lock.lock();
        try {
            while (true) {
                if (this.closed) {
                    throw new IllegalStateException("Pool is closed");
                }

                T resource = this.idle.poll();
                if (resource != null) {
                    this.active.add(resource);
                    return resource;
                }

                if (this.active.size() < this.maxSize) {
                    resource = this.resourceFactory.apply(this.idCounter.incrementAndGet());
                    this.active.add(resource);
                    return resource;
                }

                final boolean arrived = this.hasResources.await(this.acquireTimeoutMs, TimeUnit.MILLISECONDS);
                if (!arrived) {
                    throw new PoolExhaustedException("Pool exhausted");
                }
            }
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public void release(T resource) {
        if (resource == null) {
            throw new IllegalArgumentException("resource must not be null");
        }

        this.lock.lock();
        try {
            if (this.closed) {
                throw new IllegalStateException("Pool is closed");
            }

            if (!this.active.remove(resource)) {
                return;
            }

            if (resource.isHealthy()) {
                this.idle.add(resource);
            } else {
                closeQuietly(resource);
            }
            this.hasResources.signal();
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public int availableCount() {
        this.lock.lock();
        try {
            return this.idle.size();
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public int activeCount() {
        this.lock.lock();
        try {
            return this.active.size();
        } finally {
            this.lock.unlock();
        }
    }

    @Override
    public void close() {
        this.lock.lock();
        try {
            if (this.closed) {
                return;
            }
            this.closed = true;

            this.idle.forEach(this::closeQuietly);
            this.active.forEach(this::closeQuietly);
            this.idle.clear();
            this.active.clear();
            this.hasResources.signalAll();
        } finally {
            this.lock.unlock();
        }
    }

    private void closeQuietly(final T resource) {
        try {
            resource.close();
        } catch (final Exception ignored) {
            //TODO: Scope outside from the coding challenge.
        }
    }

}
