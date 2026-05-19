# Key-Value Store

> In-memory KV store with TTL, persistence, and caching layers.

## Requirements

- PUT, GET, DELETE operations
- TTL (Time-To-Live) for keys
- Persistence to disk
- Thread-safe concurrent access
- Optional: LRU cache eviction

## Domain Model

```
KVStore
  ├── StorageEngine (in-memory map)
  ├── TTLManager (scheduled cleanup)
  ├── PersistenceEngine (disk I/O)
  └── Cache (LRU eviction)
```

## Key Patterns

### Proxy Pattern (Lazy Loading / Caching)
```java
class CachedKVStore implements KVStore {
    private final KVStore backingStore;
    private final LRUCache<String, String> cache;

    CachedKVStore(KVStore backing, int cacheSize) {
        this.backingStore = backing;
        this.cache = new LRUCache<>(cacheSize);
    }

    public String get(String key) {
        String value = cache.get(key);
        if (value == null) {
            value = backingStore.get(key);
            if (value != null) cache.put(key, value);
        }
        return value;
    }

    public void put(String key, String value, Duration ttl) {
        cache.put(key, value);
        backingStore.put(key, value, ttl);
    }
}
```

### Decorator Pattern (Adding Features)
```java
// Base store → add TTL → add persistence → add caching
KVStore store = new InMemoryKVStore();
store = new TTLDecorator(store);
store = new PersistentDecorator(store, "data.db");
store = new CachedKVStore(store, 1000);
```

## Core Implementation

```java
class InMemoryKVStore implements KVStore {
    private final ConcurrentHashMap<String, Entry> store = new ConcurrentHashMap<>();

    static class Entry {
        final String value;
        final Instant expiresAt;
        Entry(String value, Duration ttl) {
            this.value = value;
            this.expiresAt = ttl != null ? Instant.now().plus(ttl) : null;
        }
        boolean isExpired() {
            return expiresAt != null && Instant.now().isAfter(expiresAt);
        }
    }

    public void put(String key, String value, Duration ttl) {
        store.put(key, new Entry(value, ttl));
    }

    public String get(String key) {
        Entry entry = store.get(key);
        if (entry == null || entry.isExpired()) {
            store.remove(key);  // Clean up expired
            return null;
        }
        return entry.value;
    }

    public boolean delete(String key) {
        return store.remove(key) != null;
    }

    // Background cleanup
    void startCleanup(Duration interval) {
        Executors.newSingleThreadScheduledExecutor()
            .scheduleAtFixedRate(this::cleanupExpired,
                interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void cleanupExpired() {
        store.entrySet().removeIf(e -> e.getValue().isExpired());
    }
}

// LRU Cache
class LRUCache<K, V> extends LinkedHashMap<K, V> {
    private final int capacity;
    LRUCache(int capacity) {
        super(capacity, 0.75f, true);  // Access order
        this.capacity = capacity;
    }
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > capacity;
    }
}
```

## Interview Tips

1. **ConcurrentHashMap** for thread safety — no manual synchronization needed
2. **TTL cleanup**: Background thread vs lazy deletion on GET
3. **LRU eviction**: `LinkedHashMap` with access order does this natively
4. **Facebook Memcache architecture**: Multi-tier caching (L1 in-process, L2 distributed)
5. **Persistence**: Write-ahead log (WAL) vs periodic snapshots
