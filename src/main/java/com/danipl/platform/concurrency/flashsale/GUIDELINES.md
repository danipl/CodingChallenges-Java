# Challenge: Flash Sale Ticket Reservation System - Live Coding Guidelines

## 1. Challenge Presentation

### What You're Building

A thread-safe Flash Sale system that handles mass ticket sales under extreme concurrency.
Thousands of buyers compete for limited tickets — the system must prevent double-selling,
support temporary reservations with expiration, and allow lock-free status reads.

This is a **local locking** problem: in production you'd use Redis/Redlock, but here we
simulate the concurrency semantics using only JDK primitives (`ReentrantLock`, `volatile`,
`ConcurrentHashMap`).

### Core Contract

```
Ticket Lifecycle:
  AVAILABLE ──reserve──▶ RESERVED ──confirm──▶ SOLD
      ▲                     │
      └─────cancel/expire───┘

Invariants:
  - A SOLD ticket can never be reserved again
  - Only one RESERVATION per ticket at a time
  - Expired reservations release the ticket back to AVAILABLE
  - getTicketStatus() is lock-free (volatile read)
```

### Interface Summary

| Method | Purpose |
|--------|---------|
| `of(Config)` | Factory — initialize tickets and timeout |
| `reserveTicket(ticketId, customerId)` | Hold a ticket temporarily; returns Reservation |
| `confirmPurchase(reservationId)` | Finalize purchase; transitions to SOLD |
| `cancelReservation(reservationId)` | Release hold; returns to AVAILABLE |
| `getTicketStatus(ticketId)` | Lock-free status read (volatile) |
| `releaseExpiredReservations()` | Sweep and release stale holds |
| `totalTickets()` / `availableTickets()` / `reservedTickets()` / `soldTickets()` | Inventory counts |

### What Interviewers Evaluate

1. **Lock granularity** — Global lock vs per-ticket lock. A global lock is correct but
   fails under contention. Per-ticket locks show systems thinking.
2. **Volatile usage** — Understanding when `volatile` suffices (single-writer visibility)
   vs when a lock is needed (compound check-then-act).
3. **TOCTOU awareness** — Recognizing that `getTicketStatus()` is inherently racy and
   discussing why that's acceptable for a status query but not for a reservation decision.

---

## 2. Edge & Corner Cases

### How to Identify Them Before Coding

Think about the **state transitions** and what can go wrong at each boundary:
- What if two threads try to reserve the same ticket simultaneously?
- What if a reservation expires between the check and the confirm?
- What if the same reservation is confirmed twice?
- What if a cancel arrives after expiration?

| # | Edge Case | How It Surfaces | How to Handle |
|---|-----------|-----------------|---------------|
| 1 | **Double reservation** | Two threads pass the AVAILABLE check simultaneously | Per-ticket lock: only one thread enters the critical section |
| 2 | **Confirm after expiry** | Reservation timeout elapses before confirm arrives | Check expiration inside the lock; reject if expired |
| 3 | **Double confirm** | Same reservationId confirmed twice | Remove reservation from registry on first confirm; second finds nothing |
| 4 | **Cancel after expiry** | Expiry sweep runs, then cancel arrives | Cancel finds no reservation → return false (idempotent) |
| 5 | **Non-existent ticket** | Caller passes unknown ticketId | Throw FlashSaleException (fail-fast) |
| 6 | **SOLD ticket re-reservation** | Someone tries to reserve an already-purchased ticket | Throw FlashSaleException |

### Quick Pre-Implementation Checklist

```
▢ Per-ticket lock (not global) to avoid contention
▢ volatile status for lock-free reads
▢ Lock ordering: ticket lock → reservation lock (never reverse)
▢ Clock injection for testable time
▢ Expiration sweep must lock both ticket and reservation
```

---

## 3. First Approach - Chain of Thinking

### Minute 0-2: Clarify Requirements

- "Is the reservation timeout per-sale or configurable per reservation?"
- "Should `getTicketStatus` be strongly consistent or is eventual consistency acceptable?"
- "Can the same customer reserve multiple different tickets simultaneously?"
- "What happens to a reservation that is confirmed after it has expired?"

### Minute 2-5: Design

Sketch the data structures:

```
tickets: ConcurrentHashMap<String, TicketEntry>
  where TicketEntry = {
    ReentrantLock lock
    volatile TicketStatus status
    volatile String currentReservationId
  }

reservations: ConcurrentHashMap<String, Reservation>
  (reservationId → Reservation record)

reservationLock: ReentrantLock  // protects reservation registry mutations
```

Key insight: **two levels of locking**:
1. Per-ticket lock for state transitions (AVAILABLE → RESERVED → SOLD)
2. Global reservation lock for registry operations (confirm/cancel/expire)

### Minute 5-10: Sketch the Core Flow

```
reserveTicket(ticketId, customerId):
  entry = tickets.get(ticketId)
  if entry == null → throw
  entry.lock.lock()
  try:
    if entry.status != AVAILABLE → throw
    reservationId = UUID.randomUUID()
    expiresAt = clock.instant().plusMillis(timeout)
    reservation = new Reservation(reservationId, ticketId, customerId, expiresAt)
    entry.status = RESERVED          // volatile write
    entry.currentReservationId = reservationId
    reservations.put(reservationId, reservation)
    return reservation
  finally:
    entry.lock.unlock()

confirmPurchase(reservationId):
  reservationLock.lock()
  try:
    reservation = reservations.get(reservationId)
    if reservation == null → return false
    if clock.instant().isAfter(reservation.expiresAt()):
      // Expired: release ticket
      releaseTicket(reservation)
      return false
    entry = tickets.get(reservation.ticketId())
    entry.lock.lock()
    try:
      entry.status = SOLD
      entry.currentReservationId = null
    finally:
      entry.lock.unlock()
    reservations.remove(reservationId)
    return true
  finally:
    reservationLock.unlock()
```

### Minute 10-25: Implement

1. Initialize `tickets` map in constructor with one `TicketEntry` per ticketId
2. Implement `reserveTicket` with per-ticket lock
3. Implement `confirmPurchase` with reservation lock + ticket lock
4. Implement `cancelReservation` (similar to confirm but sets status back to AVAILABLE)
5. Implement `releaseExpiredReservations` (iterate reservations, check expiry)
6. Implement `getTicketStatus` as a simple volatile read (no lock)
7. Implement counter methods by iterating ticket entries

---

## 4. Communication Approach During the Interview

### What to Say Out Loud

| Moment | Say This |
|--------|----------|
| Starting | "I'll use per-ticket locks to avoid global contention. A single lock would serialize all reservations even for different tickets." |
| Before volatile | "Status reads don't need a lock — volatile gives us visibility. The trade-off is TOCTOU, which is acceptable for a status query." |
| About expiration | "The expiration sweep needs both the reservation lock and the ticket lock. I'll acquire them in a consistent order to prevent deadlocks." |
| About confirm | "Confirm checks expiry inside the lock. If expired, it releases the ticket and returns false — the caller can retry with a different ticket." |

### When Stuck

```
I notice the expiration sweep needs to modify both the reservation registry and the ticket status.
The risk is deadlock if we acquire locks in different orders.
Two options: (A) always lock ticket first, then reservation registry, or (B) use a single global lock for the sweep.
I'll go with (A) because it maintains the per-ticket granularity for normal operations.
Does that align with your expectations?
```

---

## 5. Implementation Structure

### Recommended File Layout

```java
public final class FlashSaleImpl implements FlashSale {
    // === Fields ===
    private final Config config;
    private final Clock clock;
    private final Map<String, TicketEntry> tickets;     // ConcurrentHashMap
    private final Map<String, Reservation> reservations; // ConcurrentHashMap
    private final ReentrantLock reservationLock;         // global reservation lock

    // === Constructor ===
    // Initialize tickets map, set clock

    // === Core methods ===
    // reserveTicket: per-ticket lock, check-then-act
    // confirmPurchase: reservation lock → ticket lock
    // cancelReservation: reservation lock → ticket lock
    // releaseExpiredReservations: iterate + lock

    // === Read methods (lock-free) ===
    // getTicketStatus: volatile read
    // availableTickets/reservedTickets/soldTickets: iterate + count

    // === Internal types ===
    static final class TicketEntry { ... }
}
```

### Key Implementation Pattern

**Per-ticket lock with volatile status:**

```java
// Lock-free read (volatile)
public TicketStatus getTicketStatus(String ticketId) {
    TicketEntry entry = tickets.get(ticketId);
    if (entry == null) throw new FlashSaleException("Unknown ticket: " + ticketId);
    return entry.status;  // volatile read — no lock needed
}

// Locked write (per-ticket lock)
public Reservation reserveTicket(String ticketId, String customerId) {
    TicketEntry entry = tickets.get(ticketId);
    if (entry == null) throw new FlashSaleException("Unknown ticket: " + ticketId);
    entry.lock.lock();
    try {
        if (entry.status != TicketStatus.AVAILABLE) {
            throw new FlashSaleException("Ticket not available: " + ticketId);
        }
        // ... create reservation, update status
        entry.status = TicketStatus.RESERVED;  // volatile write
    } finally {
        entry.lock.unlock();
    }
}
```

---

## 6. Technical Pro Tips

### Lock Granularity Comparison

| Strategy | Pros | Cons |
|----------|------|------|
| **Global lock** | Simple, correct | Serializes ALL operations; fails under high contention |
| **Per-ticket lock** | High throughput for different tickets | More complex; need lock ordering discipline |
| **Striped lock** (fixed array of locks, hash ticketId) | Bounded memory, good contention | Slightly more complex; hash collisions |
| **Lock-free (CAS)** | Maximum throughput | Very complex for compound operations; not practical here |

**Interview answer**: "Per-ticket locks. A global lock would serialize 10,000 concurrent buyers
even if they want different tickets. Per-ticket locks let non-conflicting reservations proceed
in parallel."

### Volatile vs Lock Decision

| Operation | Sufficient? | Why |
|-----------|-------------|-----|
| `getTicketStatus()` | `volatile` | Single read, no compound check |
| `reserveTicket()` | `lock` | Check-then-act: read status → write status + create reservation |
| `confirmPurchase()` | `lock` | Compound: check expiry → update status → remove reservation |
| Counter methods | Iterate + read volatile | Each read is consistent; total may be slightly stale but that's acceptable |

### What Senior Engineers Demonstrate

1. **Lock ordering discipline** — Always acquire ticket lock before reservation lock.
   Mention this proactively: "I need to establish a lock ordering to prevent deadlocks."
2. **TOCTOU acknowledgment** — "getTicketStatus is inherently racy. In production, you'd
   return a reservation attempt result instead of a separate status check."
3. **Testability** — Clock injection from day one. "I'll inject Clock so tests can control
   time without Thread.sleep."

---

## 7. Common Mistakes to Avoid

| Mistake | Why It Fails | Fix |
|---------|-------------|-----|
| **Global lock for everything** | Serializes all operations; 100 threads for 1 ticket take 100x longer | Per-ticket lock |
| **Using `synchronized` on the whole object** | Same as global lock; poor throughput | Use `ReentrantLock` per ticket |
| **Reading status without volatile** | Thread may see stale value indefinitely | `volatile TicketStatus status` |
| **Not checking expiry inside the lock** | Reservation may expire between check and confirm | Check `clock.instant().isAfter(expiresAt)` inside the lock |
| **Forgetting to remove reservation on confirm** | Second confirm finds the reservation and "sells" again | `reservations.remove(reservationId)` inside the lock |
| **Lock ordering violation** | Thread A: ticket→reservation, Thread B: reservation→ticket = deadlock | Always: ticket lock first, then reservation lock |
| **Not handling null ticket** | NPE instead of clear error | Check `tickets.get()` result; throw FlashSaleException |

---

## 8. Verification Checklist

### Functional

- [ ] Reserve transitions AVAILABLE → RESERVED
- [ ] Confirm transitions RESERVED → SOLD
- [ ] Cancel transitions RESERVED → AVAILABLE
- [ ] Expired reservations release back to AVAILABLE
- [ ] Cannot reserve a SOLD or already-RESERVED ticket
- [ ] Cannot confirm an expired or non-existent reservation
- [ ] Counter methods return consistent totals

### Thread Safety

- [ ] 100 threads compete for 1 ticket → exactly 1 wins
- [ ] 5 threads reserve 5 different tickets → all succeed
- [ ] Concurrent reserve + confirm → no double-sells
- [ ] Concurrent cancel + expire → no double-release
- [ ] No deadlocks under mixed operations

### Edge Cases

- [ ] Unknown ticketId throws FlashSaleException
- [ ] Unknown reservationId returns false
- [ ] Config rejects empty ticket list and zero timeout
- [ ] Double-confirm returns false on second call

---

## 9. Extension Points (Bonus Discussion)

1. **Distributed locking (Redis/Redlock)** — In production, tickets live on multiple servers.
   Discuss how Redis SETNX with TTL replaces per-ticket ReentrantLock.

2. **Reservation queue / waitlist** — When a ticket is reserved, queue interested buyers.
   If the reservation expires, automatically offer to the next in line.

3. **Idempotency keys** — Prevent duplicate purchases by requiring a client-generated
   idempotency key. Store it with the reservation to detect retries.

4. **Metrics and observability** — Track reservation-to-confirm ratio, average hold time,
   expiration rate. Use `LongAdder` for high-throughput counters.

5. **Graceful shutdown** — On shutdown, release all reservations so tickets aren't permanently
   locked. Discuss `ExecutorService.awaitTermination` and a shutdown hook.

---

## 10. Production References

| Resource | Why It Matters |
|----------|---------------|
| [Java Concurrency in Practice (Goetz)](https://jcip.net/) | The definitive guide to volatile, locks, and safe publication |
| [Redis Distributed Locking](https://redis.io/docs/manual/patterns/distributed-locks/) | How production flash sales actually implement locking |
| [Martin Kleppmann - Designing Data-Intensive Applications](https://dataintegrity.net/) | Chapter on consensus and distributed locks |
| [Resilience4j](https://resilience4j.github.io/resilience4j/) | Production-grade resilience patterns in Java |
| [Doug Lea's JSR-166 Expert Group](https://g.oswego.edu/dl/concurrency-interest/) | The origin of java.util.concurrent; read the source |

---

*This guideline follows the standard platform challenge template: presentation → edge cases → chain of thinking →
communication → implementation → pro tips → mistakes → verification → extensions → references.*
