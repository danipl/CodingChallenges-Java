# Java 21 Map Implementations Guide

## Overview

Java provides multiple `Map` implementations via `java.util` and `java.util.concurrent`. Each is optimized for different
access patterns, thread safety requirements, and ordering guarantees.

---

## 1. HashMap

```java
Map<K, V> map = new HashMap<>();
```

### Characteristics

| Property        | Value                                |
|-----------------|--------------------------------------|
| **Ordering**    | None (unpredictable iteration order) |
| **Null keys**   | 1 allowed                            |
| **Null values** | Multiple allowed                     |
| **Thread-safe** | No                                   |
| **Performance** | O(1) average for get/put/containsKey |

### Complexity

| Operation          | Average     | Worst Case  |
|--------------------|-------------|-------------|
| `get(key)`         | O(1)        | O(log n)    |
| `put(key, value)`  | O(1)        | O(log n)    |
| `remove(key)`      | O(1)        | O(log n)    |
| `containsKey(key)` | O(1)        | O(log n)    |
| Iteration          | O(capacity) | O(capacity) |

> **Worst case note**: Since Java 8, HashMap uses balanced trees for collision chains (bin size > 8), improving worst
> case from O(n) to O(log n).

### When to Use

- Default choice for key-value storage when ordering doesn't matter
- Memoization in DP/recursion (our current pattern in `development/recursion/`)
- Character frequency counting, entity lookup, caches
- Non-concurrent scenarios with single-threaded access

### Magic Methods (Java 21)

```java
// Get or default (avoids explicit containsKey checks)
V value = map.getOrDefault(key, defaultValue);

// Put only if absent - ideal for memoization init
V existing = map.putIfAbsent(key, value);

// Compute value lazily - atomically computes based on current mapping
map.compute(key, (k, v) -> v == null ? initialValue : v + 1);

// Compute if absent - perfect for first-time memoization
map.computeIfAbsent(key, k -> expensiveComputation(k));

// Compute if present - update existing entries only
map.computeIfPresent(key, (k, v) -> v + 1);

// Merge values - combine or insert
map.merge(key, newValue, (existing, incoming) -> existing + incoming);

// Replace all values via function
map.replaceAll((k, v) -> transform(v));

// Remove by key-value pair (only removes if both match)
map.remove(key, value);

// Replace single value
map.replace(key, newValue);

// Replace only if current value matches
map.replace(key, oldValue, newValue);
```

**Practical memoization pattern:**

```java
// BEFORE: verbose
if (!memo.containsKey(n)) {
    memo.put(n, fib(n - 1) + fib(n - 2));
}
return memo.get(n);

// AFTER: idiomatic Java 21
return memo.computeIfAbsent(n, k -> fib(k - 1) + fib(k - 2));
```

---

## 2. LinkedHashMap

```java
Map<K, V> map = new LinkedHashMap<>();
```

### Characteristics

| Property        | Value                                     |
|-----------------|-------------------------------------------|
| **Ordering**    | Insertion order (default) or access order |
| **Null keys**   | 1 allowed                                 |
| **Null values** | Multiple allowed                          |
| **Thread-safe** | No                                        |
| **Performance** | O(1) average, slight overhead vs HashMap  |

### Complexity

| Operation        | Average | Worst Case |
|------------------|---------|------------|
| `get/put/remove` | O(1)    | O(log n)   |
| Iteration        | O(n)    | O(n)       |

> Iteration cost is O(n) regardless of capacity, unlike HashMap's O(capacity). This is a key advantage when capacity >>
> size.

### Constructor with Access Order

```java
// accessOrder=true: moves accessed entries to end
// Used as basis for LRU caches
Map<K, V> lruMap = new LinkedHashMap<>(16, 0.75f, true);
```

### When to Use

- **Insertion order matters**: Our `DependencyResolverImpl` uses this for deterministic build order
- **LRU cache**: With `accessOrder=true`, combined with `removeEldestEntry()` override
- **Predictable iteration**: Testing, serialization, reproducible output

### Magic Methods

All HashMap methods apply, plus:

```java
// Override to create bounded LRU cache
// Subclass and override this method
class LruCache<K, V> extends LinkedHashMap<K, V> {
    private final int maxSize;

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }
}

// Usage with access order
LruCache<String, Integer> cache = new LinkedHashMap<>(16, 0.75f, true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<String, Integer> eldest) {
        return size() > 100;  // Keep at most 100 entries
    }
};
```

**Practical LRU pattern** (relevant to `platform/challenge07/LruCacheImpl.java`):

```java
// LinkedHashMap-based LRU in ~5 lines
Map<K, V> lru = new LinkedHashMap<>(initialCapacity, loadFactor, /* accessOrder */ true) {
    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > limit;
    }
};
```

---

## 3. TreeMap

```java
Map<K, V> map = new TreeMap<>();
```

### Characteristics

| Property        | Value                                       |
|-----------------|---------------------------------------------|
| **Ordering**    | Natural order of keys, or custom Comparator |
| **Null keys**   | Not allowed (throws NullPointerException)   |
| **Null values** | Multiple allowed                            |
| **Thread-safe** | No                                          |
| **Backed by**   | Red-Black tree (self-balancing BST)         |

### Complexity

| Operation                              | Average & Worst |
|----------------------------------------|-----------------|
| `get/put/remove`                       | O(log n)        |
| `containsKey`                          | O(log n)        |
| Iteration                              | O(n)            |
| `firstKey()` / `lastKey()`             | O(log n)        |
| `subMap()` / `headMap()` / `tailMap()` | O(log n)        |

### When to Use

- **Sorted keys needed**: range queries, finding closest key, ordered traversal
- **Natural ordering**: alphabetized lookups, sorted timestamps, numeric ranges
- **Range operations**: `subMap(fromKey, toKey)`, `headMap(toKey)`, `tailMap(fromKey)`
- **Finding nearest keys**: `floorKey()`, `ceilingKey()`, `lowerKey()`, `higherKey()`

**DO NOT use** for simple lookups - O(log n) vs HashMap's O(1).

### Magic Methods

```java
// Range queries
SortedMap<K, V> range = map.subMap(startKey, endKey);
SortedMap<K, V> head = map.headMap(exclusiveKey);
SortedMap<K, V> tail = map.tailMap(inclusiveKey);

// NavigableMap methods (Java 6+) - TreeMap implements this
K floorKey = map.floorKey(target);    // greatest key <= target
K ceilingKey = map.ceilingKey(target); // least key >= target
K lowerKey = map.lowerKey(target);     // greatest key < target
K higherKey = map.higherKey(target);   // least key > target

// First/last entries
Map.Entry<K, V> first = map.firstEntry();
Map.Entry<K, V> last = map.lastEntry();

// Remove and return first/last
Map.Entry<K, V> first = map.pollFirstEntry();
Map.Entry<K, V> last = map.pollLastEntry();

// Descending view
NavigableMap<K, V> reversed = map.descendingMap();
```

**Practical sorted lookup pattern:**

```java
TreeMap<Integer, String> timeline = new TreeMap<>();
timeline.put(1, "event_a");
timeline.put(5, "event_b");
timeline.put(10, "event_c");

// Find the event at or before timestamp 7
String event = timeline.floorEntry(7).getValue();  // "event_b"

// All events between timestamps 3 and 8
Map<Integer, String> window = timeline.subMap(3, true, 8, true);
```

---

## 4. ConcurrentHashMap

```java
Map<K, V> map = new ConcurrentHashMap<>();
```

### Characteristics

| Property        | Value                                                  |
|-----------------|--------------------------------------------------------|
| **Ordering**    | None                                                   |
| **Null keys**   | Not allowed                                            |
| **Null values** | Not allowed                                            |
| **Thread-safe** | Yes (lock-striping / CAS-based)                        |
| **Iterator**    | Weakly consistent (no ConcurrentModificationException) |

### Complexity

| Operation        | Average |
|------------------|---------|
| `get/put/remove` | O(1)    |
| Iteration        | O(n)    |

> Thread safety achieved via fine-grained locking (CAS operations + synchronized on individual bins), NOT wholesale
> locking like `Hashtable` or `Collections.synchronizedMap()`.

### When to Use

- **Concurrent read/write access**: Multi-threaded environments
- **High-throughput parallel processing**: Better than `Collections.synchronizedMap()`
- **Atomic compound operations**: via built-in atomic methods

**NEVER use** `Collections.synchronizedMap(new HashMap<>())` when `ConcurrentHashMap` suffices.

### Magic Methods

All `Map` defaults apply, plus concurrent-specific:

```java
// Atomic put-if-absent (thread-safe)
V result = map.putIfAbsent(key, value);

// Remove only if value matches (atomic)
boolean removed = map.remove(key, value);

// Replace only if current value matches (atomic CAS)
boolean replaced = map.replace(key, oldValue, newValue);

// Atomic compute methods - the MOST useful for concurrent scenarios
map.compute(key, (k, v) -> v == null ? initialValue : v + 1);
map.computeIfAbsent(key, k -> computeExpensiveValue(k));
map.computeIfPresent(key, (k, v) -> v + 1);

// Atomic merge
map.merge(key, value, (existing, incoming) -> existing + incoming);

// Bulk operations (atomic across the map)
boolean replacedAll = map.replaceAll((k, v) -> transform(v));

// Conditional map-wide operations
map.forEach((k, v) -> process(k, v));

// Parallel bulk operations (Java 8+)
map.forEach(4, (k, v) -> expensiveProcess(k, v));  // parallelism hint

// Search
K foundKey = map.search(4, (k, v) -> v.matchesCondition() ? k : null);

// Reduce
Long sum = map.reduceValues(4, Long::sum);

// Frequency mapping helper
map.merge(key, 1L, Long::sum);  // atomic increment-or-insert

// Mapping with default factory
ConcurrentHashMap<K, V> map = new ConcurrentHashMap<>();
map.computeIfAbsent(key, k -> new ConcurrentHashMap<>());  // nested concurrent maps

// Size estimation (approximate for performance)
int size = map.size();  // may be stale in concurrent scenarios
long mappingCount = map.mappingCount();  // more accurate but heavier
```

**Practical concurrent counter pattern:**

```java
ConcurrentHashMap<String, LongAccumulator> counters = new ConcurrentHashMap<>();

// Atomic increment-per-key
counters.computeIfAbsent(key, k -> new LongAdder()).increment();

// Or with merge (simpler for Long)
ConcurrentHashMap<String, Long> frequency = new ConcurrentHashMap<>();
frequency.merge(word, 1L, Long::sum);  // atomic insert-or-increment
```

---

## 5. WeakHashMap

```java
Map<K, V> map = new WeakHashMap<>();
```

### Characteristics

| Property           | Value                      |
|--------------------|----------------------------|
| **Ordering**       | None                       |
| **Null keys**      | 1 allowed                  |
| **Thread-safe**    | No                         |
| **Key references** | Weak (GC can reclaim keys) |

### When to Use

- **Metadata cache** tied to object lifecycle
- **Association maps** where entries should auto-expire when keys are no longer referenced
- Classloader-scoped caches

> Entries are automatically removed when keys become unreachable. Not a general-purpose cache.

---

## 6. Hashtable (Legacy)

```java
Map<K, V> map = new Hashtable<>();
```

### Characteristics

| Property             | Value                             |
|----------------------|-----------------------------------|
| **Thread-safe**      | Yes (whole-map synchronization)   |
| **Null keys/values** | NOT allowed                       |
| **Performance**      | Poor (single lock for entire map) |

### When to Use

**NEVER** in new code. Use `ConcurrentHashMap` instead. Exists solely for legacy compatibility (Java 1.0).

---

## Decision Matrix

| Requirement                   | Choose                               |
|-------------------------------|--------------------------------------|
| Default, no ordering          | **HashMap**                          |
| Insertion-order iteration     | **LinkedHashMap**                    |
| Access-order (LRU cache)      | **LinkedHashMap** (accessOrder=true) |
| Sorted keys / range queries   | **TreeMap**                          |
| Thread-safe concurrent access | **ConcurrentHashMap**                |
| Auto-expiring key references  | **WeakHashMap**                      |

## Performance Summary

| Implementation    | get/put  | Iteration   | Thread-Safe | Ordering         |
|-------------------|----------|-------------|-------------|------------------|
| HashMap           | O(1) avg | O(capacity) | No          | None             |
| LinkedHashMap     | O(1) avg | O(n)        | No          | Insertion/Access |
| TreeMap           | O(log n) | O(n)        | No          | Sorted           |
| ConcurrentHashMap | O(1) avg | O(n)        | Yes         | None             |
| WeakHashMap       | O(1) avg | O(capacity) | No          | None             |
| Hashtable         | O(1) avg | O(capacity) | Yes (slow)  | None             |

---

## Java 21 Map.of / Map.ofEntries (Immutable Maps)

```java
// Small immutable maps - up to 10 entries
Map<String, Integer> small = Map.of("a", 1, "b", 2, "c", 3);

// More than 10 entries
Map<String, Integer> large = Map.ofEntries(
    Map.entry("a", 1),
    Map.entry("b", 2),
    // ...
);

// Empty immutable map
Map<K, V> empty = Map.of();
```

These return compact immutable instances. No null keys or values. Operations that would modify throw
`UnsupportedOperationException`.

## Java 21 Sequenced Collections

Java 21 introduced the `SequencedMap` interface (JEP 431), providing consistent APIs for maps with a well-defined
encounter order. `LinkedHashMap` and `TreeMap` implement this.

```java
SequencedMap<K, V> sm = new LinkedHashMap<>();

sm.putFirst(key, value);   // Insert before first entry
sm.putLast(key, value);    // Insert after last entry (same as put)
sm.firstEntry();           // Get first key-value pair
sm.lastEntry();            // Get last key-value pair
sm.pollFirstEntry();       // Get and remove first
sm.pollLastEntry();        // Get and remove last
sm.reversed();             // View in reverse order
```

## Common Gotchas

1. **HashMap is NOT thread-safe**: `ConcurrentHashMap` for concurrent writes
2. **TreeMap rejects null keys**: Throws `NullPointerException`
3. **ConcurrentHashMap rejects null keys/values**: Unlike HashMap
4. **`map.get(key)` vs `map.containsKey(key)`**: Use `getOrDefault` or `computeIfAbsent` instead of the check-then-act
   pattern
5. **`map.size()` on concurrent maps**: May return stale data; prefer `mappingCount()` for ConcurrentHashMap
6. **`hashCode()` and `equals()`**: Keys MUST implement both correctly, or HashMap/TreeMap behavior breaks
7. **`LinkedHashMap` iteration is O(n)**: Unlike HashMap's O(capacity), so more efficient when capacity >> size
