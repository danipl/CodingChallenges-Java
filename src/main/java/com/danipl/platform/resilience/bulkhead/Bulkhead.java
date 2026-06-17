package com.danipl.platform.resilience.bulkhead;

import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * A thread-safe Bulkhead implementing the Bulkhead Isolation pattern.
 *
 * Prevents cascading failures by limiting concurrent executions per partition (e.g., tenant, endpoint, service).
 * Each partition has its own concurrency limit and queue capacity — a failure or overload in one partition
 * cannot starve others. This is critical in multi-tenant platforms like Revolut where noisy neighbors
 * must not degrade service for all customers.
 *
 * Key behaviors:
 *   - Each partition (identified by a String key) has independent semaphore-based concurrency limits.
 *   - When a partition's limit is reached, callers are either rejected immediately or wait up to a timeout.
 *   - Active and queued execution counts are trackable per partition and globally.
 *   - Partitions can be registered dynamically and removed when no longer needed.
 *   - All state transitions are thread-safe under concurrent access from multiple threads.
 *
 * @param <T> the type of result produced by protected operations
 */
public interface Bulkhead<T> {

    /**
     * Creates a new Bulkhead with the given configuration.
     *
     * @param config the configuration specifying default concurrency limits and queue behavior
     * @return a new thread-safe Bulkhead instance
     */
    static <T> Bulkhead<T> of(Config config) {
        return new BulkheadImpl<>(config);
    }

    /**
     * Creates a new Bulkhead with the given configuration and Clock for testable time.
     *
     * @param config the configuration specifying default concurrency limits and queue behavior
     * @param clock  the Clock to use for timeout calculations
     * @return a new thread-safe Bulkhead instance
     */
    static <T> Bulkhead<T> of(Config config, Clock clock) {
        return new BulkheadImpl<>(config, clock);
    }

    /**
     * Registers a new partition with a custom concurrency limit.
     * If the partition already exists, its limit is updated atomically.
     *
     * @param partitionKey   the unique identifier for this partition (e.g., tenant ID, endpoint path)
     * @param maxConcurrency the maximum number of concurrent executions allowed in this partition
     * @throws IllegalArgumentException if partitionKey is null/empty or maxConcurrency &lt; 1
     */
    void registerPartition(String partitionKey, int maxConcurrency);

    /**
     * Removes a partition and releases all its resources.
     * In-flight executions are NOT interrupted — only new acquisitions are prevented.
     *
     * @param partitionKey the partition to remove
     * @return true if the partition existed and was removed, false otherwise
     */
    boolean removePartition(String partitionKey);

    /**
     * Executes the given operation within the specified partition, respecting concurrency limits.
     * If the partition's limit is reached, the caller waits up to the configured timeout.
     * If the timeout expires, a BulkheadRejectedException is thrown.
     *
     * @param partitionKey the partition under which to execute
     * @param operation    the operation to execute (must be thread-safe)
     * @return the result of the operation
     * @throws BulkheadRejectedException  if the partition is full and timeout expires
     * @throws PartitionNotFoundException if the partition has not been registered
     * @throws Exception                  if the operation itself throws
     */
    T execute(String partitionKey, SupplierWithException<T> operation) throws Exception;

    /**
     * Attempts to acquire a slot in the given partition without blocking.
     * Returns a Permit that must be released when the operation completes.
     *
     * @param partitionKey the partition to acquire from
     * @return a Permit that releases the slot when closed
     * @throws BulkheadRejectedException  if the partition is full
     * @throws PartitionNotFoundException if the partition has not been registered
     */
    Permit acquire(String partitionKey);

    /**
     * Returns the number of currently active (in-flight) executions for the given partition.
     *
     * @param partitionKey the partition to query
     * @return the number of active executions, or 0 if the partition does not exist
     */
    int activeCount(String partitionKey);

    /**
     * Returns the maximum concurrency limit for the given partition.
     *
     * @param partitionKey the partition to query
     * @return the max concurrency, or -1 if the partition does not exist
     */
    int maxConcurrency(String partitionKey);

    /**
     * Returns the total number of active executions across ALL partitions.
     *
     * @return the global active count
     */
    int totalActiveCount();

    /**
     * Returns a snapshot of metrics for all registered partitions.
     * This is a point-in-time view and may be stale by the time it is read.
     *
     * @return an unmodifiable map of partition key to PartitionMetrics
     */
    Map<String, PartitionMetrics> metrics();

    /**
     * Returns the set of currently registered partition keys.
     *
     * @return an unmodifiable set of partition keys
     */
    java.util.Set<String> registeredPartitions();

    /**
     * A permit representing an acquired slot in a partition.
     * Must be released after the protected operation completes.
     */
    interface Permit extends AutoCloseable {
        /**
         * Releases the acquired slot back to the partition.
         * Safe to call multiple times — subsequent calls are no-ops.
         */
        @Override
        void close();

        /**
         * Returns the partition key this permit belongs to.
         */
        String partitionKey();
    }

    /**
     * Point-in-time metrics snapshot for a single partition.
     */
    record PartitionMetrics(
            String partitionKey,
            int maxConcurrency,
            int activeCount,
            long totalRejected,
            long totalSuccessful
    ) {
    }

    /**
     * Global configuration for the Bulkhead.
     */
    record Config(
            int defaultMaxConcurrency,
            long timeoutMs,
            boolean partitionAutoCreate,
            int maxPartitions
    ) {
        public Config {
            if (defaultMaxConcurrency < 1) {
                throw new IllegalArgumentException("defaultMaxConcurrency must be >= 1");
            }
            if (timeoutMs < 0) {
                throw new IllegalArgumentException("timeoutMs must be >= 0");
            }
            if (maxPartitions < 1) {
                throw new IllegalArgumentException("maxPartitions must be >= 1");
            }
        }

        /**
         * Creates a Config with sensible defaults: maxConcurrency=10, timeoutMs=1000,
         * partitionAutoCreate=false, maxPartitions=100.
         */
        public static Config defaults() {
            return new Config(10, 1000, false, 100);
        }
    }

    /**
     * Exception thrown when a partition's concurrency limit is reached
     * and the caller's timeout has expired.
     */
    class BulkheadRejectedException extends RuntimeException {
        public BulkheadRejectedException(String message) {
            super(message);
        }

        public BulkheadRejectedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Exception thrown when an operation references a partition that does not exist
     * and auto-create is disabled.
     */
    class PartitionNotFoundException extends RuntimeException {
        public PartitionNotFoundException(String message) {
            super(message);
        }
    }

    /**
     * Functional interface for operations that may throw checked exceptions.
     */
    @FunctionalInterface
    interface SupplierWithException<T> {
        T get() throws Exception;
    }
}
