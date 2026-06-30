# Challenge: Write Ahead Log (WAL) - Live Coding Guidelines

## 1. Challenge Presentation

### What You're Building

A thread-safe Write Ahead Log — the durability primitive behind every state machine (databases, message queues, consensus protocols). Every mutation is appended as a log entry with a monotonically increasing sequence number. Snapshots mark recovery points; on crash, load the last snapshot and replay only entries after it.

### Core Contract

```
append("SET x=1")  → LogEntry(seq=1, record="SET x=1")
append("SET y=2")  → LogEntry(seq=2, record="SET y=2")
append("SET z=3")  → LogEntry(seq=3, record="SET z=3")
markSnapshot(2)    → snapshot at seq 2
recoverFromSnapshot() → [LogEntry(2), LogEntry(3)]  // inclusive
truncateBeforeSnapshot() → removes entry 1, keeps [2, 3]
```

### Interface Summary

| Method | Purpose |
|--------|---------|
| `of(Config)` | Factory — creates WAL instance |
| `append(String)` | Append record, returns LogEntry with seq num |
| `markSnapshot(long)` | Mark recovery point at seq num |
| `recoverFromSnapshot()` | Entries from snapshot onward (inclusive) |
| `truncateBeforeSnapshot()` | Remove entries before snapshot |
| `entries()` | All entries (unmodifiable) |
| `lastSequenceNumber()` | Most recent seq num (0 if empty) |
| `size()` | Entry count |
| `snapshotSequenceNumber()` | Last snapshot seq num (0 if none) |

### What Interviewers Evaluate

1. **Thread-safety** — correct lock selection (ReadWriteLock for read-heavy workload), no race conditions under concurrent appenders
2. **State management** — monotonic sequence numbers, snapshot tracking, truncation correctness
3. **Edge case handling** — empty log, non-existent seq num, null/blank records, snapshot at boundaries

---

## 2. Edge & Corner Cases

### How to Identify Them Before Coding

Think about: empty state, boundary values (first/last entry), invalid inputs, concurrent mutations, snapshot movement.

| # | Edge Case | How It Surfaces | How to Handle |
|---|-----------|-----------------|---------------|
| 1 | **Empty log** | `recoverFromSnapshot()` on empty | Return empty list |
| 2 | **No snapshot marked** | `recoverFromSnapshot()` before `markSnapshot()` | Return all entries |
| 3 | **Snapshot at first entry** | `markSnapshot(1)` then `recoverFromSnapshot()` | Return all entries |
| 4 | **Snapshot at last entry** | `markSnapshot(last)` then `recoverFromSnapshot()` | Return only last entry |
| 5 | **Truncate with no snapshot** | `truncateBeforeSnapshot()` before any snapshot | No-op |
| 6 | **Non-existent seq num** | `markSnapshot(999)` when log has 3 entries | Throw IllegalArgumentException |
| 7 | **Null/blank record** | `append(null)` or `append("")` | Throw IllegalArgumentException |
| 8 | **Moving snapshot forward** | `markSnapshot(2)` then `markSnapshot(3)` | Update snapshot seq num |
| 9 | **Truncate then append** | Truncate removes entries, then append new | New entries get next seq num (no gaps) |
| 10 | **Concurrent appends** | 20 threads appending simultaneously | Monotonic seq nums, no lost updates |

### Quick Pre-Implementation Checklist

```
▢ Sequence numbers start at 1, increment by 1
▢ Snapshot seq num = 0 means no snapshot
▢ recoverFromSnapshot is inclusive of snapshot entry
▢ truncateBeforeSnapshot keeps snapshot entry and all after
▢ entries() returns unmodifiable list
▢ Lock: ReadWriteLock (reads >> writes)
▢ Validate: record not null/blank, seq num exists
```

---

## 3. First Approach - Chain of Thinking

### Minute 0-2: Clarify Requirements

Ask:
- "Should sequence numbers be contiguous (no gaps) even under concurrent appends?" → Yes, lock protects increment
- "Can snapshot move backward, or only forward?" → Only forward (or same), never backward
- "After truncation, do sequence numbers reset?" → No, they continue from where they left off
- "Is maxEntries a hard limit that blocks appends, or soft that forces truncation?" → Soft, forces truncation on next append

### Minute 2-5: Design

Data structures:
- `ArrayList<LogEntry>` — ordered append, index-based snapshot lookup
- `long nextSequenceNumber = 1` — next seq num to assign
- `long snapshotSeqNum = 0` — 0 = no snapshot
- `ReentrantReadWriteLock` — read lock for queries, write lock for mutations

State transitions:
```
append: writeLock → add entry, increment nextSeq
markSnapshot: writeLock → validate seq num exists, set snapshotSeqNum
recoverFromSnapshot: readLock → find snapshot index, return sublist
truncateBeforeSnapshot: writeLock → find snapshot index, remove sublist
```

### Minute 5-10: Sketch the Core Flow

```java
append(record):
  writeLock.lock()
  try:
    validate(record)
    entry = new LogEntry(nextSeqNum++, record)
    entries.add(entry)
    if maxEntries > 0 and entries.size() > maxEntries:
      truncateBeforeSnapshot()  // internal call, lock already held
    return entry
  finally:
    writeLock.unlock()

markSnapshot(seqNum):
  writeLock.lock()
  try:
    if seqNum < 1 or seqNum >= nextSeqNum:
      throw IllegalArgumentException
    snapshotSeqNum = seqNum
  finally:
    writeLock.unlock()

recoverFromSnapshot():
  readLock.lock()
  try:
    if snapshotSeqNum == 0: return List.copyOf(entries)
    startIndex = findIndex(snapshotSeqNum)
    return List.copyOf(entries.subList(startIndex, entries.size()))
  finally:
    readLock.unlock()
```

### Minute 10-25: Implement

1. Fields: `config`, `lock`, `readLock`, `writeLock`, `entries` (ArrayList), `nextSequenceNumber`, `snapshotSeqNum`
2. `append()`: validate, create LogEntry, add to list, increment seq num, check maxEntries
3. `markSnapshot()`: validate seq num exists, set snapshotSeqNum
4. `recoverFromSnapshot()`: find snapshot index, return sublist (or all if no snapshot)
5. `truncateBeforeSnapshot()`: find snapshot index, remove entries before it
6. Helpers: `findIndex(seqNum)` — binary search or linear scan (linear is fine for interview)
7. Read-only methods: `entries()`, `size()`, `lastSequenceNumber()`, `snapshotSequenceNumber()`

---

## 4. Communication Approach During the Interview

### What to Say Out Loud

| Moment | Say This |
|--------|----------|
| Starting | "I'm building a WAL with append-only semantics and snapshot-based recovery. I'll use a ReadWriteLock since reads (queries) outnumber writes (appends)." |
| Before locking | "Append and markSnapshot mutate state, so they need the write lock. Queries like entries() and size() only read, so they take the read lock — this allows concurrent readers." |
| About sequence numbers | "Sequence numbers are logical clocks — they start at 1 and increment monotonically. After truncation, they don't reset; new entries continue from the last seq num." |
| About snapshot | "The snapshot is a single seq num, not a list of entries. recoverFromSnapshot finds that entry's index and returns everything from there onward." |

### When Stuck

```
I notice I need to find the index of a snapshot entry by its sequence number.
The risk is that linear scan is O(n), but for an interview, that's acceptable.
Two options: linear scan or binary search (since entries are sorted by seq num).
I'll go with linear scan for simplicity. If this were production with millions of entries,
I'd use binary search or maintain a HashMap<seqNum, index>. Does that align with your expectations?
```

---

## 5. Implementation Structure

### Recommended File Layout

```java
public final class WriteAheadLogImpl implements WriteAheadLog {
    // === Fields ===
    private final Config config;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    private final List<LogEntry> entries = new ArrayList<>();
    private long nextSequenceNumber = 1;
    private long snapshotSeqNum = 0;

    // === Constructor ===
    public WriteAheadLogImpl(Config config) { this.config = config; }

    // === Core methods ===
    public LogEntry append(String record) { ... }
    public void markSnapshot(long seqNum) { ... }
    public List<LogEntry> recoverFromSnapshot() { ... }
    public void truncateBeforeSnapshot() { ... }

    // === Read-only methods ===
    public List<LogEntry> entries() { ... }
    public long lastSequenceNumber() { ... }
    public int size() { ... }
    public long snapshotSequenceNumber() { ... }

    // === Private helpers ===
    private int findIndex(long seqNum) { ... }
}
```

### Key Implementation Pattern

**Finding snapshot index** (linear scan):

```java
private int findIndex(long seqNum) {
    for (int i = 0; i < entries.size(); i++) {
        if (entries.get(i).sequenceNumber() == seqNum) return i;
    }
    return -1; // not found
}
```

**Truncation**:

```java
public void truncateBeforeSnapshot() {
    writeLock.lock();
    try {
        if (snapshotSeqNum == 0) return; // no snapshot, no-op
        int snapshotIndex = findIndex(snapshotSeqNum);
        if (snapshotIndex > 0) {
            entries.subList(0, snapshotIndex).clear();
        }
    } finally {
        writeLock.unlock();
    }
}
```

---

## 6. Technical Pro Tips

### Lock Selection: ReadWriteLock vs ReentrantLock

| Lock Type | Use When | This Challenge |
|-----------|----------|----------------|
| `ReentrantLock` | Write-heavy, single writer | ❌ Not this — we have many concurrent readers |
| `ReentrantReadWriteLock` | Read-heavy, reads >> writes | ✅ Perfect — queries outnumber appends |
| `ReentrantLock` + `Condition` | Blocking wait with signal | ❌ Not needed — no blocking operations |

**Why ReadWriteLock here?** In a real WAL, you might have 100 threads reading `entries()` for replication while only 1 thread appends. ReadWriteLock allows all 100 readers to proceed concurrently; ReentrantLock would serialize them.

### Production vs Interview Considerations

| Aspect | Interview | Production |
|--------|-----------|------------|
| Storage | In-memory ArrayList | Disk-backed (file, mmap) with fsync |
| Recovery | From snapshot in memory | Load snapshot from disk, replay log |
| Max entries | Soft limit, force truncation | Hard limit, block appends or spill to disk |
| Snapshot | Single marker | Multiple snapshots (compaction) |
| Concurrency | ReadWriteLock | Lock-free (ConcurrentLinkedQueue) or segmented locks |

### What Senior Engineers Demonstrate

1. **Defensive validation** — check record not null/blank, seq num exists, before mutating state
2. **Unmodifiable returns** — `List.copyOf()` or `Collections.unmodifiableList()` to prevent external mutation
3. **Edge case coverage** — empty log, no snapshot, snapshot at boundaries, concurrent appends

---

## 7. Common Mistakes to Avoid

| Mistake | Why It Fails | Fix |
|---------|-------------|-----|
| **Using ReentrantLock instead of ReadWriteLock** | Serializes readers, poor concurrency | Use ReadWriteLock for read-heavy workload |
| **Forgetting to validate seq num in markSnapshot** | Allows marking non-existent entries | Check `seqNum >= 1 && seqNum < nextSequenceNumber` |
| **Returning mutable list from entries()** | External code can corrupt internal state | Return `List.copyOf(entries)` or `Collections.unmodifiableList()` |
| **Resetting sequence numbers after truncation** | Breaks monotonicity, confuses recovery | Keep `nextSequenceNumber` unchanged; new entries continue from last |
| **Not handling empty log in recoverFromSnapshot** | Throws IndexOutOfBounds | Check `entries.isEmpty()` first, return empty list |
| **Using linear scan for findIndex in production** | O(n) per query | Use binary search or HashMap<seqNum, index> |
| **Forgetting write lock in truncateBeforeSnapshot** | Race condition with concurrent appends | Always acquire write lock before mutating `entries` |

---

## 8. Verification Checklist

### Functional

- [ ] Sequence numbers start at 1, increment by 1, no gaps
- [ ] `append(null)` and `append("")` throw IllegalArgumentException
- [ ] `markSnapshot(0)` and `markSnapshot(nonExistent)` throw IllegalArgumentException
- [ ] `recoverFromSnapshot()` with no snapshot returns all entries
- [ ] `recoverFromSnapshot()` with snapshot returns entries from snapshot onward (inclusive)
- [ ] `truncateBeforeSnapshot()` with no snapshot is no-op
- [ ] `truncateBeforeSnapshot()` removes entries before snapshot, keeps snapshot and after
- [ ] After truncation, new appends continue from last sequence number (no reset)

### Thread Safety

- [ ] 20 concurrent appenders → no lost updates, monotonic seq nums
- [ ] Concurrent readers + writers → no exceptions, consistent state
- [ ] Concurrent markSnapshot + truncate → snapshot always <= all remaining entries
- [ ] ReadWriteLock used correctly: read lock for queries, write lock for mutations

### Edge Cases

- [ ] Empty log: `size()=0`, `lastSequenceNumber()=0`, `entries()=[]`, `recoverFromSnapshot()=[]`
- [ ] Snapshot at first entry: `recoverFromSnapshot()` returns all entries
- [ ] Snapshot at last entry: `recoverFromSnapshot()` returns only last entry
- [ ] Multiple `markSnapshot()` calls: snapshot moves forward, never backward
- [ ] `entries()` returns unmodifiable list (throws on add/remove)

---

## 9. Extension Points (Bonus Discussion)

If you finish early, mention:

1. **Disk-backed WAL** — Replace ArrayList with file I/O. Use `FileChannel` + `MappedByteBuffer` for memory-mapped writes. `fsync()` after each append for durability.

2. **Binary search for snapshot index** — Since entries are sorted by seq num, use `Collections.binarySearch()` or custom binary search for O(log n) instead of O(n).

3. **Multiple snapshots (compaction)** — Instead of one snapshot marker, maintain a list of snapshots. Periodically compact: write snapshot to disk, truncate log before it.

4. **Lock-free WAL** — Use `ConcurrentLinkedQueue<LogEntry>` + `AtomicLong` for seq num. Readers iterate queue; writers use `add()`. Trade-off: no truncation (queue doesn't support efficient removal from head).

5. **Recovery protocol** — On startup: load last snapshot from disk, read WAL from disk, replay entries after snapshot. This is how Kafka, PostgreSQL, and Raft consensus work.

---

## 10. Production References

| Resource | Why It Matters |
|----------|---------------|
| [Kafka Log Design](https://kafka.apache.org/documentation/#design_log) | Kafka's commit log is a WAL — append-only, segmented, with snapshots (compaction) |
| [PostgreSQL WAL](https://www.postgresql.org/docs/current/wal-intro.html) | PostgreSQL uses WAL for crash recovery — every change written to log before applied |
| [Raft Consensus](https://raft.github.io/raft.pdf) | Raft's log replication is a distributed WAL — leader appends, followers replicate |
| [Resilience4j](https://resilience4j.readme.io/) | Java library for resilience patterns — understand how production systems handle failure |
| [Martin Kleppmann - Designing Data-Intensive Applications](https://dataintegrity.net/) | Chapter 3: Storage and Retrieval — deep dive into WALs, LSM trees, B-trees |

---

*This guideline follows the standard platform challenge template: presentation → edge cases → chain of thinking → communication → implementation → pro tips → mistakes → verification → extensions → references.*
