# Java 21 Queue/Deque Implementations Guide

## Quick-Reference: Implementation Selection Matrix

| Use Case                                     | Best Choice             | Key Reason                                      | Ordering  | Thread-Safe | Blocking | offer/poll | Nulls | Bounded  |
|----------------------------------------------|-------------------------|-------------------------------------------------|-----------|-------------|----------|------------|-------|----------|
| **Default Deque (non-concurrent)**           | `ArrayDeque`            | Fastest, resizable circular array               | FIFO/LIFO | ❌           | ❌        | O(1)       | ❌     | No       |
| **Default Queue (non-concurrent)**           | `ArrayDeque`            | Faster than `LinkedList` for queue ops          | FIFO      | ❌           | ❌        | O(1)       | ❌     | No       |
| **Stack (LIFO)**                             | `ArrayDeque`            | Faster than `java.util.Stack`                   | LIFO      | ❌           | ❌        | O(1)       | ❌     | No       |
| **Need null elements**                       | `LinkedList`            | Only non-concurrent Queue/Deque allowing nulls  | FIFO/LIFO | ❌           | ❌        | O(1)       | ✅     | No       |
| **Need List + Deque in one**                 | `LinkedList`            | Implements both `List` and `Deque`              | FIFO/LIFO | ❌           | ❌        | O(1)       | ✅     | No       |
| **Priority-ordered (single-thread)**         | `PriorityQueue`         | Binary min-heap, O(log n) insert/extract        | Priority  | ❌           | ❌        | O(log n)   | ❌     | No       |
| **Top-K / heap sort**                        | `PriorityQueue`         | Maintain heap of size K                         | Priority  | ❌           | ❌        | O(log n)   | ❌     | No       |
| **High-throughput concurrent (no blocking)** | `ConcurrentLinkedQueue` | Lock-free CAS, `size()` is O(n) ⚠️              | FIFO      | ✅ (CAS)     | ❌        | O(1)       | ❌     | No       |
| **Producer-consumer (high throughput)**      | `LinkedBlockingQueue`   | Dual locks (put/take separate), less contention | FIFO      | ✅ (2 locks) | ✅        | O(1)       | ❌     | Either   |
| **Bounded producer-consumer (low memory)**   | `ArrayBlockingQueue`    | Fixed array, single lock, must set capacity     | FIFO      | ✅ (1 lock)  | ✅        | O(1)       | ❌     | Required |
| **Concurrent priority task execution**       | `PriorityBlockingQueue` | Thread-safe heap, unbounded                     | Priority  | ✅ (1 lock)  | Partial  | O(log n)   | ❌     | No       |
| **Delayed/scheduled task execution**         | `DelayQueue`            | Elements expire after delay, `take()` blocks    | Delay     | ✅ (lock)    | Partial  | O(log n)   | ❌     | No       |
| **Direct thread handoff (no buffering)**     | `SynchronousQueue`      | Zero-capacity, `put` blocks until `take` ready  | Handoff   | ✅ (CAS)     | ✅        | O(1)       | ❌     | Zero     |
| **Concurrent work-stealing (double-ended)**  | `LinkedBlockingDeque`   | Only concurrent deque blocking at both ends     | FIFO/LIFO | ✅ (1 lock)  | ✅        | O(1)       | ❌     | Either   |
| **BFS / topological sort**                   | `ArrayDeque`            | Kahn's algorithm, zero-in-degree node queue     | FIFO      | ❌           | ❌        | O(1)       | ❌     | No       |
| **Sliding window algorithm**                 | `ArrayDeque`            | Add/remove at both ends efficiently             | FIFO      | ❌           | ❌        | O(1)       | ❌     | No       |

### At-A-Glance Decision Flow

```
Need thread-safety?
  ├─ YES → Need blocking (producer-consumer)?
  │          ├─ YES → Need double-ended (both ends)?
  │          │          └─ YES → LinkedBlockingDeque
  │          │          └─ NO  → Need priority ordering?
  │          │                     ├─ YES → PriorityBlockingQueue
  │          │                     └─ NO  → Need bounded + low memory?
  │          │                                ├─ YES → ArrayBlockingQueue
  │          │                                └─ NO  → LinkedBlockingQueue
  │          ├─ NO  → Need priority ordering?
  │          │          ├─ YES → PriorityBlockingQueue
  │          │          └─ NO  → Direct handoff (no buffering)?
  │          │                     ├─ YES → SynchronousQueue
  │          │                     └─ NO  → ConcurrentLinkedQueue
  │          └─ Delayed execution?
  │             └─ YES → DelayQueue
  └─ NO  → Need priority ordering?
             ├─ YES → PriorityQueue
             └─ NO  → Need both List + Deque (or nulls)?
                        ├─ YES → LinkedList
                        └─ NO  → ArrayDeque (default)
```

---

## Overview

Java provides multiple `Queue` and `Deque` implementations via `java.util` and `java.util.concurrent`. Each is
optimized for different access patterns, ordering guarantees, concurrency models, and blocking behavior.

The `java.util.Queue` interface extends `Collection` and adds `offer`, `poll`, and `peek` methods. The
`java.util.Deque` interface (double-ended queue) extends `Queue` and supports element insertion/removal at both ends.

Since Java 21, several implementations also implement `SequencedCollection` (JEP 431), providing `getFirst`/`getLast`,
`removeFirst`/`removeLast`, and `reversed()` methods.

> **BlockingQueue method taxonomy**: All `BlockingQueue` implementations support four forms of operation that differ
> in how they handle unsatisfied requests: **throws exception**, **returns special value**, **blocks indefinitely**,
> or **blocks with timeout**. See the *Queue Method Behavior Matrix* section for details.

---

## 1. ArrayDeque

```java
Deque<E> dq = new ArrayDeque<>();
```

### Characteristics

| Property          | Value                                                       |
|-------------------|-------------------------------------------------------------|
| **Ordering**      | FIFO (as Queue) or LIFO (as Stack)                          |
| **Null elements** | Not allowed (throws NullPointerException)                   |
| **Thread-safe**   | No                                                          |
| **Performance**   | Faster than LinkedList as queue; faster than Stack as stack |
| **Backed by**     | Resizable circular array                                    |

### Complexity

| Operation     | Average | Worst Case |
|---------------|---------|------------|
| `add/offer`   | O(1)    | O(n)       |
| `poll/remove` | O(1)    | O(1)       |
| `peek`        | O(1)    | O(1)       |
| `contains`    | O(n)    | O(n)       |
| Iteration     | O(n)    | O(n)       |

> **Resizing note**: Worst case O(n) on `add` occurs only when the internal array must grow (capacity doubled).
> Amortized cost is O(1). No synchronization overhead — fastest non-concurrent deque.

### When to Use

- Use as your **default Deque implementation** — faster than `LinkedList` for both queue and stack patterns.
- Ideal for **BFS traversal** and topological sort — see `DependencyResolverImpl` which uses `ArrayDeque<String>`
  for Kahn's algorithm in the build-order resolver.
- Great for **DFS/stack** usage — `push`/`pop` outperform `java.util.Stack` (which is synchronized).
- Suitable for **sliding window** algorithms where elements enter/exit at both ends.
- **Never for multi-threaded access** — no synchronization.

### Magic Methods (Java 21)

```java
// Queue interface (FIFO)
dq.offer(e);          // Add to tail; returns false if capacity exceeded (never for unbounded)
dq.poll();            // Retrieve and remove head; null if empty
dq.peek();            // Retrieve head without removal; null if empty

// Deque interface (double-ended) — ArrayDeque supports all of these
dq.addFirst(e);       // Insert at head
dq.addLast(e);        // Insert at tail (same as offer/add for queue usage)
dq.removeFirst();     // Remove and return head; throws NoSuchElementException if empty
dq.removeLast();      // Remove and return tail; throws NoSuchElementException if empty
dq.getFirst();        // Return head without removal; throws if empty
dq.getLast();         // Return tail without removal; throws if empty
dq.peekFirst();       // Return head; null if empty
dq.peekLast();        // Return tail; null if empty

// Stack usage (LIFO)
dq.push(e);           // Equivalent to addFirst
dq.pop();             // Equivalent to removeFirst

// SequencedCollection (Java 21, JEP 431) — ArrayDeque implements this
E first = dq.getFirst();   // Same as getFirst above
E last = dq.getLast();     // Same as getLast above
E removedFirst = dq.removeFirst();   // Same as removeFirst above
E removedLast = dq.removeLast();     // Same as removeLast above
SequencedCollection<E> reversed = dq.reversed();  // Reverse-order view (lightweight wrapper)

// Collection methods
dq.contains(e);       // O(n) linear scan
dq.remove(e);         // Remove first occurrence; O(n)
dq.size();            // O(1)
dq.isEmpty();         // O(1)
```

**Practical topological sort pattern** (from `platform/challenge03/DependencyResolverImpl.java`):

```java
// BEFORE: verbose with index tracking
List<String> order = new ArrayList<>();
int[] inDegree = computeInDegree(graph);
int zeroIdx = findFirstZero(inDegree);  // manual scan
while (zeroIdx != -1) {
    order.add(zeroIdx);
    // ... update inDegree, rescan for next zero
    zeroIdx = findFirstZero(inDegree);  // O(n) per iteration
}

// AFTER: ArrayDeque-based Kahn's algorithm
Deque<String> queue = new ArrayDeque<>();
for (var entry : inDegree.entrySet()) {
    if (entry.getValue() == 0) {
        queue.add(entry.getKey());       // O(1) enqueue all zero-in-degree nodes
    }
}
List<String> order = new ArrayList<>(inDegree.size());
while (!queue.isEmpty()) {
    String node = queue.poll();           // O(1) dequeue
    order.add(node);
    for (String neighbor : graph.getOrDefault(node, List.of())) {
        if (inDegree.merge(neighbor, -1, Integer::sum) == 0) {
            queue.add(neighbor);          // O(1) enqueue newly zero nodes
        }
    }
}
// If order.size() != graph.size() → circular dependency detected
```

---

## 2. LinkedList

```java
Queue<E> q = new LinkedList<>();
// or
Deque<E> dq = new LinkedList<>();
```

### Characteristics

| Property          | Value                                              |
|-------------------|----------------------------------------------------|
| **Ordering**      | FIFO (as Queue) or LIFO (as Stack)                 |
| **Null elements** | Allowed                                            |
| **Thread-safe**   | No                                                 |
| **Performance**   | Higher allocation overhead than ArrayDeque; slower |
| **Backed by**     | Doubly-linked list nodes (one object per element)  |

### Complexity

| Operation     | Average | Worst Case |
|---------------|---------|------------|
| `add/offer`   | O(1)    | O(1)       |
| `poll/remove` | O(1)    | O(1)       |
| `peek`        | O(1)    | O(1)       |
| `contains`    | O(n)    | O(n)       |
| Iteration     | O(n)    | O(n)       |

> **Memory overhead**: Each element creates a `Node<E>` object with 3 fields (item, prev, next) — ~24-32 bytes per
> element plus the element itself. ArrayDeque uses a single array.

### When to Use

- When **null elements are required** — LinkedList allows nulls, ArrayDeque does not.
- When you need **both List and Deque** APIs in a single instance (LinkedList implements both).
- Seen in coding challenges: `SortArrayByParity.java` and `MoveZeroes.java` use
  `Queue<Integer> queue = new LinkedList<>()` for tracking swap positions.
- For **frequent head/tail removals with mid-list access** — though mid-list is O(n) for both implementations.

> **Prefer ArrayDeque** unless you specifically need null support or the List interface. The JavaDoc for
> `ArrayDeque` states: *"This class is likely to be faster than Stack when used as a stack, and faster than
> LinkedList when used as a queue."*

### Magic Methods (Java 21)

```java
// All Deque methods supported (same as ArrayDeque)
dq.offer(e);
dq.poll();
dq.peek();
dq.addFirst(e);
dq.addLast(e);
dq.removeFirst();
dq.removeLast();
dq.getFirst();
dq.getLast();

// SequencedCollection (Java 21) — LinkedList implements this
SequencedCollection<E> reversed = dq.reversed();

// List-specific (unique to LinkedList among Deque impls)
LinkedList<E> list = new LinkedList<>();
list.add(index, e);       // Insert at arbitrary position
list.get(index);          // Get by index — O(n) traversal
list.set(index, e);       // Replace at index — O(n)
list.listIterator(idx);   // Bi-directional iterator from position
```

**Practical BFS with position tracking** (pattern from `MoveZeroes.java`):

```java
// LinkedList as Queue in LeetCode-style in-place array manipulation
public void moveZeroes(int[] nums) {
    Queue<Integer> zeroPositions = new LinkedList<>();
    for (int i = 0; i < nums.length; i++) {
        if (nums[i] == 0) {
            zeroPositions.add(i);          // Track zero positions
        } else if (!zeroPositions.isEmpty()) {
            int zeroIdx = zeroPositions.poll();  // Swap with non-zero
            int tmp = nums[i];
            nums[i] = nums[zeroIdx];
            nums[zeroIdx] = tmp;
            zeroPositions.add(i);          // Current position now holds zero
        }
    }
}
```

---

## 3. PriorityQueue

```java
Queue<E> pq = new PriorityQueue<>();
// With custom ordering
Queue<Task> pq = new PriorityQueue<>(Comparator.comparingInt(Task::priority));
```

### Characteristics

| Property          | Value                                                   |
|-------------------|---------------------------------------------------------|
| **Ordering**      | Priority order (natural or custom Comparator), NOT FIFO |
| **Null elements** | Not allowed                                             |
| **Thread-safe**   | No                                                      |
| **Performance**   | O(log n) insert/extract; O(1) peek                      |
| **Backed by**     | Binary min-heap (priority queue)                        |

### Complexity

| Operation     | Average  | Worst Case |
|---------------|----------|------------|
| `offer(e)`    | O(log n) | O(log n)   |
| `poll()`      | O(log n) | O(log n)   |
| `peek()`      | O(1)     | O(1)       |
| `contains(e)` | O(n)     | O(n)       |
| `remove(e)`   | O(n)     | O(n)       |
| Iteration     | O(n)     | O(n)       |

> **Not a Deque**: PriorityQueue does NOT implement `Deque`. It does not support `addFirst`/`addLast` operations.
> Elements are ordered by priority — iteration order is NOT sorted order. No stability guarantee for equal-priority
> elements. Implements `SequencedCollection` since Java 21 (but NOT `Deque`).

### When to Use

- **Dijkstra's shortest path** — extract minimum distance node efficiently.
- **Heap sort** / **Top-K** problems — maintain a heap of size K.
- **Event-driven simulation** — process events in timestamp order.
- **Merge K sorted lists** — head-of-list comparison via min-heap.
- **Task scheduling by priority** — highest-priority task always at head.
- For **delayed execution** patterns, consider `DelayQueue` instead (built on PriorityQueue with timing).

### Magic Methods (Java 21)

```java
// Basic queue operations (priority-ordered, not FIFO)
pq.offer(task);           // O(log n) insert
Task next = pq.poll();    // O(log n) extract highest-priority (min)
Task head = pq.peek();    // O(1) view highest-priority without removal

// Bulk operations
pq.addAll(collection);    // O(k log n) or O(n) via heapify
pq.remove(task);          // O(n) — linear scan then re-heapify

// Comparator construction
PriorityQueue<Integer> minHeap = new PriorityQueue<>();                    // natural order (min first)
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder()); // max first
PriorityQueue<Task> byPrio = new PriorityQueue<>(Comparator.comparingInt(Task::priority));
PriorityQueue<Task> byTimeDesc = new PriorityQueue<>(
    Comparator.comparingLong(Task::timestamp).reversed()
);

// SequencedCollection (Java 21, JEP 431) — PriorityQueue implements this, NOT Deque
E first = pq.getFirst();     // Minimum element (head of heap)
E last = pq.getLast();       // Max element (O(n) scan)
E removedFirst = pq.removeFirst(); // Remove min — O(log n) via poll
E removedLast = pq.removeLast();   // Remove max — O(n) removal
SequencedCollection<E> reversed = pq.reversed();

// Java 21: PriorityQueue does NOT support addFirst/addLast
// pq.addFirst(e);  // ❌ Compilation error — PriorityQueue is NOT a Deque
```

**Practical Top-K pattern** (K largest elements):

```java
// Find K largest elements in a stream
public List<Integer> topK(int[] nums, int k) {
    // Min-heap of size K — smallest of the K largest is always at head
    PriorityQueue<Integer> minHeap = new PriorityQueue<>(k);

    for (int n : nums) {
        if (minHeap.size() < k) {
            minHeap.offer(n);                // Fill up to K
        } else if (n > minHeap.peek()) {
            minHeap.poll();                   // Remove current minimum
            minHeap.offer(n);                 // Insert larger element
        }
    }
    // Drain heap — results are NOT sorted, just top K
    List<Integer> result = new ArrayList<>(minHeap);
    result.sort(Comparator.reverseOrder());   // Sort descending if needed
    return result;
}
```

---

## 4. ConcurrentLinkedQueue

```java
Queue<E> q = new ConcurrentLinkedQueue<>();
```

### Characteristics

| Property          | Value                                                   |
|-------------------|---------------------------------------------------------|
| **Ordering**      | FIFO                                                    |
| **Null elements** | Not allowed                                             |
| **Thread-safe**   | Yes (lock-free, CAS-based)                              |
| **Performance**   | High throughput under contention; O(1) offer/poll       |
| **Backed by**     | Linked list with CAS (Compare-And-Swap) synchronization |

### Complexity

| Operation     | Average | Worst Case |
|---------------|---------|------------|
| `offer(e)`    | O(1)    | O(1)       |
| `poll()`      | O(1)    | O(1)       |
| `peek()`      | O(1)    | O(1)       |
| `contains(e)` | O(n)    | O(n)       |
| `size()`      | O(n)    | O(n)       |
| Iteration     | O(n)    | O(n)       |

> **Critical**: `size()` is O(n), not O(1). The queue must traverse all nodes to count. Avoid calling `size()` in
> tight loops — prefer `isEmpty()` which is O(1). Unbounded — no capacity limits.

### When to Use

- **High-throughput concurrent producer-consumer** where blocking is NOT desired.
- **Event logging / metrics** — multiple threads enqueue, single thread drains.
- **Work-stealing patterns** — each thread has its own queue, shares via this queue when idle.
- When you need **non-blocking** behavior: `offer`/`poll` return immediately, never block.
- For **bounded** concurrent queues or blocking behavior, use `ArrayBlockingQueue` or
  `LinkedBlockingQueue` instead.

### Magic Methods (Java 21)

```java
// Non-blocking concurrent operations
q.offer(e);             // Always succeeds (unbounded); O(1) CAS insert
E item = q.poll();      // Remove and return head; null if empty; O(1) CAS
E head = q.peek();      // View head without removal; null if empty
boolean empty = q.isEmpty();  // O(1) — preferred over size()

// ⚠ AVOID: O(n) traversal
int n = q.size();       // O(n) — counts all nodes
boolean has = q.contains(e);  // O(n) — full scan

// Bulk operations (not atomic — concurrent modifications may occur during)
q.addAll(collection);   // Sequential offers
q.iterator();           // Weakly consistent — reflects some concurrent updates,
                        // never throws ConcurrentModificationException

// SequencedCollection (Java 21) — ConcurrentLinkedQueue implements this
E first = q.getFirst();   // Head element; throws NoSuchElementException if empty
SequencedCollection<E> reversed = q.reversed();
```

**Practical non-blocking metrics pipeline**:

```java
// Producer-consumer without blocking — metrics collection
ConcurrentLinkedQueue<MetricEvent> metrics = new ConcurrentLinkedQueue<>();

// Multiple producer threads (web handlers, etc.) — never block
void recordMetric(MetricEvent event) {
    metrics.offer(event);   // O(1) CAS, always succeeds
}

// Single consumer thread — drain and flush
void flushMetrics() {
    MetricEvent event;
    while ((event = metrics.poll()) != null) {  // Poll until empty
        storage.write(event);
    }
    // Or batch drain (more efficient)
    List<MetricEvent> batch = new ArrayList<>();
    while ((event = metrics.poll()) != null && batch.size() < BATCH_SIZE) {
        batch.add(event);
    }
    storage.writeBatch(batch);
}
```

---

## 5. LinkedBlockingQueue

```java
BlockingQueue<E> lbq = new LinkedBlockingQueue<>();           // Unbounded (Integer.MAX_VALUE)
BlockingQueue<E> lbq = new LinkedBlockingQueue<>(capacity);   // Bounded
```

### Characteristics

| Property          | Value                                                       |
|-------------------|-------------------------------------------------------------|
| **Ordering**      | FIFO                                                        |
| **Null elements** | Not allowed                                                 |
| **Thread-safe**   | Yes (two-lock: separate putLock and takeLock)               |
| **Performance**   | High throughput with separate locks for producers/consumers |
| **Backed by**     | Linked list nodes with dual ReentrantLocks                  |

### Complexity

| Operation   | Average | Worst Case |
|-------------|---------|------------|
| `offer/put` | O(1)    | O(1)       |
| `poll/take` | O(1)    | O(1)       |
| `peek`      | O(1)    | O(1)       |
| `contains`  | O(n)    | O(n)       |
| Iteration   | O(n)    | O(n)       |

> **Dual-lock design**: `putLock` guards producers, `takeLock` guards consumers. Producers and consumers can
> operate simultaneously without contention (unlike `ArrayBlockingQueue`'s single lock). Default capacity is
> `Integer.MAX_VALUE` (effectively unbounded) unless bounded constructor is used.

### When to Use

- **Producer-consumer pipelines** with separate producer and consumer threads — dual locks reduce contention.
- **Bounded buffering** — prevent memory exhaustion by setting a capacity:
  `new LinkedBlockingQueue<>(1000)`.
- **Log aggregation** — multiple handlers produce, multiple workers consume.
- **Task distribution** — submit tasks from multiple threads, pool of workers consumes.
- When you need **blocking semantics**: `put()` waits for space, `take()` waits for elements.
- For **fixed-capacity with lower memory footprint**, prefer `ArrayBlockingQueue`.

### Magic Methods (Java 21)

```java
// 4 forms of each operation (BlockingQueue contract)

// INSERT — add element
lbq.add(e);         // Throws IllegalStateException if full (bounded)
lbq.offer(e);       // Returns false if full; O(1)
lbq.put(e);         // BLOCKS until space available; may interrupt
lbq.offer(e, 5, TimeUnit.SECONDS);  // Blocks up to timeout; returns false on expiry

// REMOVE — retrieve and remove head
E item = lbq.remove();      // Throws NoSuchElementException if empty
E item = lbq.poll();        // Returns null if empty; O(1)
E item = lbq.take();        // BLOCKS until element available; may interrupt
E item = lbq.poll(5, TimeUnit.SECONDS);  // Blocks up to timeout; null on expiry

// EXAMINE — view head without removal
E head = lbq.element();     // Throws NoSuchElementException if empty
E head = lbq.peek();        // Returns null if empty; O(1)

// Other concurrent operations
int remaining = lbq.remainingCapacity();  // O(1)
boolean drained = lbq.drainTo(collection, maxElements);  // Bulk remove — efficient
int removed = lbq.removeIf(predicate);    // Java 8+ bulk conditional remove
```

**Practical producer-consumer pipeline**:

```java
// Bounded LinkedBlockingQueue for log processing
BlockingQueue<LogEntry> logs = new LinkedBlockingQueue<>(10_000);

// Producer — web request handler
void handleRequest(Request req) {
    LogEntry entry = new LogEntry(req);
    try {
        logs.put(entry);  // Blocks if queue is full — backpressure applied
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}

// Consumer — log writer thread pool
void startLogWriter() {
    executor.submit(() -> {
        while (true) {
            try {
                LogEntry entry = logs.take();  // Blocks until log available
                writer.write(entry);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    });
}
```

---

## 6. ArrayBlockingQueue

```java
BlockingQueue<E> abq = new ArrayBlockingQueue<>(capacity);  // Must specify capacity
```

### Characteristics

| Property          | Value                                                           |
|-------------------|-----------------------------------------------------------------|
| **Ordering**      | FIFO                                                            |
| **Null elements** | Not allowed                                                     |
| **Thread-safe**   | Yes (single ReentrantLock with two Conditions)                  |
| **Performance**   | Lower memory overhead; single lock may contend under heavy load |
| **Backed by**     | Fixed-size circular array with one lock                         |

### Complexity

| Operation   | Average | Worst Case |
|-------------|---------|------------|
| `offer/put` | O(1)    | O(1)       |
| `poll/take` | O(1)    | O(1)       |
| `peek`      | O(1)    | O(1)       |
| `contains`  | O(n)    | O(n)       |
| Iteration   | O(n)    | O(n)       |

> **Mandatory capacity**: Constructor requires capacity — cannot be unbounded. Single lock protects both producers
> and consumers, which creates a contention point under heavy concurrent access. Lower per-element memory overhead
> than `LinkedBlockingQueue` (no node objects).

### When to Use

- **Bounded producer-consumer** with known capacity — fixed-size, no resizing overhead.
- **Memory-constrained environments** — array backing uses less memory than linked nodes.
- **Resource pool** — see `ResourcePool.java` which documents the blocking acquire pattern that
  `ArrayBlockingQueue` natively provides (pool of `N` resources, blocks when exhausted).
- When **capacity is known at construction** and should not change.
- For **lower contention** or unbounded scenarios, prefer `LinkedBlockingQueue`.

### Magic Methods (Java 21)

```java
// Same 4-form BlockingQueue API as LinkedBlockingQueue

// INSERT
abq.add(e);         // Throws if full
abq.offer(e);       // Returns false if full
abq.put(e);         // Blocks until space
abq.offer(e, timeout, unit);  // Block with timeout

// REMOVE
E item = abq.remove();      // Throws if empty
E item = abq.poll();        // null if empty
E item = abq.take();        // Blocks until available
E item = abq.poll(timeout, unit);  // Block with timeout

// EXAMINE
E head = abq.element();     // Throws if empty
E head = abq.peek();        // null if empty

// Additional methods
int remaining = abq.remainingCapacity();  // O(1)
int drained = abq.drainTo(targetCollection);  // Bulk move to another collection
int drained = abq.drainTo(targetCollection, maxElements);  // Limited bulk

// Fairness configuration
// fair=true: threads granted access in FIFO order (slightly slower, more fair)
BlockingQueue<E> fair = new ArrayBlockingQueue<>(capacity, true);
```

**Practical resource pool pattern** (matching `challenge08/ResourcePool.java` interface):

```java
// Resource pool using ArrayBlockingQueue (simplifies ResourcePoolImpl)
public class SimpleResourcePool<T> {
    private final BlockingQueue<T> available;
    private final Function<Integer, T> factory;
    private final int maxSize;

    public SimpleResourcePool(int maxSize, Function<Integer, T> factory, long timeoutMs) {
        this.maxSize = maxSize;
        this.factory = factory;
        this.available = new ArrayBlockingQueue<>(maxSize);
        // Pre-populate pool
        for (int i = 0; i < maxSize; i++) {
            available.add(factory.apply(i));
        }
    }

    // Acquire with timeout — matches ResourcePool.acquire() contract
    public T acquire(long timeoutMs) throws PoolExhaustedException {
        try {
            T resource = available.poll(timeoutMs, TimeUnit.MILLISECONDS);
            if (resource == null) {
                throw new PoolExhaustedException("No resource available within " + timeoutMs + "ms");
            }
            return resource;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PoolExhaustedException("Interrupted while waiting");
        }
    }

    // Return resource — if invalid, create replacement
    public void release(T resource) {
        boolean offered = available.offer(resource);
        if (!offered || !resource.isHealthy()) {
            resource.close();  // Discard invalid/pool-full resource
        }
    }
}
```

---

## 7. PriorityBlockingQueue

```java
BlockingQueue<E> pbq = new PriorityBlockingQueue<>();
// With custom ordering
BlockingQueue<Task> pbq = new PriorityBlockingQueue<>(
    11,                                    // initial capacity
    Comparator.comparingInt(Task::urgency) // ordering
);
```

### Characteristics

| Property          | Value                                                  |
|-------------------|--------------------------------------------------------|
| **Ordering**      | Priority order (natural or Comparator)                 |
| **Null elements** | Not allowed                                            |
| **Thread-safe**   | Yes (single ReentrantLock — all operations serialized) |
| **Performance**   | O(log n) insert/extract; unbounded (grows as needed)   |
| **Backed by**     | Array-based binary heap with lock                      |

### Complexity

| Operation   | Average  | Worst Case |
|-------------|----------|------------|
| `offer/put` | O(log n) | O(log n)   |
| `poll/take` | O(log n) | O(log n)   |
| `peek`      | O(1)     | O(1)       |
| `contains`  | O(n)     | O(n)       |
| Iteration   | O(n)     | O(n)       |

> **Unbounded**: Capacity grows automatically (similar to `ArrayList`). `put()` never blocks since queue is
> unbounded. Only `take()` blocks (when empty). Single lock means producers and consumers contend.

### When to Use

- **Concurrent priority-based task execution** — highest-priority task is always consumed first.
- **Concurrent event processing by severity** — ERROR before WARN before INFO.
- **Multi-threaded Dijkstra** — shared priority frontier across threads.
- **Work items with SLA/urgency levels** — SLA-critical items processed first regardless of arrival order.
- When bounded capacity is needed, use a wrapper or consider alternative patterns.

### Magic Methods (Java 21)

```java
// BlockingQueue API with priority ordering

// INSERT — never blocks (unbounded)
pbq.offer(task);      // O(log n) — always succeeds
pbq.put(task);        // Same as offer — never blocks for PriorityBlockingQueue
pbq.add(task);        // Same — throws only on null

// REMOVE — blocks only when empty
Task next = pbq.poll();       // O(log n) — null if empty
Task next = pbq.take();       // O(log n) — BLOCKS until task available
Task next = pbq.poll(5, SECONDS);  // Block with timeout

// EXAMINE
Task head = pbq.peek();       // O(1) — highest priority, no removal

// Bulk draining (non-priority order — heap order)
List<Task> batch = new ArrayList<>();
int count = pbq.drainTo(batch, BATCH_SIZE);  // Efficient bulk removal

// Remove specific element — O(n) scan + O(log n) reheapify
boolean removed = pbq.remove(specificTask);

// Replace head atomically — not provided; use drainTo/offer if needed
```

**Practical concurrent severity-based processing**:

```java
// Process incidents by severity: CRITICAL > ERROR > WARN > INFO
enum Severity { CRITICAL(0), ERROR(1), WARN(2), INFO(3) }
record Incident(Severity severity, String message, Instant timestamp)
    implements Comparable<Incident> {
    @Override
    public int compareTo(Incident other) {
        int c = Integer.compare(this.severity.ordinal(), other.severity.ordinal());
        if (c != 0) return c;
        return this.timestamp.compareTo(other.timestamp);  // FIFO within same severity
    }
}

BlockingQueue<Incident> incidents = new PriorityBlockingQueue<>();

// Producers — multiple alert sources report concurrently
void reportAlert(Incident incident) { incidents.put(incident); }

// Consumer — incident responder process by priority
void startResponder() {
    executor.submit(() -> {
        while (!shutdown) {
            Incident next = incidents.take();  // Always highest severity first
            handleIncident(next);              // CRITICAL processed before INFO
        }
    });
}
```

---

## 8. DelayQueue

```java
BlockingQueue<Delayed> dq = new DelayQueue<>();
```

### Characteristics

| Property          | Value                                                           |
|-------------------|-----------------------------------------------------------------|
| **Ordering**      | By delay expiration time (earliest expiring first)              |
| **Null elements** | Not allowed                                                     |
| **Thread-safe**   | Yes (uses `PriorityQueue` internally with locking)              |
| **Performance**   | O(log n) insert; O(log n) extract; blocking until delay expires |
| **Backed by**     | PriorityQueue of `Delayed` elements with a lock                 |

### Complexity

| Operation  | Average  | Worst Case |
|------------|----------|------------|
| `offer(e)` | O(log n) | O(log n)   |
| `poll()`   | O(log n) | O(log n)   |
| `take()`   | O(log n) | O(log n)   |
| `peek()`   | O(1)     | O(1)       |
| `contains` | O(n)     | O(n)       |
| Iteration  | O(n)     | O(n)       |

> **Elements must implement `java.util.concurrent.Delayed`**: `getDelay(TimeUnit)` returns remaining wait time.
> An element can only be removed via `take()` or `poll()` when its delay has expired. Unbounded — no capacity
> limit. `take()` blocks until the head element's delay expires. Use case: see `challenge10/TaskScheduler.java`
> which mentions `DelayQueue` for delayed execution.

### When to Use

- **Delayed task execution** — schedule work to run after a specific time (e.g., retry after backoff).
- **Scheduled cleanup** — expire stale entries after a TTL.
- **Rate limiting with token replenish** — tokens become available at specific times.
- **Session timeout management** — sessions expire after inactivity period.
- **Task scheduling** — directly relevant to `platform/challenge10/TaskScheduler.java`, which
  builds a delayed execution scheduler. For production, consider `ScheduledExecutorService`.

### Magic Methods (Java 21)

```java
// Delayed element interface — MUST be implemented by all queue elements
class DelayedTask implements Delayed {
    private final Instant executionTime;
    private final Runnable task;

    @Override
    public long getDelay(TimeUnit unit) {
        long remaining = Duration.between(Instant.now(), executionTime).toMillis();
        return unit.convert(remaining, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        return Long.compare(getDelay(TimeUnit.MILLISECONDS), other.getDelay(TimeUnit.MILLISECONDS));
    }
}

// DelayQueue with Delayed elements
DelayQueue<DelayedTask> delayQueue = new DelayQueue<>();

// INSERT — always succeeds (unbounded)
delayQueue.offer(task);       // O(log n) insert

// REMOVE — blocks until element's delay expires
DelayedTask next = delayQueue.poll();    // null if no element has expired yet
DelayedTask next = delayQueue.take();    // BLOCKS until earliest-delay element expires
DelayedTask next = delayQueue.poll(10, SECONDS);  // Block up to timeout

// EXAMINE
DelayedTask head = delayQueue.peek();    // Head element (may not yet be expired)

// Bulk drain (only expired elements)
List<DelayedTask> due = new ArrayList<>();
delayQueue.drainTo(due);  // Removes all currently-expired elements
```

**Practical delayed task execution** (relevant to `challenge10/TaskScheduler.java`):

```java
// DelayQueue-based task scheduler (simplified TaskScheduler implementation)
public class DelayQueueScheduler implements TaskScheduler {
    private final DelayQueue<ScheduledTask> queue = new DelayQueue<>();
    private final ExecutorService executor;
    private volatile boolean running = true;

    public DelayQueueScheduler(int poolSize) {
        this.executor = Executors.newFixedThreadPool(poolSize);
        // Dispatcher thread
        new Thread(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    ScheduledTask task = queue.take();  // Blocks until due
                    executor.submit(task::run);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "delay-dispatcher").start();
    }

    @Override
    public void schedule(Runnable task, long delay, TimeUnit unit) {
        queue.offer(new ScheduledTask(task, delay, unit));
    }

    private record ScheduledTask(Runnable task, Instant deadline) implements Delayed {
        ScheduledTask(Runnable task, long delay, TimeUnit unit) {
            this(task, Instant.now().plusMillis(unit.toMillis(delay)));
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(Duration.between(Instant.now(), deadline).toMillis(), MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            return Long.compare(getDelay(MILLISECONDS), other.getDelay(MILLISECONDS));
        }

        void run() { task.run(); }
    }
}
```

---

## 9. SynchronousQueue

```java
BlockingQueue<E> sq = new SynchronousQueue<>();
// With fairness
BlockingQueue<E> fair = new SynchronousQueue<>(true);
```

### Characteristics

| Property          | Value                                                          |
|-------------------|----------------------------------------------------------------|
| **Ordering**      | Direct handoff (FIFO if fair=true, otherwise unspecified)      |
| **Null elements** | Not allowed                                                    |
| **Thread-safe**   | Yes (lock-free CAS + optional ReentrantLock for fairness mode) |
| **Performance**   | Zero-capacity — every `put` blocks until a `take` is ready     |
| **Backed by**     | No storage — direct thread-to-thread handoff                   |

### Complexity

| Operation   | Average | Worst Case                               |
|-------------|---------|------------------------------------------|
| `offer/put` | O(1)    | O(1)                                     |
| `poll/take` | O(1)    | O(1)                                     |
| `peek()`    | N/A     | Always returns null (no stored elements) |
| `contains`  | O(1)    | Always false (capacity is zero)          |
| Iteration   | N/A     | Always empty                             |

> **Zero-capacity queue**: Elements are NOT stored. Each `put` blocks until another thread calls `take`, and vice
> versa. Think of it as a rendezvous point between two threads. `size()` is always 0. `isEmpty()` is always true.
> This is the **default queue** for `Executors.newCachedThreadPool()`.

### When to Use

- **Direct handoff** between producer and consumer — no buffering, no intermediate storage.
- **Pipeline stages** where each stage waits for the previous to be ready.
- **Cached thread pool** work distribution — `Executors.newCachedThreadPool()` uses SynchronousQueue.
- **Load balancing** — tasks go directly to an available worker, not queued.
- **When producer should NOT proceed** until consumer is ready to process (flow control).

### Magic Methods (Java 21)

```java
// SynchronousQueue — no capacity, direct handoff

// INSERT — blocks until another thread takes it
sq.offer(e);        // Returns true only if another thread is waiting to take
                    // Returns false immediately if no consumer is waiting (non-blocking mode)
sq.add(e);          // Same semantics as offer but throws on null
sq.put(e);          // BLOCKS until a consumer thread calls take()
sq.offer(e, timeout, unit);  // Blocks until consumer arrives or timeout expires

// REMOVE — blocks until another thread puts an item
E item = sq.poll();        // Returns null immediately if no producer waiting
E item = sq.take();        // BLOCKS until a producer thread calls put()
E item = sq.poll(timeout, unit);  // Waits for producer or timeout

// EXAMINE — always empty
E head = sq.peek();        // Always returns null (no elements stored)
boolean empty = sq.isEmpty();  // Always true
int size = sq.size();      // Always 0

// Fairness
// fair=true: FIFO ordering of waiting threads (slower but fair)
// fair=false: unspecified ordering (faster, default)
SynchronousQueue<E> fair = new SynchronousQueue<>(true);
SynchronousQueue<E> unfair = new SynchronousQueue<>(false);
// or just new SynchronousQueue<>() — defaults to unfair

// Does NOT implement SequencedCollection — no elements to sequence
```

**Practical direct handoff pattern**:

```java
// SynchronousQueue for work handoff between two threads
SynchronousQueue<WorkItem> handoff = new SynchronousQueue<>();

// Producer thread — waits until consumer is ready
void producer() {
    WorkItem item = createWork();
    try {
        handoff.put(item);  // BLOCKS here until consumer calls take()
        // Producer knows consumer has received the item
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}

// Consumer thread — waits until producer has work
void consumer() {
    try {
        WorkItem item = handoff.take();  // BLOCKS here until producer calls put()
        process(item);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}

// Real-world: newCachedThreadPool uses this pattern internally
// Thread pool hands work directly to available threads without queuing
ExecutorService cached = Executors.newCachedThreadPool();
// Internally: new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60s, new SynchronousQueue<>());
```

---

## 10. LinkedBlockingDeque

```java
BlockingDeque<E> lbd = new LinkedBlockingDeque<>();
// Bounded
BlockingDeque<E> lbd = new LinkedBlockingDeque<>(capacity);
```

### Characteristics

| Property          | Value                                                                 |
|-------------------|-----------------------------------------------------------------------|
| **Ordering**      | FIFO (or LIFO when used as deque)                                     |
| **Null elements** | Not allowed                                                           |
| **Thread-safe**   | Yes (single ReentrantLock with two Conditions — notFirst and notLast) |
| **Performance**   | O(1) for both ends; lock contention under high concurrency            |
| **Backed by**     | Doubly-linked list nodes with one lock                                |

### Complexity

| Operation          | Average | Worst Case |
|--------------------|---------|------------|
| `add/offer/put`    | O(1)    | O(1)       |
| `poll/remove/take` | O(1)    | O(1)       |
| `peek`             | O(1)    | O(1)       |
| `contains`         | O(n)    | O(n)       |
| Iteration          | O(n)    | O(n)       |

> **Doubly-ended blocking queue**: The ONLY concurrent implementation supporting insertion/removal at BOTH
> ends. Single lock means less throughput than `LinkedBlockingQueue` for single-end operations, but provides
> deque semantics under concurrency. Optional capacity bound.

### When to Use

- **Concurrent work-stealing** — workers pop from their end, steal from others' opposite end.
- **Undo/redo** with concurrent access — push/pop from both ends thread-safely.
- **Dual-ended producer-consumer** — producers add at tail, consumers can steal from head or tail.
- **Bounded concurrent deque** — need both blocking and double-ended operations.
- For single-end concurrent blocking, prefer `LinkedBlockingQueue` (two locks, less contention).

### Magic Methods (Java 21)

```java
// All Deque methods in blocking form — both ends supported

// TAIL operations (standard queue end)
lbd.offerLast(e);        // Add at tail; false if full (bounded)
lbd.offerLast(e, t, u);  // Block with timeout
lbd.putLast(e);          // Block until space at tail
lbd.pollLast();          // Remove from tail; null if empty
lbd.pollLast(t, u);      // Block until item or timeout
lbd.takeLast();          // Block until item available at tail
lbd.peekLast();          // View tail without removal; null if empty

// HEAD operations (other deque end)
lbd.offerFirst(e);       // Add at head; false if full
lbd.offerFirst(e, t, u); // Block with timeout
lbd.putFirst(e);         // Block until space at head
lbd.pollFirst();         // Remove from head; null if empty
lbd.pollFirst(t, u);     // Block until item or timeout
lbd.takeFirst();         // Block until item available at head
lbd.peekFirst();         // View head without removal; null if empty

// Standard Deque aliases
lbd.add(e);              // Same as addLast — throws if full
lbd.remove();            // Same as removeFirst — throws if empty
lbd.element();           // Same as getFirst — throws if empty

// Bulk operations
int drained = lbd.drainTo(targetCollection);     // Drain from head
int drained = lbd.drainTo(targetCollection, max); // Limited drain
```

**Practical work-stealing deque pattern**:

```java
// Work-stealing pool using LinkedBlockingDeque
// Each worker has its own deque; steals from others when idle
class WorkStealingDeque<T> {
    private final BlockingDeque<T> deque = new LinkedBlockingDeque<>();

    // Worker processes its own work from its end (LIFO — good for cache)
    public T pollWork() {
        return deque.pollLast();  // Pop from own stack end — recent items
    }

    // Other workers steal from the opposite end (FIFO for fairness)
    public T stealWork() {
        return deque.pollFirst();  // Steal from head — oldest items
    }

    // Submit work
    public boolean submitWork(T task, int timeoutMs) throws InterruptedException {
        return deque.offerFirst(task, timeoutMs, TimeUnit.MILLISECONDS);
    }

    public int workCount() {
        return deque.size();  // O(n) traversal
    }
}
```

---

## Decision Matrix

| Requirement                                     | Choose                    |
|-------------------------------------------------|---------------------------|
| Default non-concurrent Deque                    | **ArrayDeque**            |
| Non-concurrent List + Deque (needs nulls)       | **LinkedList**            |
| Priority-ordered single-threaded                | **PriorityQueue**         |
| Non-blocking concurrent queue (high throughput) | **ConcurrentLinkedQueue** |
| Concurrent FIFO, producer-consumer              | **LinkedBlockingQueue**   |
| Bounded concurrent FIFO, lower memory           | **ArrayBlockingQueue**    |
| Concurrent priority-ordered processing          | **PriorityBlockingQueue** |
| Delayed execution (time-based)                  | **DelayQueue**            |
| Zero-capacity handoff between threads           | **SynchronousQueue**      |
| Concurrent double-ended blocking queue          | **LinkedBlockingDeque**   |

---

## Performance Summary

| Implementation        | offer/poll | Thread-Safe    | Blocking | Ordering  | Nulls | Bounded  |
|-----------------------|------------|----------------|----------|-----------|-------|----------|
| ArrayDeque            | O(1) avg   | No             | No       | FIFO/LIFO | No    | No       |
| LinkedList            | O(1)       | No             | No       | FIFO/LIFO | Yes   | No       |
| PriorityQueue         | O(log n)   | No             | No       | Priority  | No    | No       |
| ConcurrentLinkedQueue | O(1)       | Yes (CAS)      | No       | FIFO      | No    | No       |
| LinkedBlockingQueue   | O(1)       | Yes (2 locks)  | Yes      | FIFO      | No    | Either   |
| ArrayBlockingQueue    | O(1)       | Yes (1 lock)   | Yes      | FIFO      | No    | Required |
| PriorityBlockingQueue | O(log n)   | Yes (1 lock)   | Partial  | Priority  | No    | No       |
| DelayQueue            | O(log n)   | Yes (lock)     | Partial  | Delay     | No    | No       |
| SynchronousQueue      | O(1)       | Yes (CAS/lock) | Yes      | Handoff   | No    | Zero     |
| LinkedBlockingDeque   | O(1)       | Yes (1 lock)   | Yes      | FIFO/LIFO | No    | Either   |

> **Blocking partial**: `PriorityBlockingQueue` and `DelayQueue` are unbounded, so `put()` never blocks.
> `take()` blocks when empty — `poll()` returns null immediately.

---

## Java 21 SequencedCollection (JEP 431)

Java 21 introduced the `SequencedCollection` interface (JEP 431), providing uniform APIs for ordered
collections. This extends `Collection` with `getFirst()`, `getLast()`, `removeFirst()`, `removeLast()`,
and `reversed()`.

```java
// SequencedCollection interface methods
SequencedCollection<E> sc = ...;

sc.getFirst();           // Return first element; throws NoSuchElementException if empty
sc.getLast();            // Return last element; throws NoSuchElementException if empty
sc.removeFirst();        // Remove and return first element; throws if empty
sc.removeLast();         // Remove and return last element; throws if empty
SequencedCollection<E> reversed = sc.reversed();  // Lightweight reverse-order view

sc.addFirst(e);          // Insert before current first (available on SequencedCollection)
sc.addLast(e);           // Insert after current last (same as add/offer for queues)
```

> `reversed()` returns a **view** — it's backed by the original collection. Modifications to the reversed view
> affect the original collection. The view is created in O(1); iteration is O(n) in reverse order.

---

## Java 21 Deque as SequencedCollection

The following `Queue`/`Deque` implementations implement `SequencedCollection`:

| Implementation            | Deque | SequencedCollection | addFirst/addLast | reversed |
|---------------------------|-------|---------------------|------------------|----------|
| **ArrayDeque**            | Yes   | ✅ Yes               | ✅ Full support   | ✅ Full   |
| **LinkedList**            | Yes   | ✅ Yes               | ✅ Full support   | ✅ Full   |
| **ConcurrentLinkedQueue** | No    | ✅ Yes               | ❌ No             | ✅ Yes    |
| **PriorityQueue**         | No    | ✅ Yes               | ❌ No             | ✅ O(n)   |

> **Important distinction**: `PriorityQueue` implements `SequencedCollection` since Java 21 but does **NOT**
> implement `Deque`. `getFirst()` returns the priority head (heap root). `getLast()` scans for the max
> element — O(n) operation. `removeLast()` also O(n). `addFirst`/`addLast` are NOT available.

```java
// ArrayDeque: full Deque + SequencedCollection (recommended)
Deque<String> ad = new ArrayDeque<>();
ad.add("a"); ad.add("b"); ad.add("c");
System.out.println(ad.getFirst());  // "a"
System.out.println(ad.getLast());   // "c"
for (String s : ad.reversed()) {    // c, b, a — reverse iteration
    System.out.println(s);
}

// PriorityQueue: SequencedCollection but NOT Deque
PriorityQueue<Integer> pq = new PriorityQueue<>(List.of(3, 1, 4, 1, 5));
System.out.println(pq.getFirst());  // 1 (min priority)
System.out.println(pq.getLast());   // 5 (max — O(n) scan)
// pq.addFirst(0);  // ❌ Does not compile — no addFirst on PriorityQueue
// pq.addLast(0);   // ❌ Does not compile

// LinkedBlockingDeque: BlockingDeque but does NOT implement SequencedCollection
BlockingDeque<String> lbd = new LinkedBlockingDeque<>();
// lbd.reversed();  // ❌ Does not compile — not SequencedCollection
// SequencedCollection methods available only on head/tail individually:
lbd.peekFirst();   // O(1)
lbd.peekLast();    // O(1)
```

> `LinkedBlockingDeque`, `ArrayBlockingQueue`, `PriorityBlockingQueue`, `DelayQueue`, and
> `SynchronousQueue` do NOT implement `SequencedCollection` — they are blocking-focused and the interface
> wouldn't make semantic sense for zero-capacity or time-delayed queues.

---

## Queue Method Behavior Matrix

The `Queue` and `BlockingQueue` interfaces define operations with different failure-handling strategies:

| Operation   | Throws Exception | Special Value | Blocks   | Times Out        |
|-------------|------------------|---------------|----------|------------------|
| **Insert**  | `add(e)`         | `offer(e)`    | `put(e)` | `offer(e, t, u)` |
| **Remove**  | `remove()`       | `poll()`      | `take()` | `poll(t, u)`     |
| **Examine** | `element()`      | `peek()`      | N/A      | N/A              |

### Throws Exception

- `add(e)`: `IllegalStateException` if capacity-restricted, `NullPointerException` if null not allowed
- `remove()`: `NoSuchElementException` if empty
- `element()`: `NoSuchElementException` if empty

### Special Value

- `offer(e)`: `false` if element cannot be added (capacity restriction), `true` otherwise
- `poll()`: `null` if empty
- `peek()`: `null` if empty

### Blocks (BlockingQueue only)

- `put(e)`: Blocks indefinitely until space is available; responds to thread interruption
- `take()`: Blocks indefinitely until an element is available; responds to thread interruption

### Times Out (BlockingQueue only)

- `offer(e, timeout, unit)`: Blocks up to timeout; returns `false` if space not available in time
- `poll(timeout, unit)`: Blocks up to timeout; returns `null` if no element arrived in time

> **Non-blocking Queue** (`Queue` interface): `add`, `offer`, `remove`, `poll`, `element`, `peek`.
> **Blocking Queue** (`BlockingQueue` interface): All 10 methods above including `put`, `take`, and
> timeout variants.

### Implementation Support Summary

| Implementation        | add/offer | remove/poll | element/peek | put/take | offer/poll(timeout) |
|-----------------------|-----------|-------------|--------------|----------|---------------------|
| ArrayDeque            | ✅/✅       | ✅/✅         | ✅/✅          | ❌        | ❌                   |
| LinkedList            | ✅/✅       | ✅/✅         | ✅/✅          | ❌        | ❌                   |
| PriorityQueue         | ✅/✅       | ✅/✅         | ✅/✅          | ❌        | ❌                   |
| ConcurrentLinkedQueue | ✅/✅       | ✅/✅         | ✅/✅          | ❌        | ❌                   |
| LinkedBlockingQueue   | ✅/✅       | ✅/✅         | ✅/✅          | ✅/✅      | ✅/✅                 |
| ArrayBlockingQueue    | ✅/✅       | ✅/✅         | ✅/✅          | ✅/✅      | ✅/✅                 |
| PriorityBlockingQueue | ✅/✅       | ✅/✅         | ✅/✅          | ❌/✅      | ❌/✅                 |
| DelayQueue            | ✅/✅       | ✅/✅         | ✅/✅          | ❌/✅      | ❌/✅                 |
| SynchronousQueue      | ✅/✅       | ✅/✅         | ✅/✅¹         | ✅/✅      | ✅/✅                 |
| LinkedBlockingDeque   | ✅/✅       | ✅/✅         | ✅/✅          | ✅/✅      | ✅/✅                 |

> ¹ `SynchronousQueue.peek()` always returns `null` — no elements are stored.
> `put` is marked ❌ for `PriorityBlockingQueue` and `DelayQueue` since they are unbounded and never block on insert
> (equivalent to `offer`).

---

## Common Gotchas

1. **ArrayDeque rejects nulls** — Unlike `LinkedList`, calling `add(null)` on `ArrayDeque` throws
   `NullPointerException`.
   Always check for null if inputs may contain null.

2. **ConcurrentLinkedQueue.size() is O(n)** — The queue has no size counter. Each `size()` call traverses all nodes.
   Use `isEmpty()` (O(1)) for emptiness checks. Never use `size()` in hot loops.

3. **PriorityQueue iteration order is NOT sorted** — The iterator returns elements in heap array order, not priority
   order. To iterate in sorted order, drain via `poll()` or use `Arrays.sort(pq.toArray())`.

4. **PriorityQueue has NO stability guarantee** — Elements with equal priority may appear in any order. Don't rely
   on FIFO behavior for equal-priority elements.

5. **BlockingQueue `put()` vs `offer()`** — `put()` blocks indefinitely and may throw `InterruptedException`.
   `offer()` returns immediately. Choose based on whether backpressure or non-blocking is desired.

6. **SynchronousQueue `peek()` always returns null** — Zero-capacity means no elements are ever stored.
   `size()` is always 0, `isEmpty()` is always true. Use only for handoff patterns.

7. **LinkedList is a memory hog** — Each element creates a `Node` object with prev/next/item fields (~32 bytes
   overhead). For large queues, `ArrayDeque` uses ~4-8x less memory.

8. **DelayQueue requires `Delayed` implementation** — You cannot put arbitrary objects into `DelayQueue`.
   Elements must implement `java.util.concurrent.Delayed` with `getDelay()` and `compareTo()`.

9. **`ArrayBlockingQueue` requires capacity at construction** — Unlike `LinkedBlockingQueue` which defaults to
   unbounded, `ArrayBlockingQueue` has no no-arg constructor. Capacity is fixed and cannot change.

10. **`LinkedBlockingDeque` does NOT implement `SequencedCollection`** — Despite being a deque, the blocking variant
    doesn't support `reversed()`, `getFirst()`, or `getLast()` from JEP 431. Use `peekFirst()`/`peekLast()` instead.

11. **Queue `remove()` vs `poll()`** — `remove()` throws `NoSuchElementException` when empty; `poll()` returns null.
    Prefer `poll()` to avoid exception handling overhead unless empty-is-an-error.

12. **`PriorityBlockingQueue` is unbounded** — `put()` never blocks (same as `offer`). Only `take()` blocks when empty.
    Don't rely on `put()` for backpressure — wrap with a bounded wrapper if needed.
