package com.danipl.platform.challenge04;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class RateLimiterImpl implements RateLimiter {

    private double capacity;
    private int tokensBucket = 0;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    private final ScheduledExecutorService scheduledExecutorService;

    final Runnable tokenCreator = () -> {
        writeLock.lock();
        try {
            if (tokensBucket < capacity) {
                tokensBucket++;
            }
        } finally {
            writeLock.unlock();
        }
    };

    /**
     * Keeping it for a coding-challenge tests compatibility.
     * <p>
     * Looking at "public int availableTokens()" it seems to be a clear unalignment in the interface. Between double
     * and integer, I think the most obvious choice for a rate limiter token bucket is integer.
     *
     * @param capacity
     * @param refillRatePerSecond
     */
    public RateLimiterImpl(final double capacity, final double refillRatePerSecond) {
        this((int) capacity, (int) refillRatePerSecond);
    }

    public RateLimiterImpl(final int capacity, final int refillRatePerSecond) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        if (refillRatePerSecond < 0) {
            throw new IllegalArgumentException("Refill rate must be positive");
        }
        this.capacity = capacity;
        this.tokensBucket = capacity;
        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        if (refillRatePerSecond > 0) {
            final int refillAtMillis = Math.max(1, 1000 / refillRatePerSecond);
            this.scheduledExecutorService.scheduleAtFixedRate(tokenCreator, 0, refillAtMillis, MILLISECONDS);
        }
    }

    @Override
    public boolean tryAcquire() {
        writeLock.lock();
        try {
            if (tokensBucket == 0) {
                return false;
            }
            tokensBucket--;
            return true;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean tryAcquire(final int tokens) {
        if (tokens < 0) {
            return false;
        }
        if (tokens == 0) {
            return true;
        }
        writeLock.lock();
        try {
            if (tokensBucket < tokens) {
                return false;
            }
            tokensBucket -= tokens;
            return true;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        final long deadlineNanos = System.nanoTime() + unit.toNanos(timeout);
        final long intervalMillis = unit.toMillis(timeout) / 10;
        while (System.nanoTime() < deadlineNanos) {
            if (tryAcquire()) {
                return true;
            }
            Thread.sleep(intervalMillis);
        }
        return tryAcquire();
    }

    @Override
    public int availableTokens() {
        readLock.lock();
        try {
            return tokensBucket;
        } finally {
            readLock.unlock();
        }
    }

}
