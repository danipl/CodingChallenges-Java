# Challenge: BuildCache - Guidelines

## 1. Challenge Presentation

### What You're Building

A file-backed build cache: store build artifacts by key so developers don't recompile unchanged inputs. A colleague's PR
has landed on your desk — tests are failing intermittently and devs report it "returning results that make no sense."

This is the **code review pairing format**: you're not building from scratch, you're reviewing, diagnosing, and fixing a
real PR. The test suite is the spec — read it as the PR's acceptance criteria.

### Core Contract

```
put(key, content) --> <cacheDir>/<key>.bin
get(key)          --> Optional<bytes> | CacheException(key)
stats()           --> Stats(hits, misses)  // accurate under concurrency
```

### Interface Summary

| Method                | Purpose                                                  |
|-----------------------|----------------------------------------------------------|
| `get(String)`         | Read entry; empty if absent, `CacheException` if corrupt |
| `put(String, byte[])` | Store entry; `CacheException` if write fails             |
| `stats()`             | Hits/misses — must not lose updates under concurrency    |
| `CacheException`      | Message must name the key — that's what the dev sees     |

### What Interviewers Evaluate

1. **Review mindset** — Do you find *root causes*, not just fix failing tests? Are the fixes minimal and surgical, not
   rewrites?
2. **Security awareness** — Do you spot the path traversal immediately? A DevEx tool that can write outside its
   directory is a supply-chain bug.
3. **Failure honesty** — Does the cache *tell the truth* when it fails, or silently pretend?

---

## 2. Edge & Corner Cases

### How to Identify Them

This is a **review**, not greenfield. Don't hunt edge cases by brainstorming — *read the tests, then read the impl
against the contract*, and ask: "where does the impl violate what the tests promise?"

| # | Edge Case                                | How It Surfaces                                                  | How to Handle                                                |
|---|------------------------------------------|------------------------------------------------------------------|--------------------------------------------------------------|
| 1 | `../escape` key                          | `resolve()` normalizes → writes outside cacheDir                 | Validate key: `[a-zA-Z0-9._-]+` → `IllegalArgumentException` |
| 2 | Null/blank key                           | `"null.bin"` becomes a real file                                 | Same validation, fail fast                                   |
| 3 | `a/b`, `a\b` keys                        | Path separators → nested dirs / traversal on Windows             | Same validation                                              |
| 4 | Corrupt entry (dir where file should be) | `readAllBytes` throws IOException → *silently treated as miss!*  | `CacheException("corrupt cache entry: <key>")`               |
| 5 | Write failure (cacheDir is a file)       | `Files.write` throws → *silently swallowed!*                     | `CacheException("failed to write cache entry: <key>")`       |
| 6 | Corrupt entry counted as miss            | Stats lie — devs see "cache miss" for a present-but-broken entry | Corrupt ≠ miss; don't increment misses on failure            |
| 7 | Concurrent gets/puts                     | `hits++`/`misses++` lose updates                                 | `AtomicLong` (or `LongAdder`)                                |
| 8 | Constructor dir-creation failure         | Swallowed `IOException` — "we'll notice when we write"           | Fail fast or surface on first use                            |

### Quick Pre-Implementation Checklist

```
▢ Have I read ALL tests as the spec before touching code?
▢ Have I named each failure's ROOT CAUSE (not its symptom)?
▢ Does each fix stay minimal and inside the impl?
```

---

## 3. First Approach - Chain of Thinking

### Minute 0-2: Read the PR Like a Reviewer

Start with the review framing, out loud:

- "Let me read the contract and the tests first — they tell me what this PR *promises*."
- "Then I'll read the impl and look for where it *violates* those promises."

### Minute 2-5: Diagnose, Don't Patch

Run the tests. Group the failures by root cause — notice they cluster:

```
Root cause 1: NO KEY VALIDATION  -> 5 failures (null, blank, traversal, separators, escape-file)
Root cause 2: SWALLOWED IO       -> 3 failures (corrupt entry, failed write, corrupt-not-miss)
Root cause 3: NON-ATOMIC COUNTERS -> 1 failure (lost updates)
```

Three root causes, nine failing tests. That's the review insight: **fix the pattern, not the symptoms.**

### Minute 5-10: Fix in Order of Severity

1. **Key validation** (security — do this first)
2. **Failure honesty** (correctness of the cache's contract)
3. **Atomic counters** (concurrency correctness)

### Minute 10-25: Implement

- Validation: one small guard method, called by both `get` and `put`
- `get`: let `IOException` escape as `CacheException` with the key; don't count as miss
- `put`: let `IOException` escape as `CacheException` with the key
- Counters: `AtomicLong`

---

## 4. Communication Approach During the Interview

### What to Say Out Loud (Review Mode — blameless!)

| Moment                 | Say This                                                                                                                                                                                       |
|------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| On the traversal       | "The `resolve(key)` without validation means a key like `../escape` writes outside the cache dir. I'll add a validation guard — that's the kind of thing a CI cache really shouldn't allow."   |
| On the swallowed write | "`put` catches and ignores `IOException` — so a full disk or read-only cache dir looks like a successful cache. I'd rather fail loudly with the key so the dev knows caching is off."          |
| On the silent miss     | "A corrupt entry is counted as a miss and returns empty — so a broken cache silently recompiles everything. Worse, stats lie. I'll surface it as `CacheException` and not count it as a miss." |
| On the counters        | "`hits++` isn't atomic — under concurrent builds we lose counts. `AtomicLong` fixes it without a lock."                                                                                        |
| Reviewing the author   | "The structure is actually clean — single class, clear intent. The issues are validation and error handling, which are easy to miss in a first PR."                                            |

### When Stuck

```
I notice three failure clusters in the test results.
The risk is fixing symptoms and leaving the root cause.
Two options: [A] validate keys at the boundary, [B] pre-escape every path manually.
I'll go with [A] because one guard covers get and put. Does that align?
```

---

## 5. Implementation Structure

```java
public final class BuildCacheImpl implements BuildCache {
    // VALID_KEY_PATTERN = Pattern.compile("[a-zA-Z0-9._-]+")
    // get(String)  -> validateKey, resolve, exists? read : miss
    // put(String, byte[]) -> validateKey, resolve, write, translate IOException
    // validateKey(String) -> throws IllegalArgumentException
    // AtomicLong hits, misses
}
```

### Key Implementation Pattern

```java
private static final Pattern VALID_KEY = Pattern.compile("[a-zA-Z0-9._-]+");

private void validateKey(final String key) {
    if (key == null || key.isBlank() || !VALID_KEY.matcher(key).matches()) {
        throw new IllegalArgumentException("invalid cache key: '" + key + "'");
    }
}

// get: catch (IOException e) -> throw new CacheException("corrupt cache entry: " + key, e)
// put: catch (IOException e) -> throw new CacheException("failed to write cache entry: " + key, e)
```

---

## 6. Technical Pro Tips

### Swallowed Exceptions — the Review Catch

| Pattern                                              | Looks like      | Reality                                      |
|------------------------------------------------------|-----------------|----------------------------------------------|
| `catch (IOException e) {}`                           | "best effort"   | Silent failure — the code lies about success |
| `catch (IOException e) { return Optional.empty(); }` | "treat as miss" | Corrupt ≠ absent — stats lie too             |
| `catch (Exception e)`                                | "defensive"     | Hides programming bugs                       |

The rule: **only catch what you can meaningfully recover from, and never silently.**

### `resolve()` and Path Traversal

`Paths.resolve("../x")` normalizes — it *walks up*. Validation is not optional for any path built from user input. In a
real review, this is a must-block finding.

### AtomicCounters

`long hits++` is three operations (read, add, write) — two threads interleaving lose an update.
`AtomicLong.getAndIncrement()` is one. For high-contention counters, `LongAdder` scales better.

### What Senior Engineers Demonstrate

1. **Fix patterns, not tests** — nine failures, three root causes.
2. **Security first** — traversal before correctness; a cache that can write anywhere is worse than no cache.
3. **Blameless review language** — "easy to miss in a first PR", never "this code is garbage".

---

## 7. Common Mistakes to Avoid

| Mistake                                                  | Why It Fails                       | Fix                                                   |
|----------------------------------------------------------|------------------------------------|-------------------------------------------------------|
| Adding `catch` blocks around the fixes                   | Re-introduces silent failure       | Translate to `CacheException` with key, don't swallow |
| Counting corrupt entries as misses                       | Stats lie → devs chase ghosts      | Only count true absences as misses                    |
| Validating in `get` but not `put` (or vice versa)        | Half the surface still unsafe      | One guard method, both call it                        |
| `String key + ".bin"` concatenation instead of `resolve` | Same result, hides the intent      | `cacheDir.resolve(key + ".bin")` after validation     |
| Locking the whole cache for counters                     | Overkill, serializes reads         | `AtomicLong` is enough                                |
| Rewriting the whole class "while I'm here"               | Review is surgical, not greenfield | Fix the three root causes, stop                       |

---

## 8. Verification Checklist

### Security

- [ ] Null, blank, separator, and `../` keys all throw `IllegalArgumentException`
- [ ] A rejected traversal writes nothing outside the cache directory

### Correctness

- [ ] Round-trip, overwrite, and absent-key behavior unchanged
- [ ] Corrupt entry → `CacheException` with key in message
- [ ] Failed write → `CacheException` with key in message
- [ ] Corrupt entries not counted as misses

### Concurrency

- [ ] Stats exact under 20 threads × 100 ops (hits and misses)

### Test Invocation

```bash
./gradlew test --tests "com.danipl.practise.cli.buildcache.*"
```

---

## 9. Extension Points (Bonus Discussion)

- **TTL/staleness** — should `get` check age and evict stale entries? (The classic build-cache failure: "I changed the
  code but got yesterday's artifact.")
- **Atomic writes** — write to a temp file + `ATOMIC_MOVE` so readers never see a half-written entry.
- **Checksums** — store a hash of the input alongside the artifact and verify on read.
- **Eviction** — LRU by last-access time when the cache dir grows unbounded.
- **Keyed by content** — hash the input, not a human-chosen key (the actual "never recompile unchanged inputs" design).

---

## 10. Production References

| Resource                      | Why It Matters                                              |
|-------------------------------|-------------------------------------------------------------|
| Effective Java (Bloch)        | Item 70/71: checked vs unchecked, exception honesty         |
| OWASP Path Traversal          | The exact vulnerability class in the key validation bug     |
| `java.util.concurrent.atomic` | AtomicLong/LongAdder for correct counters                   |
| `java.nio.file.Files`         | `readAllBytes`, `write`, `createDirectories`, `ATOMIC_MOVE` |

---

*This guideline follows the standard practise-coach template: presentation → edge cases → chain of thinking →
communication → implementation → pro tips → mistakes → verification → extensions → references.*
