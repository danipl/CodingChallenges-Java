# Java 21 PriorityQueue (Heap) Patterns Guide

## Quick-Reference: Heap Pattern Selection Matrix

| Use Case                 | Best Pattern                | Heap Type          | Key Reason                            | Time Complexity | Space | When to Use                         |
|--------------------------|-----------------------------|--------------------|---------------------------------------|-----------------|-------|-------------------------------------|
| **Top-K Smallest**       | Size-K Min-Heap             | Min-Heap           | Keep K smallest, eject largest        | O(n log k)      | O(k)  | Kth smallest, smallest K elements   |
| **Top-K Largest**        | Size-K Max-Heap             | Max-Heap           | Keep K largest, eject smallest        | O(n log k)      | O(k)  | Kth largest, largest K elements     |
| **K Closest to Target**  | Custom Comparator Max-Heap  | Max-Heap + Dist    | Evict farthest when size > K          | O(n log k)      | O(k)  | K closest points, nearest neighbors |
| **Merge K Sorted Lists** | Min-Heap of Heads           | Min-Heap           | Always pick smallest current head     | O(n log k)      | O(k)  | Merge streams, sorted list merge    |
| **Running Median**       | Two-Heap (Max + Min)        | Max + Min          | Balance lower/upper halves            | O(n log n)      | O(n)  | Median of data stream               |
| **Event Scheduling**     | Single Min-Heap             | Min-Heap           | Process earliest timestamp first      | O(n log n)      | O(n)  | Simulator, priority task execution  |
| **Dijkstra Frontier**    | Min-Heap Distance Pairs     | Min-Heap           | Extract minimum distance node         | O((V+E) log V)  | O(V)  | Shortest path in weighted graph     |
| **Frequency Top-K**      | Min-Heap with Frequency Map | Min-Heap + HashMap | Count then extract top-K by frequency | O(n log k)      | O(n)  | Top-K frequent elements             |

### At-A-Glance Decision Flow

```
Need to maintain order by priority/value?
├─ YES → Single-threaded?
│          ├─ YES → Need min or max first?
│          │          ├─ Min first → PriorityQueue (natural order)
│          │          └─ Max first → PriorityQueue(Comparator.reverseOrder())
│          └─ NO (concurrent) → PriorityBlockingQueue
├─ Top-K pattern?
│          ├─ K smallest → Min-Heap of size K (evict max)
│          ├─ K largest → Max-Heap of size K (evict min)
│          └─ K closest → Max-Heap with distance comparator
├─ Merge K sorted inputs?
│          └─ Min-Heap of K heads (extract min, insert next)
├─ Running median?
│          └─ Two heaps: maxHeap (lower half) + minHeap (upper half)
└─ Graph shortest path?
           └─ Min-Heap of (distance, node) pairs with relaxation
```

---

## Overview

`PriorityQueue` is Java's binary heap implementation — the workhorse for priority-ordered problems in coding challenges.
Unlike standard queues (FIFO), elements are dequeued by priority (natural ordering or custom `Comparator`). Critical for
Top-K, merging, median finding, and graph algorithms.

**Key properties:**

- **Min-heap by default** — smallest element at head
- **O(log n) insert/extract**, O(1) peek
- **NOT a Deque** — does not support `addFirst`/`addLast`
- **Iteration order NOT sorted** — heap order only
- **Implements `SequencedCollection`** since Java 21 (JEP 431)

---

## 1. Min-Heap (Default PriorityQueue)

```java
Queue<Integer> minHeap = new PriorityQueue<>();
```

### Characteristics

| Property          | Value                               |
|-------------------|-------------------------------------|
| **Ordering**      | Natural (ascending, smallest first) |
| **Null elements** | Not allowed                         |
| **Thread-safe**   | No                                  |
| **Performance**   | O(log n) offer/poll, O(1) peek      |
| **Backed by**     | Binary min-heap (array-based)       |

### Complexity

| Operation     | Average  | Worst Case |
|---------------|----------|------------|
| `offer(e)`    | O(log n) | O(log n)   |
| `poll()`      | O(log n) | O(log n)   |
| `peek()`      | O(1)     | O(1)       |
| `contains(e)` | O(n)     | O(n)       |
| `remove(e)`   | O(n)     | O(n)       |
| Iteration     | O(n)     | O(n)       |

### When to Use

- **Top-K smallest** — maintain heap of size K, evict largest
- **Merge K sorted lists** — min-heap of list heads
- **Event-driven simulation** — process by earliest time
- **Dijkstra's algorithm** — extract minimum distance node
- **Huffman coding** — combine two smallest frequencies

### Magic Methods

```java
// Basic operations
pq.offer(element);     // O(log n) insert

Integer min = pq.poll(); // O(log n) extract minimum
Integer head = pq.peek(); // O(1) view minimum without removal

// Bulk operations
pq.

addAll(collection); // O(k log n) or O(n) via heapify
pq.

comparator();       // Returns null for natural ordering

// Java 21 SequencedCollection (PriorityQueue implements this, NOT Deque)
Integer first = pq.getFirst();  // Same as peek() — minimum element
Integer last = pq.getLast();    // Maximum element — O(n) scan
pq.

removeFirst();               // Same as poll() — O(log n)
pq.

removeLast();                // Remove maximum — O(n)
```

**Practical min-heap pattern:**

```java
// Extract K smallest elements
public List<Integer> kSmallest(int[] nums, int k) {
    PriorityQueue<Integer> minHeap = new PriorityQueue<>();
    for (int n : nums) {
        minHeap.offer(n);
    }
    List<Integer> result = new ArrayList<>();
    for (int i = 0; i < k && !minHeap.isEmpty(); i++) {
        result.add(minHeap.poll());
    }
    return result;
}
```

---

## 2. Max-Heap (Reverse Order)

```java
Queue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());
```

### Characteristics

| Property          | Value                              |
|-------------------|------------------------------------|
| **Ordering**      | Natural descending (largest first) |
| **Null elements** | Not allowed                        |
| **Thread-safe**   | No                                 |
| **Performance**   | O(log n) offer/poll, O(1) peek     |
| **Backed by**     | Binary max-heap (via comparator)   |

### When to Use

- **Top-K largest** — maintain heap of size K, evict smallest
- **K closest with max-heap** — evict farthest when size > K
- **Median finder** — max-heap for lower half
- **Priority inversion** — highest value = highest priority

### Construction Patterns

```java
// Max-heap of integers
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());

// Max-heap by custom field
PriorityQueue<Task> byPriorityDesc = new PriorityQueue<>(
        Comparator.comparingInt(Task::priority).reversed()
);

// Max-heap multi-key
PriorityQueue<Person> byAgeThenName = new PriorityQueue<>(
        Comparator.comparingInt(Person::age)
                .reversed()
                .thenComparing(Person::name)
);
```

**Practical Top-K largest pattern:**

```java
// Find K largest elements — O(n log k)
public List<Integer> topKLargest(int[] nums, int k) {
    // Min-heap of size K — smallest of the K largest at head
    PriorityQueue<Integer> minHeap = new PriorityQueue<>(k);

    for (int n : nums) {
        if (minHeap.size() < k) {
            minHeap.offer(n);
        } else if (n > minHeap.peek()) {
            minHeap.poll();
            minHeap.offer(n);
        }
    }
    // Drain — NOT sorted, just top K
    return new ArrayList<>(minHeap);
}

// Alternative: Max-heap, take K elements — O(n + k log n)
public List<Integer> topKLargestAlt(int[] nums, int k) {
    PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder());
    for (int n : nums) {
        maxHeap.offer(n);
    }
    List<Integer> result = new ArrayList<>(k);
    for (int i = 0; i < k && !maxHeap.isEmpty(); i++) {
        result.add(maxHeap.poll());
    }
    return result;
}
```

---

## 3. Custom Comparator Heap

```java
PriorityQueue<Point> pq = new PriorityQueue<>(
        Comparator.comparingInt(p -> distance(p, origin))
);
```

### Characteristics

| Property      | Value                                |
|---------------|--------------------------------------|
| **Ordering**  | Defined by Comparator                |
| **Lambda**    | Supported (clean inline comparators) |
| **Multi-key** | `.thenComparing()` chaining          |
| **Primitive** | Use `comparingInt`, `comparingLong`  |

### When to Use

- **K closest elements** — compare by distance to target
- **Multi-key sorting** — primary then secondary criteria
- **Custom priority logic** — SLA, urgency, timestamp combinations

### Lambda Comparator Patterns

```java
// Distance-based (K closest points)
PriorityQueue<int[]> kClosest = new PriorityQueue<>(
                (a, b) -> Integer.compare(
                        a[0] * a[0] + a[1] * a[1],  // Compare squared distance (no sqrt needed)
                        b[0] * b[0] + b[1] * b[1]
                )
        );

// Multi-key: age descending, then name ascending
PriorityQueue<Person> pq = new PriorityQueue<>(
        Comparator.<Person>comparingInt(Person::age).reversed()
                .thenComparing(Person::name)
);

// Timestamp with urgency tiebreaker
PriorityQueue<Event> pq = new PriorityQueue<>(
        Comparator.comparingLong(Event::timestamp)
                .thenComparingInt(Event::urgency)
);
```

**Practical K closest pattern:**

```java
// K closest points to origin — max-heap evicts farthest
public int[][] kClosest(int[][] points, int k) {
    // Max-heap by distance (evict farthest when size > k)
    PriorityQueue<int[]> maxHeap = new PriorityQueue<>((a, b) -> {
        int distA = a[0] * a[0] + a[1] * a[1];
        int distB = b[0] * b[0] + b[1] * b[1];
        return Integer.compare(distB, distA);  // Max-heap
    });

    for (int[] point : points) {
        maxHeap.offer(point);
        if (maxHeap.size() > k) {
            maxHeap.poll();  // Remove farthest
        }
    }

    // Convert to array
    return maxHeap.toArray(new int[k][]);
}
```

---

## 4. Two-Heap Pattern (Median Finder)

```java
PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Comparator.reverseOrder()); // Lower half
PriorityQueue<Integer> minHeap = new PriorityQueue<>();                          // Upper half
```

### Characteristics

| Property       | Value                          |
|----------------|--------------------------------|
| **Structure**  | Two heaps balancing each other |
| **Invariant**  | Sizes differ by at most 1      |
| **Median**     | Top of larger heap or average  |
| **Operations** | O(log n) add, O(1) findMedian  |

### When to Use

- **Running median** — data stream median at any point
- **Balanced partitioning** — split into lower/upper halves
- **Sliding window median** — with lazy removal

**Practical median finder:**

```java
class MedianFinder {
    private final PriorityQueue<Integer> maxHeap; // Lower half (reversed)
    private final PriorityQueue<Integer> minHeap; // Upper half

    public MedianFinder() {
        maxHeap = new PriorityQueue<>(Comparator.reverseOrder());
        minHeap = new PriorityQueue<>();
    }

    // O(log n) add number
    public void addNum(int num) {
        // Add to appropriate heap
        if (maxHeap.isEmpty() || num <= maxHeap.peek()) {
            maxHeap.offer(num);
        } else {
            minHeap.offer(num);
        }

        // Balance sizes (differ by at most 1)
        if (maxHeap.size() > minHeap.size() + 1) {
            minHeap.offer(maxHeap.poll());
        } else if (minHeap.size() > maxHeap.size()) {
            maxHeap.offer(minHeap.poll());
        }
    }

    // O(1) find median
    public double findMedian() {
        if (maxHeap.size() == minHeap.size()) {
            return (maxHeap.peek() + minHeap.peek()) / 2.0;
        }
        return maxHeap.peek();  // maxHeap has one more element
    }
}
```

---

## 5. Merge K Sorted Lists Pattern

```java
PriorityQueue<ListNode> pq = new PriorityQueue<>(Comparator.comparingInt(n -> n.val));
```

### Characteristics

| Property       | Value                                   |
|----------------|-----------------------------------------|
| **Structure**  | Min-heap of list heads                  |
| **Operation**  | Extract min, insert next from same list |
| **Complexity** | O(n log k) where n = total elements     |
| **Space**      | O(k) for heap                           |

### When to Use

- **Merge K sorted lists/arrays**
- **External merge sort** — merge sorted chunks
- **Multi-way merge** — any K sorted streams

**Practical merge K lists:**

```java
public ListNode mergeKLists(ListNode[] lists) {
    // Min-heap by node value
    PriorityQueue<ListNode> pq = new PriorityQueue<>(
            Comparator.comparingInt(n -> n.val)
    );

    // Initialize with head of each non-empty list
    for (ListNode list : lists) {
        if (list != null) {
            pq.offer(list);
        }
    }

    ListNode dummy = new ListNode(0);
    ListNode current = dummy;

    // Extract min, insert next
    while (!pq.isEmpty()) {
        ListNode node = pq.poll();
        current.next = node;
        current = current.next;

        if (node.next != null) {
            pq.offer(node.next);
        }
    }

    return dummy.next;
}
```

---

## 6. Dijkstra's Shortest Path Pattern

```java
PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
// Each element: [node, distance]
```

### Characteristics

| Property       | Value                                     |
|----------------|-------------------------------------------|
| **Structure**  | Min-heap of (node, distance) pairs        |
| **Operation**  | Extract minimum distance, relax neighbors |
| **Complexity** | O((V+E) log V)                            |
| **Space**      | O(V) for distances + heap                 |

### When to Use

- **Single-source shortest path** — weighted graphs
- **Network latency calculation**
- **Pathfinding with costs**

**Practical Dijkstra:**

```java
public int dijkstra(int n, List<List<int[]>> graph, int start, int end) {
    // graph: adjacency list where graph.get(u) = List of [v, weight]
    int[] dist = new int[n];
    Arrays.fill(dist, Integer.MAX_VALUE);
    dist[start] = 0;

    // Min-heap: [node, distance]
    PriorityQueue<int[]> pq = new PriorityQueue<>(
            Comparator.comparingInt(a -> a[1])
    );
    pq.offer(new int[]{start, 0});

    while (!pq.isEmpty()) {
        int[] current = pq.poll();
        int u = current[0], d = current[1];

        // Skip if we found a better path already
        if (d > dist[u]) continue;

        // Early exit if reached target
        if (u == end) return d;

        // Relax neighbors
        for (int[] edge : graph.get(u)) {
            int v = edge[0], weight = edge[1];
            if (dist[u] + weight < dist[v]) {
                dist[v] = dist[u] + weight;
                pq.offer(new int[]{v, dist[v]});
            }
        }
    }

    return dist[end] == Integer.MAX_VALUE ? -1 : dist[end];
}
```

---

## Core Heap Operations Reference

### Basic Queue Methods

```java
PriorityQueue<Integer> pq = new PriorityQueue<>();

// Insert
pq.

offer(5);        // O(log n) — returns false only if capacity-exhausted
pq.

add(10);         // Same as offer, throws on null

// Extract
Integer min = pq.poll();      // O(log n) — null if empty
Integer head = pq.peek();     // O(1) — null if empty

// Inspection
boolean has = pq.contains(5); // O(n) — linear scan
int size = pq.size();         // O(1)
boolean empty = pq.isEmpty(); // O(1)

// Remove
pq.

remove(5);       // O(n) — scan + re-heapify
pq.

clear();         // O(n)
```

### Comparator Construction

```java
// Natural order (min-heap)
PriorityQueue<Integer> min = new PriorityQueue<>();

// Max-heap
PriorityQueue<Integer> max = new PriorityQueue<>(Comparator.reverseOrder());
PriorityQueue<Integer> max2 = new PriorityQueue<>((a, b) -> b - a);

// Custom field
PriorityQueue<Task> pq = new PriorityQueue<>(
        Comparator.comparingInt(Task::priority)
);

// Multi-key
PriorityQueue<Person> pq = new PriorityQueue<>(
        Comparator.comparingInt(Person::age)
                .thenComparing(Person::name)
);

// Reverse multi-key
PriorityQueue<Person> pq = new PriorityQueue<>(
        Comparator.comparingInt(Person::age).reversed()
                .thenComparing(Person::name)
);
```

### Java 21 SequencedCollection on PriorityQueue

```java
PriorityQueue<Integer> pq = new PriorityQueue<>();
pq.

addAll(List.of(3, 1,4,1,5));

// SequencedCollection methods (Java 21)
Integer first = pq.getFirst();  // Minimum element (same as peek)
Integer last = pq.getLast();    // Maximum element — O(n) scan
Integer removed = pq.removeFirst(); // Same as poll()
pq.

removeLast();                // Remove maximum — O(n)

SequencedCollection<Integer> reversed = pq.reversed(); // Reverse-order view

// NOTE: PriorityQueue does NOT support Deque methods
// pq.addFirst(e);   // ❌ Compilation error
// pq.addLast(e);    // ❌ Compilation error
```

---

## Complexity Summary

| Operation             | Time Complexity    | Notes                                          |
|-----------------------|--------------------|------------------------------------------------|
| `offer(e)`            | O(log n)           | Heap insertion with bubble-up                  |
| `poll()`              | O(log n)           | Extract root, bubble-down                      |
| `peek()`              | O(1)               | View root without removal                      |
| `contains(e)`         | O(n)               | Linear scan — heap not sorted                  |
| `remove(e)`           | O(n)               | Scan + re-heapify                              |
| `addAll(collection)`  | O(k log n) or O(n) | k elements or heapify if building from scratch |
| Build heap from array | O(n)               | Floyd's heap construction                      |
| Iteration             | O(n)               | Heap order, NOT sorted order                   |
| `getFirst()`          | O(1)               | Same as peek (Java 21)                         |
| `getLast()`           | O(n)               | Scan for max element                           |

---

## Common Gotchas

1. **Iteration order is NOT sorted** — PriorityQueue only guarantees head is min/max. Iterating via `for (e : pq)`
   visits in heap order, not sorted order.

2. **`contains()` and `remove()` are O(n)** — Linear scan required. Avoid in tight loops; use HashSet alongside if
   frequent membership checks needed.

3. **No stability guarantee** — Equal-priority elements have no guaranteed ordering. For stable priority, include
   sequence number in comparator.

   ```java
   // Unstable: equal priorities may dequeue in any order
   PriorityQueue<Task> pq = new PriorityQueue<>(Comparator.comparingInt(Task::priority));
   
   // Stable: tie-break by insertion order
   AtomicLong seq = new AtomicLong();
   PriorityQueue<StableTask> pq = new PriorityQueue<>(
       Comparator.comparingInt(StableTask::priority)
                 .thenComparingLong(StableTask::sequence)
   );
   record StableTask(int priority, long sequence, Task task) {}
   ```

4. **Heapify via `addAll()` vs individual `add()`** — `addAll()` on empty queue uses O(n) heapify construction.
   Individual `add()` calls are O(n log n) total. Prefer `addAll()` for bulk initialization.

   ```java
   // SLOW: O(n log n)
   PriorityQueue<Integer> pq = new PriorityQueue<>();
   for (int n : nums) {
       pq.add(n);
   }
   
   // FAST: O(n) via heapify
   PriorityQueue<Integer> pq = new PriorityQueue<>();
   pq.addAll(nums);  // Single bulk operation
   ```

5. **Null elements not allowed** — `offer(null)` throws `NullPointerException`. Unlike some collections, PriorityQueue
   rejects nulls entirely.

6. **Not thread-safe** — Use `PriorityBlockingQueue` for concurrent access. `PriorityQueue` has no synchronization.

7. **Capacity unbounded by default** — PriorityQueue grows automatically. For bounded priority queues, manually enforce
   size limits (Top-K pattern).

8. **Min-heap is default** — New users often assume max-heap. Explicitly specify `Comparator.reverseOrder()` for
   max-heap.

---

## See Also

- **MAP_GUIDE.md** — Use `TreeMap` for sorted key-value storage with range queries (`floorKey`, `ceilingKey`) as an
  alternative to heap when you need random access by key.

- **QUEUE_GUIDE.md** — Basic `PriorityQueue` coverage including `SequencedCollection` methods and comparison with
  `PriorityBlockingQueue` for concurrent scenarios.

- **development/recursion/Fibonacci.java** — See memoization pattern contrast: PriorityQueue for priority ordering vs
  HashMap for caching overlapping subproblems.

- **development/recursion/GridTraveler.java** — Alternative DP solution for path counting; PriorityQueue appears in
  graph shortest path variants.

- **platform/challenge10/TaskScheduler.java** — `DelayQueue` builds on PriorityQueue for time-based task scheduling (
  elements expire after delay).

- **development/PREPARATION.md** — Heap patterns section with two-pointer and sliding window alternatives; Union-Find
  for connected components vs heap-based approaches.

---

## Performance Summary

| Pattern                | Time Complexity | Space Complexity | Best For                        |
|------------------------|-----------------|------------------|---------------------------------|
| Min-Heap (default)     | O(log n) ops    | O(n)             | Smallest-first extraction       |
| Max-Heap (reversed)    | O(log n) ops    | O(n)             | Largest-first extraction        |
| Top-K (size-K heap)    | O(n log k)      | O(k)             | K smallest/largest elements     |
| Merge K Lists          | O(n log k)      | O(k)             | Multi-way sorted merge          |
| Two-Heap Median        | O(log n) add    | O(n)             | Running median from stream      |
| K Closest (custom cmp) | O(n log k)      | O(k)             | Nearest neighbors by distance   |
| Dijkstra               | O((V+E) log V)  | O(V)             | Shortest path in weighted graph |

---

## Java 21 Features: SequencedCollection

Java 21 added `SequencedCollection` interface (JEP 431) to PriorityQueue:

```java
PriorityQueue<Integer> pq = new PriorityQueue<>(List.of(3, 1, 4, 5, 9, 2, 6));

// New Java 21 methods
pq.

getFirst();      // 1 (minimum, same as peek)
pq.

getLast();       // 9 (maximum, requires O(n) scan)
pq.

removeFirst();   // 1 (same as poll)
pq.

removeLast();    // 9 (O(n) removal + re-heapify)

SequencedCollection<Integer> reversed = pq.reversed();
for(
int n :reversed){
        // Iterate in reverse heap order (NOT descending sorted order)
        }
```

> **Important**: `getLast()` and `removeLast()` are O(n) operations because heap doesn't track maximum element position.
> Use only when necessary.

(End of file - total ~560 lines)
