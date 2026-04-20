# Java 21 Set Implementations Guide

## Quick-Reference: Implementation Selection Matrix

| Use Case                          | Best Choice                     | Key Reason                                               | Ordering  | Thread-Safe | add/contains/remove | Null elements |
|-----------------------------------|---------------------------------|----------------------------------------------------------|-----------|-------------|---------------------|---------------|
| **Default unique elements**       | `HashSet`                       | Fastest general-purpose, O(1) avg                        | None      | ❌           | O(1) avg            | ✅ 1           |
| **Insertion-order preservation**  | `LinkedHashSet`                 | Predictable iteration, O(1) ops                          | Insertion | ❌           | O(1) avg            | ✅ 1           |
| **Sorted unique / range queries** | `TreeSet`                       | `floor()`, `ceiling()`, `subSet()`                       | Sorted    | ❌           | O(log n)            | ❌             |
| **Enum-only sets**                | `EnumSet`                       | Bit-vector backed, ultra-fast                            | Enum decl | ❌           | O(1)                | ❌             |
| **Thread-safe read-heavy**        | `CopyOnWriteArraySet`           | Snapshot iteration, no `ConcurrentModificationException` | Insertion | ✅           | O(n) add/remove     | ✅ 1           |
| **Concurrent read/write**         | `ConcurrentHashMap.newKeySet()` | Lock-striping, CAS-based                                 | None      | ✅           | O(1) avg            | ❌             |
| **Immutable constant set**        | `Set.of()`                      | Zero-overhead, thread-safe read-only                     | None      | ✅ (read)    | O(1)                | ❌             |
| **Legacy code (DO NOT use new)**  | ~~`Vector`~~ + manual dedup     | Use `HashSet` or `CopyOnWriteArraySet` instead           | None      | ✅ (slow)    | O(n)                | ✅ 1           |

### At-A-Glance Decision Flow

```
Need thread-safety?
  ├─ YES → Read-heavy, rare writes?
  │          ├─ YES → CopyOnWriteArraySet
  │          └─ NO  → ConcurrentHashMap.newKeySet()
  └─ NO  → Elements are enum constants?
             ├─ YES → EnumSet (fastest possible)
             └─ NO  → Need sorted order or range queries?
                        ├─ YES → TreeSet
                        └─ NO  → Need insertion-order iteration?
                                   ├─ YES → LinkedHashSet
                                   └─ NO  → HashSet (default choice)
```

---

## Overview

Java provides multiple `Set` implementations via `java.util` and `java.util.concurrent`. Each is optimized for different
access patterns, ordering guarantees, and thread safety requirements.

A `Set` is a collection that contains **no duplicate elements**. The interface contract guarantees that no two elements
`e1` and `e2` exist where `e1.equals(e2)` is true. Most implementations achieve this by delegating to an underlying
`Map` (e.g., `HashSet` uses `HashMap`, `TreeSet` uses `TreeMap`).

---

## 1. HashSet

```java
Set<E> set = new HashSet<>();
```

### Characteristics

| Property          | Value                                |
|-------------------|--------------------------------------|
| **Ordering**      | None (unpredictable iteration order) |
| **Null elements** | 1 allowed                            |
| **Thread-safe**   | No                                   |
| **Performance**   | O(1) average for add/contains/remove |
| **Backed by**     | `HashMap` (internal `PRESENT` dummy) |

### Complexity

| Operation           | Average     | Worst Case  |
|---------------------|-------------|-------------|
| `add(element)`      | O(1)        | O(log n)    |
| `contains(element)` | O(1)        | O(log n)    |
| `remove(element)`   | O(1)        | O(log n)    |
| Iteration           | O(capacity) | O(capacity) |

> **Backed by HashMap**: Every element is stored as a key in an internal `HashMap<E, Object>` with a shared dummy
> value (`PRESENT`). This is why `HashSet` allows 1 null (same as HashMap allows 1 null key).
>
> **Worst case note**: Since Java 8, HashMap uses balanced trees for collision chains (bin size > 8), improving worst
> case from O(n) to O(log n).

### When to Use

- **Your default choice** for uniqueness checks when ordering doesn't matter.
- **Frequency dedup**: Seen tracking in array problems — the pattern from `FindAllNumbersDisappearedInAnArray.java`.
- **Fast membership tests**: O(1) average lookup for "have we seen this?"
- **Set operations**: `retainAll`, `removeAll`, `removeIf` for bulk filtering.
- **Only for single-threaded scenarios** — it's not thread-safe.

### Magic Methods (Java 21)

```java
// Bulk operations - HashSet excels at these
Set<Integer> set = new HashSet<>(List.of(1, 2, 3, 4, 5));

// Retain only elements in another collection (intersection)
set.

retainAll(List.of(2, 4));  // set is now [2, 4]

// Remove all elements in another collection (difference)
        set.

removeAll(List.of(2));     // set is now [4]

// Remove by predicate (Java 8+)
        set.

removeIf(n ->n %2==0); // Remove even numbers

// Contains any element from another collection
boolean hasAny = Collections.disjoint(set, List.of(1, 10));  // false (4 is common)

// Convert to array
Integer[] arr = set.toArray(new Integer[0]);

// Stream operations
set.

stream()
   .

filter(n ->n >2)
        .

collect(Collectors.toSet());  // Back to HashSet
```

**Practical deduplication pattern** (from `FindAllNumbersDisappearedInAnArray.java`):

```java
// BEFORE: Manual duplicate tracking with nested loops - O(n²)
List<Integer> disappeared = new ArrayList<>();
for(
int i = 1;
i <=n;i++){
boolean found = false;
    for(
int num :nums){
        if(num ==i){
found =true;
        break;
        }
        }
        if(!found)disappeared.

add(i);
}

// AFTER: HashSet-based - O(n) time, O(n) space
Set<Integer> present = new HashSet<>();
for(
int num :nums){
        present.

add(num);  // O(1) insert
}
List<Integer> disappeared = new ArrayList<>();
for(
int i = 1;
i <=n;i++){
        if(!present.

contains(i)){  // O(1) lookup
        disappeared.

add(i);
    }
            }
            return disappeared;
```

**Top-K tracking pattern** (from `ThirdMaximumNumber.java`):

```java
// Maintain at most K elements using HashSet
Set<Integer> top3 = new HashSet<>();
for(
int num :nums){
        top3.

add(num);  // Duplicates auto-ignored
    if(top3.

size() >3){
        top3.

remove(Collections.min(top3));  // Remove smallest
        }
        }
// If exactly 3 distinct elements, return minimum (3rd max); else return maximum
        return(top3.

size() ==3)?Collections.

min(top3) :Collections.

max(top3);
```

---

## 2. LinkedHashSet

```java
Set<E> set = new LinkedHashSet<>();
```

### Characteristics

| Property          | Value                                    |
|-------------------|------------------------------------------|
| **Ordering**      | Insertion order (NOT access order)       |
| **Null elements** | 1 allowed                                |
| **Thread-safe**   | No                                       |
| **Performance**   | O(1) average, slight overhead vs HashSet |
| **Backed by**     | `LinkedHashMap` (insertion-order mode)   |

### Complexity

| Operation             | Average | Worst Case |
|-----------------------|---------|------------|
| `add/contains/remove` | O(1)    | O(log n)   |
| Iteration             | O(n)    | O(n)       |

> **Iteration efficiency**: Unlike HashSet's O(capacity), LinkedHashSet iteration is O(n) where n is the number of
> elements. This makes it more efficient when capacity >> size.
>
> **NOT access-order**: Unlike `LinkedHashMap`, `LinkedHashSet` does NOT support access-order iteration. It only
> preserves insertion order.

### When to Use

- **Predictable iteration order**: When you need deterministic output (testing, serialization).
- **Preserve input order**: Remove duplicates while maintaining the original element sequence.
- **LRU-like patterns**: Track "recently seen" with manual eviction (remove oldest by iterator).
- **Debugging**: Reproducible iteration makes debugging easier.

### Magic Methods

All `HashSet` methods apply. Additional patterns:

```java
// Remove oldest (first inserted) element
Set<String> set = new LinkedHashSet<>(List.of("a", "b", "c", "d"));
String oldest = set.iterator().next();  // "a"
set.

remove(oldest);  // Remove first

// Manual bounded set pattern (evict oldest)
Set<String> bounded = new LinkedHashSet<>();
int limit = 100;

void addWithEviction(String item) {
    if (bounded.size() >= limit && !bounded.contains(item)) {
        bounded.remove(bounded.iterator().next());  // Evict oldest
    }
    bounded.add(item);
}
```

**Practical in-place deduplication pattern** (related to `RemoveDuplicatesFromSortedArray.java`):

```java
// For sorted arrays, two-pointer is more efficient (O(1) space)
// HashSet approach pattern when input is NOT sorted:
public int[] deduplicate(int[] nums) {
    Set<Integer> seen = new LinkedHashSet<>();  // Preserve order
    for (int num : nums) {
        seen.add(num);  // Duplicates ignored, order preserved
    }
    return seen.stream().mapToInt(Integer::intValue).toArray();
}
```

---

## 3. TreeSet

```java
Set<E> set = new TreeSet<>();
// With custom comparator
Set<String> set = new TreeSet<>(Comparator.comparingInt(String::length));
```

### Characteristics

| Property          | Value                                     |
|-------------------|-------------------------------------------|
| **Ordering**      | Natural order, or custom Comparator       |
| **Null elements** | Not allowed (throws NullPointerException) |
| **Thread-safe**   | No                                        |
| **Backed by**     | `TreeMap` (internal `PRESENT` dummy)      |
| **Structure**     | Red-Black tree (self-balancing BST)       |

### Complexity

| Operation                              | Average & Worst |
|----------------------------------------|-----------------|
| `add/contains/remove`                  | O(log n)        |
| Iteration                              | O(n)            |
| `first()` / `last()`                   | O(log n)        |
| `floor()` / `ceiling()`                | O(log n)        |
| `lower()` / `higher()`                 | O(log n)        |
| `subSet()` / `headSet()` / `tailSet()` | O(log n)        |

### When to Use

- **Sorted unique elements**: Automatically maintain elements in sorted order.
- **Range queries**: Find all elements between values X and Y.
- **Nearest-value lookups**: `floor()`, `ceiling()`, `lower()`, `higher()`.
- **Top-K with ordering**: Track K largest/smallest in sorted order.
- **Find min/max efficiently**: `first()` and `last()` give sorted extremes.

**DO NOT use** for simple uniqueness — O(log n) vs HashSet's O(1).

### Magic Methods

```java
TreeSet<Integer> set = new TreeSet<>(List.of(1, 3, 5, 7, 9));

// Range queries (NavigableSet interface - TreeSet implements this)
SortedSet<Integer> range = set.subSet(3, true, 7, true);    // [3, 5, 7]
SortedSet<Integer> head = set.headSet(5, true);  // [1, 3, 5]
SortedSet<Integer> tail = set.tailSet(5, true);  // [5, 7, 9]

// Nearest-value lookups
Integer floor = set.floor(6);    // 5 (greatest element <= 6)
Integer ceiling = set.ceiling(6); // 7 (least element >= 6)
Integer lower = set.lower(5);     // 3 (greatest element < 5)
Integer higher = set.higher(5);   // 7 (least element > 5)

// First/last elements
Integer first = set.first();   // 1
Integer last = set.last();     // 9

// Remove and return first/last
Integer removed = set.pollFirst();  // Remove and return 1
Integer removedLast = set.pollLast(); // Remove and return 9

// Reverse order view
NavigableSet<Integer> reversed = set.descendingSet();  // [9, 7, 5, 3, 1]
```

**Practical sorted Top-K pattern** (alternative to `ThirdMaximumNumber.java`):

```java
// TreeSet-based Top-3 - more elegant than HashSet + Collections.min/max
public int thirdMax(int[] nums) {
    TreeSet<Integer> top3 = new TreeSet<>();  // Auto-sorted

    for (int num : nums) {
        top3.add(num);  // Duplicates auto-ignored
        if (top3.size() > 3) {
            top3.remove(top3.first());  // Remove smallest (first)
        }
    }

    return (top3.size() == 3) ? top3.first() : top3.last();
}
```

**Range query example**:

```java
TreeSet<LocalDateTime> timestamps = new TreeSet<>();
timestamps.

add(LocalDateTime.of(2024, 1,1,10,0));
        timestamps.

add(LocalDateTime.of(2024, 1,1,12,0));
        timestamps.

add(LocalDateTime.of(2024, 1,1,14,0));
        timestamps.

add(LocalDateTime.of(2024, 1,1,16,0));

// Find timestamps between 11:00 and 15:00
SortedSet<LocalDateTime> window = timestamps.subSet(
        LocalDateTime.of(2024, 1, 1, 11, 0), true,
        LocalDateTime.of(2024, 1, 1, 15, 0), true
);
// Result: [12:00, 14:00]
```

---

## 4. EnumSet

```java
Set<DayOfWeek> set = EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
// All values
Set<DayOfWeek> all = EnumSet.allOf(DayOfWeek.class);
// Range of enum constants
Set<DayOfWeek> range = EnumSet.range(DayOfWeek.MONDAY, DayOfWeek.FRIDAY);
// Complement of another EnumSet
Set<DayOfWeek> complement = EnumSet.complementOf(weekdays);
```

### Characteristics

| Property          | Value                                              |
|-------------------|----------------------------------------------------|
| **Ordering**      | Enum declaration order                             |
| **Null elements** | Not allowed (throws NullPointerException)          |
| **Thread-safe**   | No                                                 |
| **Performance**   | O(1) - bit-vector operations (faster than HashSet) |
| **Memory**        | Extremely compact - one bit per enum constant      |
| **Restriction**   | ALL elements must be from SAME enum type           |

### Complexity

| Operation             | Time                               |
|-----------------------|------------------------------------|
| `add/contains/remove` | O(1)                               |
| Iteration             | O(n) where n = enum constant count |

> **Bit-vector backed**: `EnumSet` uses a single `long` (for enums with <= 64 constants) or `long[]` array for larger
> enums. Each enum constant maps to a bit position. Operations are bitwise: `add` = OR, `remove` = AND-NOT, `contains` =
> bit test.

### When to Use

- **Enum-only sets**: You have an `enum` and need a set of its values.
- **Flag combinations**: Alternative to `@BitFlags` or bit-masks — more type-safe.
- **Performance-critical enum operations**: Faster than `HashSet<YourEnum>`.
- **Memory-constrained scenarios**: Minimal footprint.

### Magic Methods

```java
enum Status {NEW, IN_PROGRESS, REVIEW, DONE, BLOCKED}

// Create sets of enum values
Set<Status> active = EnumSet.of(Status.NEW, Status.IN_PROGRESS, Status.REVIEW);
Set<Status> terminal = EnumSet.of(Status.DONE, Status.BLOCKED);
Set<Status> all = EnumSet.allOf(Status.class);

// Set operations
Set<Status> complement = EnumSet.complementOf(active);  // [REVIEW, DONE, BLOCKED]
Set<Status> range = EnumSet.range(Status.IN_PROGRESS, Status.BLOCKED);  // [IN_PROGRESS, REVIEW, DONE, BLOCKED]

// Copy
Set<Status> copy = EnumSet.copyOf(active);

// Usage in filtering
List<Task> tasks = ...;
        tasks.

stream()
    .

filter(t ->active.

contains(t.getStatus()))
        .

collect(Collectors.toList());
```

**Practical flag pattern** (type-safe alternative to bit-flags):

```java
enum Permission {READ, WRITE, EXECUTE, ADMIN}

class FileResource {
    private final Set<Permission> permissions = EnumSet.noneOf(Permission.class);

    void addPermission(Permission p) {
        permissions.add(p);
    }

    boolean hasPermission(Permission p) {
        return permissions.contains(p);  // O(1) check
    }

    boolean hasAllPermissions(Set<Permission> required) {
        return permissions.containsAll(required);
    }
}
```

---

## 5. CopyOnWriteArraySet

```java
Set<E> set = new CopyOnWriteArraySet<>();
```

### Characteristics

| Property          | Value                                        |
|-------------------|----------------------------------------------|
| **Ordering**      | Insertion order                              |
| **Null elements** | 1 allowed                                    |
| **Thread-safe**   | Yes (snapshot isolation on iteration)        |
| **Performance**   | O(n) add/remove; O(1) iteration (no locking) |
| **Backed by**     | `CopyOnWriteArrayList`                       |
| **Best for**      | Read-heavy, rare writes                      |

### Complexity

| Operation           | Time |
|---------------------|------|
| `add(element)`      | O(n) |
| `contains(element)` | O(n) |
| `remove(element)`   | O(n) |
| Iteration           | O(n) |

> **Copy-on-Write semantics**: Every mutating operation (add/remove) creates a fresh copy of the underlying array. *
*Reads lock-free** — iterators work on a snapshot and never throw `ConcurrentModificationException`. Expensive for
> writes, ideal for read-heavy scenarios.

### When to Use

- **Read-heavy, write-rare**: Configuration sets, listener registries.
- **Safe iteration during modification**: Iterate without `ConcurrentModificationException`.
- **Listener patterns**: Add/remove listeners while notifying them.
- **Small set sizes**: O(n) cost is acceptable for small n.

**DO NOT use** for:

- High-write scenarios (copy overhead kills performance)
- Large sets (copying large arrays is expensive)
- When `ConcurrentHashMap.newKeySet()` suffices

### Magic Methods

```java
Set<String> set = new CopyOnWriteArraySet<>(List.of("a", "b", "c"));

// Safe iteration during modification
for(
String item :set){
        set.

add(item.toUpperCase());  // Doesn't affect current iteration
        }
// Iteration sees original snapshot; new elements added after

// Bulk operations (all create new array copies)
        set.

addAll(List.of("d", "e"));
        set.

removeAll(List.of("a", "b"));
        set.

retainAll(List.of("a", "c"));

// Contains any
boolean hasAny = !Collections.disjoint(set, List.of("x", "y"));
```

**Practical listener registry pattern**:

```java
class EventDispatcher {
    private final Set<EventListener> listeners = new CopyOnWriteArraySet<>();

    void addListener(EventListener listener) {
        listeners.add(listener);  // Thread-safe
    }

    void removeListener(EventListener listener) {
        listeners.remove(listener);  // Thread-safe
    }

    void fireEvent(Event event) {
        // Safe iteration - even if listeners modify during callback
        for (EventListener listener : listeners) {
            listener.onEvent(event);  // Listener may add/remove itself
        }
    }

    interface EventListener {
        void onEvent(Event event);
    }
}
```

---

## 6. ConcurrentHashMap.newKeySet()

```java
Set<E> set = ConcurrentHashMap.newKeySet();
// With initial capacity
Set<E> set = ConcurrentHashMap.newKeySet(64);
```

### Characteristics

| Property          | Value                                            |
|-------------------|--------------------------------------------------|
| **Ordering**      | None                                             |
| **Null elements** | Not allowed                                      |
| **Thread-safe**   | Yes (CAS-based, lock-striping)                   |
| **Performance**   | O(1) average for add/contains/remove             |
| **Backed by**     | `ConcurrentHashMap` (similar to HashSet→HashMap) |
| **Best for**      | Concurrent read/write scenarios                  |

### Complexity

| Operation             | Average |
|-----------------------|---------|
| `add/contains/remove` | O(1)    |
| Iteration             | O(n)    |

> **Thread safety via CAS**: Uses compare-and-swap operations for fine-grained locking (not wholesale locking like
`Collections.synchronizedSet()`). More efficient than wrapping a `HashSet` with `Collections.synchronizedSet()`.

### When to Use

- **Concurrent read/write access**: Multiple threads adding/removing.
- **High-throughput parallel processing**: Better than `Collections.synchronizedSet()`.
- **Atomic compound operations**: via backing map's atomic methods.

**NEVER use** `Collections.synchronizedSet(new HashSet<>())` when this suffices.

### Magic Methods

```java
Set<String> set = ConcurrentHashMap.newKeySet();

// All standard Set operations are thread-safe
set.

add("item");
set.

contains("item");
set.

remove("item");

// Bulk operations (iterators are weakly consistent)
set.

addAll(List.of("a", "b","c"));

// Safe iteration during concurrent modification
        for(
String item :set){

process(item);  // Other threads can add/remove without CME
}

// Parallel bulk operations (Java 8+)
        set.

parallelStream().

forEach(this::expensiveProcess);
```

**Practical concurrent seen-tracker pattern**:

```java
// Concurrent frequency counting across threads
class ConcurrentVisitorTracker {
    private final Set<String> visitedUrls = ConcurrentHashMap.newKeySet();

    void recordVisit(String url) {
        visitedUrls.add(url);  // Thread-safe
    }

    boolean hasVisited(String url) {
        return visitedUrls.contains(url);
    }

    Set<String> getVisitedUrls() {
        return Collections.unmodifiableSet(visitedUrls);
    }
}
```

---

## 7. Immutable Sets (Set.of / Set.copyOf)

```java
Set<String> set = Set.of("a", "b", "c");  // Up to 10 elements
Set<String> largeSet = Set.ofEntries("a", "b", /* ... */);  // More than 10
Set<String> empty = Set.of();  // Empty immutable set
Set<String> copy = Set.copyOf(existing);  // Immutable copy
```

### Characteristics

| Property          | Value                                              |
|-------------------|----------------------------------------------------|
| **Ordering**      | None (implementation-defined)                      |
| **Null elements** | Not allowed                                        |
| **Thread-safe**   | Yes (immutable = inherently safe)                  |
| **Performance**   | Zero-overhead after creation                       |
| **Mutability**    | Immutable (throws `UnsupportedOperationException`) |

### When to Use

- **Constant sets**: Predefined collections of values.
- **Return values**: Prevent caller modification.
- **Configuration**: Fixed sets of options, permissions, etc.
- **Thread-safe without synchronization**: Immutability guarantees safety.

---

## Decision Matrix

| Requirement                  | Choose                            |
|------------------------------|-----------------------------------|
| Default unique elements      | **HashSet**                       |
| Insertion-order iteration    | **LinkedHashSet**                 |
| Sorted order / range queries | **TreeSet**                       |
| Enum constants only          | **EnumSet**                       |
| Read-heavy concurrent access | **CopyOnWriteArraySet**           |
| Read/write concurrent access | **ConcurrentHashMap.newKeySet()** |
| Immutable constant set       | **Set.of()**                      |

---

## Performance Summary

| Implementation                | add/contains/remove | Thread-Safe | Ordering  | Nulls | Backed By                |
|-------------------------------|---------------------|-------------|-----------|-------|--------------------------|
| HashSet                       | O(1) avg            | No          | None      | 1     | HashMap                  |
| LinkedHashSet                 | O(1) avg            | No          | Insertion | 1     | LinkedHashMap            |
| TreeSet                       | O(log n)            | No          | Sorted    | ❌     | TreeMap                  |
| EnumSet                       | O(1)                | No          | Enum decl | ❌     | Bit-vector               |
| CopyOnWriteArraySet           | O(n)                | Yes         | Insertion | 1     | CopyOnWriteArrayList     |
| ConcurrentHashMap.newKeySet() | O(1) avg            | Yes         | None      | ❌     | ConcurrentHashMap        |
| Set.of() (immutable)          | N/A (throws)        | Yes (read)  | None      | ❌     | Compact immutable struct |

---

## Java 21 SequencedSet (JEP 431)

Java 21 introduced the `SequencedSet` interface (JEP 431), providing consistent APIs for sets with a well-defined
encounter order. `LinkedHashSet` and `TreeSet` implement this.

```java
SequencedSet<String> ss = new LinkedHashSet<>();
ss.

add("a");
ss.

add("b");
ss.

add("c");

ss.

getFirst();    // "a" - first element in iteration order
ss.

getLast();     // "c" - last element in iteration order
ss.

removeFirst(); // Remove and return "a"
ss.

removeLast();  // Remove and return "c"

SequencedSet<String> reversed = ss.reversed();  // Reverse-order view
```

> `reversed()` returns a **view** — it's backed by the original set. Modifications to the reversed view affect the
> original set. The view is created in O(1); iteration is O(n) in reverse order.
>
> **Note**: `HashSet` does NOT implement `SequencedSet` because it has no defined encounter order.

---

## Common Gotchas

1. **HashSet iteration order is NOT stable**: May change when rehashing occurs. Use `LinkedHashSet` for predictable
   order.

2. **TreeSet rejects null elements**: Throws `NullPointerException`. Use `HashSet` or `LinkedHashSet` if nulls are
   needed.

3. **TreeSet requires Comparable or custom Comparator**: Elements must be mutually comparable, or `ClassCastException`
   is thrown.

4. **Null handling differs across implementations**:
    - `HashSet`, `LinkedHashSet`, `CopyOnWriteArraySet`: Allow 1 null
    - `TreeSet`, `EnumSet`, `ConcurrentHashMap.newKeySet()`: Reject nulls

5. **`ConcurrentModificationException` on HashSet during iteration**:
   ```java
   Set<Integer> set = new HashSet<>(List.of(1, 2, 3));
   for (Integer i : set) {
       set.remove(i);  // ❌ Throws CME
   }
   // FIX: Use iterator or removeIf
   set.removeIf(i -> i % 2 == 0);  // ✅ Safe
   Iterator<Integer> it = set.iterator();
   while (it.hasNext()) {
       if (it.hasNext() && it.next() % 2 == 0) {
           it.remove();  // ✅ Safe via iterator
       }
   }
   ```

6. **`CopyOnWriteArraySet` is expensive for writes**: Every add/remove copies the entire array. Only use for read-heavy
   scenarios.

7. **`EnumSet` requires all elements from same enum type**: Cannot mix enum types; will fail at compile time.

8. **`Set.of()` is immutable**: Any mutation attempt throws `UnsupportedOperationException`.

9. **TreeSet `subSet()` views are live**: Changes to the backing set are reflected in the view (and vice versa).

10. **HashSet performance degrades with poor `hashCode()`**: Elements with colliding hash codes end up in same bin.
    Always implement `hashCode()` and `equals()` correctly.

---

## See Also

- **[MAP_GUIDE.md](MAP_GUIDE.md)**: HashSet is backed by HashMap, TreeSet by TreeMap — understand the Map internals for
  deep Set knowledge.
- **[QUEUE_GUIDE.md](QUEUE_GUIDE.md)**: For priority-based unique collections, compare `PriorityQueue` (allows
  duplicates) vs `TreeSet` (unique, sorted).
- **[development/PREPARATION.md](development/PREPARATION.md)**: Java Collections decision tree includes Set selection
  guidance.

---

## Practical Challenge Patterns Summary

### Pattern 1: Find Missing Elements (from FindAllNumbersDisappearedInAnArray.java)

```java
Set<Integer> present = new HashSet<>();
for(
int num :nums){
        present.

add(num);
}
List<Integer> missing = new ArrayList<>();
for(
int i = 1;
i <=n;i++){
        if(!present.

contains(i)){
        missing.

add(i);
    }
            }
            return missing;
```

### Pattern 2: Track Top-K Distinct Elements (from ThirdMaximumNumber.java)

```java
// HashSet approach
Set<Integer> top3 = new HashSet<>();
for(
int num :nums){
        top3.

add(num);
    if(top3.

size() >3){
        top3.

remove(Collections.min(top3));
        }
        }

// TreeSet approach (cleaner)
TreeSet<Integer> top3 = new TreeSet<>();
for(
int num :nums){
        top3.

add(num);
    if(top3.

size() >3){
        top3.

remove(top3.first());
        }
        }
```

### Pattern 3: Remove Duplicates While Preserving Order

```java
// LinkedHashSet preserves insertion order
Set<Integer> seen = new LinkedHashSet<>();
for(
int num :nums){
        seen.

add(num);
}
// Convert back to list/array - order preserved
List<Integer> deduped = new ArrayList<>(seen);
```
