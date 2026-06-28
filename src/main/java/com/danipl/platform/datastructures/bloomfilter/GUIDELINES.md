# Challenge: Bloom Filter - Live Coding Guidelines

## 1. Challenge Presentation

### What You're Building

A **Bloom Filter** is a space-efficient probabilistic data structure used to test whether an element is a member of a set. It can return false positives (reporting an element is present when it's not) but never false negatives (reporting an element is absent when it's actually present). This makes it ideal for caching layers, database query optimization, CDN routing, and any scenario where occasional false positives are acceptable but false negatives are not.

Real-world use cases:
- **Caching**: Before querying an expensive cache or database, check if the key might exist
- **Database optimization**: Cassandra uses Bloom Filters to avoid checking SSTables that don't contain a key
- **Web crawlers**: Avoid revisiting URLs already processed
- **CDN routing**: Quickly determine if content might be cached at an edge location

### Core Contract

```
add(item)
  ↓
Compute k hash values → Set k bits in BitSet
  ↓
mightContain(item)
  ↓
Compute same k hash values → Check if all k bits are set
  ↓
All bits set? → "might contain" (possible false positive)
Any bit not set? → "definitely not contained" (no false negatives)
```

### Interface Summary

| Method | Purpose |
|--------|---------|
| `of(Config)` | Factory - creates a Bloom Filter with expected insertions and target false-positive probability |
| `add(T item)` | Inserts an element by setting bits at k hash-determined positions |
| `mightContain(T item)` | Returns `true` if element might be in set (possible false positive), `false` if definitely not |
| `size()` | Returns number of `add()` calls made (includes duplicates) |
| `expectedFalsePositiveProbability()` | Returns current expected FPP based on items added |
| `clear()` | Resets filter to empty state |
| `config()` | Returns the configuration used to create the filter |

### What Interviewers Evaluate

1. **Understanding of probabilistic data structures** — Can you explain false positives vs false negatives? Why can't Bloom Filters support deletion?
2. **Hash function strategy** — Do you know the Kirsch-Mitzenmacher optimization (use 2 hash functions to simulate k)?
3. **Thread-safety** — Can you protect shared state (BitSet, counter) with appropriate locking?
4. **Mathematical foundations** — Do you understand the formulas for optimal bit array size and number of hash functions?

---

## 2. Edge & Corner Cases

### How to Identify Them Before Coding

Think about:
- **Null handling**: What happens if someone passes `null`?
- **Empty filter**: What should `mightContain` return before any `add` calls?
- **Duplicate adds**: Does adding the same item twice change the filter? (No, but it increments the counter)
- **Overflow**: What if more items are added than `expectedInsertions`?
- **Config validation**: What are the valid ranges for `expectedInsertions` and `falsePositiveProbability`?

| # | Edge Case | How It Surfaces | How to Handle |
|---|-----------|-----------------|---------------|
| 1 | **Null item** | `add(null)` or `mightContain(null)` | Throw `NullPointerException` immediately |
| 2 | **Empty filter** | `mightContain("x")` before any `add` | Return `false` (no bits are set) |
| 3 | **Duplicate adds** | `add("x")` called twice | Bits remain set (idempotent for bits), but `size()` increments |
| 4 | **Overflow capacity** | More items added than `expectedInsertions` | FPP increases beyond configured value; filter still works but less accurate |
| 5 | **Config validation** | `expectedInsertions < 1` or `falsePositiveProbability` out of (0, 1) | Throw `IllegalArgumentException` in Config constructor |
| 6 | **Clear during concurrent access** | `clear()` called while other threads are adding | Write lock ensures atomicity; readers may see partial state if not careful |

### Quick Pre-Implementation Checklist

```
▢ Null checks on all public methods
▢ Config validation in record compact constructor
▢ Thread-safe BitSet access (lock or synchronized)
▢ Atomic counter for size (AtomicLong)
▢ Hash function: handle negative hash codes (use Math.abs or bitwise AND)
▢ Bit array bounds: ensure hash values are within [0, bitsetSize)
```

---

## 3. First Approach - Chain of Thinking

### Minute 0-2: Clarify Requirements

Ask the interviewer:
- "Should the filter support deletion? (No — standard Bloom Filters don't support deletion because multiple items may share the same bits)"
- "Is thread-safety required? (Yes — assume concurrent access)"
- "Should I track the exact count of distinct elements, or just the number of `add()` calls? (Just the number of calls — tracking distinct elements requires additional data structures)"
- "What hash function should I use? (Use `Object.hashCode()` combined with a secondary hash, or implement Murmur3 if time permits)"

### Minute 2-5: Design

**Data structures:**
- `BitSet bits` — the bit array (size computed from config)
- `AtomicLong insertions` — counter for number of `add()` calls
- `ReentrantReadWriteLock lock` — protects BitSet (reads >> writes)

**Key formulas:**
```
Bit array size (m) = -(n * ln(p)) / (ln(2)^2)
  where n = expectedInsertions, p = falsePositiveProbability

Number of hash functions (k) = (m / n) * ln(2)
```

**Hash strategy (Kirsch-Mitzenmacher):**
Instead of computing k independent hash functions, compute two:
```
h1(x) = x.hashCode()
h2(x) = some secondary hash (e.g., reverse bits, or use a different algorithm)

g_i(x) = (h1(x) + i * h2(x)) mod m, for i = 0, 1, ..., k-1
```

### Minute 5-10: Sketch the Core Flow

**add(item):**
```
1. Check null → throw NPE
2. Acquire write lock
3. Compute k bit positions using hash(item)
4. For each position, set the bit: bits.set(position)
5. Release write lock
6. Increment insertions counter (atomic, outside lock)
```

**mightContain(item):**
```
1. Check null → throw NPE
2. Acquire read lock
3. Compute k bit positions using hash(item)
4. For each position, check if bit is set: bits.get(position)
5. If any bit is not set → return false
6. Release read lock
7. Return true
```

**hash(item):**
```
1. Compute h1 = item.hashCode()
2. Compute h2 = secondary hash (e.g., Integer.reverseBytes(h1) ^ 0x5bd1e995)
3. For i = 0 to k-1:
     position_i = Math.abs((h1 + i * h2) % bitsetSize)
4. Return array of k positions
```

### Minute 10-25: Implement

**Step-by-step order:**
1. Implement `hash(item)` — the core algorithm
2. Implement `add(item)` — set bits + increment counter
3. Implement `mightContain(item)` — check bits
4. Implement `size()` — return counter
5. Implement `clear()` — reset BitSet and counter
6. Implement `expectedFalsePositiveProbability()` — compute current FPP
7. Implement `config()` — return config

---

## 4. Communication Approach During the Interview

### What to Say Out Loud

| Moment | Say This |
|--------|----------|
| Starting | "I'll implement a thread-safe Bloom Filter using a BitSet and ReadWriteLock. The key insight is that we use k hash functions to map each item to k bit positions." |
| Before locking | "I'm using a ReadWriteLock because reads (mightContain) are more frequent than writes (add), and multiple readers can proceed concurrently." |
| About hash strategy | "Instead of computing k independent hash functions, I'll use the Kirsch-Mitzenmacher optimization: two hash functions h1 and h2, then simulate k functions as g_i(x) = h1(x) + i * h2(x). This is mathematically equivalent but faster." |
| About false positives | "The false-positive probability starts at the configured value and increases as we add more items. If we exceed expectedInsertions, the filter becomes less accurate but still correct (no false negatives)." |

### When Stuck

```
I notice I need a secondary hash function but Object.hashCode() is the only built-in.
The risk is poor hash distribution leading to higher false-positive rates.
Two options: (A) use Integer.reverseBytes(h1) as h2, or (B) implement a simple mixing function.
I'll go with (A) because it's simple and provides reasonable distribution. Does that align with your expectations?
```

---

## 5. Implementation Structure

### Recommended File Layout

```java
public final class BloomFilterImpl<T> implements BloomFilter<T> {
    // === Fields ===
    private final Config config;
    private final int bitsetSize;
    private final int numHashFunctions;
    private final BitSet bits;
    private final AtomicLong insertions;
    private final ReentrantReadWriteLock lock;
    private final ReentrantReadWriteLock.ReadLock readLock;
    private final ReentrantReadWriteLock.WriteLock writeLock;

    // === Constructor ===
    public BloomFilterImpl(Config config) {
        // Initialize fields from config
    }

    // === Core methods ===
    @Override
    public void add(T item) {
        // Null check, write lock, set bits, unlock, increment counter
    }

    @Override
    public boolean mightContain(T item) {
        // Null check, read lock, check bits, unlock, return result
    }

    // === Helper methods ===
    private int[] hash(T item) {
        // Compute k bit positions using Kirsch-Mitzenmacher
    }
}
```

### Key Implementation Pattern

**Kirsch-Mitzenmacher hash optimization:**
```java
private int[] hash(T item) {
    int h1 = item.hashCode();
    int h2 = Integer.reverseBytes(h1) ^ 0x5bd1e995; // Secondary hash
    int[] positions = new int[numHashFunctions];
    
    for (int i = 0; i < numHashFunctions; i++) {
        int combinedHash = h1 + i * h2;
        // Ensure non-negative and within bounds
        positions[i] = (combinedHash & Integer.MAX_VALUE) % bitsetSize;
    }
    
    return positions;
}
```

**Thread-safe add:**
```java
@Override
public void add(T item) {
    if (item == null) {
        throw new NullPointerException("item must not be null");
    }
    
    writeLock.lock();
    try {
        int[] positions = hash(item);
        for (int pos : positions) {
            bits.set(pos);
        }
    } finally {
        writeLock.unlock();
    }
    
    insertions.incrementAndGet(); // Atomic, outside lock
}
```

---

## 6. Technical Pro Tips

### Hash Function Quality

| Approach | Pros | Cons |
|----------|------|------|
| `Object.hashCode()` only | Simple, built-in | Poor distribution for some types |
| `hashCode()` + `reverseBytes()` | Better distribution, fast | Still not cryptographically strong |
| Murmur3 implementation | Excellent distribution | More complex, slower |
| MD5/SHA (truncated) | Very good distribution | Overkill, slow |

**Recommendation**: For interviews, use `hashCode()` + `reverseBytes()`. Mention Murmur3 as a production improvement.

### ReadWriteLock vs ReentrantLock

| Lock Type | Use When | Bloom Filter Fit |
|-----------|----------|------------------|
| `ReentrantLock` | Write-heavy or equal read/write | ❌ Not ideal |
| `ReentrantReadWriteLock` | Read-heavy (reads >> writes) | ✅ Perfect fit |
| `StampedLock` | Optimistic reads, very high throughput | ⚠️ Overkill for this use case |

**Why ReadWriteLock?** In typical usage, `mightContain()` is called far more often than `add()`. ReadWriteLock allows multiple concurrent readers, maximizing throughput.

### What Senior Engineers Demonstrate

1. **Mathematical rigor** — They derive the optimal bit array size and hash count from first principles, not just memorize formulas.
2. **Production awareness** — They mention that real Bloom Filters (e.g., Guava's) use Murmur3 and handle serialization, partitioning for large filters, and concurrent updates with lock striping.
3. **Tradeoff articulation** — They explain when NOT to use a Bloom Filter (e.g., when you need deletion, counting, or exact membership).

---

## 7. Common Mistakes to Avoid

| Mistake | Why It Fails | Fix |
|---------|-------------|-----|
| **Using only one hash function** | All k bits will be the same → filter degenerates to a single-bit check | Use Kirsch-Mitzenmacher: compute h1 and h2, then g_i = h1 + i * h2 |
| **Forgetting null checks** | `null.hashCode()` throws NPE with confusing message | Add explicit null check at method start with clear error message |
| **Not handling negative hash codes** | `hashCode()` can return negative values → array index out of bounds | Use `(hash & Integer.MAX_VALUE) % bitsetSize` or `Math.abs(hash) % bitsetSize` |
| **Incrementing counter inside lock** | Unnecessary contention; counter can be atomic outside lock | Use `AtomicLong.incrementAndGet()` outside the write lock |
| **Using `synchronized` instead of ReadWriteLock** | Serializes all access; misses read concurrency opportunity | Use `ReentrantReadWriteLock` with separate read/write locks |
| **Returning exact count of distinct elements** | Bloom Filters don't track distinct elements natively | Return number of `add()` calls (includes duplicates) or document the limitation |

---

## 8. Verification Checklist

### Functional

- [ ] `add()` followed by `mightContain()` returns `true` for the same item
- [ ] `mightContain()` returns `false` for items never added (most of the time — false positives are possible)
- [ ] `size()` returns the number of `add()` calls made
- [ ] `clear()` resets the filter: `size()` returns 0, `mightContain()` returns `false`
- [ ] `config()` returns the correct configuration

### Thread Safety

- [ ] Concurrent `add()` calls from multiple threads don't lose updates
- [ ] Concurrent `mightContain()` calls proceed in parallel (read lock)
- [ ] Mixed concurrent `add()` and `mightContain()` are safe
- [ ] `clear()` is atomic (no partial state visible to readers)

### Edge Cases

- [ ] `add(null)` throws `NullPointerException`
- [ ] `mightContain(null)` throws `NullPointerException`
- [ ] Config with `expectedInsertions < 1` throws `IllegalArgumentException`
- [ ] Config with `falsePositiveProbability` outside (0, 1) throws `IllegalArgumentException`
- [ ] Adding the same item multiple times increments `size()` but doesn't corrupt the filter

---

## 9. Extension Points (Bonus Discussion)

If you finish early, mention these advanced topics:

1. **Counting Bloom Filter** — Uses an array of counters instead of bits. Supports deletion by decrementing counters. Tradeoff: 4-8x more memory.

2. **Partitioned Bloom Filter** — Splits the bit array into k partitions, one per hash function. Each hash function maps to exactly one partition. Reduces false positives slightly.

3. **Serializable Bloom Filter** — Guava's Bloom Filter supports serialization. You can save it to disk or send it over the network. Requires storing the BitSet, config, and hash strategy.

4. **Lock Striping** — For very high-throughput scenarios, partition the BitSet into segments, each with its own lock. Reduces contention by allowing concurrent writes to different segments.

5. **Optimal Hash Functions** — Production implementations use Murmur3 or CRC32 for better distribution. You can also use cryptographic hashes (SHA-256) truncated to the required bit length.

---

## 10. Production References

| Resource | Why It Matters |
|----------|---------------|
| [Guava BloomFilter](https://github.com/google/guava/blob/master/guava/src/com/google/common/hash/BloomFilter.java) | Production-quality implementation with Murmur3, serialization, and optimal math |
| [Cassandra Bloom Filters](https://cassandra.apache.org/doc/latest/cassandra/data_model/bloom_filters.html) | Real-world use case: avoiding unnecessary disk reads in LSM-tree databases |
| [Kirsch-Mitzenmacher Paper](https://www.eecs.harvard.edu/~kirsch/pubs/bloomfilter/esa06.pdf) | Theoretical foundation for using 2 hash functions to simulate k |
| [Probabilistic Data Structures for Web Analytics and Data Mining](https://highlyscalable.wordpress.com/2012/05/01/probabilistic-structures-web-analytics-data-mining/) | Broader context: Bloom Filters, HyperLogLog, Count-Min Sketch |
| [Bloom Filter Calculator](https://hur.st/bloomfilter/) | Interactive tool to compute optimal parameters for given n and p |

---

*This guideline follows the standard platform challenge template: presentation → edge cases → chain of thinking → communication → implementation → pro tips → mistakes → verification → extensions → references.*
