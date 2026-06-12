# Challenge 11: Bulkhead Isolation - Live Coding Guidelines

## 1. Challenge Presentation

### What You're Building

A thread-safe Bulkhead that isolates concurrent executions per partition (tenant, endpoint, service).
When one partition hits its concurrency limit, callers are rejected or timeout — without affecting other partitions.
This is the core pattern behind multi-tenant isolation at companies like Revolut, where a noisy customer must not
degrade service for everyone else.

### Core Contract

```
┌─────────────────────────────────────────────────┐
│                   Bulkhead                       │
│                                                  │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐      │
│  │tenant-a  │  │tenant-b  │  │tenant-c  │      │
│  │ limit: 4 │  │ limit: 2 │  │ limit: 8 │      │
│  │ active:3 │  │ active:0 │  │ active:5 │      │
│  │ [||| ]   │  │ [  ]     │  │ [|||||  ]│      │
│  └──────────┘  └──────────┘  └──────────┘      │
│                                                  │
│  Each partition has its own Semaphore.           │
│  Exhausting one does NOT affect others.          │
└─────────────────────────────────────────────────┘
```

### Interface Summary

| Method | Purpose |
|--------|---------|
| `of(Config)` | Factory — creates a new Bulkhead instance |
| `registerPartition(key, maxConcurrency)` | Register or update a partition's concurrency limit |
| `removePartition(key)` | Remove a partition (in-flight ops continue) |
| `execute(key, operation)` | Run operation within partition's concurrency limit |
| `acquire(key)` | Non-blocking slot acquisition, returns a Permit |
| `activeCount(key)` | Current in-flight executions for a partition |
| `totalActiveCount()` | Global active count across all partitions |
| `metrics()` | Snapshot of all partition metrics |
| `registeredPartitions()` | Set of all registered partition keys |

### What Interviewers Evaluate

1. **Semaphore understanding** — Can you use `Semaphore` for concurrency limiting, not just `synchronized`?
2. **Partition isolation** — Do you ensure each partition has independent state (no shared bottleneck)?
3. **Thread safety under contention** — Can you handle concurrent register/execute/remove without race conditions?
4. **Resource cleanup** — Does `Permit.close()` correctly release the semaphore and update counters?

---

## 2. Edge & Corner Cases

### How to Identify Them Before Coding

Think about lifecycle: partition creation → execution → removal. At each stage, what can go wrong?

| # | Edge Case | How It Surfaces | How to Handle |
|---|-----------|-----------------|---------------|
| 1 | **Partition not registered** | `execute()` called on unknown key | Throw `PartitionNotFoundException` (or auto-create if configured) |
| 2 | **Concurrent limit reached** | All semaphore permits taken | Block up to timeout, then throw `BulkheadRejectedException` |
| 3 | **Double-close of Permit** | `close()` called twice on same permit | Use `AtomicBoolean` guard — second call is no-op |
| 4 | **Partition removed while active** | `removePartition()` called with in-flight ops | Mark as removed, don't interrupt — let ops finish naturally |
| 5 | **Register with zero concurrency** | `registerPartition(key, 0)` | Validate and throw `IllegalArgumentException` |
| 6 | **Max partitions exceeded** | More partitions registered than `maxPartitions` | Reject with `IllegalStateException` |
| 7 | **Operation throws exception** | Protected operation fails | Must still release the semaphore (use try/finally) |
| 8 | **Null or empty partition key** | `execute(null, ...)` or `execute("", ...)` | Validate early, throw `IllegalArgumentException` |

### Quick Pre-Implementation Checklist

```
▢ Partition key validation (null, empty)
▢ Semaphore.acquire() wrapped with timeout
▢ try/finally ensures semaphore release on exception
▢ Permit.close() is idempotent (AtomicBoolean guard)
▢ Registry lock protects partition map mutations
▢ Metrics counters use AtomicLong (lock-free reads)
```

---

## 3. First Approach - Chain of Thinking

### Minute 0-2: Clarify Requirements

- "Should partitions be auto-created on first use, or must they be registered explicitly?"
- "What happens when a partition is removed — do we interrupt in-flight operations?"
- "Is the timeout per-acquisition, or is there also a queue capacity?"
- "Do we need metrics (active count, rejection count) or just the core isolation?"

### Minute 2-5: Design

**Data structures:**
- `ConcurrentHashMap<String, Partition>` — partition registry
- Each `Partition` contains: `Semaphore` (concurrency limit), `AtomicInteger` (active count), `AtomicLong` (metrics)
- `ReentrantReadWriteLock` — protects registry mutations (register/remove)
- `Clock` — for timeout calculations

**Key insight:** The Semaphore IS the bulkhead. No need for a separate queue — `Semaphore.tryAcquire(timeout)` handles blocking.

### Minute 5-10: Sketch the Core Flow

```java
public T execute(String key, SupplierWithException<T> op) throws Exception {
    Partition p = getPartitionOrThrow(key);
    if (!p.semaphore.tryAcquire(config.timeoutMs(), MILLISECONDS)) {
        p.totalRejected.incrementAndGet();
        throw new BulkheadRejectedException(...);
    }
    try {
        p.activeCount.incrementAndGet();
        return op.get();
    } finally {
        p.activeCount.decrementAndGet();
        p.totalSuccessful.incrementAndGet();
        p.semaphore.release();
    }
}
```

### Minute 10-25: Implement

1. Fields: `Config`, `Clock`, `ReentrantReadWriteLock`, `ConcurrentHashMap<String, Partition>`
2. `registerPartition()` — write-lock, validate, put new Partition
3. `getPartitionOrThrow()` — read-lock, lookup, throw if missing
4. `execute()` — acquire semaphore, increment active, run op, finally release
5. `acquire()` — tryAcquire(0), return BulkheadPermit
6. `BulkheadPermit.close()` — release semaphore, decrement active (idempotent)
7. `metrics()` — iterate partitions, snapshot each

---

## 4. Communication Approach During the Interview

### What to Say Out Loud

| Moment | Say This |
|--------|----------|
| Starting | "I'll use a Semaphore per partition for concurrency limiting. Each partition is fully independent — no shared bottleneck." |
| Before locking | "I need a ReadWriteLock for the registry map since register/remove are rare but execute is hot. Read-lock for lookups, write-lock for mutations." |
| About exception safety | "The finally block is critical — if the operation throws, we MUST release the semaphore and decrement active count, or the partition leaks." |
| About Permit | "Permit uses AtomicBoolean for idempotent close — calling close() twice shouldn't double-release the semaphore." |

### When Stuck

```
I notice the semaphore acquire and activeCount increment aren't atomic.
The risk is a brief window where activeCount is stale between acquire and increment.
Two options: (A) accept the race for metrics (they're approximate anyway), or (B) use a combined lock.
I'll go with (A) because metrics are point-in-time snapshots, not exact counters. Does that align with your expectations?
```

---

## 5. Implementation Structure

### Recommended File Layout

```java
public final class BulkheadImpl<T> implements Bulkhead<T> {
    // === Fields ===
    private final Config config;
    private final Clock clock;
    private final ReentrantReadWriteLock registryLock;
    private final Map<String, Partition<T>> partitions;

    // === Constructors ===
    // === Public methods (execute, acquire, register, remove) ===
    // === Private helpers (getPartitionOrThrow, getOrCreatePartition) ===
    // === Inner class: Partition (Semaphore + counters) ===
    // === Inner class: BulkheadPermit (AutoCloseable) ===
}
```

### Key Implementation Pattern

```java
// execute() — the core pattern
public T execute(String key, SupplierWithException<T> op) throws Exception {
    Partition<T> p = getPartitionOrThrow(key);

    if (!p.semaphore.tryAcquire(config.timeoutMs(), MILLISECONDS)) {
        p.totalRejected.incrementAndGet();
        throw new BulkheadRejectedException("Partition '" + key + "' is full");
    }

    try {
        p.activeCount.incrementAndGet();
        return op.get();
    } catch (Exception e) {
        throw e;
    } finally {
        p.activeCount.decrementAndGet();
        p.totalSuccessful.incrementAndGet();
        p.semaphore.release();
    }
}
```

---

## 6. Technical Pro Tips

### Semaphore vs ReentrantLock for Bulkhead

| Feature | Semaphore | ReentrantLock |
|---------|-----------|---------------|
| Concurrency limit | N permits | 1 (or fair queue) |
| Timeout support | `tryAcquire(timeout)` | `tryLock(timeout)` |
| Permits tracking | `availablePermits()` | Manual counter needed |
| Reentrancy | No | Yes |
| Best for | Bulkhead, thread pools | State machines, exclusive access |

**Why Semaphore wins:** A bulkhead IS a semaphore — it limits N concurrent actors. ReentrantLock is for mutual exclusion (N=1).

### Production vs Interview Considerations

- **Production**: Resilience4j's `Bulkhead` uses a `Semaphore` internally — same approach. They also add metrics export, event listeners, and CircuitBreaker integration.
- **Interview**: Focus on the core: partition isolation, semaphore acquire/release, thread safety. Metrics are a bonus.

### What Senior Engineers Demonstrate

1. **Composition over inheritance** — `Partition` is a private static class, not a base class.
2. **Defensive copying** — `metrics()` returns a snapshot, not a live view.
3. **Fail-fast validation** — null/empty key checks at method entry, not deep inside logic.
4. **Idempotent cleanup** — `Permit.close()` is safe to call multiple times (try-with-resources friendly).

---

## 7. Common Mistakes to Avoid

| Mistake | Why It Fails | Fix |
|---------|-------------|-----|
| Using `synchronized` on the whole Bulkhead | Single bottleneck — defeats partition isolation purpose | Use per-partition Semaphore |
| Forgetting `finally` block on semaphore release | Exception in operation → semaphore permit leaked → partition deadlocks | Always wrap in try/finally |
| Not guarding Permit.close() with AtomicBoolean | Double-release inflates semaphore permits beyond limit | `if (!released.compareAndSet(false, true)) return;` |
| Using `HashMap` instead of `ConcurrentHashMap` | ConcurrentModificationException during iteration | `ConcurrentHashMap` for the registry |
| Blocking inside a write lock | `execute()` holds write lock while operation runs → all other ops blocked | Read-lock only for lookup, no lock during operation |
| Metrics counters without atomic types | Lost updates under concurrent access | `AtomicLong` for totalRejected, totalSuccessful |

---

## 8. Verification Checklist

### Functional

- [ ] `execute()` returns operation result
- [ ] `execute()` throws `BulkheadRejectedException` when partition is full
- [ ] `execute()` throws `PartitionNotFoundException` for unregistered key (autoCreate=false)
- [ ] `activeCount` increments during execution, decrements after
- [ ] `Permit.close()` releases the semaphore slot
- [ ] `removePartition()` doesn't interrupt in-flight operations

### Thread Safety

- [ ] Concurrent `execute()` on different partitions don't block each other
- [ ] Concurrent `registerPartition()` and `execute()` don't race
- [ ] `metrics()` can be called while operations are in-flight
- [ ] `Permit.close()` is idempotent under concurrent calls

### Edge Cases

- [ ] Config validation rejects invalid values
- [ ] Operation exception propagates (not swallowed)
- [ ] Empty/null partition key throws `IllegalArgumentException`
- [ ] Double-close of Permit is safe

---

## 9. Extension Points (Bonus Discussion)

1. **Queue capacity** — Add a bounded wait queue per partition (not just timeout). Use `Semaphore` + `BlockingQueue` for FIFO ordering.
2. **Priority partitions** — Some tenants get higher priority. Implement weighted semaphore allocation.
3. **CircuitBreaker integration** — Combine Bulkhead with CircuitBreaker: reject fast when circuit is open, isolate when bulkhead is full.
4. **Virtual thread awareness** — With Java 21 virtual threads, the bulkhead becomes even more critical since millions of VTs can be spawned.
5. **Metrics export** — Integrate with Micrometer for Prometheus/Grafana dashboards (partition-level P99 latency, rejection rate).

---

## 10. Production References

| Resource | Why It Matters |
|----------|---------------|
| [Resilience4j Bulkhead](https://resilience4j.readme.io/docs/bulkhead) | Production implementation — uses Semaphore internally, same pattern |
| [Netflix Hystrix Thread Pool Isolation](https://github.com/Netflix/Hystrix/wiki/How-it-Works) | Alternative approach: separate thread pools per partition (heavier but stronger isolation) |
| [Go golang.org/x/sync/semaphore](https://pkg.go.dev/golang.org/x/sync/semaphore) | Same pattern in Go — shows this is a universal concurrency primitive |
| [Revolut Engineering Blog](https://engineering.revolut.com/) | Real-world multi-tenant isolation challenges in fintech |

---

*This guideline follows the standard platform challenge template: presentation → edge cases → chain of thinking →
communication → implementation → pro tips → mistakes → verification → extensions → references.*
