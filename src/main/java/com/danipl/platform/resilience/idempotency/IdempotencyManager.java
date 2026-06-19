package com.danipl.platform.resilience.idempotency;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A thread-safe Idempotency Manager implementing the idempotency key pattern.
 *
 * In distributed systems, network failures can cause clients to retry operations. Without idempotency,
 * a payment gateway might charge a user twice for the same transaction. This manager tracks each operation
 * by a unique idempotency key and ensures the operation executes exactly once, returning cached results
 * for subsequent retries.
 *
 * Key behaviors:
 *   - First request with a key: executes the operation and caches the result
 *   - Subsequent requests with the same key: returns the cached result without re-executing
 *   - Concurrent requests with the same key: only one executes, others wait for the result
 *   - Expired entries (beyond TTL) are automatically cleaned up
 */
public interface IdempotencyManager {

    /**
     * Creates a new instance with the given configuration.
     *
     * @param config the configuration parameters
     * @return a new IdempotencyManager instance
     */
    static IdempotencyManager of(Config config) {
        return new IdempotencyManagerImpl(config);
    }

    /**
     * Creates a new instance with the given configuration and clock (for testing).
     *
     * @param config the configuration parameters
     * @param clock the clock to use for time-based operations
     * @return a new IdempotencyManager instance
     */
    static IdempotencyManager of(Config config, Clock clock) {
        return new IdempotencyManagerImpl(config, clock);
    }

    /**
     * Executes an operation with idempotency guarantee.
     *
     * If this is the first request with the given key, the operation is executed and the result is cached.
     * If a previous request with the same key is in progress, this call blocks until the result is available.
     * If a previous request completed, the cached result is returned immediately.
     *
     * @param idempotencyKey unique identifier for this operation (typically a UUID)
     * @param operation the operation to execute
     * @param <T> the result type
     * @return the result of the operation (either freshly executed or cached)
     * @throws IdempotencyException if the operation fails or times out
     */
    <T> T execute(String idempotencyKey, Supplier<T> operation) throws IdempotencyException;

    /**
     * Executes an operation with idempotency guarantee (void variant).
     *
     * @param idempotencyKey unique identifier for this operation
     * @param operation the operation to execute
     * @throws IdempotencyException if the operation fails or times out
     */
    void executeVoid(String idempotencyKey, Runnable operation) throws IdempotencyException;

    /**
     * Retrieves the cached result for an idempotency key, if it exists.
     *
     * @param idempotencyKey the key to look up
     * @param <T> the result type
     * @return Optional containing the cached result, or empty if not found
     */
    <T> Optional<T> getCachedResult(String idempotencyKey);

    /**
     * Checks if an operation with the given key is currently in progress.
     *
     * @param idempotencyKey the key to check
     * @return true if an operation is in progress, false otherwise
     */
    boolean isInProgress(String idempotencyKey);

    /**
     * Gets the current state of an idempotency key.
     *
     * @param idempotencyKey the key to check
     * @return the current state, or empty if the key doesn't exist
     */
    Optional<State> getState(String idempotencyKey);

    /**
     * Removes expired entries from the cache.
     *
     * This method should be called periodically to prevent memory leaks.
     * Entries older than the configured TTL are removed.
     *
     * @return the number of entries removed
     */
    int cleanup();

    /**
     * Gets the total number of entries currently in the cache (including expired but not yet cleaned).
     *
     * @return the cache size
     */
    int size();

    // === Nested types ===

    /**
     * Configuration record with validation.
     *
     * @param ttl time-to-live for cached entries
     * @param maxCacheSize maximum number of entries before cleanup is triggered
     */
    record Config(Duration ttl, int maxCacheSize) {
        public Config {
            if (ttl == null || ttl.isNegative() || ttl.isZero()) {
                throw new IllegalArgumentException("TTL must be positive");
            }
            if (maxCacheSize < 1) {
                throw new IllegalArgumentException("maxCacheSize must be >= 1");
            }
        }
    }

    /**
     * Exception thrown when idempotency operations fail.
     */
    class IdempotencyException extends RuntimeException {
        public IdempotencyException(String message) {
            super(message);
        }

        public IdempotencyException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * State of an idempotency key.
     */
    enum State {
        /** Operation is currently executing */
        IN_PROGRESS,
        /** Operation completed successfully */
        SUCCESS,
        /** Operation failed */
        FAILED
    }
}
