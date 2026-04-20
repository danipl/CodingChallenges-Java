# Java 21 List Implementations Guide

## Quick-Reference: Implementation Selection Matrix

| Use Case                     | Best Choice            | Key Reason                                 | Ordering  | Thread-Safe | Random Access | `add`  | `get` | `remove` | Null Elements |
|------------------------------|------------------------|--------------------------------------------|-----------|-------------|---------------|--------|-------|----------|---------------|
| **Default, random access**   | `ArrayList`            | Fastest get(n), compact memory             | Insertion | ❌           | ✅ O(1)        | O(1)*  | O(1)  | O(n)     | ✅ Multiple    |
| **Frequent add/remove ends** | `LinkedList`           | O(1) at ends via DeQue interface           | Insertion | ❌           | ❌ O(n)        | O(1)** | O(n)  | O(1)**   | ✅ Multiple    |
| **Queue + List operations**  | `LinkedList`           | Implements both `List` and `Deque`         | Insertion | ❌           | ❌ O(n)        | O(1)** | O(n)  | O(1)**   | ✅ Multiple    |
| **Thread-safe legacy**       | ~~`Vector`~~           | Avoid — use `CopyOnWriteArrayList` instead | Insertion | ✅ (slow)    | ✅ O(1)        | O(1)*  | O(1)  | O(n)     | ✅ Multiple    |
| **Stack operations**         | ~~`Stack`~~            | Avoid — use `ArrayDeque` instead           | LIFO      | ✅ (slow)    | ❌ O(n)        | O(1)   | O(n)  | O(1)     | ✅ Multiple    |
| **Thread-safe modern**       | `CopyOnWriteArrayList` | Snapshot iterators, read-optimized         | Insertion | ✅           | ✅ O(1)        | O(n)   | O(1)  | O(n)     | ✅ Multiple    |

\* Amortized O(1) — resizing overhead occasionally  
\*\* O(1) only at head/tail via DeQue methods (`addFirst`, `addLast`, `removeFirst`, `removeLast`)

### At-A-Glance Decision Flow

```
Need thread-safety?
  ├─ YES → Mostly reads, rare writes?
  │          ├─ YES → CopyOnWriteArrayList
  │          └─ NO  → Collections.synchronizedList(new ArrayList<>())
  └─ NO  → Need frequent add/remove at BOTH ends?
              ├─ YES → Need List interface + random access?
              │          ├─ YES → LinkedList (but avoid get(n))
              │          └─ NO  → ArrayDeque (better queue, NOT a List)
              └─ NO  → Default choice → ArrayList
```

---

## Overview

Java provides multiple `List` implementations via `java.util` and `java.util.concurrent`. Each is optimized for
different access patterns, thread safety requirements, and modification frequencies. This guide focuses on practical
usage patterns from coding challenges.

---

## 1. ArrayList

```java
List<T> list = new ArrayList<>();
```

### Characteristics

| Property          | Value                                         |
|-------------------|-----------------------------------------------|
| **Ordering**      | Insertion order                               |
| **Null elements** | Multiple allowed                              |
| **Thread-safe**   | No                                            |
| **Backing**       | Dynamic array (resizes 1.5x when full)        |
| **Performance**   | O(1) random access, O(n) middle insert/remove |

### Complexity

| Operation             | Average        | Worst Case  |
|-----------------------|----------------|-------------|
| `get(index)`          | O(1)           | O(1)        |
| `add(element)`        | O(1) amortized | O(n) resize |
| `add(index, element)` | O(n)           | O(n)        |
| `remove(index)`       | O(n)           | O(n)        |
| `contains(element)`   | O(n)           | O(n)        |
| Iteration             | O(n)           | O(n)        |
| `subList(from, to)`   | O(1)           | O(1)        |

> **Resize note**: ArrayList grows by 50% when capacity exceeded. Pre-size with `new ArrayList<>(expectedSize)` to avoid
> resizing overhead in loops.

### When to Use

- **Default choice for 90% of challenges**: When you need a dynamic array with fast random access.
- **Accumulating results**: Our `FizzBuzz.java` builds result lists incrementally — perfect ArrayList use case.
- **SubList operations**: Need views into portions of data without copying.
- **Sorting in place**: `Collections.sort(list)` or `list.sort(comparator)` — ArrayList excels here.
- **Memoization collections**: `AllConstruct.java` uses `ArrayList` to accumulate combination results.
- **Single-threaded scenarios only** — wrap with `Collections.synchronizedList()` if needed.

**DO NOT use** for frequent head/middle insertions or removals — O(n) shifting.

### Magic Methods (Java 21)

```java
// SubList view - backed by original list, changes reflect
List<T> view = list.subList(fromIndex, toIndex);

// Sort with Comparator
list.

sort(Comparator.naturalOrder());
        list.

sort((a, b) ->b -a);  // descending

// Collections utilities
        Collections.

sort(list);
Collections.

reverse(list);
Collections.

shuffle(list);
Collections.

swap(list, i, j);
Collections.

rotate(list, distance);
Collections.

binarySearch(list, key);  // requires sorted list

// Replace all via function
list.

replaceAll(x ->

transform(x));

// Remove if predicate matches
        list.

removeIf(x ->x ==null||x.

isEmpty());

// Java 21 SequencedCollection methods
        list.

getFirst();    // throws if empty
list.

getLast();     // throws if empty
list.

reversed();    // view, not copy
```

**Practical accumulation pattern** (from `FizzBuzz.java`):

```java
// BEFORE: Manual array management
String[] result = new String[n];
int size = 0;
// ... add elements, track size manually

// AFTER: Idiomatic ArrayList
List<String> list = new ArrayList<>(n);  // pre-size to avoid resize
for(
int pos = 1;
pos <=n;pos++){
        list.

add(buildFizzBuzz(pos));
        }
        return list;
```

**Practical memoization pattern** (from `AllConstruct.java`):

```java
// Accumulating combinations in recursion
Collection<List<String>> combs = new ArrayList<>();
for(
String seq :seqs){
        if(text.

startsWith(seq)){
String subText = text.substring(seq.length());
Collection<List<String>> subCombs = memo(subText, seqs, memo);
        for(
List<String> subComb :subCombs){
List<String> newComb = new ArrayList<>(subComb);
            newComb.

add(seq);  // Efficient ArrayList add
            combs.

add(newComb);
        }
                }
                }
                memo.

put(text, combs);
```

---

## 2. LinkedList

```java
List<T> list = new LinkedList<>();
```

### Characteristics

| Property          | Value                                 |
|-------------------|---------------------------------------|
| **Ordering**      | Insertion order                       |
| **Null elements** | Multiple allowed                      |
| **Thread-safe**   | No                                    |
| **Backing**       | Doubly-linked list (node per element) |
| **Performance**   | O(1) at ends, O(n) random access      |

### Complexity

| Operation             | Average | Worst Case |
|-----------------------|---------|------------|
| `get(index)`          | O(n)    | O(n)       |
| `add(element)` (tail) | O(1)    | O(1)       |
| `add(index, element)` | O(n)    | O(n)       |
| `remove(index)`       | O(n)    | O(n)       |
| `addFirst(element)`   | O(1)    | O(1)       |
| `addLast(element)`    | O(1)    | O(1)       |
| `removeFirst()`       | O(1)    | O(1)       |
| `removeLast()`        | O(1)    | O(1)       |
| Iteration             | O(n)    | O(n)       |

> **Memory overhead**: Each element requires a Node object with 2 references (prev, next) + payload. 24-32 bytes
> overhead per element vs ArrayList's 4 bytes per reference.

### When to Use

- **Queue pattern with List interface**: When you need both List methods AND Deque operations.
- **Sliding window with indices**: Our `MoveZeroes.java` uses LinkedList as queue to track zero positions.
- **Frequent add/remove at BOTH ends**: But consider `ArrayDeque` if you don't need List interface.
- **Iterator-heavy modifications**: Adding/removing via `ListIterator` is O(1) once positioned.

**DO NOT use** for random access patterns — `get(n)` traverses from nearest end, O(n).  
**DO NOT use** just for queue — prefer `ArrayDeque` (better cache locality, less memory).

### Magic Methods (Deque Operations via LinkedList)

LinkedList implements both `List<T>` and `Deque<T>`:

```java
// As Deque (Queue pattern)
list.addFirst(element);   // push to front
list.

addLast(element);    // push to back (same as add)
list.

removeFirst();       // pop from front (throws if empty)
list.

removeLast();        // pop from back (throws if empty)
list.

getFirst();          // peek front
list.

getLast();           // peek back
list.

offerFirst(element); // push front (returns false if bounded)
list.

offerLast(element);  // push back
list.

pollFirst();         // pop front (returns null if empty)
list.

pollLast();          // pop back

// As List (standard operations)
list.

add(element);        // same as addLast
list.

get(index);          // O(n) - avoid in loops
list.

set(index, element); // O(n)
list.

remove(index);       // O(n)

// Java 21 SequencedCollection methods
list.

reversed();          // view in reverse order
```

**Practical queue pattern** (from `MoveZeroes.java`):

```java
// Tracking zero positions for in-place swaps
Queue<Integer> zeroIndices = new LinkedList<>();
for(
int reader = 0;
reader<nums.length;reader++){
        if(nums[reader]==0){
        queue.

add(reader);  // track zero position
    }else if(!queue.

isEmpty()){
int writer = queue.poll();  // get oldest zero position
// Swap non-zero to zero position
int temp = nums[reader];
nums[reader]=nums[writer];
nums[writer]=temp;
        queue.

add(reader);  // current position is now zero
    }
            }
```

**Note**: Could use `ArrayDeque<Integer>` instead — faster, less memory — but LinkedList chosen when List interface
needed elsewhere.

---

## 3. Vector (Legacy)

```java
List<T> list = new Vector<>();
```

### Characteristics

| Property          | Value                                  |
|-------------------|----------------------------------------|
| **Thread-safe**   | Yes (whole-list synchronization)       |
| **Null elements** | Multiple allowed                       |
| **Performance**   | Poor (single lock for entire list)     |
| **Resizing**      | Doubles capacity (vs ArrayList's 1.5x) |

### When to Use

**NEVER** in new code. Use one of these instead:

| Old Vector Pattern            | Modern Replacement                                |
|-------------------------------|---------------------------------------------------|
| Thread-safe list              | `Collections.synchronizedList(new ArrayList<>())` |
| Concurrent reads, rare writes | `CopyOnWriteArrayList`                            |
| Concurrent read/write         | `ConcurrentLinkedQueue` (if queue pattern)        |

Exists solely for legacy compatibility (Java 1.0). Slower than synchronized ArrayList due to method-level
synchronization overhead.

---

## 4. Stack (Deprecated Pattern)

```java
Stack<T> stack = new Stack<>();  // ❌ DO NOT USE
```

### Characteristics

| Property        | Value                           |
|-----------------|---------------------------------|
| **Extends**     | `Vector` (inherits all baggage) |
| **Thread-safe** | Yes (slow)                      |
| **Ordering**    | LIFO                            |

### When to Use

**NEVER** in new code. `Stack` is fundamentally broken:

1. Extends `Vector` — inherits random access methods that break stack abstraction
2. Allows access to bottom elements — violates LIFO principle
3. Synchronization overhead — most use cases don't need it

**Use `ArrayDeque` instead:**

```java
// CORRECT: Modern stack
Deque<T> stack = new ArrayDeque<>();
stack.

push(element);    // push

T top = stack.pop();    // pop (throws if empty)
T peek = stack.peek();  // peek (null if empty)

// DFS pattern from graph challenges
Deque<Integer> stack = new ArrayDeque<>();
stack.

push(startNode);
while(!stack.

isEmpty()){
int node = stack.pop();
// process(node)
    for(
int neighbor :graph.

get(node)){
        if(!visited.

contains(neighbor)){
        stack.

push(neighbor);
        }
                }
                }
```

See `QUEUE_GUIDE.md` for ArrayDeque stack/queue patterns.

---

## 5. CopyOnWriteArrayList (Concurrent Reads)

```java
List<T> list = new CopyOnWriteArrayList<>();
```

### Characteristics

| Property          | Value                                           |
|-------------------|-------------------------------------------------|
| **Thread-safe**   | Yes (copy-on-write strategy)                    |
| **Null elements** | Multiple allowed                                |
| **Iterator**      | Snapshot (no `ConcurrentModificationException`) |
| **Writes**        | O(n) — copies entire array on each write        |
| **Reads**         | O(1) — lock-free                                |

### Complexity

| Operation       | Cost |
|-----------------|------|
| `get(index)`    | O(1) |
| `add(element)`  | O(n) |
| `remove(index)` | O(n) |
| Iteration       | O(n) |

> **Snapshot semantics**: Iterators see the list state at iterator creation time. No concurrent modification exceptions,
> but may see stale data.

### When to Use

- **Read-heavy, write-rare**: Listener lists, configuration caches, observer patterns.
- **Safe iteration during modification**: When you need to iterate while other threads modify.
- **Event listener registries**: Add/remove listeners while dispatching events.

**NEVER use** for write-heavy workloads or large lists — copy overhead kills performance.

### Magic Methods

```java
// All standard List methods work, but writes copy the array
list.add(element);           // copies array, O(n)
list.

remove(index);          // copies array, O(n)
list.

set(index, element);    // copies array, O(n)

// Safe iteration during modification
for(
String item :list){

// No ConcurrentModificationException even if other threads modify
process(item);
}
```

**Practical listener pattern**:

```java
// Thread-safe listener registry
private final List<EventListener> listeners = new CopyOnWriteArrayList<>();

public void fireEvent(String event) {
    // Safe: snapshot taken, no CME if listeners modified during dispatch
    for (EventListener listener : listeners) {
        listener.onEvent(event);
    }
}

public void addListener(EventListener listener) {
    listeners.add(listener);  // O(n) copy, but acceptable for rare adds
}
```

---

## Decision Matrix

| Requirement                        | Choose                              |
|------------------------------------|-------------------------------------|
| Default, random access             | **ArrayList**                       |
| Frequent add/remove at ends        | **LinkedList** (if List needed)     |
| Queue pattern (no List needed)     | **ArrayDeque** (see QUEUE_GUIDE.md) |
| Stack pattern                      | **ArrayDeque** (NOT `Stack`)        |
| Thread-safe, read-heavy            | **CopyOnWriteArrayList**            |
| Thread-safe, balanced read/write   | **Collections.synchronizedList()**  |
| Safe iteration during modification | **CopyOnWriteArrayList**            |
| Memory efficiency                  | **ArrayList**                       |

## Performance Summary

| Implementation       | get(n) | add (tail) | add (middle) | remove (middle) | Thread-Safe | Memory/Element |
|----------------------|--------|------------|--------------|-----------------|-------------|----------------|
| ArrayList            | O(1)   | O(1)*      | O(n)         | O(n)            | No          | 4 bytes (ref)  |
| LinkedList           | O(n)   | O(1)       | O(n)         | O(n)            | No          | 24-32 bytes    |
| Vector               | O(1)   | O(1)*      | O(n)         | O(n)            | Yes (slow)  | 4 bytes (ref)  |
| CopyOnWriteArrayList | O(1)   | O(n)       | O(n)         | O(n)            | Yes         | 4 bytes (ref)  |

\* Amortized — resizing overhead occasionally

---

## Java 21 SequencedCollection Features

Java 21 introduced the `SequencedCollection<E>` interface (JEP 431), providing consistent APIs for collections with a
well-defined encounter order. `ArrayList`, `LinkedList`, and `Vector` implement this.

```java
List<String> list = new ArrayList<>(List.of("a", "b", "c", "d"));

// First/last access (throws NoSuchElementException if empty)
String first = list.getFirst();   // "a"
String last = list.getLast();     // "d"

// Remove first/last (throws if empty)
String removedFirst = list.removeFirst();  // "a"
String removedLast = list.removeLast();    // "d"

// Reversed view (lazy, O(1) — no copy)
List<String> reversed = list.reversed();   // ["c", "b"]

// Safe variants (return null instead of throwing)
String first = list.isEmpty() ? null : list.getFirst();
// Or use Optional
Optional.

ofNullable(list.isEmpty() ?null:list.

getFirst());
```

**Practical usage** (from metrics challenges):

```java
// Processing time windows in MetricsAggregatorImpl-style challenges
ArrayDeque<LogEntry> window = new ArrayDeque<>();

// Java 21: cleaner first/last access
if(!window.

isEmpty()){
LogEntry oldest = window.getFirst();
LogEntry newest = window.getLast();

// Remove expired entries from front
    while(!window.

isEmpty() &&window.

getFirst().

timestamp() <threshold){
        window.

removeFirst();
    }
            }
```

---

## Common Gotchas

1. **ArrayList resizing overhead in loops**: Pre-size with `new ArrayList<>(expectedCapacity)` when you know the size
   upfront. Resizing happens at 1.5x intervals.

2. **LinkedList memory bloat**: Each element has 24-32 bytes overhead (prev/next refs + node object). For 1M elements,
   that's 24-32 MB overhead vs ArrayList's 4 MB.

3. **ConcurrentModificationException**: Iterating while modifying (outside `ListIterator` methods):
   ```java
   for (String item : list) {
       if (shouldRemove(item)) {
           list.remove(item);  // ❌ CME!
       }
   }
   // CORRECT:
   list.removeIf(item -> shouldRemove(item));
   // OR:
   Iterator<String> it = list.iterator();
   while (it.hasNext()) {
       if (shouldRemove(it.next())) {
           it.remove();  // ✅ Safe via iterator
       }
   }
   ```

4. **Vector is NOT the thread-safe ArrayList**: It's slower due to method-level synchronization. Use
   `Collections.synchronizedList(new ArrayList<>())` instead.

5. **Stack class is deprecated**: Use `ArrayDeque` for stack operations — `push()`, `pop()`, `peek()`.

6. **`subList()` returns a view, not a copy**: Modifications reflect in original list. To copy:
   `new ArrayList<>(list.subList(from, to))`.

7. **LinkedList get(n) in loops**: Calling `list.get(i)` in a for-loop creates O(n²) behavior:
   ```java
   // ❌ O(n²) with LinkedList
   for (int i = 0; i < list.size(); i++) {
       process(list.get(i));
   }
   // ✅ O(n) with iterator or for-each
   for (T item : list) {
       process(item);
   }
   ```

8. **CopyOnWriteArrayList write cost**: Every write copies the entire array. Fine for 10 listeners, catastrophic for
   100K elements.

---

## See Also

- **QUEUE_GUIDE.md**: `LinkedList` as queue implementation, `ArrayDeque` for stack/queue patterns
- **SumOf1DArray.java**: In-place array accumulation pattern (alternative to `List`)
- **MoveZeroes.java**: `LinkedList` as queue for tracking zero positions in sliding window
- **FizzBuzz.java**: `ArrayList` for accumulating string results
- **AllConstruct.java**: `ArrayList` for memoization result collection in recursive DP
- **DependencyResolverImpl.java**: `ArrayList` for dependency tracking in topological sort
- **MetricsAggregatorImpl.java**: `ArrayList` for collecting and sorting time-window entries (P95 calculation)

---

(End of file)
