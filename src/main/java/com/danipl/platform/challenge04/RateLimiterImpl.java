package com.danipl.platform.challenge04;

import java.util.concurrent.TimeUnit;

public class RateLimiterImpl implements RateLimiter {

    public RateLimiterImpl(double capacity, double refillRatePerSecond) {
    }

    @Override
    public boolean tryAcquire() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean tryAcquire(int tokens) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int availableTokens() {
        throw new UnsupportedOperationException("Not implemented");
    }
}
