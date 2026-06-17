# Iterator Pattern

## Overview

**Definition**: Iterator provides a way to access the elements of an aggregate object sequentially without exposing its
underlying representation (list, tree, array, set, etc.).

**Core Problem**: How to traverse different data structures uniformly without exposing their internal storage details,
while supporting multiple concurrent traversals.

**One-Line Interview Answer**: "Iterator separates the traversal logic from the collection, providing a uniform way to
access elements sequentially regardless of the underlying data structure."

## Problem Statement

### Real-World Scenario: Report Generator

A report generator needs to traverse data from various sources: a database result set, a list of transactions, a tree of
organizational hierarchy, and a custom graph of dependencies. Each data source has a different structure and requires
different traversal code:

```java
public class ReportGenerator {
    public void generateReport(Object dataSource) {
        if (dataSource instanceof List) {
            List<?> list = (List<?>) dataSource;
            for (int i = 0; i < list.size(); i++) {
                process(list.get(i));
            }
        } else if (dataSource instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) dataSource;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                process(entry.getValue());
            }
        } else if (dataSource instanceof TreeNode) {
            traverseTree((TreeNode) dataSource); // recursive
        } else if (dataSource instanceof ResultSet) {
            ResultSet rs = (ResultSet) dataSource;
            while (rs.next()) {
                process(rs.getObject(1));
            }
        }
        // Each new data type requires a new traversal implementation
    }
}
```

### Pain Points of the Naive Approach

1. **Exposed Internals**: Clients must know whether the collection uses arrays, linked lists, or trees. Changes to the
   internal structure break all traversal code.
2. **Duplicate Traversal Logic**: Every client that needs to iterate reimplements the same traversal algorithm.
3. **No Concurrent Traversal**: If the collection is modified during iteration, you get
   `ConcurrentModificationException` — no built-in snapshot strategy.
4. **Cannot Traverse the Same Collection Multiple Times**: Two nested loops over the same collection require independent
   cursors.
5. **No Standard Abstractions**: Each data structure has a different iteration API (index, cursor, recursive callback).

### Why This Matters in Production

The Iterator pattern is so fundamental that Java made it a language-level feature via `Iterable<T>` and the enhanced
for-each loop. Every Java developer uses it daily. Understanding how to implement custom iterators is essential for
building custom collections, tree traversals, and streaming APIs.

## Solution

### How Iterator Solves This

Iterator defines a standard interface with `hasNext()` and `next()` methods. Each collection provides an iterator via
`iterator()`. The client uses these methods uniformly, regardless of the underlying structure.

### Key Participants

| Participant            | Role                                                                           |
|------------------------|--------------------------------------------------------------------------------|
| `Iterator` (interface) | Declares `hasNext()`, `next()`, optionally `remove()` and `forEachRemaining()` |
| `ConcreteIterator`     | Implements traversal logic for a specific collection                           |
| `Iterable` (interface) | Declares `iterator()` method that returns an Iterator                          |
| `ConcreteCollection`   | Implements `iterator()` returning the appropriate ConcreteIterator             |

### Step-by-Step Flow

1. Client calls `collection.iterator()` to get an Iterator object
2. In a loop, client calls `iterator.hasNext()` to check for more elements
3. If true, client calls `iterator.next()` to get the next element
4. The iterator maintains a cursor (or stack) tracking the current position
5. Multiple iterators can exist independently on the same collection

### UML-Style Structure

```
┌──────────────┐       ┌──────────────────┐
│ «interface»  │       │ «interface»      │
│  Iterable    │       │   Iterator       │
│              │       │                  │
│ +iterator()  │──────→│ +hasNext():bool  │
└──────────────┘       │ +next(): T       │
                        └────────┬─────────┘
                                 │
                     ┌───────────┴───────────┐
                     │ ConcreteIterator      │
                     │                       │
                     │ -cursor               │
                     │ +hasNext()            │
                     │ +next()               │
                     └───────────────────────┘
```

## Java Implementation

### Custom Iterator for a Binary Search Tree

```java
package behavioral.iterator;

import java.util.*;

// Binary Search Tree with Iterator support
class TreeNode<T extends Comparable<T>> {
    T value;
    TreeNode<T> left;
    TreeNode<T> right;

    public TreeNode(T value) {
        this.value = value;
    }
}

class BinarySearchTree<T extends Comparable<T>> implements Iterable<T> {
    private TreeNode<T> root;

    public void insert(T value) {
        root = insertRec(root, value);
    }

    private TreeNode<T> insertRec(TreeNode<T> node, T value) {
        if (node == null) return new TreeNode<>(value);
        if (value.compareTo(node.value) < 0) {
            node.left = insertRec(node.left, value);
        } else if (value.compareTo(node.value) > 0) {
            node.right = insertRec(node.right, value);
        }
        return node;
    }

    @Override
    public Iterator<T> iterator() {
        return new InOrderIterator();
    }

    // Return different iterator implementations for different traversals
    public Iterator<T> preOrderIterator() {
        return new PreOrderIterator();
    }

    public Iterator<T> postOrderIterator() {
        return new PostOrderIterator();
    }

    // In-Order Iterator (LNR): left → node → right
    private class InOrderIterator implements Iterator<T> {
        private final Deque<TreeNode<T>> stack = new ArrayDeque<>();
        private TreeNode<T> current = root;

        InOrderIterator() {
            // Push all left nodes to simulate recursion start
            pushLeftBranch(current);
        }

        private void pushLeftBranch(TreeNode<T> node) {
            while (node != null) {
                stack.push(node);
                node = node.left;
            }
        }

        @Override
        public boolean hasNext() {
            return !stack.isEmpty();
        }

        @Override
        public T next() {
            if (!hasNext()) throw new NoSuchElementException();
            TreeNode<T> node = stack.pop();
            // After visiting the node, go right and then all the way left
            pushLeftBranch(node.right);
            return node.value;
        }
    }

    // Pre-Order Iterator (NLR): node → left → right
    private class PreOrderIterator implements Iterator<T> {
        private final Deque<TreeNode<T>> stack = new ArrayDeque<>();

        PreOrderIterator() {
            if (root != null) stack.push(root);
        }

        @Override
        public boolean hasNext() {
            return !stack.isEmpty();
        }

        @Override
        public T next() {
            if (!hasNext()) throw new NoSuchElementException();
            TreeNode<T> node = stack.pop();
            // Push right first so left is processed first (LIFO)
            if (node.right != null) stack.push(node.right);
            if (node.left != null) stack.push(node.left);
            return node.value;
        }
    }

    // Post-Order Iterator (LRN): left → right → node
    private class PostOrderIterator implements Iterator<T> {
        private final Deque<TreeNode<T>> stack = new ArrayDeque<>();
        private TreeNode<T> lastVisited;

        PostOrderIterator() {
            pushLeftBranch(root);
        }

        private void pushLeftBranch(TreeNode<T> node) {
            while (node != null) {
                stack.push(node);
                node = node.left;
            }
        }

        @Override
        public boolean hasNext() {
            return !stack.isEmpty();
        }

        @Override
        public T next() {
            while (!stack.isEmpty()) {
                TreeNode<T> node = stack.peek();
                // If right child exists and hasn't been visited, go right
                if (node.right != null && node.right != lastVisited) {
                    pushLeftBranch(node.right);
                } else {
                    // Visit this node
                    stack.pop();
                    lastVisited = node;
                    return node.value;
                }
            }
            throw new NoSuchElementException();
        }
    }
}
```

### Custom Iterator for a Circular Buffer

```java
import java.util.Iterator;
import java.util.NoSuchElementException;

class CircularBuffer<T> implements Iterable<T> {
    private final Object[] buffer;
    private int head;
    private int size;

    public CircularBuffer(int capacity) {
        this.buffer = new Object[capacity];
        this.head = 0;
        this.size = 0;
    }

    public void add(T item) {
        if (size == buffer.length) {
            // Overwrite oldest
            head = (head + 1) % buffer.length;
        } else {
            size++;
        }
        int tail = (head + size - 1) % buffer.length;
        buffer[tail] = item;
    }

    @Override
    public Iterator<T> iterator() {
        return new CircularBufferIterator();
    }

    // Snapshot iterator — captures elements at creation time
    private class CircularBufferIterator implements Iterator<T> {
        private final Object[] snapshot;
        private int cursor = 0;

        @SuppressWarnings("unchecked")
        CircularBufferIterator() {
            snapshot = new Object[size];
            for (int i = 0; i < size; i++) {
                snapshot[i] = buffer[(head + i) % buffer.length];
            }
        }

        @Override
        public boolean hasNext() {
            return cursor < snapshot.length;
        }

        @Override
        @SuppressWarnings("unchecked")
        public T next() {
            if (!hasNext()) throw new NoSuchElementException();
            return (T) snapshot[cursor++];
        }
    }
}
```

### Fail-Fast vs Fail-Safe Iterator

```java
import java.util.concurrent.CopyOnWriteArrayList;

class IteratorBehaviorDemo {
    public static void main(String[] args) {
        // Fail-Fast: throws ConcurrentModificationException if collection modified during iteration
        var failFast = new ArrayList<>(List.of("A", "B", "C"));
        try {
            for (String s : failFast) {
                if (s.equals("B")) failFast.remove(s); // Throws!
            }
        } catch (ConcurrentModificationException e) {
            System.out.println("Fail-fast detected modification: " + e);
        }

        // Fail-Safe: iterates over a snapshot; safe under modification
        var failSafe = new CopyOnWriteArrayList<>(List.of("A", "B", "C"));
        for (String s : failSafe) {
            if (s.equals("B")) failSafe.remove(s); // Safe
            System.out.println("Reading: " + s);
        }
        System.out.println("After removal: " + failSafe);
    }
}
```

### Lazy Iterator for Paginated API

```java
import java.util.Iterator;
import java.util.List;

// Iterator that lazily fetches pages from an API
class PaginatedApiIterator implements Iterator<Record> {
    private final ApiClient client;
    private List<Record> currentPage;
    private int cursor;
    private int totalPages;
    private int currentPageNum;

    public PaginatedApiIterator(ApiClient client) {
        this.client = client;
        this.currentPageNum = 1;
        fetchPage();
    }

    private void fetchPage() {
        var response = client.fetchPage(currentPageNum);
        currentPage = response.records();
        totalPages = response.totalPages();
        cursor = 0;
    }

    @Override
    public boolean hasNext() {
        if (cursor < currentPage.size()) return true;
        if (currentPageNum < totalPages) {
            currentPageNum++;
            fetchPage();
            return !currentPage.isEmpty();
        }
        return false;
    }

    @Override
    public Record next() {
        if (!hasNext()) throw new NoSuchElementException();
        return currentPage.get(cursor++);
    }
}

record ApiClient() {
    ApiResponse fetchPage(int pageNum) {
        return new ApiResponse(List.of(new Record("data-" + pageNum)), 3);
    }
}
record ApiResponse(List<Record> records, int totalPages) {}
record Record(String data) {}
```

### Java's Built-in Iterator: Iterable + enhanced for-each

```java
class BuiltInIterationDemo {
    public static void main(String[] args) {
        // All Java Collections are Iterable
        List<String> list = List.of("Java", "Python", "Rust");
        Set<Integer> set = Set.of(1, 2, 3);
        Queue<Double> queue = new ArrayDeque<>(List.of(1.0, 2.0, 3.0));

        // Enhanced for-each works on all Iterables
        for (String lang : list) System.out.println(lang);

        // Manual iterator (equivalent to for-each)
        Iterator<Integer> it = set.iterator();
        while (it.hasNext()) {
            System.out.println(it.next());
        }

        // Java 8+ forEach with lambda
        queue.forEach(System.out::println);
    }
}
```

### Usage Demo

```java
public class IteratorDemo {
    public static void main(String[] args) {
        // BST with different traversals
        var bst = new BinarySearchTree<Integer>();
        bst.insert(5);
        bst.insert(3);
        bst.insert(7);
        bst.insert(2);
        bst.insert(4);
        bst.insert(6);
        bst.insert(8);

        System.out.println("In-Order (sorted):");
        bst.forEach(v -> System.out.print(v + " "));
        System.out.println();

        System.out.println("Pre-Order:");
        for (var it = bst.preOrderIterator(); it.hasNext(); ) {
            System.out.print(it.next() + " ");
        }
        System.out.println();

        System.out.println("Post-Order:");
        for (var it = bst.postOrderIterator(); it.hasNext(); ) {
            System.out.print(it.next() + " ");
        }
        System.out.println();

        // Multiple simultaneous iterators
        System.out.println("\nMultiple iterators (same tree):");
        var it1 = bst.iterator();
        var it2 = bst.iterator();
        System.out.print("It1: ");
        while (it1.hasNext()) {
            System.out.print(it1.next() + " ");
            if (it2.hasNext()) System.out.print("[It2: " + it2.next() + "] ");
        }
        System.out.println();

        // Circular buffer
        var buffer = new CircularBuffer<String>(3);
        buffer.add("A");
        buffer.add("B");
        buffer.add("C");
        buffer.add("D"); // overwrites A
        System.out.println("\nCircular buffer:");
        for (String s : buffer) System.out.print(s + " ");
        System.out.println();

        // Snapshot iterator — modification safe
        var safeList = new CopyOnWriteArrayList<>(List.of("X", "Y", "Z"));
        for (String s : safeList) {
            if (s.equals("Y")) safeList.add("W"); // No ConcurrentModificationException
            System.out.println(s);
        }
    }
}
```

## When to Use

1. **Custom Collections**: When you build a custom data structure (tree, graph, circular buffer, sparse matrix),
   implementing `Iterable<T>` makes it usable with the enhanced for-each loop and the Stream API.

2. **Lazy / Infinite Sequences**: When the dataset is too large to fit in memory (paginated API results, database
   cursor, infinite sequence). The iterator fetches elements on demand.

3. **Multiple Traversal Algorithms**: When a collection supports multiple traversal orders (BST: in-order, pre-order,
   post-order; Graph: BFS, DFS). Each traversal is a different Iterator class.

4. **Snapshot Iteration**: When you need to iterate over a collection that might be modified concurrently. A snapshot
   iterator captures the state at creation time.

5. **Uniform API Over Heterogeneous Data**: When you want to iterate over different data structures (arrays, lists, SQL
   result sets, files) with the same API pattern.

### Framework Examples

- **`java.util.Iterator<T>` and `java.util.Iterable<T>`**: Foundation of all Java collections iteration.
- **`java.util.stream.Stream<T>.iterator()`**: Converts a stream back to an iterator for imperative traversal.
- **`java.util.Scanner`**: Implements `Iterator<String>` for tokenized input traversal.
- **`java.sql.ResultSet`**: Though not implementing `Iterator`, it follows the same pattern: `next()` returns boolean,
  `getXxx()` retrieves data.

## When NOT to Use

1. **Simple Array Access**: If you're just iterating an array, use the enhanced for-each. Creating a custom Iterator for
   a simple array is over-engineering.

2. **Random Access Patterns**: If you need random access (index-based or key-based lookup), Iterator is the wrong tool.
   It's sequential only.

3. **CPU-Bound, High-Frequency Iteration**: Iterator adds virtual method call overhead per element. In tight loops (
   image processing, scientific computing), direct indexed access is faster.

4. **Single Traversal with No Abstraction Need**: If you traverse the collection once in one place and don't need to
   abstract over different collection types, direct iteration is fine.

5. **Endless Caution with `remove()`**: The optional `remove()` on Iterator is complex (must be called after `next()`
   exactly once). Prefer collection-level `removeIf()` or explicit removal via `ListIterator`.

## Interview Questions

### Q1: What is the Iterator pattern and how does it relate to `Iterable`?

**Answer**: Iterator provides sequential access to elements of a collection without exposing its internal structure.
`Iterable<T>` is the source that produces `Iterator<T>` objects. The enhanced for-each loop (`for (T x : collection)`)
works on any `Iterable`, calling `iterator()` and using `hasNext()`/`next()` internally.

### Q2: What is `ConcurrentModificationException` and how does it work?

**Answer**: Java's collection iterators are "fail-fast": they track a modification count. If the collection is
structurally modified after the iterator is created (except via the iterator's own `remove()`), the iterator throws
`ConcurrentModificationException`. This detects bugs early but is not guaranteed for all concurrent modifications.

### Q3: What's the difference between fail-fast and fail-safe iterators?

**Answer**: Fail-fast iterators throw `ConcurrentModificationException` on concurrent modification (`ArrayList`,
`HashMap`). Fail-safe iterators iterate over a snapshot or copy, allowing modification without exception (
`CopyOnWriteArrayList`, `ConcurrentHashMap`). Fail-safe uses more memory but is safe under concurrency.

### Q4: How would you implement an Iterator that supports `remove()`?

**Answer**: Track a `lastReturned` field set by `next()`. In `remove()`, check that `lastReturned` is valid (not null
and not already removed), then remove the element at that position via the backing collection, and set `lastReturned` to
null. Throw `IllegalStateException` if `next()` hasn't been called or `remove()` already called.

### Q5: How does the Iterator pattern relate to the Single Responsibility Principle?

**Answer**: Iterator separates traversal logic (the Iterator class) from storage logic (the Collection). The collection
manages data; the iterator manages traversal. Each has a single responsibility. You can add new traversal strategies
without modifying the collection.

### Q6: What is `ListIterator` and how does it extend `Iterator`?

**Answer**: `ListIterator<E>` extends `Iterator<E>`, adding `hasPrevious()`, `previous()`, `nextIndex()`,
`previousIndex()`, `set(E)`, and `add(E)`. It enables bidirectional traversal and modification, but only works on `List`
implementations.

### Q7: How would you iterate over a large database result set without loading everything into memory?

**Answer**: Implement a lazy `Iterator<T>` that fetches rows in batches from the database cursor. `hasNext()` checks if
there are more rows in the current batch or fetches the next batch via JDBC. `next()` returns one mapped row object.
This keeps memory usage O(batchSize) regardless of total rows.

### Q8: How does the enhanced for-each loop work with arrays vs Iterables?

**Answer**: For arrays, the compiler generates an indexed for loop (`for (int i = 0; i < arr.length; i++)`). For
`Iterable`, it generates an iterator loop (
`Iterator<T> it = iterable.iterator(); while (it.hasNext()) { T x = it.next(); }`). The syntax is identical; the
compiled bytecode differs.

### Follow-Up Question

**Interviewer**: "Design an iterator that traverses a binary tree level by level (breadth-first)."

**Answer**: Use a `Queue<TreeNode>` initialized with the root. `next()` dequeues a node, enqueues its left/right
children, and returns the dequeued value. `hasNext()` checks if the queue is non-empty. This is BFS/level-order
traversal, easily implemented alongside the DFS iterators shown earlier.

## Pros & Cons

### Advantages

- **Uniform Interface**: Same `hasNext()`/`next()` for all data structures
- **Multiple Traversals**: Independent iterators over the same collection
- **Encapsulation**: Internal structure of the collection is hidden
- **Lazy Evaluation**: Supports lazy/infinite sequences without pre-computation
- **Single Responsibility**: Separate classes for data storage and traversal
- **Language Support**: Enhanced for-each and forEach() lambda in Java

### Disadvantages

- **Overhead**: Iterator objects consume memory and CPU for method dispatch
- **Sequential Only**: No random access; must iterate to find an element
- **Stateful**: Iterators are stateful and can't be reset (must request a new one)
- **Complexity for Simple Iteration**: Array indexing is simpler and faster
- **Concurrent Modification Handling**: Fail-fast vs fail-safe complexity

## Related Patterns

### Iterator vs Composite

**Composite** represents tree structures. **Iterator** can traverse them. You often apply Iterator to Composite
structures. A tree of UI components (Composite) uses Iterator to recursively traverse all children for rendering. The
Iterator hides the Composite's recursive structure behind a flat sequence.

### Iterator vs Visitor

**Visitor** performs an operation on each element of a structure. **Iterator** provides sequential access to those
elements. You can combine them: iterate over the structure with Iterator and apply a Visitor to each element. The
Iterator manages traversal; the Visitor manages the operation.

### Iterator vs Stream API

**Stream** is the modern, functional evolution of Iterator. Streams support laziness, parallel processing, and
declarative operations (map, filter, reduce). Iterator is imperative (hasNext/next loop). Use Iterator when you need
explicit control or when the data source is inherently stateful (database cursor).

## Key Takeaways

1. **`Iterable<T>` + enhanced for-each** — This is the most widely used pattern in Java. Every Java developer benefits
   from it daily.

2. **Three things to implement** — `hasNext()`, `next()`, and optionally `remove()`. These three methods are the entire
   pattern.

3. **Lazy by default** — Iterators compute elements on demand. This enables processing infinite or huge datasets with
   bounded memory.

4. **Fail-fast for safety** — Java's concurrent modification detection is an Iterator concern, not a collection concern.

5. **Interview memory aid** — "Iterator = sequential access, hasNext + next, uniform traversal, lazy evaluation, single
   responsibility."
