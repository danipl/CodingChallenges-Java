package com.danipl.platform.resilience.circuitbreaker;

/**
 * A thread-safe Circuit Breaker implementing the resilience pattern.
 *
 * States:
 *   CLOSED    - Normal operation. Requests pass through.
 *               Transitions to OPEN after N consecutive failures.
 *   OPEN      - All requests fail fast with CircuitBreakerOpenException.
 *               Transitions to HALF_OPEN after the configured timeout expires.
 *   HALF_OPEN - Allows a single probe request through.
 *               Success -> CLOSED (reset failure count)
 *               Failure -> OPEN (restart timeout)
 */
public interface CircuitBreaker {

    enum State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    class CircuitBreakerOpenException extends RuntimeException {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }

    record Config(int failureThreshold, long openTimeoutMillis) {
        public Config {
            if (failureThreshold < 1) {
                throw new IllegalArgumentException("failureThreshold must be >= 1");
            }
            if (openTimeoutMillis < 1) {
                throw new IllegalArgumentException("openTimeoutMillis must be >= 1");
            }
        }
    }

    static CircuitBreaker of(Config config) {
        return new CircuitBreakerImpl(config);
    }

    <T> T execute(Supplier<T> supplier) throws CircuitBreakerOpenException;

    void execute(Runnable runnable) throws CircuitBreakerOpenException;

    State getState();

    /** Returns the total number of calls (including rejected). */
    int getTotalCalls();

    /** Returns the number of calls that failed (not rejected). */
    int getFailedCalls();

    /** Returns the number of calls rejected because the circuit was OPEN. */
    int getRejectedCalls();

    /** Returns the number of successful calls. */
    int getSuccessfulCalls();

    State getCurrentState();

    void reset();

    void forceOpen();

    @FunctionalInterface
    interface Supplier<T> {
        T get() throws Exception;
    }
}
