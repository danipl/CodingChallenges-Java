# Challenge 05: Metrics Aggregator - Live Coding Guidelines

## 1. Challenge Presentation

### What You're Building

A **thread-safe Streaming Log/Metrics Aggregator** — ingests `LogEntry` records and computes real-time metrics over a
sliding 1-minute window: error rate, P95 response time, error count, and total entry count. Used in observability
pipelines for alerting, dashboards, and SLO tracking.

### Core Contract

```
     ingest(entry) ───▶ [Sliding Window] ───▶ Metrics
                            │
          ┌─────────────────┼─────────────────┐
          │                 │                 │
   getErrorRate(now)  getP95ResponseTime(now) getErrorCount(now)
   (ERROR+FATAL/total)  (95th percentile ms)  (ERROR+FATAL count)
          │                 │                 │
          └─────────────────┴─────────────────┘
                    All bounded by [now-60s, now]
```

### Interface Summary

| Method                                   | Purpose                                           |
|------------------------------------------|---------------------------------------------------|
| `of()`                                   | Factory - creates instance                        |
| `void ingest(LogEntry entry)`            | Thread-safe ingestion of a single log entry       |
| `double getErrorRateLastMinute(now)`     | Error rate (0.0-1.0) for ERROR+FATAL in window    |
| `long getP95ResponseTimeLastMinute(now)` | P95 response time (ms) in window, 0 if no entries |
| `long getErrorCountLastMinute(now)`      | Count of ERROR+FATAL entries in window            |
| `int getTotalEntriesLastMinute(now)`     | Total entries in window                           |

### What Interviewers Evaluate

1. **Sliding window correctness** - entries outside [now-60s, now] excluded, boundary edges handled precisely
2. **P95 calculation** - must sort by response time before computing percentile; unsorted data returns garbage
3. **Thread safety** - concurrent ingestion and reads don't corrupt state or lose entries
4. **Memory management** - old entries eventually evicted; unbounded growth is a production killer
5. **API design** - `now` parameter enables testability without `Clock` injection; candidate should discuss trade-offs

---

## 2. Edge & Corner Cases

### How to Identify Them Before Coding

Draw the sliding window timeline. Every boundary, empty state, and ordering assumption is a potential edge case.

| #  | Edge Case                          | How It Surfaces                                        | How to Handle                                                              |
|----|------------------------------------|--------------------------------------------------------|----------------------------------------------------------------------------|
| 1  | **Empty window**                   | No entries ingested, query any metric                  | Return 0.0 for rate, 0 for count/P95                                       |
| 2  | **Single entry**                   | P95 of one element = that element's response time      | P95 index = ceil(0.95 × 1) - 1 = 0 → correct                               |
| 3  | **All entries outside window**     | Old entries exist, query with `now` far in future      | Filter returns 0 entries; all metrics return 0                             |
| 4  | **Entry at exact boundary**        | `entry.timestamp == now - 60_000`                      | Include - use `>=` not `>` for lower bound                                 |
| 5  | **P95 with unsorted ingestion**    | Entries arrive: [500ms, 10ms, 20ms, 3ms]               | **MUST sort before percentile** - insertion order ≠ response time order    |
| 6  | **Concurrent ingest + read**       | Thread A ingests, Thread B reads simultaneously        | ReadWriteLock - write lock blocks reads, reads concurrent with reads       |
| 7  | **MAX_ENTRIES cap hit**            | 1M entries ingested, new entry arrives                 | `pollFirst()` oldest to make room (FIFO eviction)                          |
| 8  | **No time-based eviction**         | Low throughput: 1 entry/sec for 1 hour = 3,600 entries | Only 60 are relevant; rest are stale. Evict on ingest or on query          |
| 9  | **P95 with empty filtered result** | Entries exist but all outside window                   | Return 0 (contract); guard `if (filtered.isEmpty()) return 0`              |
| 10 | **Error rate division by zero**    | No entries in window, compute rate                     | Return 0.0 explicitly before division                                      |
| 11 | **Future-dated entries**           | `entry.timestamp > now`                                | Exclude - they're not in the window ending at `now`                        |
| 12 | **Historical window queries**      | Caller passes `now` from past to query old windows     | Valid use case - don't eagerly evict based on `System.currentTimeMillis()` |

### Quick Pre-Implementation Checklist

```
▢ ingest() is thread-safe (write lock)
▢ All metrics filter by [now-60_000, now] inclusive
▢ P95 sorts by responseTimeMs before computing percentile
▢ P95 returns 0 for empty window
▢ Error rate returns 0.0 for empty window (no division by zero)
▢ ERROR and FATAL both count as errors
▢ MAX_ENTRIES cap with FIFO eviction
▢ ReadWriteLock: write for ingest, read for queries
▢ Lambda mutation uses effectively-final (array trick OR atomics, not both)
```

---

## 3. First Approach - Chain of Thinking

### Minute 0-2: Clarify Requirements

Ask the interviewer:

- _"Should the sliding window evict old entries automatically, or keep all history?"_ → Cap at MAX_ENTRIES, discuss
  time-based eviction as production improvement.
- _"How is P95 defined - nearest-rank or interpolation?"_ → Nearest-rank: `index = ceil(0.95 × n) - 1`.
- _"Can the caller query arbitrary timestamps (historical windows)?"_ → Yes, `now` is caller-supplied.
- _"Is there a `Clock` abstraction needed?"_ → No, `now` parameter enables testability without injection.

### Minute 2-5: Design the Data Structure

Sketch on whiteboard/shared doc:

```
Data structure:
  - ArrayDeque<LogEntry> slidingWindow  ← FIFO order by ingest time
  - ReentrantReadWriteLock              ← write for ingest, read for queries
  - MAX_ENTRIES = 1_000_000             ← hard cap

Key insight:
  - ArrayDeque gives O(1) add/poll, O(n) iteration
  - Entries are ordered by ingest time, so early-exit on timestamps > now
  - For P95: copy to ArrayList, sort by responseTimeMs, pick index

Timestamp filter:
  - lowerThreshold = now - 60_000
  - entry.timestamp >= lowerThreshold && entry.timestamp <= now
```

### Minute 5-10: Sketch the Core Flow

```java
void ingest(LogEntry entry) {
    writeLock.lock();
    try {
        if (slidingWindow.size() == MAX_ENTRIES) {
            slidingWindow.pollFirst();  // Evict oldest
        }
        slidingWindow.add(entry);
    } finally {
        writeLock.unlock();
    }
}

double getErrorRateLastMinute(long now) {
    long[] counters = {0, 0}; // total, errors
    iterateWindow(now, e -> {
        counters[0]++;
        if (e.level() == ERROR || e.level() == FATAL) counters[1]++;
    });
    return counters[0] == 0 ? 0.0 : (double) counters[1] / counters[0];
}

long getP95ResponseTimeLastMinute(long now) {
    List<LogEntry> inWindow = new ArrayList<>();
    iterateWindow(now, inWindow::add);
    if (inWindow.isEmpty()) return 0;
    inWindow.sort(Comparator.comparingLong(LogEntry::responseTimeMs));
    int idx = (int) (Math.ceil(0.95 * inWindow.size()) - 1);
    return inWindow.get(idx).responseTimeMs();
}
```

### Minute 10-25: Implement

1. Fields + constructor → done
2. `ingest()` with write lock and cap → done
3. `iterateWindow(now, consumer)` shared read-locked helper → done
4. `getErrorRateLastMinute()` → done
5. `getP95ResponseTimeLastMinute()` with sort → done
6. `getErrorCountLastMinute()` → done
7. `getTotalEntriesLastMinute()` → done

---

## 4. Communication Approach During the Interview

### What to Say Out Loud

| Moment            | Say This                                                                                                                                                                                                        |
|-------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Starting          | "I'll use an ArrayDeque for FIFO ordering by ingest time, protected by a ReadWriteLock. The sliding window filters [now-60s, now] on query."                                                                    |
| Before P95        | "P95 requires sorting by response time, not ingest order. I'll copy matching entries to an ArrayList, sort, then pick the nearest-rank index. This is O(n log n) - in production I'd use QuickSelect for O(n)." |
| About locking     | "Write lock for ingest, read lock for queries. ArrayDeque isn't thread-safe, so reads need the lock too. In production with high write throughput I'd consider a lock-free ring buffer."                        |
| About mutation    | "Inside the read-locked iteration I'll use `long[] counters = {0}` for the effectively-final trick. No need for AtomicLong since only one thread executes the lambda per invocation."                           |
| About eviction    | "I'm capping at MAX_ENTRIES with FIFO eviction. Since `now` is caller-supplied, the caller owns the timeline. In a real system I'd add `evictBefore(long)` for memory management."                              |
| About `now` param | "Passing `now` instead of using System.currentTimeMillis() makes this trivially testable - no Clock injection needed. The trade-off is that stale entries accumulate unless eagerly evicted."                   |

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
public class MetricsAggregatorImpl implements MetricsAggregator {
    // === Constants ===
    private static final long WINDOW_MILLIS = 60_000;
    private static final int MAX_ENTRIES = 1_000_000;

    // === Fields ===
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ArrayDeque<LogEntry> slidingWindow = new ArrayDeque<>(MAX_ENTRIES);

    // === ingest() ===                  ← ~10 lines, write lock + cap
    // === getErrorRateLastMinute() ===  ← ~10 lines, two counters
    // === getP95ResponseTimeLastMinute() === ← ~12 lines, copy + sort + index
    // === getErrorCountLastMinute() === ← ~8 lines, single counter
    // === getTotalEntriesLastMinute() === ← ~5 lines, single counter
    // === iterateWindow(now, consumer) === ← private helper, read lock, early-exit
}
```

### Key Implementation Patterns

**Shared iteration helper:**

```java
private void iterateWindow(long now, Consumer<LogEntry> consumer) {
    readLock.lock();
    try {
        long lower = now - WINDOW_MILLIS;
        for (var it = slidingWindow.iterator(); it.hasNext(); ) {
            var entry = it.next();
            if (entry.timestamp() < lower) continue;      // Too old
            if (entry.timestamp() > now) break;            // Future-dated
            consumer.accept(entry);
        }
    } finally {
        readLock.unlock();
    }
}
```

**P95 nearest-rank:**

```java
long getP95ResponseTimeLastMinute(long now) {
    List<LogEntry> inWindow = new ArrayList<>();
    iterateWindow(now, inWindow::add);
    if (inWindow.isEmpty()) return 0;
    inWindow.sort(Comparator.comparingLong(LogEntry::responseTimeMs));
    int idx = (int) Math.max(0, Math.ceil(0.95 * inWindow.size()) - 1);
    return inWindow.get(idx).responseTimeMs();
}
```

**Why `Math.max(0, ...)` on the index?** Edge case: 1 entry → `ceil(0.95 × 1) - 1 = 0`. Safe, but defensive for
robustness.

---

## 6. Technical Pro Tips

### Thread Safety: Lock Choices

| Approach                        | Pros                              | Cons                           | When to Use                              |
|---------------------------------|-----------------------------------|--------------------------------|------------------------------------------|
| `ReentrantReadWriteLock`        | Concurrent reads, clear reasoning | Write blocks all reads         | **Interview default** - easy to explain  |
| `ReentrantLock` (plain)         | Simplest                          | No read concurrency            | OK if reads are rare                     |
| `ConcurrentLinkedDeque`         | Lock-free ingest                  | Snapshot reads still need care | High-write-throughput production systems |
| `StampedLock` (optimistic read) | Maximum read throughput           | Complex api, retry on conflict | Extreme read-heavy workloads             |

### P95: Nearest-Rank vs Interpolation vs QuickSelect

| Method            | Complexity | Accuracy      | Interview Feasibility |
|-------------------|------------|---------------|-----------------------|
| **Sort + index**  | O(n log n) | Nearest-rank  | ✅ Implement in 5 min  |
| **QuickSelect**   | O(n) avg   | Nearest-rank  | ⚠️ ~15 min to code    |
| **t-digest**      | O(log n)   | Approximate   | ❌ External library    |
| **Linear interp** | O(n log n) | Between ranks | ⚠️ Edge case math     |

**Nearest-rank (this challenge):**

```
index = ceil(percentile/100 × n) - 1
```

For P95 of 20 items: `ceil(0.95 × 20) - 1 = 19 - 1 = 18` → 19th element (0-indexed).

**Why not QuickSelect in the interview?** It's correct and O(n), but the extra code complexity rarely justifies the time
cost in a 30-45 minute session. Mention it as a production improvement.

### Sliding Window: Data Structure Comparison

| Structure               | Add   | Remove | Iterate | Memory | Thread-Safe? |
|-------------------------|-------|--------|---------|--------|--------------|
| **ArrayDeque**          | O(1)  | O(1)   | O(n)    | O(n)   | No           |
| `LinkedList`            | O(1)  | O(1)   | O(n)    | O(n×3) | No           |
| `ArrayList`             | O(1)* | O(n)   | O(n)    | O(n)   | No           |
| `ConcurrentLinkedDeque` | O(1)  | O(1)   | O(n)    | O(n×2) | Yes          |
| Ring buffer (custom)    | O(1)  | O(1)   | O(n)    | O(1)   | With atomics |

*Amortized. ArrayDeque is the right choice for this challenge — compact memory, O(1) add/poll, fast iteration.

### Mutation Inside Lambda: Effectively-Final Options

| Approach                  | Overhead | When to Use                          |
|---------------------------|----------|--------------------------------------|
| `long[] counter = {0}`    | None     | **Interview default** - zero CAS     |
| `AtomicLong`              | CAS      | Only if actual multi-thread mutation |
| `AtomicInteger`           | CAS      | Same as AtomicLong                   |
| Plain `long` (pre-Java 8) | N/A      | Before lambda era                    |

**Inside a read-locked section, only one thread executes the lambda per call.** `AtomicLong` adds CAS overhead for no
benefit. The `long[]` array trick is the clean choice:

```java
// Correct: array reference is effectively-final, contents mutable
final long[] counters = {0, 0};
iterateWindow(now, e -> {
    counters[0]++;  // mutates array contents, not the reference
    if (isError(e)) counters[1]++;
});
```

### Time Handling: `now` Parameter vs Clock Injection

| Approach                     | Pros                             | Cons                       |
|------------------------------|----------------------------------|----------------------------|
| **`now` parameter**          | Explicit, testable, no injection | Caller must track time     |
| `Clock` field                | Automatic, production-friendly   | Needs injection for tests  |
| `System.currentTimeMillis()` | Simple                           | Untestable without mocking |

The `now` parameter is a good middle ground for this challenge. It lets the caller query any window — past, present, or
future — without needing a `Clock` abstraction.

### What Senior Engineers Demonstrate

1. **P95 requires sorting** - "Percentile is about response time, not arrival order. I sort before computing the index."
2. **Memory boundedness** - "I cap at 1M entries with FIFO eviction. In production I'd also evict by age — stale data
   wastes memory."
3. **Single-pass optimization** - "If all 4 metrics are needed together, I'd compute them in one pass instead of four
   separate iterations."
4. **QuickSelect awareness** - "For O(n) P95 I'd use QuickSelect instead of full sort. Only the k-th element matters."
5. **Observability on the aggregator** - "In production I'd track window size, eviction rate, and query latency metrics
   on the aggregator itself."
6. **`now` parameter trade-offs** - "Passing `now` makes this testable but means stale data accumulates. I'd document
   the eviction semantics clearly."

---

## 7. Common Mistakes to Avoid

| Mistake                                         | Why It Fails                                                | Fix                                               |
|-------------------------------------------------|-------------------------------------------------------------|---------------------------------------------------|
| **P95 without sorting**                         | Returns value at Nth insertion position, not Nth percentile | Sort by `responseTimeMs` before picking index     |
| Loop-based P95 index extraction                 | O(n) walk when `list.get(index)` is O(1)                    | Direct indexing: `list.get(idx).responseTimeMs()` |
| `AtomicLong` inside read-locked lambda          | CAS overhead for no benefit                                 | Use `long[]` array trick                          |
| Mixing `double[]` and `AtomicLong` for counters | Inconsistent patterns, looks like partial fix               | Pick one pattern and use consistently             |
| Using `>` instead of `>=` for lower bound       | Boundary entries excluded                                   | `entry.timestamp() >= now - 60_000`               |
| Division by zero in error rate                  | `ArithmeticException` or NaN                                | Guard: `total == 0 ? 0.0 : errors / total`        |
| Iterating entire deque without timestamp break  | Processes all 1M entries even when only 60 valid            | `if (entry.timestamp() > now) break;`             |
| `LinkedList` instead of `ArrayDeque`            | 3x memory overhead (prev/next pointers)                     | `ArrayDeque` — compact array-backed               |
| Not handling empty window in P95                | `IndexOutOfBoundsException` or implicit 0 by luck           | Guard: `if (inWindow.isEmpty()) return 0;`        |
| `System.currentTimeMillis()` instead of `now`   | Tests need `Thread.sleep()`, flaky                          | Use the `now` parameter consistently              |

---

## 8. Verification Checklist

Before declaring done, verify:

### Functional

- [ ] Empty window: error rate = 0.0, P95 = 0, counts = 0
- [ ] Single entry: P95 = entry's response time, count = 1
- [ ] P95 of 20 entries = correct nearest-rank value
- [ ] All ERROR+FATAL entries: error rate = 1.0
- [ ] Mixed levels: error rate = errors / total (exact ratio)
- [ ] Entries outside window excluded from all metrics
- [ ] Entries at exact boundary (now - 60_000) included
- [ ] Future-dated entries excluded
- [ ] MAX_ENTRIES cap with FIFO eviction works

### Thread Safety

- [ ] Concurrent ingestion from 20 threads: total entries match expected
- [ ] Concurrent reads + writes: no exceptions, no corruption
- [ ] ReadWritelock: multiple readers concurrent, writer exclusive
- [ ] No deadlocks (single read lock, single write lock, no nesting)

### Edge Cases

- [ ] Error rate with 0 entries = 0.0 (no division by zero)
- [ ] P95 with 1 entry = that entry's response time
- [ ] P95 requires sort — test with randomly ordered response times
- [ ] Boundary entry: `timestamp == now - 60_000` is included
- [ ] Query at `now + 1` excludes boundary entries correctly

---

## 9. Extension Points (Bonus Discussion)

If you finish early, mention these to stand out:

1. **Single-pass `getMetrics(long now)`** - "If callers need all 4 metrics, I'd offer a batch method that computes
   everything in one pass — one lock acquisition, one iteration."
2. **QuickSelect for P95** - "O(n) average vs O(n log n) for full sort. Only the k-th element matters, no need to order
   the rest."
3. **Lazy time-based eviction** - "On ingest, evict entries older than `entry.timestamp() - 60_000`. Bounds memory by
   time, not just cap."
4. **`MetricsSnapshot` record** - "Return all metrics atomically:
   `record MetricsSnapshot(int total, long errors, double errorRate, long p95)`."
5. **Lock-free ring buffer** - "At >10K writes/sec, `ConcurrentLinkedDeque` with a custom ring buffer and `volatile`
   head/tail indices avoids lock contention."
6. **Approximate percentiles** - "For massive windows, t-digest or DDSketch gives approximate P95 with constant memory —
   what Datadog and Prometheus use."
7. **Incremental counters** - "Maintain running error count/total, evict by decrementing when entries slide out. O(1)
   query instead of O(n)."

---

## 10. Production References

| Resource                                                                            | Why It Matters                                          |
|-------------------------------------------------------------------------------------|---------------------------------------------------------|
| [Micrometer](https://micrometer.io/)                                                | Most popular Java metrics library - timers, percentiles |
| [HdrHistogram](https://github.com/HdrHistogram/HdrHistogram)                        | High Dynamic Range histogram - accurate percentiles     |
| [Datadog t-Digest](https://github.com/tdunning/t-digest)                            | Approximate percentiles with bounded memory             |
| [Prometheus Histogram](https://prometheus.io/docs/practices/histograms/)            | Production percentile tracking in monitoring systems    |
| [Martin Fowler - Sliding Window](https://martinfowler.com/bliki/SlidingWindow.html) | Pattern description for time-bounded aggregation        |

---

*This guideline follows the established challenge template: presentation → edge cases → chain of thinking →
communication → implementation → pro tips → mistakes → verification.*
