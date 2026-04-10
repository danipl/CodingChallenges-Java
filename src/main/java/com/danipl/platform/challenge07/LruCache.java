package com.danipl.platform.challenge07;

import java.time.Clock;
import java.util.Optional;

/**
 * A thread-safe LRU Cache with TTL (time-to-live) expiration.
 *
 * Features:
 *   - LRU eviction when at capacity (evict least recently used entry)
 *   - TTL expiration: entries expired past their TTL are evicted
 *   - DO NOT use LinkedHashMap; implement LRU from scratch via doubly-linked list + hash map
 *   - Thread-safe for concurrent get/put/remove
 */
public interface LruCache<K, V> {

    static <K, V> LruCache<K, V> of(int capacity, long ttlMs, Clock clock) {
        return new LruCacheImpl<>(capacity, ttlMs, clock);
    }

    /**
     * Puts a key-value pair into the cache.
     * If the key already exists, updates the value and marks as recently used.
     * If at capacity and no expired entries can be evicted, evicts the least recently used entry.
     *
     * @throws IllegalArgumentException if key is null
     */
    void put(K key, V value);

    /**
     * Retrieves the value for the given key if present and not expired.
     * Updates recency on hit.
     */
    Optional<V> get(K key);

    /**
     * Returns true if the key exists and is not expired.
     */
    boolean containsKey(K key);

    /**
     * Returns the number of non-expired entries in the cache.
     */
    int size();

    /**
     * Removes a key from the cache if present.
     */
    void remove(K key);

    /**
     * Removes all entries from the cache.
     */
    void clear();
}
