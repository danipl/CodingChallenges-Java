# Challenge 04: Rate Limiter - Live Coding Guidelines

## 1. Challenge Presentation

### What You're Building

A **thread-safe Rate Limiter** using the Token Bucket algorithm — controls throughput by allowing bursts up to capacity,
then sustaining a steady refill rate. Protects services from overload while permitting legitimate traffic spikes.

### Core Contract

```
                    refill (rate/sec)
                       │
          ┌────────────▼────────────┐
          │      TOKEN BUCKET       │
          │  capacity = N tokens    │
          │  refill at R tokens/sec │
          │                         │
  tryAcquire() ── token consumed    │
  tryAcquire(n) ── n tokens consumed
  (reject if insufficient tokens)   │
          └─────────────────────────┘
```

### Algorithm: Token Bucket

| Property        | Behavior                                             |
|-----------------|------------------------------------------------------|
| **Capacity**    | Max tokens the bucket holds (burst ceiling)          |
| **Refill Rate** | Tokens added per second (sustained throughput limit) |
| **Acquire**     | Consumes token(s) if available; rejects otherwise    |
| **Blocking**    | Optionally wait up to timeout for a token to arrive  |
| **Burst**       | Empty bucket refilled → full capacity (unlike leaky) |

### Interface Summary

| Method                                            | Purpose                                |
|---------------------------------------------------|----------------------------------------|
| `of(double capacity, double refillRatePerSecond)` | Factory - creates instance with config |
| `boolean tryAcquire()`                            | Non-blocking single token acquire      |
| `boolean tryAcquire(int tokens)`                  | Non-blocking multi-token acquire       |
| `boolean tryAcquire(long timeout, TimeUnit unit)` | Blocking acquire with deadline         |
| `int availableTokens()`                           | Observable current token count         |

### What Interviewers Evaluate

1. **Token bucket correctness** - burst, refill, cap-at-max, multi-token semantics
2. **Thread safety** - concurrent acquires don't over-issue tokens
3. **Refill precision** - time-based refill matches the configured rate
4. **Blocking acquire semantics** - timeout actually waits, respects interrupts
5. **Resource lifecycle** - executors shut down properly

---

## 2. Edge & Corner Cases

### How to Identify Them Before Coding

Draw the token bucket lifecycle. Every operation is a potential edge case.

| #  | Edge Case                               | How It Surfaces                                             | How to Handle                                                         |
|----|-----------------------------------------|-------------------------------------------------------------|-----------------------------------------------------------------------|
| 1  | **Zero/negative capacity**              | `RateLimiter.of(0, 1.0)` or `RateLimiter.of(-5, 1.0)`       | Validate in constructor, throw `IllegalArgumentException`             |
| 2  | **Negative refill rate**                | `RateLimiter.of(10, -5.0)`                                  | Validate - must be >= 0 (zero means no refill)                        |
| 3  | **capacity=1**                          | Single token bucket, acquire-drain-refill cycle             | No special logic; ensure `== 0` check, not `< 0`                      |
| 4  | **Concurrent acquires**                 | 10 threads × 15 attempts, capacity=100                      | All token operations under lock; exactly 100 should succeed           |
| 5  | **Refill overshoot**                    | High refill rate + coarse interval → tokens exceed capacity | Cap refill at `min(capacity, current + tokens)` or add 1-at-a-time    |
| 6  | **tryAcquire(N) where N > capacity**    | Request more tokens than bucket can ever hold               | Reject immediately - no point checking                                |
| 7  | **tryAcquire(0)**                       | Zero-token request                                          | Always succeed - no tokens consumed                                   |
| 8  | **tryAcquire(-1)**                      | Negative token request                                      | Reject - return `false`                                               |
| 9  | **Timeout acquire, no refill**          | `refillRate=0`, bucket empty, `tryAcquire(500, ms)`         | Must return `false` after timeout expires                             |
| 10 | **Timeout acquire, refill during wait** | Bucket empty, tokens arriving mid-wait                      | Must return `true` as soon as token available (not wait full timeout) |
| 11 | **Interrupted during blocking acquire** | `Thread.interrupt()` while `tryAcquire(timeout)` sleeps     | Propagate `InterruptedException`, restore interrupt status            |
| 12 | **Refill caps at max**                  | Idle bucket at rate=100/sec, sleep 2 sec                    | Available tokens never exceed initial capacity                        |

### Quick Pre-Implementation Checklist

```
▢ capacity > 0 validated
▢ refillRatePerSecond >= 0 validated
▢ Bucket starts full (at capacity)
▢ tryAcquire() decrements by 1, rejects at 0
▢ tryAcquire(n) decrements by n, rejects if insufficient
▢ tryAcquire(n) rejects if n > capacity
▢ tryAcquire(0) always succeeds
▢ tryAcquire(-1) returns false
▢ Refill adds 1 token at a time, capped at capacity
▢ Blocking acquire polls until deadline, not fixed sleep
▢ InterruptedException propagated
▢ All bucket mutations under write lock
▢ availableTokens() under read lock
```

---

## 3. First Approach - Chain of Thinking

### Minute 0-2: Clarify Requirements

Ask the interviewer:

- _"Should this be token bucket, or another algorithm like leaky bucket or sliding window?"_ → Token bucket.
- _"Can `tryAcquire(n)` request more than capacity?"_ → No, reject immediately.
- _"Should blocking acquire poll or use wait/notify?"_ → Polling is fine for interview; mention `Condition` as
  production improvement.
- _"Do I need to implement `close()` / resource cleanup?"_ → Yes, mention `AutoCloseable` even if time is short.

### Minute 2-5: Design the Token Bucket

Sketch on whiteboard/shared doc:

```
Fields needed:
  - int tokensBucket          ← current token count
  - int capacity              ← maximum tokens
  - int refillRatePerSecond   ← tokens added per second
  - ReentrantReadWriteLock    ← protects bucket state
  - ScheduledExecutorService  ← periodic refill
  - (Optional) Condition      ← for efficient blocking acquire

Key insight:
  - Refill interval = max(1, 1000 / refillRatePerSecond) ms
  - Each interval adds 1 token, capped at capacity
  - Higher refill rate = shorter interval between single-token additions
```

### Minute 5-10: Sketch the Core Flow

```java
boolean tryAcquire(int tokens) {
    writeLock.lock();
    try {
        if (tokensBucket < tokens) return false;
        tokensBucket -= tokens;
        return true;
    } finally {
        writeLock.unlock();
    }
}

// Refill tick
Runnable tokenCreator = () -> {
    writeLock.lock();
    try {
        if (tokensBucket < capacity) tokensBucket++;
    } finally {
        writeLock.unlock();
    }
};
```

### Minute 10-25: Implement

1. Constructor with validation → done
2. `tryAcquire()` single token → done
3. `tryAcquire(int)` multi-token → done
4. Refill scheduler → done
5. `tryAcquire(timeout)` polling loop → done
6. `availableTokens()` reader → done

---

## 4. Communication Approach During the Interview

### What to Say Out Loud

| Moment            | Say This                                                                                                                                                                                               |
|-------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Starting          | "I'll implement a token bucket. It allows bursts up to capacity, then sustains at the refill rate. I'll use a ReentrantReadWriteLock - write lock for mutations, read lock for queries."               |
| Before scheduling | "I'll use `scheduleAtFixedRate` with interval = 1000 / refillRate. Each tick adds 1 token. This avoids the 'all-at-once refill' problem of scheduling once per second."                                |
| About locking     | "Every bucket read-modify-write is under write lock. The `availableTokens()` uses read lock so multiple observers don't block each other."                                                             |
| About blocking    | "For `tryAcquire(timeout)`, I'll use a polling loop with a nanosecond deadline. In production I'd use `Condition.awaitNanos()` to avoid busy-wait, but polling is correct and simpler for this scope." |
| About precision   | "I'm using `System.nanoTime()` for deadline calculation, not `System.currentTimeMillis()`. `nanoTime` is monotonic - not affected by wall-clock changes."                                              |
| About overflow    | "Each refill tick adds exactly 1 token, capped at capacity. This prevents overshoot even at high refill rates."                                                                                        |

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
public class RateLimiterImpl implements RateLimiter {
    // === Fields ===
    private double capacity;
    private int tokensBucket = 0;
    private final ReentrantReadWriteLock lock;
    private final ScheduledExecutorService scheduler;

    // === Constructor ===
    //   - Validate capacity > 0
    //   - Validate refillRate >= 0
    //   - Set tokensBucket = capacity
    //   - Start scheduler if refillRate > 0

    // === tryAcquire() ===              ← single token, ~10 lines
    // === tryAcquire(int tokens) ===    ← multi-token, guard clauses + check
    // === tryAcquire(timeout, unit) === ← polling loop with nanoTime deadline
    // === availableTokens() ===         ← read lock, simple getter
    // === tokenCreator runnable ===     ← scheduler callback, +1 capped
}
```

### Key Implementation Patterns

**Refill interval calculation:**

```java
final int refillAtMillis = Math.max(1, 1000 / refillRatePerSecond);
scheduler.scheduleAtFixedRate(tokenCreator, 0, refillAtMillis, MILLISECONDS);
```

- `refillRate = 10` → 1 token every 100ms → ~10/sec
- `refillRate = 100` → 1 token every 10ms → ~100/sec
- `refillRate = 0` → no scheduler started

**Blocking acquire with deadline:**

```java
final long deadlineNanos = System.nanoTime() + unit.toNanos(timeout);
while (System.nanoTime() < deadlineNanos) {
    if (tryAcquire()) return true;
    Thread.sleep(1);
}
return tryAcquire();  // final chance at deadline boundary
```

**Why the final `tryAcquire()` after the loop?** Catches the race where a token arrives in the last nanosecond before
the loop exits.

---

## 6. Technical Pro Tips

### Thread Safety: Lock Choices

| Approach                         | Pros                                          | Cons                             | When to Use                               |
|----------------------------------|-----------------------------------------------|----------------------------------|-------------------------------------------|
| `ReentrantReadWriteLock`         | Separate read/write, clear reasoning          | Write lock blocks all readers    | **Interview default** - easy to explain   |
| `ReentrantLock` (plain)          | Simplest, supports `tryLock`                  | Readers blocked during reads too | Good if `availableTokens()` rarely called |
| `AtomicInteger` + CAS            | Lock-free, better throughput under contention | Compound check-and-update tricky | Production high-throughput systems        |
| `AtomicInteger` + `updateAndGet` | Lock-free, safe for compound ops              | Lambda allocation per call       | Modern alternative to locks               |

### Token Bucket vs Other Rate Limiting Algorithms

| Algorithm                  | Memory | Accuracy | Burst Handling       | Best For                                  |
|----------------------------|--------|----------|----------------------|-------------------------------------------|
| **Token Bucket**           | O(1)   | Exact    | **Allows bursts**    | **Default choice** - burst + average rate |
| **Leaky Bucket**           | O(1)   | Exact    | Smooths output       | Network shaping, strict rate enforcement  |
| **Fixed Window**           | O(1)   | Approx   | 2x burst at boundary | Simple internal limits, login throttling  |
| **Sliding Window Log**     | O(n)   | Exact    | No bursts            | High-value APIs, billing-grade            |
| **Sliding Window Counter** | O(2)   | ~99%     | Smooth approximation | Distributed APIs, general purpose         |

**Why Token Bucket for this challenge?**

- Allows legitimate bursts (user clicking, batch jobs)
- Simple O(1) memory
- Natural "capacity + refill" mental model
- What Guava's `RateLimiter` implements

### Guava RateLimiter Comparison

What Guava does that interviewers appreciate you knowing:

| Feature              | This Challenge           | Guava RateLimiter                                      |
|----------------------|--------------------------|--------------------------------------------------------|
| Algorithm            | Token bucket (discrete)  | Smooth bursty (continuous, microsecond precision)      |
| Burst behavior       | Up to capacity instantly | Stored permits + underutilization tracking             |
| Blocking             | Polling loop             | `Thread.sleep` with precise wake time                  |
| Multi-permit acquire | `tryAcquire(n)`          | `acquire(n)` - blocks, never rejects                   |
| Warm-up mode         | Not included             | `SmoothWarmingUp` - gradual rate ramp for cold servers |
| Precision            | Millisecond (int tokens) | Microsecond (double permits)                           |

### Rate Limiter vs Related Patterns

Interviewers often ask: _"When would you use Rate Limiter vs Circuit Breaker vs Retry?"_

| Pattern             | Problem It Solves             | Key Question                                 |
|---------------------|-------------------------------|----------------------------------------------|
| **Rate Limiter**    | Protect service from overload | "Am I sending too many requests?"            |
| **Circuit Breaker** | Downstream is unhealthy       | "Is the other service down?"                 |
| **Retry**           | Transient failures            | "Did this fail because of a temporary blip?" |
| **Bulkhead**        | Isolate resources             | "Should one slow call affect others?"        |

**Production pattern**: Combine all four. Rate Limiter protects ingress, Circuit Breaker stops hitting dead downstream
services, Retry recovers from transient errors, Bulkhead isolates resource pools.

### Refill Strategy: Interval vs Elapsed-Time

| Strategy               | Implementation                | Pros                         | Cons                                 |
|------------------------|-------------------------------|------------------------------|--------------------------------------|
| **Fixed interval**     | `scheduleAtFixedRate` + tick  | Simple, works for interview  | Coarse - integer division truncation |
| **Elapsed-time**       | `nanoTime` delta on each call | Exact precision, no drift    | More complex, needs state tracking   |
| **Continuous (Guava)** | `nextFreeTicketMicros`        | Smooth, handles underutilize | Complex math                         |

The challenge uses fixed-interval (1 token at a time) because it's:

- Simple to implement under time pressure
- Correct enough (error < 1 token/sec even at truncation boundaries)
- Easy to explain

In production, elapsed-time calculation is preferred:

```java
long now = System.nanoTime();
long elapsedNanos = now - lastRefillTime;
double tokensToAdd = (elapsedNanos / 1e9) * refillRatePerSecond;
if (tokensToAdd >= 1.0) {
    tokensBucket = (int) Math.min(capacity, tokensBucket + tokensToAdd);
    lastRefillTime = now;
}
```

### Testing Strategy

```java
// Without Clock (what tests do - acceptable for interview)
Thread.sleep(200);  // Wait for refill
assertTrue(rl.availableTokens() >= 1);

// With Clock (production - not possible with ScheduledExecutorService)
// Would need injectable time source + manual tick mechanism
```

**Limitation of current design**: `ScheduledExecutorService` uses real wall-clock time. Tests rely on `Thread.sleep()`
which is flaky under CI load. Fix: use elapsed-time refill with injectable `Clock`, and tests advance time
programmatically.

### What Senior Engineers Demonstrate

1. **Algorithm awareness** - "I chose token bucket over leaky bucket because it allows bursts while maintaining average
   throughput"
2. **Precision tradeoffs** - "For production I'd use nanosecond elapsed-time calculation instead of fixed-interval ticks
   to avoid truncation error"
3. **Blocking efficiency** - "Polling works but wastes CPU. `Condition.awaitNanos()` would be zero-waste - signal on
   refill, sleep until token or deadline"
4. **Resource lifecycle** - "Real applications need `AutoCloseable` to shut down the scheduler. Leaked threads
   accumulate in long-running services"
5. **Distributed scaling** - "This is a local rate limiter. For distributed systems I'd use Redis with Lua scripts for
   atomic token operations"
6. **Observability mindset** - "In production I'd add metrics: acquisition rate, rejection rate, wait time percentiles"

---

## 7. Common Mistakes to Avoid

| Mistake                                                  | Why It Fails                                     | Fix                                                                            |
|----------------------------------------------------------|--------------------------------------------------|--------------------------------------------------------------------------------|
| Refill all tokens once per second                        | Burst behavior wrong - 10 tokens arrive at once  | Add 1 token at a time at `1000/refillRate` ms interval                         |
| Refill can overflow capacity                             | Tokens exceed max, breaks contract               | `Math.min(capacity, current + refill)` or check before                         |
| `tryAcquire(timeout)` uses fixed `Thread.sleep(timeout)` | Returns `false` immediately if no token ready    | Poll loop with small sleep + nanosecond deadline                               |
| Checking bucket outside lock, then modifying             | Race: bucket changes between check and update    | Entire check-and-modify under write lock                                       |
| Using `System.currentTimeMillis()` for deadline          | Wall-clock changes (NTP, DST) break timing       | Use `System.nanoTime()` - monotonic                                            |
| `AtomicInteger` + `ReentrantLock` together               | Redundant - lock already serializes access       | Pick one: lock + plain int, or AtomicInteger with CAS                          |
| No validation on constructor params                      | `capacity = 0` or negative causes weird behavior | Validate early, throw `IllegalArgumentException`                               |
| Not handling `InterruptedException` properly             | Broken interrupt chain propagates up             | Re-throw or restore interrupt status with `Thread.currentThread().interrupt()` |

---

## 8. Verification Checklist

Before declaring done, verify:

### Functional

- [ ] Bucket starts at full capacity
- [ ] Single acquire decrements by 1
- [ ] Multi-acquire decrements by N
- [ ] Acquire fails when insufficient tokens
- [ ] Acquire fails when N > capacity
- [ ] Acquire(0) always succeeds
- [ ] Acquire(-1) returns false
- [ ] Refill adds tokens over time
- [ ] Refill caps at max capacity
- [ ] Blocking acquire returns true when token arrives
- [ ] Blocking acquire returns false when timeout expires
- [ ] Blocking acquire respects interrupt

### Thread Safety

- [ ] All bucket mutations under write lock
- [ ] `availableTokens()` under read lock
- [ ] Concurrent acquires don't over-issue (10 threads × 15 attempts = exactly 100)
- [ ] Refill concurrent with acquire doesn't cause race
- [ ] No deadlocks (single lock acquisition path)

### Edge Cases

- [ ] capacity=1 works correctly
- [ ] refillRate=0 (no refill) - bucket drains and stays empty
- [ ] High refill rate (100/sec) - tokens arrive fast, cap at max
- [ ] Timeout with refill during wait - returns before full timeout
- [ ] Timeout with refillRate=0 - always times out

---

## 9. Extension Points (Bonus Discussion)

If you finish early, mention these to stand out:

1. **`AutoCloseable` for scheduler** - "I'd add `close()` to shut down the `ScheduledExecutorService`. Without it, each
   rate limiter leaks a thread."
2. **`Condition` variable for blocking acquire** - "`Condition.awaitNanos()` with `signalAll()` on refill would
   eliminate polling overhead. Zero CPU waste versus 1ms spin."
3. **Elapsed-time refill** - "Instead of `scheduleAtFixedRate`, I'd compute tokens based on nanoTime delta. Exact
   precision, no interval truncation error."
4. **Distributed rate limiting** - "For multi-instance services, I'd use Redis with Lua scripts. The token bucket
   state (tokens, lastRefillTime) lives in Redis. Atomicity via Lua eliminates read-write races."
5. **Hierarchical rate limiting** - "Per-user + per-IP + global. Three rate limiters chained: user-level first (
   fastest), then IP, then global. Rejected at any level means rejected."
6. **Metrics and observability** - "Track acquisition rate, rejection rate, P50/P99 wait times. Alert when rejection
   rate exceeds threshold - indicates service under attack or misconfigured limits."
7. **Adaptive rate limiting** - "Adjust refill rate based on service health. High error rate → lower rate. Low latency →
   allow higher burst. Self-tuning based on downstream signals."

---

## 10. Production References

| Resource                                                                                                                      | Why It Matters                                             |
|-------------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------|
| [Guava RateLimiter](https://github.com/google/guava/blob/master/guava/src/com/google/common/util/concurrent/RateLimiter.java) | Most popular Java rate limiter - token bucket variant      |
| [Resilience4j RateLimiter](https://resilience4j.readthedocs.io/en/latest/docs/ratelimiter.html)                               | Production-ready, integrates with resilience ecosystem     |
| [Redis Rate Limiting](https://redis.io/learn/howtos/ratelimiting)                                                             | Distributed rate limiting with multiple algorithm patterns |
| [AWS WAF Rate-Based Rules](https://docs.aws.amazon.com/waf/latest/developerguide/waf-rule-statement-type-rate-based.html)     | Cloud-native rate limiting at the edge                     |
| [Martin Fowler - Rate Limit](https://martinfowler.com/articles/rate-limiter.html)                                             | Architectural patterns and tradeoffs                       |
| [Stripe Engineering - Rate Limiting](https://stripe.com/blog/rate-limiters)                                                   | Production story: multi-tier, distributed, with fallback   |
