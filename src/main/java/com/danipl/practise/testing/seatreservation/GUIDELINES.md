# Challenge: SeatReservation — Guidelines

## 1. Challenge Presentation

### What You're Building
Tests for a seat reservation component. The implementation is done — your job is to write a comprehensive test suite that proves it works correctly and handles all edge cases.

### Core Contract
- `reserve(id)` — reserves a seat; throws on null/blank/duplicate
- `release(id)` — releases a seat; throws on null/blank/not-reserved
- `isReserved(id)` — returns boolean; false for null/blank
- `reservedCount()` — returns current count

### Interface Summary
| Method | Happy Path | Throws |
|--------|-----------|--------|
| `reserve` | Adds seat to reserved set | `IllegalArgumentException` (null/blank), `IllegalStateException` (duplicate) |
| `release` | Removes seat from reserved set | `IllegalArgumentException` (null/blank), `IllegalStateException` (not reserved) |
| `isReserved` | Returns `true`/`false` | Never throws |
| `reservedCount` | Returns current size | Never throws |

---

## 2. Edge & Corner Cases

### How to Identify Them
Read every branch in the implementation. Each `if` is a test case. Each exception throw is an `assertThrows`.

| # | Edge Case | How It Surfaces | How to Handle |
|---|-----------|-----------------|---------------|
| 1 | `null` seat ID | `IllegalArgumentException` | `assertThrows` + verify message |
| 2 | `""` (empty string) | `IllegalArgumentException` | `assertThrows` |
| 3 | `"  "` (blank/whitespace) | `IllegalArgumentException` | `assertThrows` |
| 4 | Reserve same seat twice | `IllegalStateException` | Reserve once, then assertThrows on second |
| 5 | Release non-reserved seat | `IllegalStateException` | `assertThrows` without prior reserve |
| 6 | `isReserved(null)` | Returns `false` (no throw) | `assertFalse` |
| 7 | Reserve → release → reserve again | Should succeed | Verify full lifecycle |
| 8 | Count after mixed operations | Must reflect actual state | Reserve 3, release 1, assert count == 2 |

---

## 3. First Approach — Chain of Thinking

- **Minute 0–2**: Read `SeatReservationImpl.java` line by line. Identify every branch.
- **Minute 2–5**: Map each branch to a test. Count: you need at least 1 test per branch.
- **Minute 5–15**: Write tests. Start with happy paths, then exceptions, then edge cases.
- **Minute 15–20**: Run tests. Fix any that fail. Ensure 100% branch coverage.
- **Minute 20–25**: Review — did you miss any combination? Is naming clear?

---

## 4. Communication Approach

Explain your test design out loud:
- Why you grouped tests into `@Nested` classes the way you did
- How you verify exception messages (exact match vs. `contains`)
- Whether you test `isReserved` after `release` (lifecycle coverage)

---

## 5. Implementation Structure

```java
@Nested
@DisplayName("reserve")
class Reserve {
    @Test
    void reserveValidSeat() {
        reservation.reserve("A1");
        assertTrue(reservation.isReserved("A1"));
    }

    @Test
    void reserveNullThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> reservation.reserve(null));
    }
}
```

---

## 6. Technical Pro Tips

- Use `assertThrows` with a lambda — don't catch exceptions manually.
- Verify exception *type* at minimum; message checks are a bonus.
- Each test should be independent — no shared mutable state between tests.
- `@BeforeEach` gives you a fresh instance per test — use it.
- Group by method under test (`@Nested` per method) for clear structure.

---

## 7. Common Mistakes to Avoid

- **Testing only happy paths** — the exceptions and null/blank inputs are where bugs hide.
- **Shared state between tests** — if test B depends on test A's side effects, it will fail in isolation.
- **Missing lifecycle tests** — reserve → release → reserve again is a common bug pattern.
- **Not testing `isReserved` with null/blank** — it returns `false` instead of throwing; that's a deliberate design choice you should verify.

---

## 8. Verification Checklist

- [ ] All 11 TODO tests implemented
- [ ] `assertThrows` used for all exception cases (not manual try/catch)
- [ ] Null, empty, and blank inputs all tested
- [ ] Reserve → release → reserve lifecycle tested
- [ ] `reservedCount` tested after mixed operations
- [ ] All tests pass: `./gradlew test --tests "com.danipl.practise.testing.seatreservation.*"`

---

## 9. Extension Points

- How would you test this if the backing store was a database instead of a `HashSet`?
- What if `reserve` needed to be thread-safe? How would your tests change?
- How would you add a `reserveAll(List<String> ids)` method and test it atomically?

---

## 10. Production References

- *Effective Java* (Joshua Bloch) — Item 72: Favor the use of standard exceptions
- *Pragmatic Unit Testing in Java 8 with JUnit* (Andy Hunt & Dave Thomas)
