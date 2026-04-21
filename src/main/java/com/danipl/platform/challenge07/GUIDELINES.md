# Challenge 07: LRU Cache with TTL - Live Coding Guidelines

## 1. Challenge Presentation

### What You're Building

A **thread-safe LRU Cache with TTL (time-to-live) expiration** — stores key-value pairs, evicts the least recently used
entry when at capacity, and expires entries that exceed their TTL. Core building block for caching layers in API
gateways, session stores, and rate limiters.

### Core Contract

```
     put(key, value) ──▶ [HashMap + Doubly-Linked List]
                               │
              ┌────────────────┼────────────────┐
              │                │                │
         If at capacity:   TTL check on      Move to head
         evict LRU entry   access: expired   (mark as MRU)
                           → remove
                               │
         get(key) ◀───────────┘
         returns Optional<V> (empty if missing or expired)
```

**LRU eviction:** least recently used entry = tail of the doubly-linked list.
**TTL expiration:** per-entry, checked lazily on access. `expireAt = clock.millis() + ttlMs`.

### Interface Summary

| Method                                      | Purpose                                         |
|---------------------------------------------|-------------------------------------------------|
| `of(int capacity, long ttlMs, Clock clock)` | Factory - creates instance                      |
| `void put(K key, V value)`                  | Insert or update, evict LRU if at capacity      |
| `Optional<V> get(K key)`                    | Retrieve if present and not expired, update LRU |
| `boolean containsKey(K key)`                | TTL-aware existence check                       |
| `int size()`                                | Count of non-expired entries                    |
| `void remove(K key)`                        | Remove key from both map and list               |
| `void clear()`                              | Reset cache to empty                            |

### Data Structures Used

| Structure                   | Role                          | Complexity |
|-----------------------------|-------------------------------|------------|
| `HashMap<K, Node>`          | O(1) key → node lookup        | O(1)       |
| Doubly-linked list (custom) | O(1) LRU order, MRU promotion | O(1)       |
| Sentinel nodes (head/tail)  | Eliminate null edge cases     | —          |

**Why not `LinkedHashMap`?** It doesn't support per-entry TTL, isn't thread-safe, and interviewers want to see you build
the data structure from scratch.

### What Interviewers Evaluate

1. **O(1) correctness** — both get and put must be constant time. HashMap for lookup, DLL for ordering.
2. **DLL manipulation** — correct pointer updates for insert at head, remove, and move-to-head. One wrong pointer =
   corrupted list.
3. **TTL implementation** — per-entry expiration, lazy eviction on access, TTL=0 means immediate expiry.
4. **Thread safety** — all operations must be serialized or use read/write lock. ConcurrentHashMap alone doesn't track
   ordering.
5. **Capacity management** — eviction only on **new** key insert at capacity, NOT on key update.

---

## 2. Edge & Corner Cases

### How to Identify Them Before Coding

Draw the DLL + HashMap state after each operation. Every insertion, eviction, and TTL check changes both structures.

| #  | Edge Case                              | How It Surfaces                           | How to Handle                                                              |
|----|----------------------------------------|-------------------------------------------|----------------------------------------------------------------------------|
| 1  | **capacity=1**                         | Every new put evicts the previous entry   | Evict LRU, insert new. The DLL always has exactly 1 node between sentinels |
| 2  | **TTL=0**                              | Entry expires immediately after insertion | `clock.millis() + 0 == clock.millis()` → expired on next `get()`           |
| 3  | **Update existing key**                | Key exists at capacity, put new value     | **Do NOT evict** — update value, move to head. Eviction only for NEW keys  |
| 4  | **get() on expired entry**             | Entry in map but past TTL                 | Lazy eviction: remove from both structures, return `Optional.empty()`      |
| 5  | **containsKey() on expired entry**     | Entry technically in map but expired      | Must check TTL — return false                                              |
| 6  | **size() with mixed expired/fresh**    | Some entries expired, some fresh          | O(N) traversal counting only non-expired (`k != null && clock < ttlMs`)    |
| 7  | **remove() on non-existent key**       | Key never inserted or already evicted     | Map returns null — no-op                                                   |
| 8  | **EvictLast on empty cache**           | `tail.prev == head` (only sentinels)      | `evictLast()` returns `Optional.empty()` — caller skips map removal        |
| 9  | **First insertion**                    | `head.next == tail`, empty DLL            | Link new node: head → node → tail                                          |
| 10 | **Sentinel nodes in size() traversal** | Head/tail have `k=null`, `ttlMs=0`        | Skip entries where `k == null`                                             |
| 11 | **Null key or value in put()**         | `null` breaks HashMap semantics           | Throw `NullPointerException` before acquiring lock                         |
| 12 | **capacity < 1 in constructor**        | Cache that cannot hold anything           | Throw `IllegalArgumentException`                                           |

### Quick Pre-Implementation Checklist

```
▢ HashMap<K, Node> for O(1) lookup
▢ Doubly-linked list with head/tail sentinels
▢ Sentinel init: head.next = tail, tail.prev = head
▢ put(): lookup first, update if exists, else evict+insert
▢ get(): lookup, TTL check via evict(), move to head if valid
▢ nodeToHead(): unlink from current position, relink after head
▢ evictLast(): unlink node before tail, update tail.prev
▢ evict(): unlink from DLL + map.remove() (single responsibility)
▢ Thread-safe: ReentrantLock with try-finally
▢ Null validation before lock acquisition
```

---

## 3. First Approach - Chain of Thinking

### Minute 0-2: Clarify Requirements

Ask the interviewer:

- *"Should TTL be checked proactively (background thread) or lazily (on access)?"* → This challenge uses **lazy eviction
  ** on access.
- *"Should updating an existing key count against capacity?"* → **No** — updates don't change cache size.
- *"Can I use LinkedHashMap?"* → No — implement with HashMap + custom DLL.
- *"What should size() return — all entries or only non-expired?"* → Only **non-expired** entries.

### Minute 2-5: Design the Data Structure

Sketch on whiteboard/shared doc:

```
Structure:
  head <-> [A] <-> [B] <-> [C] <-> tail
    ▲                    │
    │                    LRU (least recently used)
    MRU (most recently used)

Map:  { "a" → Node(A), "b" → Node(B), "c" → Node(C) }

Sentinel nodes:
  head: k=null, ttlMs=0
  tail: k=null, ttlMs=0

Node struct:
  final K k
  V v
  final long ttlMs    ← absolute expiry time
  Node prev, next

Operations:
  put(key, value):
    if key exists → update value, move to head
    if key new → if full, evictLast; create node, insert at head

  get(key):
    node = map.get(key)
    if null → return empty
    if expired → evict(node, force=true), return empty
    move to head, return value

  containsKey(key):
    node = map.get(key)
    return node != null && !expired

  size():
    iterate DLL, count non-expired (k != null && clock < ttlMs)
```

### Minute 5-10: Sketch the Core Flow

```java
void put(K key, V value) {
    Node<K, V> current = map.get(key);
    if (current != null) {
        current.v = value;
        nodeToHead(current);
    } else {
        if (map.size() == capacity) {
            evictLast().ifPresent(n -> map.remove(n.k));
        }
        Node<K, V> node = new Node<>(key, value, clock.millis() + ttlMs);
        map.put(key, node);
        newNodeToHead(node);  // fresh node: no stale prev/next to unlink
    }
}

Optional<V> get(K key) {
    Node<K, V> current = map.get(key);
    if (current == null || evict(current, false)) {
        return Optional.empty();
    }
    nodeToHead(current);
    return Optional.of(current.v);
}
```

### Minute 10-25: Implement

1. Node class (static) + sentinels → done
2. Constructor with validation → done
3. `put()` with lookup-first logic → done
4. `get()` with TTL-aware eviction → done
5. `containsKey()` → done
6. `size()` with sentinel skip → done
7. `remove()` and `clear()` → done
8. `nodeToHead()` (relink existing node) → done
9. `newNodeToHead()` (link fresh node) → done
10. `evict()` (unlink + map remove) → done
11. `evictLast()` (unlink LRU node) → done
12. Thread safety via `ReentrantLock` on all public methods → done

---

## 4. Communication Approach During the Interview

### What to Say Out Loud

| Moment              | Say This                                                                                                                                                                                                                             |
|---------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Starting            | "I'll use a HashMap for O(1) lookup and a custom doubly-linked list with sentinel nodes for O(1) LRU ordering. All operations will be protected by a ReentrantLock."                                                                 |
| Before put()        | "I lookup the key first. If it exists, I update the value and move it to MRU. If it's new and we're at capacity, I evict the LRU entry (tail of the list). The key insight: **eviction only happens on new insert, not on update.**" |
| About sentinels     | "Head and tail sentinel nodes eliminate null checks on prev/next. Head always points to MRU, tail precedes LRU. They have null keys and TTL=0 so they're never counted in size()."                                                   |
| About TTL           | "TTL is per-entry, stored as an absolute timestamp. Lazy eviction on access: if TTL has passed when get() is called, the entry is removed. For production with large caches I'd add background cleanup."                             |
| About thread safety | "Single ReentrantLock serializes all operations. For read-heavy production workloads I'd switch to ReentrantReadWriteLock — allows concurrent get() calls while serializing writes."                                                 |
| About newNodeToHead | "Fresh nodes don't have stale prev/next pointers, so newNodeToHead() is a subset of nodeToHead() — fewer assignments. I split them for clarity and a minor performance win."                                                         |

### When Stuck

```
I notice [specific issue].
The risk is [consequence].
Two options: [A] or [B].
I'll go with [A] because [reason]. Does that align with your expectations?
```

---

## 5. Implementation Structure

### Recommended File Layout

```java
public final class LruCacheImpl<K, V> implements LruCache<K, V> {
    // === Inner class ===
    private static class Node<K, V> { ... }

    // === Fields ===
    private final int capacity;
    private final long ttlMs;
    private final Clock clock;
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<K, Node<K, V>> map = new HashMap<>();
    private final Node<K, V> head;  // sentinel
    private final Node<K, V> tail;  // sentinel

    // === Constructor ===                 ← validate capacity, init sentinels

    // === put() ===                       ← ~15 lines, lookup-first, evict on new insert
    // === get() ===                       ← ~12 lines, TTL check, move to head
    // === containsKey() ===               ← ~6 lines, TTL-aware
    // === size() ===                      ← ~12 lines, O(N) traversal, skip sentinels
    // === remove() ===                    ← ~6 lines, evict() handles both structures
    // === clear() ===                     ← ~5 lines, map.clear() + reset sentinels

    // === evict(node, force) ===          ← unlink from DLL + map.remove()
    // === nodeToHead(node) ===            ← unlink from current position, relink at MRU
    // === newNodeToHead(node) ===         ← link fresh node at MRU position
    // === evictLast() ===                 ← unlink node before tail, return it
}
```

### Key Implementation Patterns

**put() — capacity check in else branch:**

```java
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
```

**evictLast() — DLL integrity:**

```java
final Node<K, V> prev = tail.prev;
if (prev == null) return Optional.empty();
final Node<K, V> newPrev = prev.prev;
newPrev.next = tail;
tail.prev = newPrev;   // CRITICAL: must update backward link
prev.prev = null;
prev.next = null;
return Optional.of(prev);
```

**Sentinel-aware size():**

```java
var curr = head.next;
var count = 0;
while (curr != null) {
    if (clock.millis() < curr.ttlMs && curr.k != null) {
        count++;
    }
    curr = curr.next;
}
return count;
```

**Thread safety pattern:**

```java
lock.lock();
try {
    // operation logic
} finally {
    lock.unlock();
}
```

---

## 6. Technical Pro Tips

### Data Structure Selection

| Approach                          | Pros                                                                 | Cons                                   | When to Use             |
|-----------------------------------|----------------------------------------------------------------------|----------------------------------------|-------------------------|
| **HashMap + custom DLL**          | Full control, TTL support, O(1)         ~200 lines, DLL pointer bugs | **Interview default**                  |
| `LinkedHashMap`                   | Built-in LRU via `removeEldestEntry`                                 | No per-entry TTL, not thread-safe      | Prototype / non-TTL use |
| `ConcurrentHashMap` + `AtomicRef` | Lock-free reads                                                      | No ordering info, eviction complex     | High-read production    |
| Caffeine / Guava Cache            | Production-ready, TinyLFU, stats                                     | External dependency, overkill for test | Real production systems |

### Thread Safety: Lock Granularity

| Approach                        | Pros                            | Cons                          | When to Use           |
|---------------------------------|---------------------------------|-------------------------------|-----------------------|
| **ReentrantLock (all ops)**     | Simple, correct, easy to reason | All reads serialized          | **Interview default** |
| `ReentrantReadWriteLock`        | Concurrent reads                | Write lock upgrade complexity | Read-heavy production |
| `StampedLock` (optimistic read) | Max read throughput             | Complex API, retry logic      | Extreme performance   |

### TTL Strategy: Lazy vs Eager

| Strategy                | Eviction Timing          | Pros                           | Cons                                |
|-------------------------|--------------------------|--------------------------------|-------------------------------------|
| **Lazy (this impl)**    | On access (get/contains) | Zero background overhead       | Expired entries occupy slots        |
| Eager (background)      | Periodic cleanup         | Dead entries reclaimed         | `ScheduledExecutorService` overhead |
| Mixed (lazy + periodic) | Best of both             | Fast access + eventual cleanup | Most complex to implement           |

**Interview tip:** Implement lazy eviction, then mention: "For production with large caches and low hit rates, I'd add a
background `ScheduledExecutorService` that sweeps expired entries every N milliseconds."

### newNodeToHead() Optimization

Fresh nodes don't have stale `prev`/`next` pointers (both null). `nodeToHead()` does 8 assignments to properly unlink +
relink. For a brand-new node, `newNodeToHead()` needs only 4:

```java
// newNodeToHead (4 assignments):
prev = head;
next = head.next;
next.prev = this;
head.next = this;

// nodeToHead (8 assignments):
head.next.prev = this;
prev.next = next;
next.prev = prev;
this.prev = head;
this.next = head.next;
head.next = this;
```

Splitting them is a minor perf win and shows attention to detail.

### What Senior Engineers Demonstrate

1. **Cap check in else branch** — "Eviction only for new keys. Updating existing keys changes their LRU order but not
   cache size."
2. **Sentinel nodes** — "Head/tail sentinels eliminate null checks on prev/next. They're never counted (k=null) and
   never expired (ttlMs=0)."
3. **O(1) guarantee** — "HashMap gives O(1) lookup, DLL gives O(1) insert/remove/reorder. No traversal needed for
   get/put."
4. **Lazy TTL eviction** — "Entries expire on access, not proactively. This avoids background thread overhead.
   Trade-off: expired entries occupy slots until accessed."
5. **Thread-safety reasoning** — "ReentrantLock is correct and simple. For production read-heavy workloads I'd use
   ReadWriteLock to allow concurrent reads."
6. **Production awareness** — "For real systems, Caffeine is the answer — TinyLFU admission policy, async refresh,
   built-in statistics. This exercise proves I understand the internals."

---

## 7. Common Mistakes to Avoid

| Mistake                                         | Why It Fails                                                          | Fix                                                            |
|-------------------------------------------------|-----------------------------------------------------------------------|----------------------------------------------------------------|
| **Evict BEFORE checking if key exists**         | Unnecessary eviction on key update — cache shrinks below capacity     | `map.get(key)` first, evict only in else branch                |
| **Missing `tail.prev` update in evictLast()**   | DLL backward link stale → next eviction or traversal corrupted        | Always set `tail.prev = newPrev`                               |
| **Double map.remove() in remove()**             | Redundant (harmless but reveals design confusion)                     | `evict()` handles map removal — don't call again               |
| **Node as non-static inner class**              | Each node carries hidden reference to LruCacheImpl.this → memory leak | `private static class Node<K, V>`                              |
| **Using `^` for exponent in TTL calc**          | `^` is XOR in Java, not power                                         | `clock.millis() + ttlMs` — simple addition, no exponent needed |
| **`size()` counting sentinel nodes**            | Head/tail have k=null — guard with `curr.k != null`                   | Check `curr.k != null` in traversal                            |
| **No null validation before lock**              | Lock acquired then exception thrown → unnecessary contention          | Validate key/value BEFORE `lock.lock()`                        |
| **LinkedHashMap instead of custom DLL**         | Interview explicitly requires from-scratch implementation             | HashMap + doubly-linked list with manual pointer management    |
| **System.currentTimeMillis() instead of Clock** | TTL not testable without mocking                                      | Inject Clock in constructor                                    |
| **Forgetting `lock.unlock()` in finally**       | Lock held on exception → deadlock for all subsequent operations       | Always `try { ... } finally { lock.unlock(); }`                |

---

## 8. Verification Checklist

Before declaring done, verify:

### Functional

- [ ] put + get roundtrip: retrieve exact value stored
- [ ] get non-existent: returns `Optional.empty()`
- [ ] Update existing key: value overwritten, LRU order updated, no eviction
- [ ] containsKey: true for present, false for absent, false for expired
- [ ] LRU eviction: evicts least recently used, not oldest inserted
- [ ] get() updates recency: accessed entry survives next eviction
- [ ] capacity=1: each new put evicts the previous entry
- [ ] TTL=0: entry expires immediately, get() returns empty
- [ ] Expired entries not counted in size()
- [ ] Fresh entries counted in size()
- [ ] remove existing: entry gone from both map and list
- [ ] remove non-existent: no exception
- [ ] clear: all entries removed, cache reset to empty state

### Thread Safety

- [ ] Concurrent put + get from 20 threads: no exceptions, no data loss
- [ ] Concurrent remove from 10 threads: no exceptions, no corruption
- [ ] Lock release in finally block: no deadlocks on exception paths
- [ ] Null validation before lock: no spurious lock contention on bad input

### Edge Cases

- [ ] First insertion: head → node → tail, DLL intact
- [ ] Eviction on empty cache: evictLast() returns empty, map untouched
- [ ] Multiple sequential evictions: DLL backward links always correct
- [ ] capacity < 0: IllegalArgumentException at construction
- [ ] Null key in put(): NullPointerException thrown

---

## 9. Extension Points (Bonus Discussion)

If you finish early, mention these to stand out:

1. **ReadWriteLock upgrade** — "Single ReentrantLock works but serializes all reads. `ReentrantReadWriteLock` allows
   concurrent `get()`, `containsKey()`, `size()` — better throughput for read-heavy workloads."
2. **Background TTL cleanup** — "`ScheduledExecutorService` that sweeps expired entries every N ms. Prevents dead
   entries from occupying valuable cache slots."
3. **`size()` O(1) optimization** — "Track a `liveCount` counter, increment on insert, decrement on evict/expire. O(1)
   read instead of O(N) traversal."
4. **Per-entry TTL** — "Currently all entries share the same TTL. Some caches benefit from variable TTL per entry (e.g.,
   authentication tokens vs static config)."
5. **Statistics** — "Hit rate, miss rate, eviction count. Useful for dashboarding and cache sizing decisions."
6. **Soft references** — "Wrap values in `SoftReference<V>`. JVM can reclaim cache memory under heap pressure before
   OOM."
7. **Caffeine comparison** — "For production, use Caffeine. It uses TinyLFU (not LRU) for better hit rates, supports
   async refresh, and handles all edge cases above."
8. **Segmented caching** — "Like ConcurrentHashMap's segments: split the cache into N independent shards, each with its
   own lock. Reduces contention under high write throughput."
9. **Cache stampede prevention** — "When a popular key expires, many threads simultaneously miss and hammer the backend.
   `computeIfAbsent` or request coalescing prevents this."

---

## 10. Production References

| Resource                                                                                                                                         | Why It Matters                                           |
|--------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------|
| [Caffeine](https://github.com/ben-manes/caffeine)                                                                                                | Industry-standard Java cache — TinyLFU, 17k+ stars       |
| [Guava Cache](https://github.com/google/guava/wiki/CachesExplained)                                                                              | Google's caching library — LRU, TTL, size-based eviction |
| [LinkedHashMap.removeEldestEntry](https://docs.oracle.com/javase/8/docs/api/java/util/LinkedHashMap.html#removeEldestEntry-java.util.Map.Entry-) | JDK's built-in LRU extension (no TTL, no thread-safety)  |
| [Google SRE - Distributed Caching](https://sre.google/sre-book/machine-intelligence-in-sre/)                                                     | Cache strategies at scale                                |
| [Redis LRU Eviction](https://redis.io/docs/manual/eviction/)                                                                                     | Distributed LRU — approximated LRU for memory efficiency |

---

*This guideline follows the established challenge template: presentation → edge cases → chain of thinking →
communication → implementation → pro tips → mistakes → verification.*
