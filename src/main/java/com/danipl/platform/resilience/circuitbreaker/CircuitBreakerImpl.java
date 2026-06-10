package com.danipl.platform.resilience.circuitbreaker;

import java.time.Clock;
import java.util.concurrent.locks.ReentrantLock;

public final class CircuitBreakerImpl implements CircuitBreaker {

    private final Config config;
    private int consecutiveFailures = 0;
    private long lastFailureTime = 0;

    private volatile State state = State.CLOSED;
    private volatile int totalCalls = 0;
    private volatile int successfulCalls = 0;
    private volatile int failedCalls = 0;
    private volatile int rejectedCalls = 0;

    private final Clock clock;
    private final ReentrantLock lock = new ReentrantLock();

    public CircuitBreakerImpl(final Config config) {
        this(config, Clock.systemDefaultZone());
    }

    public CircuitBreakerImpl(final Config config, final Clock clock) {
        this.config = config;
        this.clock = clock;
    }

    private State checkInitialState() {
        try {
            lock.lock();
            totalCalls++;
            if (state == State.OPEN && clock.millis() - lastFailureTime > config.openTimeoutMillis()) {
                state = State.HALF_OPEN;
            } else if (state == State.OPEN) {
                rejectedCalls++;
                throw new CircuitBreakerOpenException("It is open");
            } else if (state == State.HALF_OPEN) {
                rejectedCalls++;
                throw new CircuitBreakerOpenException("It is half-open");
            }
            return state;
        } finally {
            lock.unlock();
        }
    }

    private void handleSuccess(final State current) {
        try {
            lock.lock();
            successfulCalls++;
            consecutiveFailures = 0;
            if (current == State.HALF_OPEN) {
                state = State.CLOSED;
            }
        } finally {
            lock.unlock();
        }
    }

    private void handleFailure() {
        try {
            lock.lock();
            failedCalls++;
            consecutiveFailures++;
            if (consecutiveFailures >= config.failureThreshold()) {
                state = State.OPEN;
                lastFailureTime = clock.millis();
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public <T> T execute(final Supplier<T> supplier) {
        try {
            final State current = checkInitialState();
            final T t = supplier.get();
            handleSuccess(current);
            return t;
        } catch (final CircuitBreakerOpenException cboe) {
            throw cboe;
        } catch (final RuntimeException re) {
            handleFailure();
            throw re;
        } catch (final Exception ex) {
            handleFailure();
            // Not production ready!
            // Handle checked exceptions from the supplier by treating them as failures and wrapping them
            // in a RuntimeException to maintain backward compatibility with current test specifications.
            throw new RuntimeException("Unexpected exception", ex);
        }
    }

    @Override
    public void execute(final Runnable runnable) {
        try {
            final State current = checkInitialState();
            runnable.run();
            handleSuccess(current);
        } catch (final CircuitBreakerOpenException cboe) {
            throw cboe;
        } catch (final RuntimeException re) {
            handleFailure();
            throw re;
        }
    }

    @Override
    public State getState() {
        return getCurrentState();
    }

    @Override
    public int getTotalCalls() {
        return totalCalls;
    }

    @Override
    public int getFailedCalls() {
        return failedCalls;
    }

    @Override
    public int getRejectedCalls() {
        return rejectedCalls;
    }

    @Override
    public int getSuccessfulCalls() {
        return successfulCalls;
    }

    @Override
    public State getCurrentState() {
        return state;
    }

    @Override
    public void reset() {
        try {
            lock.lock();
            state = State.CLOSED;
            consecutiveFailures = 0;
            lastFailureTime = 0;
            totalCalls = 0;
            successfulCalls = 0;
            failedCalls = 0;
            rejectedCalls = 0;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void forceOpen() {
        try {
            lock.lock();
            state = State.OPEN;
            consecutiveFailures = Integer.MAX_VALUE;
            lastFailureTime = Long.MAX_VALUE;
        } finally {
            lock.unlock();
        }
    }

}
