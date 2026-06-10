# Challenge 06: Retry with Exponential Backoff - Live Coding Guidelines

## 1. Challenge Presentation

### What You're Building

A **Retry mechanism with exponential backoff and jitter** — automatically re-executes operations that fail due to
transient errors (network blips, temporary unavailability, timeouts). Used in every production system that communicates
with external services.

### Core Contract

```
     supplier.get() ──▶ fail? ──yes──▶ wait(backoff + jitter) ──▶ retry
          │                              │
        success                       attempt++
          │                              │
          └──────▶ return result ◀───────┘
                       OR
           throw MaxRetriesExceededException after maxAttempts
```

**Backoff formula:** `min(baseDelayMs × 2^(attempt-1), maxDelayMs)`
**With jitter:** `delay ± random × jitterFactor × delay`

### Interface Summary

| Method                                                            | Purpose                                                        |
|-------------------------------------------------------------------|----------------------------------------------------------------|
| `of(RetryConfig)`                                                 | Factory - creates instance with config                         |
| `<T> T execute(SupplierWithException<T>)`                         | Retry until success or max attempts, return value              |
| `<T> RetryResult<T> executeWithDetails(SupplierWithException<T>)` | Same as execute, but returns attempt count, delays, total wait |

### RetryConfig

| Field          | Type   | Constraint |
|----------------|--------|------------|
| `maxAttempts`  | int    | ≥ 1        |
| `baseDelayMs`  | long   | ≥ 0        |
| `maxDelayMs`   | long   | ≥ 0        |
| `jitterFactor` | double | 0.0 to 1.0 |

### What Interviewers Evaluate

1. **Backoff correctness** — exponential growth with proper capping at `maxDelayMs`. Common trap: `^` is XOR in Java,
   not exponentiation.
2. **Bidirectional jitter** — jitter should sometimes reduce delay, not just increase it. Prevents thundering herd.
3. **Thread interruption** — `InterruptedException` must preserve interrupt status and stop retrying promptly.
4. **MaxAttempts accounting** — exactly `maxAttempts` invocations of the supplier, no more, no less.
5. **Observability** — `executeWithDetails` must track each delay, attempt count, and total wait time accurately.

---

## 2. Edge & Corner Cases

### How to Identify Them Before Coding

Draw the retry timeline. Every attempt, every delay, every interrupt point is a potential edge case.

| #  | Edge Case                             | How It Surfaces                               | How to Handle                                                               |
|----|---------------------------------------|-----------------------------------------------|-----------------------------------------------------------------------------|
| 1  | **maxAttempts=1**                     | Single attempt, no retries                    | Supplier runs once, fails → throw `MaxRetriesExceededException` immediately |
| 2  | **baseDelayMs=0**                     | No delay between retries                      | `Thread.sleep(0)` returns immediately — loop continues                      |
| 3  | **maxDelayMs < baseDelayMs**          | Cap is lower than base                        | First attempt uses `min(base, max) = maxDelayMs`                            |
| 4  | **jitterFactor=0.0**                  | No jitter — deterministic delays              | `delay ± 0` = pure backoff                                                  |
| 5  | **jitterFactor=1.0**                  | Maximum jitter variation                      | Delay can vary from 0 to 2× backoff                                         |
| 6  | **All attempts fail**                 | Supplier always throws                        | `MaxRetriesExceededException` with `attemptsMade = maxAttempts`             |
| 7  | **Success on first try**              | Supplier never throws                         | Return immediately, no delay, `attemptsMade = 1`                            |
| 8  | **Success on retry N**                | Fails N-1 times, succeeds on Nth              | `attemptsMade = N`, delays recorded for N-1 waits                           |
| 9  | **Thread interrupted during sleep**   | `InterruptedException` caught mid-sleep       | Restore interrupt status, exit loop — do **not** attempt another retry      |
| 10 | **Exponential overflow**              | `baseDelayMs × 2^30` exceeds `Long.MAX_VALUE` | Cap with `maxDelayMs` prevents overflow if `maxDelayMs` is reasonable       |
| 11 | **`execute` vs `executeWithDetails`** | Both methods must behave identically          | Extract shared logic or delegate `execute` → `executeWithDetails().value()` |
| 12 | **Exception message has typo**        | `"3of attempts reached"` (missing space)      | Include space: `"3 of attempts reached"`                                    |

### Quick Pre-Implementation Checklist

```
▢ Loop: while (attempt < maxAttempts) && !isInterrupted()
▢ Attempt counter: starts at 0, increments after each failure
▢ Backoff: (long) pow(2, attempt - 1) — NOT ^ (XOR)
▢ Cap: min(backoff, maxDelayMs) — NOT baseDelayMs
▢ Jitter: bidirectional (±), not just additive
▢ MaxRetriesExceededException wraps last exception
▢ MaxRetriesExceededException.attemptsMade = maxAttempts
▢ InterruptedException → interrupt() + break
▢ executeWithDetails tracks: delays list, totalWaitMs, attemptsMade
▢ Fallback throw after loop — meaningful message (interrupted case)
```

---

## 3. First Approach - Chain of Thinking

### Minute 0-2: Clarify Requirements

Ask the interviewer:

- *"Should I retry on all exceptions, or only specific ones?"* → This challenge retries on **all** exceptions.
- *"Is jitter additive or bidirectional?"* → Bidirectional: `delay ± random × jitterFactor × delay`.
- *"What should happen on thread interruption?"* → Stop retrying, preserve interrupt status, exit cleanly.
- *"Should `execute` and `executeWithDetails` share logic?"* → Yes, prefer delegation to avoid duplication.

### Minute 2-5: Design the Retry Loop

Sketch on whiteboard/shared doc:

```
Loop structure:
  attempt = 0
  while (attempt < maxAttempts && !isInterrupted()):
    try:
      return supplier.get()        ← success
    catch Exception:
      if attempt + 1 == maxAttempts:
        throw MaxRetriesExceeded    ← exhausted

      attempt++
      delay = calculateDelay(attempt)
      sleep(delay)                  ← may throw InterruptedException
```

**Key insight:** The attempt counter represents "how many failures so far." After increment, `attempt` is the retry
number (1-based for delay calculation).

**Backoff calculation:**

```
attempt 1 (first retry):  baseDelayMs × 2^0 = baseDelayMs
attempt 2:                baseDelayMs × 2^1 = 2 × baseDelayMs
attempt 3:                baseDelayMs × 2^2 = 4 × baseDelayMs
...
Capped at maxDelayMs.
```

**Jitter:**

```
jitterAmount = random() × jitterFactor × delay
direction = random() >= 0.5 ? +1 : -1
finalDelay = max(0, delay + direction × jitterAmount)
```

### Minute 5-10: Sketch the Core Flow

```java
<T> T execute(SupplierWithException<T> supplier) throws MaxRetriesExceededException {
    return executeWithDetails(supplier).value();
}

<T> RetryResult<T> executeWithDetails(SupplierWithException<T> supplier) {
    int attempt = 0;
    List<Long> delays = new ArrayList<>();
    long totalWaitMs = 0;

    while (attempt < config.maxAttempts() && !Thread.currentThread().isInterrupted()) {
        try {
            T result = supplier.get();
            return new RetryResult<>(result, attempt + 1, totalWaitMs, delays);
        } catch (Exception ex) {
            attempt++;
            if (attempt == config.maxAttempts()) {
                throw new MaxRetriesExceededException(
                    config.maxAttempts() + " of attempts reached", ex, attempt);
            }
            long delay = calculateDelay(attempt);
            delays.add(delay);
            totalWaitMs += delay;
            Thread.sleep(delay);
        }
    }
    // Loop exited due to interrupt
    throw new RuntimeException("Retry interrupted after " + attempt + " attempts");
}
```

### Minute 10-25: Implement

1. Fields + constructor → done
2. `calculateBackoff(attempt)` with `Math.pow` and `maxDelayMs` cap → done
3. `calculateJitter(delay)` bidirectional → done
4. `executeWithDetails()` full loop → done
5. `execute()` delegates to `executeWithDetails().value()` → done

---

## 4. Communication Approach During the Interview

### What to Say Out Loud

| Moment              | Say This                                                                                                                                                                                                    |
|---------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Starting            | "I'll implement a retry loop with exponential backoff and jitter. The loop checks both `maxAttempts` and thread interrupt status each iteration."                                                           |
| Before backoff math | "Exponential backoff uses `Math.pow(2, attempt-1)` capped at `maxDelayMs`. A common mistake is using `^` which is XOR in Java, not exponentiation."                                                         |
| About jitter        | "Jitter is bidirectional — it can reduce or increase the delay. The interface contract says `delay ± random × jitterFactor × delay`. This prevents thundering herd when many clients retry simultaneously." |
| About interrupt     | "On `InterruptedException`, I restore the interrupt flag and break out of the loop. The caller gets a clear signal that the retry was interrupted, not naturally exhausted."                                |
| About code reuse    | "`execute()` delegates to `executeWithDetails().value()`. Both methods share the same retry logic — single source of truth, fewer bugs."                                                                    |
| About `maxDelayMs`  | "The cap is `maxDelayMs`, not `baseDelayMs`. Without this cap, exponential backoff grows unbounded. This is critical when `baseDelayMs=100` and `maxAttempts=20` — 2^19 × 100ms = ~52 seconds wait."        |

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
public class RetryImpl implements Retry {
    // === Fields ===
    private final RetryConfig config;

    // === Constructor ===

    // === execute() ===                   ← ~3 lines, delegates to executeWithDetails
    // === executeWithDetails() ===        ← ~30 lines, full retry loop
    // === calculateBackoff(attempt) ===   ← min(base × 2^(n-1), maxDelayMs)
    // === calculateJitter(delay) ===      ← max(0, delay ± random × factor × delay)
    // === calculateDelay(attempt) ===     ← backoff + jitter, guards against negative
}
```

### Key Implementation Patterns

**Delegation (single source of truth):**

```java
@Override
public <T> T execute(SupplierWithException<T> supplier) throws MaxRetriesExceededException {
    return executeWithDetails(supplier).value();
}
```

**Backoff with correct cap:**

```java
private long calculateBackoff(int attempt) {
    return Math.min(
        (long) (config.baseDelayMs() * Math.pow(2, attempt - 1)),
        config.maxDelayMs()
    );
}
```

**Bidirectional jitter:**

```java
private long calculateDelay(int attempt) {
    long backoff = calculateBackoff(attempt);
    if (config.jitterFactor() == 0.0) return backoff;
    double jitter = Math.random() * config.jitterFactor() * backoff;
    // ±: 50% chance to add, 50% to subtract
    return Math.max(0, backoff + (Math.random() >= 0.5 ? jitter : -jitter));
}
```

**Interrupt-safe loop:**

```java
while (attempt < config.maxAttempts() && !Thread.currentThread().isInterrupted()) {
    try {
        return supplier.get();
    } catch (Exception ex) {
        attempt++;
        if (attempt == config.maxAttempts()) {
            throw new MaxRetriesExceededException(
                config.maxAttempts() + " of attempts reached", ex, attempt);
        }
        long delay = calculateDelay(attempt);
        delays.add(delay);
        totalWaitMs += delay;
        try {
            Thread.sleep(delay);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            break;
        }
    }
}
// Exited via interrupt
throw new RuntimeException("Retry interrupted after " + attempt + " attempts");
```

---

## 6. Technical Pro Tips

### Exponential Backoff: `Math.pow` vs Bit Shift

| Approach              | Pros                                 | Cons                                 | When to Use               |
|-----------------------|--------------------------------------|--------------------------------------|---------------------------|
| `Math.pow(2, n)`      | Readable, handles large exponents    | Floating-point, needs cast to `long` | **Interview default**     |
| `1L << n` (bit shift) | Integer-only, fast, no cast needed   | Fails for n ≥ 63 (overflow/wrap)     | Production, known small n |
| `2 ^ n` (XOR)         | ❌ **Never** — this is XOR, not power | Compiles silently, produces garbage  | —                         |

**Interview tip:** Mentioning "`^` is not exponentiation in Java" shows operator-level knowledge.

### Jitter Strategies: Why Bidirectional?

| Strategy           | Formula                               | Effect                                     |
|--------------------|---------------------------------------|--------------------------------------------|
| **No jitter**      | `delay = backoff`                     | Deterministic — all clients retry together |
| **Full jitter**    | `random(0, backoff)`                  | Maximum spread, but mean delay reduced     |
| **Equal jitter**   | `backoff/2 + random(0, backoff/2)`    | Mean preserved, moderate spread            |
| **Bidirectional±** | `backoff ± random × factor × backoff` | **This challenge** — simple, symmetric     |

**Thundering herd problem:** When a service recovers, all clients that hit it simultaneously retry at the same time (
backoff is deterministic). Jitter spreads retries over time, preventing the service from being immediately overwhelmed
again.

### `Math.random()` vs `ThreadLocalRandom`

| Approach                      | Pros                         | Cons                         | When to Use                 |
|-------------------------------|------------------------------|------------------------------|-----------------------------|
| `Math.random()`               | Simple, no import            | Global lock under contention | **Interview default**       |
| `ThreadLocalRandom.current()` | Lock-free, better throughput | Requires import              | Production high-concurrency |

### Testing Strategy

```java
// Without Clock (what tests do - acceptable for interview)
// Use zero jitter (jitterFactor=0.0) for deterministic delay testing
retry = Retry.of(new RetryConfig(4, 50, 1000, 0.0));
// With maxAttempts=4, baseDelay=50, maxDelay=1000:
// Expected delays: 50, 100, 200 (exponential, capped at 1000)

// With zero baseDelay
retry = Retry.of(new RetryConfig(3, 0, 0, 0.0));
// Completes instantly — useful for testing maxAttempts behavior without sleep
```

**Limitation**: `Thread.sleep()` uses real wall-clock time. Tests with non-zero delays are slow. The `jitterFactor=0.0`
config in tests eliminates variation for reliable assertions.

### What Senior Engineers Demonstrate

1. **`^` is not exponent** — "In Java, `^` is XOR. I'll use `Math.pow(2, attempt-1)` or bit shift `1L << (attempt-1)`."
2. **Bidirectional jitter reasoning** — "Adding jitter only makes delays longer. The point of jitter is to spread load,
   not increase it. Bidirectional `±` keeps the mean delay close to the backoff."
3. **Interrupt semantics** — "I restore the interrupt flag AND exit the loop. Continuing to retry after interruption
   violates the caller's intent."
4. **Single source of truth** — "`execute` delegates to `executeWithDetails`. No code duplication means fewer bugs when
   fixing retry logic."
5. **Production tuning awareness** — "In production, `maxDelayMs` prevents unbounded waits. Setting `maxAttempts=20`
   with `baseDelayMs=100` and no cap means the 20th retry waits ~52 seconds — unacceptable latency."
6. **Observability mindset** — "`executeWithDetails` returns the full delay history. This is useful for dashboards — you
   can see if retries are clustered or spread."

---

## 7. Common Mistakes to Avoid

| Mistake                                                   | Why It Fails                                                 | Fix                                                           |
|-----------------------------------------------------------|--------------------------------------------------------------|---------------------------------------------------------------|
| **Using `^` for exponentiation**                          | `2 ^ 3 = 1` (XOR), not `8`. Backoff is completely wrong      | Use `Math.pow(2, n)` or `1L << n`                             |
| **Cap is `baseDelayMs` instead of `maxDelayMs`**          | Exponential growth flattened — every delay = baseDelayMs     | `Math.min(backoff, config.maxDelayMs())`                      |
| **Jitter always additive**                                | Delays only increase, defeats thundering herd prevention     | Use `±` — randomly add or subtract                            |
| **`                                                       |                                                              | ` instead of `&&` for interrupt check**                       | Interrupted thread CONTINUES retrying, defeating interrupt      | `&& !Thread.currentThread().isInterrupted()`                   |
| **No `break` after catching `InterruptedException`**      | Loop continues after interrupt — one more retry attempt      | Add `break` after `Thread.currentThread().interrupt()`        |
| **Duplicate logic in `execute` and `executeWithDetails`** | Bugs fixed in one method but not the other                   | `execute()` should delegate to `executeWithDetails().value()` |
| **Off-by-one in attempt counting**                        | `attemptsMade` in exception doesn't match actual invocations | `attempt` starts at 0, `attemptsMade = attempt` after failure |
| **`Math.pow` result not cast to `long`**                  | Type mismatch, or implicit truncation                        | `(long) Math.pow(2, attempt - 1)`                             |
| **No guard against negative delay**                       | Subtracting jitter from small backoff → negative sleep       | `Math.max(0, delay ± jitter)`                                 |
| **Error message missing space**                           | `"3of attempts reached"` in logs                             | `"3 of attempts reached"`                                     |

---

## 8. Verification Checklist

Before declaring done, verify:

### Functional

- [ ] Success on first attempt: returns immediately, no delay, `attemptsMade = 1`
- [ ] Retry then success: correct `attemptsMade`, delays recorded
- [ ] All attempts fail: `MaxRetriesExceededException` with `attemptsMade = maxAttempts`
- [ ] `MaxRetriesExceededException` wraps last exception (cause chain intact)
- [ ] `maxAttempts=1`: single invocation, no retries, throws immediately on failure
- [ ] `baseDelayMs=0`: completes instantly, no blocking
- [ ] Backoff values follow `base × 2^(n-1)` pattern
- [ ] Backoff capped at `maxDelayMs` (test with small `maxDelayMs`)
- [ ] `jitterFactor=0.0`: delays are deterministic, equal to backoff
- [ ] `jitterFactor=1.0`: delays show wide variation across runs
- [ ] `executeWithDetails` returns correct `delays` list (one entry per wait)
- [ ] `executeWithDetails` returns correct `totalWaitMs` (sum of all delays)

### Thread Safety

- [ ] Concurrent retries from 20 threads: all succeed independently
- [ ] No shared mutable state (all variables are local or final fields)
- [ ] Thread interrupt: retry stops promptly, interrupt status preserved

### Edge Cases

- [ ] Success on attempt N: exactly N supplier invocations
- [ ] Failure on all attempts: exactly `maxAttempts` supplier invocations
- [ ] Interrupt mid-sleep: no additional retry after interrupt
- [ ] Interrupt before first attempt: loop exits immediately
- [ ] `maxAttempts=1`, `baseDelayMs=0`: no sleep, single attempt
- [ ] Very large `maxAttempts` with reasonable `maxDelayMs`: no overflow, bounded wait

---

## 9. Extension Points (Bonus Discussion)

If you finish early, mention these to stand out:

1. **Retryable exception filtering** — "In production, I'd add `Predicate<Exception> isRetryable` — no point retrying
   `IllegalArgumentException` or `InsufficientFundsException`."
2. **Jitter strategies** — "AWS recommends Decorrelated Jitter for retry storms:
   `delay = random(baseDelayMs, delay × 3)`. Google's SRE book has the definitive analysis."
3. **`Clock` abstraction for delays** — "For testable delays without `Thread.sleep()`, inject a ` Clock` and ` Sleeper`
   interface. Tests can advance time programmatically."
4. **Async retry with `CompletableFuture`** — "`scheduleAfter(delay, supplier)` returns a `CompletableFuture<T>`.
   Non-blocking retry for reactive pipelines."
5. **Circuit breaker integration** — "Retry alone can overwhelm a failing service. Combine with Circuit Breaker: Retry
   handles transient errors, Circuit Breaker stops retrying when the service is down."
6. **Idempotency awareness** — "Retry is only safe for idempotent operations. Retrying a non-idempotent `POST /payments`
   creates duplicate charges. The caller must ensure idempotency keys."
7. **`ThreadLocalRandom` for jitter** — "`Math.random()` has global lock contention under load.
   `ThreadLocalRandom.current().nextDouble()` is lock-free for concurrent retries."
8. **Backoff with jitter per-attempt tracking** — "`RetryResult` already exposes the delay list. In production, I'd also
   track the jitter amount per attempt to monitor jitter distribution."

---

## 10. Production References

| Resource                                                                                                                 | Why It Matters                                             |
|--------------------------------------------------------------------------------------------------------------------------|------------------------------------------------------------|
| [Resilience4j Retry](https://resilience4j.readthedocs.io/en/latest/docs/retry.html)                                      | Most popular Java retry library — configurable, composable |
| [AWS Architecture Blog - Exponential Backoff](https://aws.amazon.com/blogs/architecture/exponential-backoff-and-jitter/) | Definitive guide on jitter strategies and why they matter  |
| [Google SRE - Handling Overload](https://sre.google/sre-book/handling-overloads/)                                        | Chapter on retry storms, backoff, and jitter in production |
| [Polly.NET — Retry & Resilience](https://github.com/App-vNext/Polly)                                                     | .NET reference — excellent API design for retry patterns   |
| [Spring Retry](https://github.com/spring-projects/spring-retry)                                                          | Spring ecosystem retry — annotations, backoff policies     |

---

*This guideline follows the established challenge template: presentation → edge cases → chain of thinking →
communication → implementation → pro tips → mistakes → verification.*
