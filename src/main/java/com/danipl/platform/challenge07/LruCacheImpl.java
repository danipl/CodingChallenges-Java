package com.danipl.platform.challenge07;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

public final class LruCacheImpl<K, V> implements LruCache<K, V> {

    private final int capacity;
    private final long ttlMs;
    private final Clock clock;
    private final ReentrantLock lock = new ReentrantLock();

    // TODO: HashMap for O(1) key lookup -> node references
    private final Map<K, Object> map = new HashMap<>();

    // TODO: Doubly-linked list head/tail sentinels for LRU ordering
    // Most recently used at head, least recently used at tail

    public LruCacheImpl(int capacity, long ttlMs, Clock clock) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be >= 1");
        this.capacity = capacity;
        this.ttlMs = ttlMs;
        this.clock = clock;
    }

    @Override
    public void put(K key, V value) {
        // TODO: insert or update key in LRU list and map, evict if needed
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public Optional<V> get(K key) {
        // TODO: lookup key, check TTL, move to head if valid, evict if expired
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean containsKey(K key) {
        // TODO: check if key exists and is not expired
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int size() {
        // TODO: count non-expired entries
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void remove(K key) {
        // TODO: remove key from both map and linked list
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void clear() {
        // TODO: remove all entries from map and reset linked list
        throw new UnsupportedOperationException("Not implemented");
    }
}
