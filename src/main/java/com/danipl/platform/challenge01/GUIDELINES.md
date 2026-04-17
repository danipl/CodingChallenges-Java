# Challenge 01: Circuit Breaker - Live Coding Guidelines

## 1. Challenge Presentation

### What You're Building

A **thread-safe Circuit Breaker** — a state machine that protects your service against cascading failures in distributed
systems. When a downstream service starts failing, the breaker opens and stops sending requests, giving the service time
to recover.

### Core Contract

```
CLOSED ──(N failures)──▶ OPEN ──(timeout expires)──▶ HALF_OPEN
   ▲                         │                              │
   │                   (fail-fast)                    ┌──────┴──────┐
   │                                                  │             │
   └────────────────────────────────────────── success │      failure│
                                               (reset) │     (reopen)
```

### Interface Summary

| Method                                                                              | Purpose                                                       |
|-------------------------------------------------------------------------------------|---------------------------------------------------------------|
| `of(Config)`                                                                        | Factory - configures `failureThreshold` + `openTimeoutMillis` |
| `execute(Supplier<T>)`                                                              | Run work through breaker, returns result                      |
| `execute(Runnable)`                                                                 | Run side-effect work through breaker                          |
| `getState()` / `getCurrentState()`                                                  | Observe current state                                         |
| `getTotalCalls()`, `getFailedCalls()`, `getRejectedCalls()`, `getSuccessfulCalls()` | Metrics                                                       |
| `reset()`, `forceOpen()`                                                            | Manual controls                                               |

### What Interviewers Evaluate

1. **State machine correctness** — all transitions match the contract exactly, no missing paths.
2. **Thread safety** — concurrent calls from multiple threads don't corrupt state or lose metrics.
3. **Exception handling** — the original exception from the supplier must propagate, and checked exceptions should be
   wrapped.
4. **Observability** — metrics are accurate even under concurrent access from multiple threads.
5. **Testability** — using a `Clock` abstraction instead of `System.currentTimeMillis()` so tests can control time.

---

## 2. Edge & Corner Cases

### How to Identify Them Before Coding

Start by drawing the state transition table. Every transition between states is a potential edge case you need to watch
for.

| #  | Edge Case                            | How It Surfaces                                                      | How to Handle                                                            |
|----|--------------------------------------|----------------------------------------------------------------------|--------------------------------------------------------------------------|
| 1  | **Zero/negative config**             | `Config(0, 100)` or `Config(3, 0)`                                   | Validate in record compact constructor, throw `IllegalArgumentException` |
| 2  | **threshold=1**                      | Single failure should open immediately                               | No special logic needed, just ensure `>=` not `>` comparison             |
| 3  | **HALF_OPEN concurrent probes**      | Multiple threads try to probe simultaneously                         | Only ONE probe allowed - use lock to guard the transition check          |
| 4  | **Concurrent failure counting**      | Two threads fail at the same time, both read `consecutiveFailures=2` | Lock around the increment-and-check logic                                |
| 5  | **Timeout boundary race**            | Thread A checks timeout at T=499ms, Thread B at T=501ms              | Use `Clock` consistently; check `now - openedAt >= timeout`              |
| 6  | **forceOpen() then timeout expires** | Manual open should NOT auto-transition to HALF_OPEN                  | Track `forceOpened` separately from failure-triggered open               |
| 7  | **reset() from CLOSED state**        | Resetting an already-closed breaker should be safe                   | Idempotent - just reset counters                                         |
| 8  | **Exception propagation**            | Supplier throws checked exception                                    | Wrap in `RuntimeException`, preserve cause                               |
| 9  | **Metrics under contention**         | 50 threads × 100 calls, totals must add up                           | Lock-protected counters or `AtomicInteger`                               |
| 10 | **Intermittent failures**            | Fail-Success-Fail-Fail-Fail (should NOT trip)                        | Success resets `consecutiveFailures` to 0                                |

### Quick Pre-Implementation Checklist

```
▢ Config validates failureThreshold >= 1
▢ Config validates openTimeoutMillis >= 1
▢ Initial state is CLOSED
▢ CLOSED: success resets consecutive failures
▢ CLOSED: failure increments, check threshold
▢ OPEN: all calls rejected immediately
▢ OPEN: timeout transitions to HALF_OPEN (not automatic, on next call)
▢ HALF_OPEN: single probe allowed
▢ HALF_OPEN: success -> CLOSED, reset counters
▢ HALF_OPEN: failure -> OPEN, restart timeout
▢ forceOpen() blocks until reset() called
▢ Metrics: total = success + failed + rejected
▢ Thread safety: all state transitions protected
```

---

## 3. First Approach - Chain of Thinking

### Minute 0-2: Clarify Requirements

Take a moment to ask the interviewer clarifying questions before jumping into code:

- *"Should I count consecutive failures or failures in a sliding window?"* → This challenge uses **consecutive**
  failures.
- *"How should checked exceptions be handled?"* → Wrap them in `RuntimeException`.
- *"Is only one probe allowed in HALF_OPEN?"* → Yes, exactly one probe at a time.
- *"Should forceOpen() override the timeout?"* → Yes — only `reset()` can close a manually opened breaker.

### Minute 2-5: Design the State Machine

Draw this on the whiteboard/shared doc:

```
Variables needed:
  - State state (volatile / AtomicReference)
  - int consecutiveFailures
  - long openedAt (timestamp when OPEN transitioned)
  - Metrics: totalCalls, successfulCalls, failedCalls, rejectedCalls
  - boolean forceOpened (to distinguish manual vs automatic open)
  - ReentrantLock lock (protects state transitions)
  - Clock clock (for testable time)
```

### Minute 5-10: Sketch the Core Flow

```java
<T> T execute(Supplier<T> supplier) {
    lock.lock();
    try {
        // 1. Handle forceOpen - always reject
        // 2. Determine effective state (consider timeout)
        // 3. If OPEN and timeout expired -> transition to HALF_OPEN
        // 4. If OPEN (timeout not expired) -> reject
        // 5. If HALF_OPEN -> allow single probe
        // 6. If CLOSED -> allow
        // 7. Update metrics
    } finally {
        lock.unlock();
    }
    // Outside lock: execute supplier
    // In finally: update metrics based on outcome
}
```

### Minute 10-25: Implement

1. Config validation → done
2. Factory method → done
3. `execute(Supplier<T>)` with full state machine
4. `execute(Runnable)` → delegates to Supplier
5. Manual controls: `reset()`, `forceOpen()`
6. Metrics getters

---

## 4. Communication Approach During the Interview

### What to Say Out Loud

| Moment          | Say This                                                                                                                                                                                                       |
|-----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Starting        | "I'll start with a state machine. Three states: CLOSED, OPEN, HALF_OPEN. I'll protect transitions with a ReentrantLock for thread safety."                                                                     |
| Before locking  | "Every state check-and-transition needs to be atomic. I'll use a ReentrantLock - in production you might consider AtomicReference + CAS for better throughput, but ReentrantLock is clearer for an interview." |
| About HALF_OPEN | "HALF_OPEN allows exactly one probe. If I allowed multiple concurrent probes, a successful one could close the circuit while another fails - causing inconsistency."                                           |
| About forceOpen | "I'll track forceOpened separately. The timeout-based transition should only apply to failure-triggered opens, not manual ones."                                                                               |
| About Clock     | "I'm using Clock instead of System.currentTimeMillis() so tests can control time without Thread.sleep(). This is a production best practice."                                                                  |
| About metrics   | "I'm tracking total, success, failed, and rejected separately. Rejected calls aren't failures - they're fast-fails. This distinction matters for observability."                                               |

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
public class CircuitBreakerImpl implements CircuitBreaker {
    // === Fields ===
    private final Config config;
    private final Clock clock;
    private final ReentrantLock lock = new ReentrantLock();

    // State
    private State state = State.CLOSED;
    private int consecutiveFailures = 0;
    private long openedAt;
    private boolean forceOpened = false;

    // Metrics
    private int totalCalls = 0;
    private int successfulCalls = 0;
    private int failedCalls = 0;
    private int rejectedCalls = 0;

    // === Constructor ===
    // === execute(Supplier<T>) ===     ← Core method, ~40 lines
    // === execute(Runnable) ===        ← Delegate to Supplier
    // === Manual controls ===          ← reset(), forceOpen()
    // === Metrics ===                  ← Simple getters
    // === Private helpers ===          ← checkState(), transitionTo(), recordSuccess()/recordFailure()
}
```

### Key Implementation Pattern

```java
private State determineEffectiveState() {
    if (forceOpened) return State.OPEN;
    if (state == State.OPEN && clock.millis() - openedAt >= config.openTimeoutMillis()) {
        transitionTo(State.HALF_OPEN);
        return State.HALF_OPEN;
    }
    return state;
}
```

**Why extract this?** The logic for "is the timeout expired?" is needed in multiple places. Extracting it avoids
duplication and makes the main `execute()` method readable.

---

## 6. Technical Pro Tips

### Thread Safety: Lock vs Atomics

| Approach                       | Pros                                      | Cons                                        | When to Use                                    |
|--------------------------------|-------------------------------------------|---------------------------------------------|------------------------------------------------|
| `ReentrantLock`                | Clear, supports tryLock, explicit control | Coarse-grained, blocks all threads          | **Interview default** - easier to reason about |
| `AtomicReference<State>` + CAS | Lock-free, better throughput              | Complex transition logic, easy to get wrong | Production high-throughput systems             |
| `synchronized`                 | Simplest                                  | No tryLock, no interruptible                | Avoid - too coarse for this pattern            |

### Resilience4j Comparison

What Resilience4j does that interviewers appreciate you knowing about:

| Feature                | This Challenge       | Resilience4j                                           |
|------------------------|----------------------|--------------------------------------------------------|
| Window type            | Consecutive failures | Sliding window (count or time based)                   |
| Half-open probes       | 1                    | Configurable (`permittedNumberOfCallsInHalfOpenState`) |
| Slow call detection    | Not included         | Separate threshold for slow calls                      |
| Failure classification | All exceptions count | `ignoreExceptions()` for business errors               |
| Decorators             | N/A                  | Chain: Bulkhead → Retry → CircuitBreaker → TimeLimiter |

### Circuit Breaker vs Related Patterns

Interviewers often ask: *"When would you use Circuit Breaker vs Retry vs Rate Limiter?"*

| Pattern             | Problem It Solves                  | Key Question                                 |
|---------------------|------------------------------------|----------------------------------------------|
| **Circuit Breaker** | Downstream is unhealthy            | "Is the other service down?"                 |
| **Retry**           | Transient failures                 | "Did this fail because of a temporary blip?" |
| **Rate Limiter**    | Protect your service from overload | "Am I sending too many requests?"            |
| **Bulkhead**        | Isolate resources                  | "Should one slow call affect others?"        |

**Production pattern**: Combine them. Retry handles transient errors, Circuit Breaker stops hammering a dead service.

### Testing Strategy

```java
// Without Clock (bad - slow, flaky)
Thread.sleep(600);  // Wait for timeout

// With Clock (good - instant, deterministic)
Clock clock = Clock.fixed(Instant.EPOCH, ZoneOffset.UTC);
// Advance time programmatically
```

### What Senior Engineers Demonstrate

1. **Failure classification awareness** - "In production, I'd add `ignoreExceptions()` for validation errors that
   shouldn't trip the breaker"
2. **Observability mindset** - "Each state transition should emit a metric/log for dashboards"
3. **Fallback design thinking** - "Rejected calls should have a fallback path - cached data, degraded response, or
   queued for retry"
4. **Production tuning** - "Start with conservative thresholds, adjust based on p99 latency, not averages"
5. **Chaos testing** - "I'd test by deliberately tripping the breaker in staging to verify fallbacks work"

---

## 7. Common Mistakes to Avoid

| Mistake                                        | Why It Fails                                   | Fix                                            |
|------------------------------------------------|------------------------------------------------|------------------------------------------------|
| Checking state outside lock, then executing    | Race: state changes between check and execute  | All check-and-transition logic inside lock     |
| Using `System.currentTimeMillis()`             | Tests need Thread.sleep, flaky CI              | Inject `Clock`                                 |
| Not resetting `consecutiveFailures` on success | Intermittent failures trip breaker incorrectly | Reset to 0 on every success                    |
| `forceOpen()` uses same flag as failure-open   | Timeout transitions force-opened breaker       | Separate `forceOpened` boolean                 |
| Metrics updated before execution completes     | Failed execution counted as success            | Update metrics in try/finally based on outcome |
| Allowing multiple HALF_OPEN probes             | State corruption: one closes, another fails    | Only one probe per HALF_OPEN entry             |
| Wrapping `CircuitBreakerOpenException`         | Loses the specific rejection signal            | Re-throw `CircuitBreakerOpenException` as-is   |

---

## 8. Verification Checklist

Before declaring done, verify:

### Functional

- [ ] Initial state is CLOSED
- [ ] N consecutive failures → OPEN
- [ ] Success at any point → reset consecutive failure counter
- [ ] OPEN rejects all calls with `CircuitBreakerOpenException`
- [ ] Timeout expiry → next call transitions to HALF_OPEN
- [ ] HALF_OPEN success → CLOSED
- [ ] HALF_OPEN failure → OPEN
- [ ] `forceOpen()` → OPEN until `reset()`
- [ ] `reset()` → CLOSED, counters reset
- [ ] Config rejects zero/negative values

### Thread Safety

- [ ] All state transitions under lock
- [ ] Metrics accurate under concurrent access
- [ ] No lost updates to counters
- [ ] No deadlocks (single lock, no nested acquisition)

### Edge Cases

- [ ] threshold=1 works (immediate open on first failure)
- [ ] timeout=1 works (near-instant half-open)
- [ ] Interleaved success/failure doesn't trip
- [ ] Exception from supplier is propagated unchanged
- [ ] Checked exceptions wrapped, not swallowed

---

## 9. Extension Points (Bonus Discussion)

If you finish early, mention these to stand out:

1. **Sliding window instead of consecutive** - "A production circuit breaker uses a sliding window (count or time-based)
   to calculate failure rate, not just consecutive failures"
2. **Failure rate threshold** - "Instead of 100% failures, open when failure rate exceeds 50%"
3. **Minimum calls** - "Don't calculate failure rate until at least N calls have been made"
4. **Slow call detection** - "Track calls exceeding a duration threshold separately from failures"
5. **Asynchronous state transitions** - "Use CompletableFuture for non-blocking execute methods"
6. **Decorators pattern** - "Wrap with Retry, Bulkhead, and RateLimiter for a complete resilience strategy"

---

## 10. Production References

| Resource                                                                              | Why It Matters                               |
|---------------------------------------------------------------------------------------|----------------------------------------------|
| [Resilience4j Docs](https://resilience4j.readthedocs.io/)                             | Most popular Java circuit breaker library    |
| [Martin Fowler - Circuit Breaker](https://martinfowler.com/bliki/CircuitBreaker.html) | Original pattern description                 |
| [Netflix Hystrix](https://github.com/Netflix/Hystrix) (archived)                      | Historical reference, shaped modern patterns |
| [Akka Circuit Breaker](https://doc.akka.io/docs/akka/current/circuitbreaker.html)     | Actor-model approach worth knowing           |

---

*This guideline is the template for all challenge guidelines. Each challenge follows this structure: presentation → edge
cases → chain of thinking → communication → implementation → pro tips → mistakes → verification.*
