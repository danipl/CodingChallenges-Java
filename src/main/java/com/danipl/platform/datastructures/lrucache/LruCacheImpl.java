package com.danipl.platform.datastructures.lrucache;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

public final class LruCacheImpl<K, V> implements LruCache<K, V> {

    private static class Node<K, V> {
        final K k;
        V v;
        final long ttlMs;
        Node prev;
        Node next;

        Node(final K k, final V v, final long ttlMs) {
            this.k = k;
            this.v = v;
            this.ttlMs = ttlMs;
        }
    }

    private final int capacity;
    private final long ttlMs;
    private final Clock clock;
    private final ReentrantLock lock = new ReentrantLock();

    private final Map<K, Node<K, V>> map = new HashMap<>();
    private final Node<K, V> head = new Node<>(null, null, 0);
    private final Node<K, V> tail = new Node<>(null, null, 0);

    public LruCacheImpl(int capacity, long ttlMs, Clock clock) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be >= 1");
        this.capacity = capacity;
        this.ttlMs = ttlMs;
        this.clock = clock;
        this.head.next = this.tail;
        this.tail.prev = this.head;
    }

    @Override
    public void put(final K key, final V value) {
        if (key == null) throw new NullPointerException("key must be provided");
        if (value == null) throw new NullPointerException("value must be provided");
        lock.lock();
        try {
            final Node<K, V> current = map.get(key);
            if (current != null) {
                current.v = value;
                nodeToHead(current);
            } else {
                if (map.size() == capacity) {
                    evictLast().ifPresent(n -> map.remove(n.k));
                }
                final Node<K, V> node = new Node<>(key, value, clock.millis() + ttlMs);
                map.put(key, node);
                newNodeToHead(node);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Optional<V> get(final K key) {
        lock.lock();
        try {
            final Node<K, V> current = map.get(key);
            if (current == null || evict(current, false)) {
                return Optional.empty();
            }
            nodeToHead(current);
            return Optional.of(current.v);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean containsKey(final K key) {
        lock.lock();
        try {
            final Node<K, V> current = map.get(key);
            return (current != null && !evict(current, false));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public int size() {
        lock.lock();
        try {
            var curr = head.next;
            var count = 0;
            while (curr != null) {
                if (clock.millis() < curr.ttlMs && curr.k != null) {
                    count++;
                }
                curr = curr.next;
            }
            return count;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void remove(final K key) {
        lock.lock();
        try {
            final var current = map.get(key);
            if (current != null) {
                evict(current, true);
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void clear() {
        lock.lock();
        try {
            map.clear();
            head.next = tail;
            tail.prev = head;
        } finally {
            lock.unlock();
        }
    }

    private boolean evict(final Node<K, V> node, final boolean force) {
        if (force || clock.millis() >= node.ttlMs) {
            final Node<K, V> prev = node.prev;
            final Node<K, V> next = node.next;
            next.prev = node.prev;
            prev.next = node.next;
            node.prev = null;
            node.next = null;
            map.remove(node.k);
            return true;
        }
        return false;
    }

    private void newNodeToHead(final Node<K, V> node) {
        final Node<K, V> next = head.next;
        node.prev = head;
        node.next = next;
        next.prev = node;
        head.next = node;
    }

    private void nodeToHead(final Node<K, V> node) {
        final Node<K, V> prev = node.prev;
        final Node<K, V> next = node.next;
        head.next.prev = node;
        prev.next = next;
        next.prev = prev;
        node.prev = head;
        node.next = head.next;
        head.next = node;
    }

    private Optional<Node<K, V>> evictLast() {
        final Node<K, V> prev = tail.prev;
        if (prev == null) {
            return Optional.empty();
        }
        final Node<K, V> newPrev = prev.prev;
        newPrev.next = tail;
        tail.prev = newPrev;
        prev.prev = null;
        prev.next = null;
        return Optional.of(prev);
    }

}
