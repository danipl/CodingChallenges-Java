package com.danipl.platform.challenge04;

import java.util.concurrent.TimeUnit;

public interface RateLimiter {

    static RateLimiter of(double capacity, double refillRatePerSecond) {
        return new RateLimiterImpl(capacity, refillRatePerSecond);
    }

    boolean tryAcquire();

    boolean tryAcquire(int tokens);

    boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException;

    int availableTokens();
}
