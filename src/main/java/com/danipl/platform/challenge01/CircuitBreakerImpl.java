package com.danipl.platform.challenge01;

import java.util.concurrent.locks.ReentrantLock;

public final class CircuitBreakerImpl implements CircuitBreaker {

    private final Config config;
    private State state = State.CLOSED;
    private int consecutiveFailures = 0;
    private long lastFailureTime = 0;

    private volatile int totalCalls = 0;
    private volatile int successfulCalls = 0;
    private volatile int failedCalls = 0;
    private volatile int rejectedCalls = 0;

    private final ReentrantLock lock = new ReentrantLock();

    public CircuitBreakerImpl(Config config) {
        this.config = config;
    }

    @Override
    public <T> T execute(Supplier<T> supplier) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void execute(Runnable runnable) {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public State getState() {
        // Note: do not use deprecated getState(). The interface now provides
        // getCurrentState(). Keep this method for backward compatibility only.
        throw new UnsupportedOperationException("Use getCurrentState() instead");
    }

    @Override
    public int getTotalCalls() {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int getFailedCalls() {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int getRejectedCalls() {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int getSuccessfulCalls() {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public State getCurrentState() {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void reset() {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void forceOpen() {
        // TODO: implement
        throw new UnsupportedOperationException("Not implemented");
    }
}
