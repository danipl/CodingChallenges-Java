# Challenge 11: Idempotency Manager - Live Coding Guidelines

## 1. Challenge Presentation

### What You're Building

An **Idempotency Manager** that guarantees operations execute exactly once, even when clients retry due to network
failures. This is critical in payment gateways where a timeout might cause a client to retry a charge — without
idempotency, the user gets charged twice.

The pattern uses a unique **idempotency key** (typically a UUID) to track each operation. The first request with a key
executes the operation and caches the result. Subsequent requests with the same key return the cached result without
re-executing.

### Core Contract

```
Client Request with Idempotency Key
         │
         ▼
┌─────────────────────────────────┐
│  Check if key exists in cache   │
└─────────────────────────────────┘
         │
    ┌────┴────┐
    │         │
  YES        NO
    │         │
    ▼         ▼
┌────────┐  ┌──────────────────┐
│Return  │  │Execute operation │
│cached  │  │and cache result  │
│result  │  └──────────────────┘
└────────┘
```

### Interface Summary

| Method                        | Purpose                                                       |
|-------------------------------|---------------------------------------------------------------|
| `of(Config)`                  | Factory - creates instance with TTL and max cache size        |
| `execute(key, operation)`     | Execute operation with idempotency guarantee (returns result) |
| `executeVoid(key, operation)` | Execute operation with idempotency guarantee (void)           |
| `getCachedResult(key)`        | Retrieve cached result without executing                      |
| `isInProgress(key)`           | Check if operation is currently executing                     |
| `getState(key)`               | Get current state (IN_PROGRESS, SUCCESS, FAILED)              |
| `cleanup()`                   | Remove expired entries beyond TTL                             |
| `size()`                      | Get current cache size                                        |

### What Interviewers Evaluate

1. **Concurrent state management** — Can you use `ConcurrentHashMap` atomic operations correctly? Do you understand
   `computeIfAbsent` vs `compute`?
2. **Race condition handling** — Can you prevent duplicate execution when multiple threads arrive with the same key
   simultaneously?
3. **Memory management** — Do you handle TTL expiration and prevent memory leaks with unbounded cache growth?

---

## 2. Edge & Corner Cases

### How to Identify Them Before Coding

Think about the **lifecycle** of an idempotency key: creation → execution → completion → expiration. What happens at
each transition? What if multiple threads hit the same state?

| # | Edge Case                           | How It Surfaces                                 | How to Handle                                                                               |
|---|-------------------------------------|-------------------------------------------------|---------------------------------------------------------------------------------------------|
| 1 | **Concurrent first requests**       | Two threads arrive with same key simultaneously | Use `computeIfAbsent` to atomically create entry; second thread waits for first to complete |
| 2 | **Operation throws exception**      | Payment gateway returns 500 error               | Cache the failure state; subsequent retries get the same exception                          |
| 3 | **TTL expiration during execution** | Long-running operation exceeds TTL              | Don't expire entries that are IN_PROGRESS; only expire completed entries                    |
| 4 | **Cache size limit exceeded**       | Memory pressure from too many keys              | Trigger cleanup when size exceeds maxCacheSize; or use LRU eviction                         |
| 5 | **Null or empty key**               | Client sends malformed request                  | Throw `NullPointerException` or `IllegalArgumentException`                                  |
| 6 | **Clock drift in tests**            | Tests fail due to system time changes           | Inject `Clock` for deterministic testing                                                    |

### Quick Pre-Implementation Checklist

```
▢ ConcurrentHashMap for thread-safe cache
▢ Atomic state transitions (no check-then-act races)
▢ Handle IN_PROGRESS state (block or return immediately?)
▢ TTL expiration logic (when to check? on access or background?)
▢ Exception handling (cache failures or not?)
▢ Memory safety (max size, cleanup trigger)
```

---

## 3. First Approach - Chain of Thinking

### Minute 0-2: Clarify Requirements

Ask the interviewer:

1. **"What happens when two threads arrive with the same key simultaneously?"**
    - Expected: Only one executes, the other waits for the result

2. **"Should we cache failed operations?"**
    - Expected: Yes, to prevent retrying operations that will fail again

3. **"How should expired entries be cleaned up?"**
    - Expected: Lazy cleanup (on access) or periodic cleanup (explicit method)?

4. **"What's the expected concurrency level?"**
    - Expected: Hundreds or thousands of concurrent requests per second

### Minute 2-5: Design

Sketch this data structure:

```java
ConcurrentHashMap<String, IdempotencyEntry<?>> cache

class IdempotencyEntry<T> {
    State state;           // IN_PROGRESS, SUCCESS, FAILED
    T result;              // cached result (null if not completed)
    Throwable error;       // cached exception (null if success)
    Instant createdAt;     // for TTL expiration
}
```

Key insight: **Use `ConcurrentHashMap.compute()` for atomic state transitions**, not `get()` + `put()`.

### Minute 5-10: Sketch the Core Flow

```java
<T> T execute(String key, Supplier<T> operation) {
    // 1. Try to get existing entry
    IdempotencyEntry<T> entry = cache.get(key);
    
    if (entry != null) {
        // 2. If completed, return cached result
        if (entry.state == SUCCESS) return entry.result;
        if (entry.state == FAILED) throw entry.error;
        
        // 3. If in progress, wait (how?)
        // ... need synchronization mechanism
    }
    
    // 4. Create new entry atomically
    // ... use computeIfAbsent
}
```

**Critical question**: How do we handle the "wait for in-progress" case?

Options:

- **Option A**: Use `CountDownLatch` per entry (complex, memory overhead)
- **Option B**: Use `CompletableFuture` as the cached value (elegant, built-in waiting)
- **Option C**: Use `synchronized` on the entry (simple, but coarse-grained)

**Recommended**: Option B — `CompletableFuture<T>` as the value in the map.

### Minute 10-25: Implement

**Step 1**: Define the entry structure

```java
// Use CompletableFuture to handle waiting
ConcurrentHashMap<String, CompletableFuture<T>> cache
```

**Step 2**: Implement `execute()` with `computeIfAbsent`

```java
public <T> T execute(String key, Supplier<T> operation) {
    CompletableFuture<T> future = cache.computeIfAbsent(key, k -> {
        CompletableFuture<T> newFuture = new CompletableFuture<>();
        
        // Execute asynchronously
        CompletableFuture.runAsync(() -> {
            try {
                T result = operation.get();
                newFuture.complete(result);
            } catch (Throwable t) {
                newFuture.completeExceptionally(t);
            }
        });
        
        return newFuture;
    });
    
    // Wait for result (blocks if in progress)
    try {
        return future.get();
    } catch (ExecutionException e) {
        throw new IdempotencyException("Operation failed", e.getCause());
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new IdempotencyException("Interrupted", e);
    }
}
```

**Step 3**: Implement TTL cleanup

```java
public int cleanup() {
    Instant cutoff = clock.instant().minus(config.ttl());
    int[] removed = {0};
    
    cache.entrySet().removeIf(entry -> {
        CompletableFuture<?> future = entry.getValue();
        if (future.isDone() && entry.getValue().createdAt.isBefore(cutoff)) {
            removed[0]++;
            return true;
        }
        return false;
    });
    
    return removed[0];
}
```

**Step 4**: Handle cache size limits

```java
// In execute(), after computeIfAbsent:
if (cache.size() > config.maxCacheSize()) {
    cleanup();
}
```

---

## 4. Communication Approach During the Interview

### What to Say Out Loud

| Moment           | Say This                                                                                                                                                             |
|------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Starting         | "I'll use `ConcurrentHashMap` with `CompletableFuture` values to handle concurrent requests atomically and provide built-in waiting for in-progress operations."     |
| Before locking   | "I don't need explicit locks here — `computeIfAbsent` gives me atomic compound operations, and `CompletableFuture` handles the synchronization for waiting threads." |
| About TTL        | "I'll check expiration lazily during `cleanup()` rather than on every access, since cleanup can be called periodically by a background task."                        |
| About exceptions | "I'll cache both successes and failures to prevent retrying operations that will fail again — this is critical for payment gateways."                                |

### When Stuck

```
I notice the race condition where two threads could both see the key as absent and both execute.
The risk is duplicate execution, which violates the idempotency guarantee.
Two options: 
  (A) Use computeIfAbsent for atomic creation
  (B) Use synchronized block on the key
I'll go with (A) because it's lock-free and leverages ConcurrentHashMap's built-in atomicity. Does that align with your expectations?
```

---

## 5. Implementation Structure

### Recommended File Layout

```java
public final class IdempotencyManagerImpl implements IdempotencyManager {
    // === Fields ===
    private final Config config;
    private final Clock clock;
    private final ConcurrentHashMap<String, CompletableFuture<Object>> cache;
    
    // === Constructor ===
    public IdempotencyManagerImpl(Config config, Clock clock) { ... }
    
    // === Core method ===
    public <T> T execute(String key, Supplier<T> operation) {
        // 1. computeIfAbsent to create future atomically
        // 2. future.get() to wait for result
        // 3. Handle exceptions
    }
    
    // === Helper methods ===
    private boolean isExpired(CompletableFuture<?> future) { ... }
    private void triggerCleanupIfNecessary() { ... }
}
```

### Key Implementation Pattern

**Atomic creation with `computeIfAbsent`:**

```java
CompletableFuture<Object> future = cache.computeIfAbsent(key, k -> {
    CompletableFuture<Object> newFuture = new CompletableFuture<>();
    
    // Execute in separate thread to avoid blocking computeIfAbsent
    CompletableFuture.runAsync(() -> {
        try {
            Object result = operation.get();
            newFuture.complete(result);
        } catch (Throwable t) {
            newFuture.completeExceptionally(t);
        }
    }, executor);  // Optional: use custom executor
    
    return newFuture;
});

// Wait for result
return (T) future.get();
```

**Why this works:**

- `computeIfAbsent` is atomic — only one thread creates the future
- Other threads get the same future and wait on `get()`
- No explicit locks needed
- `CompletableFuture` handles all synchronization

---

## 6. Technical Pro Tips

### ConcurrentHashMap.computeIfAbsent vs compute

| Method                     | When to Use                  | Atomicity                  |
|----------------------------|------------------------------|----------------------------|
| `computeIfAbsent(key, fn)` | Create value only if absent  | Atomic check-and-create    |
| `compute(key, fn)`         | Update value unconditionally | Atomic read-modify-write   |
| `putIfAbsent(key, value)`  | Simple put if absent         | Atomic, but no computation |

**For this challenge**: Use `computeIfAbsent` because we want to create the `CompletableFuture` only if the key doesn't
exist.

### CompletableFuture.get() vs join()

| Method   | Exception Handling                                  | Use When                            |
|----------|-----------------------------------------------------|-------------------------------------|
| `get()`  | Throws `ExecutionException`, `InterruptedException` | You need to handle interruption     |
| `join()` | Throws unchecked `CompletionException`              | You want simpler exception handling |

**For this challenge**: Use `get()` because we need to handle `InterruptedException` and re-interrupt the thread.

### Production vs Interview Considerations

**Interview**:

- Focus on correctness and thread safety
- Use `CompletableFuture` for elegance
- Skip background cleanup thread (just provide `cleanup()` method)

**Production**:

- Add metrics (cache hit rate, execution time)
- Use a bounded executor for async operations
- Add distributed cache (Redis) for multi-node deployments
- Implement graceful shutdown (wait for in-progress operations)
- Add circuit breaker integration (fail fast if downstream is down)

### What Senior Engineers Demonstrate

1. **Atomic thinking** — They immediately reach for `computeIfAbsent` instead of `get()` + `put()`
2. **Memory awareness** — They ask about TTL and cache size limits before implementing
3. **Exception strategy** — They cache failures to prevent retrying doomed operations

---

## 7. Common Mistakes to Avoid

| Mistake                             | Why It Fails                                                                           | Fix                                                                      |
|-------------------------------------|----------------------------------------------------------------------------------------|--------------------------------------------------------------------------|
| **Check-then-act race**             | `if (!cache.containsKey(key)) cache.put(key, value)` — two threads can both see absent | Use `computeIfAbsent` for atomic creation                                |
| **Blocking inside computeIfAbsent** | `computeIfAbsent` holds a lock; blocking inside causes deadlock                        | Execute operation asynchronously, return `CompletableFuture` immediately |
| **Not caching failures**            | Retries re-execute operations that will fail again (wastes resources)                  | Cache both successes and failures                                        |
| **Expiring IN_PROGRESS entries**    | Long-running operations get expired mid-execution, causing duplicate execution         | Only expire completed entries (SUCCESS or FAILED)                        |
| **Using synchronized**              | Coarse-grained locking reduces concurrency                                             | Use `ConcurrentHashMap` atomic operations + `CompletableFuture`          |
| **Ignoring InterruptedException**   | Swallowing interrupt breaks thread shutdown                                            | Re-interrupt thread: `Thread.currentThread().interrupt()`                |

---

## 8. Verification Checklist

### Functional

- [ ] First execution returns operation result
- [ ] Second execution with same key returns cached result
- [ ] Operation executes exactly once (verify with counter)
- [ ] Different keys execute independently
- [ ] Failed operations are cached (retries get same exception)
- [ ] `getCachedResult()` returns cached value
- [ ] `getState()` returns correct state (IN_PROGRESS, SUCCESS, FAILED)

### Thread Safety

- [ ] Concurrent executions with same key only execute once
- [ ] No race conditions in cache creation
- [ ] Concurrent reads and writes don't corrupt state
- [ ] Interruption is handled correctly

### Edge Cases

- [ ] Null key throws `NullPointerException`
- [ ] Config validation rejects invalid TTL and maxCacheSize
- [ ] Cleanup removes expired entries
- [ ] Cleanup doesn't remove IN_PROGRESS entries
- [ ] Cache size limit triggers cleanup

---

## 9. Extension Points (Bonus Discussion)

If you finish early, mention these advanced topics:

1. **Distributed idempotency** — How would you extend this to work across multiple nodes? (Redis with TTL, database
   constraints)

2. **Idempotency key generation** — Should the client generate the key, or should the server? (Client-generated is
   standard for retries)

3. **Idempotency window** — How long should we keep keys? (24 hours is typical for payment gateways)

4. **Idempotent vs safe methods** — GET requests are safe (no side effects), POST requests need idempotency keys

5. **Compensation logic** — What if we need to undo a successful operation? (Refund, rollback)

---

## 10. Production References

| Resource                                                                                                                              | Why It Matters                                                   |
|---------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------------|
| [Stripe Idempotency Docs](https://stripe.com/docs/api/idempotent_requests)                                                            | Industry-standard implementation for payment processing          |
| [AWS API Gateway Idempotency](https://docs.aws.amazon.com/apigateway/latest/developerguide/api-gateway-idempotency.html)              | AWS's approach to idempotency in API gateways                    |
| [Idempotent Receiver Pattern](https://www.enterpriseintegrationpatterns.com/IdempotentReceiver.html)                                  | Original pattern from Enterprise Integration Patterns            |
| [ConcurrentHashMap Javadoc](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/ConcurrentHashMap.html) | Deep dive into atomic operations and performance characteristics |
| [CompletableFuture Javadoc](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/concurrent/CompletableFuture.html) | Understanding async composition and exception handling           |

---

*This guideline follows the standard platform challenge template: presentation → edge cases → chain of thinking →
communication → implementation → pro tips → mistakes → verification → extensions → references.*
