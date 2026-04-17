# Challenge 03: Dependency Resolver - Live Coding Guidelines

## 1. Challenge Presentation

### What You're Building

A **thread-safe Dependency Resolver** that computes a valid build order for libraries given their dependency graph. This
is a classic **topological sort** problem with **cycle detection** — the same algorithm used by Maven, Gradle, npm, and
other package managers to resolve transitive dependencies.

### Core Contract

```
         add("lib", [deps...])
                   │
                   ▼
    ┌──────────────────────────┐
    │     Directed Graph       │
    │   (nodes → dependencies) │
    └──────────┬───────────────┘
               │
    ┌──────────▼───────────────┐     circular?     ┌──────────────────────┐
    │  Kahn's Algorithm (BFS)  │ ──── Yes ────▶    │ CircularDependency   │
    │  Topological Sort        │                   │ Exception + cycle    │
    └──────────┬───────────────┘                   └──────────────────────┘
               │ No
               ▼
    ┌──────────────────────────┐
    │  Build Order: [core,     │
    │  utils, db, auth,        │
    │  api, web]               │
    │  (dependencies first)    │
    └──────────────────────────┘
```

### Interface Summary

| Method                       | Purpose                                               |
|------------------------------|-------------------------------------------------------|
| `DependencyResolver.of()`    | Factory method — returns the implementation instance  |
| `add(library, dependencies)` | Register a library and its direct dependencies        |
| `resolveBuildOrder()`        | Compute valid build order (topological sort) or throw |
| `hasCircularDependency()`    | Boolean check — are there any cycles in the graph?    |

### What Interviewers Evaluate

1. **Graph modeling** — how you represent a directed graph, typically with adjacency lists or something equivalent.
2. **Topological sort correctness** — using Kahn's (BFS) or DFS-based approach, ensuring all dependencies come before
   their dependents.
3. **Cycle detection** — identifying and reporting circular dependencies like A → B → C → A.
4. **Thread safety** — concurrent `add()` and `resolveBuildOrder()` calls don't produce corrupted or inconsistent
   results.
5. **Algorithmic thinking** — aiming for O(V + E) time complexity rather than brute-force approaches.
6. **Edge case coverage** — empty graphs, disconnected components, self-loops, and dependencies that were never
   explicitly registered.

---

## 2. Edge & Corner Cases

### How to Identify Them Before Coding

Map the graph structure. Every structural assumption is a potential edge case.

| #  | Edge Case                                           | How It Surfaces                                | How to Handle                                                               |
|----|-----------------------------------------------------|------------------------------------------------|-----------------------------------------------------------------------------|
| 1  | **No libraries registered**                         | `resolveBuildOrder()` on empty resolver        | Return empty list — no work needed                                          |
| 2  | **Single library, no deps**                         | `add("solo", [])` → should return `["solo"]`   | Node with zero in-degree goes straight into the queue                       |
| 3  | **Self-dependency**                                 | `add("A", ["A"])` — node points to itself      | In-degree of A starts at 1, never reaches 0 → cycle detected                |
| 4  | **Direct cycle (A ↔ B)**                            | `add("A", ["B"])` + `add("B", ["A"])`          | Neither node reaches in-degree 0 → detected                                 |
| 5  | **Indirect cycle (A → B → C → A)**                  | Three-node cycle — harder to spot              | Kahn's naturally catches it — nodes in cycle never enter queue              |
| 6  | **Diamond dependency**                              | `A → B, A → C, B → D, C → D`                   | D has in-degree 2; only enters queue after both B and C processed           |
| 7  | **Disconnected subgraphs**                          | `A → B` and `X → Y` — two independent graphs   | Both subgraphs resolve; insertion order determines subgraph ordering        |
| 8  | **Implicit deps** (dep never registered as library) | `add("A", ["B"])` — B never added as a key     | B still appears in the graph via `putIfAbsent`; in-degree is 0, sorts first |
| 9  | **Duplicate dependencies**                          | `add("A", ["B"])` then `add("A", ["B"])` again | De-duplicate within `add()` to prevent double-counting in-degrees           |
| 10 | **Concurrent `add()` and `resolve()`**              | Thread A adds while Thread B resolves          | Snapshot under read lock; writes blocked until snapshot taken               |

### Quick Pre-Implementation Checklist

```
▢ add() validates library is not null
▢ add() validates dependencies list is not null
▢ add() deduplicates dependencies to avoid double in-degree counting
▢ add() also ensures the dependency appears in the graph (if implicit)
▢ Initial state is empty graph
▢ resolveBuildOrder() returns [] for empty graph
▢ resolveBuildOrder() returns valid topological ordering for DAG
▢ resolveBuildOrder() throws CircularDependencyException for cycles
▢ hasCircularDependency() returns true/false without throwing
▢ Thread safety: add() takes write lock, resolveBuildOrder() takes read lock
▢ Snapshot isolation: resolveBuildOrder() copies state before processing
```

---

## 3. First Approach - Chain of Thinking

### Minute 0-2: Clarify Requirements

Start by clarifying the requirements with the interviewer before writing any code:

- *"Should I detect cycles and throw, or just return an empty list?"* → Throw a `CircularDependencyException`.
- *"What about a library that only appears as a dependency and was never explicitly added?"* → Treat it as a node with
  no outgoing dependencies — it still needs to appear in the build order.
- *"What if the same dependency is listed twice for a library?"* → Deduplicate them so we don't double-count in-degrees.
- *"Do I need to handle concurrent access?"* → Yes — `add()` and `resolveBuildOrder()` can be called from different
  threads.

### Minute 2-5: Choose the Algorithm

**Kahn's Algorithm (BFS-based) vs DFS**

| Criterion               | Kahn's (BFS)                     | DFS (3-color)                                       |
|-------------------------|----------------------------------|-----------------------------------------------------|
| Intuitive for building  | Yes — "start from no-deps"       | Less intuitive                                      |
| Cycle detection         | Implicit — not all nodes visited | Explicit — back-edge detection                      |
| Natural ordering        | Yes — queue order is valid       | Yes — post-order reverse                            |
| Interview friendliness  | Easier to explain                | Requires explaining visited/visiting/current states |
| Production code (Maven) | ✅ Used in practice               | Less common                                         |

→ **Kahn's algorithm is the interview default.** Easier to explain, easier to get right.

### Minute 5-10: Sketch the Core Flow

```java
public List<String> resolveBuildOrder() {
    // 1. Snapshot under read lock
    // 2. Build in-degree map: for each library, count how many depend on it
    // 3. Build reverse adjacency: for each dep, who depends on it?
    // 4. Queue = all nodes with in-degree == 0
    // 5. While queue not empty:
    //      dequeue → add to result
    //      for each dependent: decrement its in-degree
    //        if in-degree == 0 → enqueue
    // 6. If result.size() != total nodes → cycle → throw
    // 7. Return ordered list
}
```

### Minute 10-25: Implement

1. Graph representation: `Map<String, List<String>>` → library → its dependencies
2. `add()` with write lock, deduplication
3. `resolveBuildOrder()` with read lock + snapshot + Kahn's
4. `hasCircularDependency()` delegates to `resolveBuildOrder()`
5. `CircularDependencyException` — simple RuntimeException

---

## 4. Communication Approach During the Interview

### What to Say Out Loud

| Moment                | Say This                                                                                                                                                                                                                                          |
|-----------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Starting              | "This is a topological sort problem. I'll model it as a directed graph and use Kahn's algorithm — BFS-based, O(V + E). Dependencies always come before dependents."                                                                               |
| Before graph build    | "I need two data structures: an in-degree map (counting how many libraries depend on each one) and a reverse adjacency list (for each node, who are its dependents). This lets me efficiently find zero in-degree nodes."                         |
| About cycle detection | "With Kahn's, cycle detection is natural. Nodes in a cycle never reach in-degree zero — they're never enqueued. If the result has fewer nodes than the graph, there must be a cycle."                                                             |
| About locking         | "I'll use ReentrantReadWriteLock. `add()` is a write — needs exclusive access. `resolveBuildOrder()` is a read — I'll snapshot the map and release the lock before the O(V + E) computation. This avoids holding the lock during expensive work." |
| About snapshot        | "Snapshotting under the lock is key — it's a consistent point-in-time copy. The algorithm works on the snapshot, not the live map. This prevents concurrent adds from corrupting the computation."                                                |
| About edge cases      | "I need to handle self-loops (A depends on A), implicit nodes (deps that were never explicitly added), and duplicate dependencies. Kahn's naturally handles self-loops since A's in-degree starts at 1 and never reaches 0."                      |

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
public class DependencyResolverImpl implements DependencyResolver {
   // === Fields ===
   private final Map<String, List<String>> libraryMap = new LinkedHashMap<>();  // preserves insertion order
   private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
   private final ReadLock readLock = lock.readLock();
   private final WriteLock writeLock = lock.writeLock();

   // === add() ===                    ← Write lock, dedup, ~15 lines
   // === resolveBuildOrder() ===      ← Read lock + snapshot + Kahn's, ~40 lines
   // === hasCircularDependency() ===  ← Delegates to resolveBuildOrder(), ~10 lines

   // Kahn's algorithm breakdown:
   //   Step 1: Snapshot under lock
   //   Step 2: Compute in-degrees (Map<String, Integer>)
   //   Step 3: Build reverse adjacency (Map<String, List<String>>)
   //   Step 4: Queue = { node | in-degree[node] == 0 }
   //   Step 5: Process queue → result
   //   Step 6: Cycle check → throw if result.size() != nodeCount
}
```

### Key Implementation Pattern

```java
// Kahn's Algorithm — In-Degree Computation
Map<String, Integer> inDegrees = new HashMap<>();
Map<String, List<String>> reverseAdj = new HashMap<>();

for(Map.Entry<String, List<String>> entry :snapshot.entrySet()){
   String library = entry.getKey();
   inDegrees.putIfAbsent(library, 0);
   for(String dep :entry.getValue()){
        // dep has one more dependent → increment its in-degree
        inDegrees.merge(library, 1,Integer::sum);
        inDegrees.putIfAbsent(dep, 0);
         // reverse: dep → library (library depends on dep)
        reverseAdj.computeIfAbsent(dep, k ->new ArrayList<>()).add(library);
    }
}

// Nodes with in-degree 0 can be built first
Queue<String> queue = new ArrayDeque<>();
inDegrees.forEach((node, degree) ->{
    if(degree ==0)queue.offer(node);
});
```

---

## 6. Technical Pro Tips

### Algorithm Choice Context

| Approach                    | Time     | Space    | Cycle Detection       | Interview Fit    |
|-----------------------------|----------|----------|-----------------------|------------------|
| **Kahn's (BFS)**            | O(V + E) | O(V + E) | Implicit — node count | ✅ **Default**    |
| DFS with 3-color marking    | O(V + E) | O(V)     | Explicit — back-edge  | Good, but harder |
| Floyd-Warshall (cycle only) | O(V³)    | O(V²)    | Yes                   | ❌ Too slow       |

### Maven's Dependency Resolution

What Maven/Gradle actually do that interviewers appreciate you knowing:

| Feature              | This Challenge   | Maven/Gradle                             |
|----------------------|------------------|------------------------------------------|
| Conflict resolution  | Not needed       | Nearest-wins / first-declared strategy   |
| Version ranges       | Not supported    | `[1.0,2.0)`, `[1.0,)`                    |
| Exclusions           | Not supported    | `<exclusions>` to skip transitive deps   |
| Scope                | Not supported    | compile, runtime, test, provided         |
| Transitive reduction | Not included     | Computes minimal closure                 |
| Cycle tolerance      | Throws exception | Maven throws; some tools warn and ignore |

### Topological Sort in Systems You'll Mention

| System           | Uses Topological Sort For                       |
|------------------|-------------------------------------------------|
| **Make / CMake** | Build target ordering                           |
| **Systemd**      | Service startup ordering (`After=` / `Before=`) |
| **Terraform**    | Resource provisioning order                     |
| **Airflow**      | DAG task execution order                        |
| **GraphQL**      | Resolver dependency ordering                    |

### Thread Safety: Why Snapshot?

```
Problem: If I compute in-degrees while holding the read lock for the entire
O(V + E) computation, concurrent adds are blocked for potentially a long time.

Solution: Snapshot (LinkedHashMap copy) under read lock → release → compute.
The snapshot is a consistent point-in-time view. Concurrent adds during
computation don't affect the current result.
```

| Approach                  | Lock Duration | Concurrent `add()` Blocked? | Correctness            |
|---------------------------|---------------|-----------------------------|------------------------|
| Hold lock for entire sort | Long (O(V+E)) | Yes                         | ✅                      |
| Snapshot then release     | Short (O(V))  | No (after snapshot)         | ✅ eventual consistency |
| No lock                   | N/A           | No                          | ❌ race conditions      |

### Testing Strategy

```java
// Happy path: simple chain
// Diamond dependency: A → B, C → D, B → D
// Circular: A → B → A
// Self-loop: A → A
// Empty: no libraries
// Concurrent: 10 threads adding while 5 resolve
```

### What Senior Engineers Demonstrate

1. **Algorithm awareness** — "Kahn's vs DFS: both O(V+E), but Kahn's is more intuitive for build-order problems and
   naturally detects cycles via node count"
2. **Snapshot isolation** — "I snapshot under the lock to get a consistent view, then release. This avoids blocking
   writers during the O(V+E) computation"
3. **Implicit node handling** — "Dependencies that were never registered as libraries still need to appear in the graph.
   `putIfAbsent` handles this"
4. **Cycle path extraction** — "For production I'd trace the actual cycle using DFS back-edge detection, not just throw
   a generic message"
5. **Deduplication awareness** — "Calling `add("A", ["B"])` twice shouldn't double-count B's contribution to A's
   in-degree"

---

## 7. Common Mistakes to Avoid

| Mistake                                               | Why It Fails                                     | Fix                                                                             |
|-------------------------------------------------------|--------------------------------------------------|---------------------------------------------------------------------------------|
| Counting in-degree wrong (dependencies vs dependents) | Nodes enter queue in wrong order or never        | In-degree = "how many nodes does THIS depend on?" not "how many depend on this" |
| Not snapshotting before Kahn's                        | Concurrent `add()` changes graph mid-computation | Copy map under read lock before processing                                      |
| `queue.poll()` vs `queue.remove()` confusion          | Both work but `poll()` is safer (returns null)   | Use `poll()` or ensure queue is never empty when dequeuing                      |
| No deduplication in `add()`                           | Same dep added twice → wrong in-degree           | Check `!currentDeps.contains(dep)` before adding                                |
| Missing implicit nodes                                | Deps never registered as keys                    | `putIfAbsent(dep, 0)` when processing each dep                                  |
| `indexOf()` returning -1 in tests                     | Tests pass because -1 < -1 is false, silently    | Assert list contains all expected elements first                                |
| Exception swallows cycle identity                     | Caller doesn't know WHICH nodes form the cycle   | Include cycle path in exception message                                         |
| Holding write lock during `resolveBuildOrder()`       | Blocks all readers unnecessarily                 | Use read lock with snapshot pattern                                             |

---

## 8. Verification Checklist

Before declaring done, verify:

### Functional

- [ ] Empty graph returns empty list
- [ ] Single node, no deps → returns `[node]`
- [ ] Linear chain A → B → C → C, B, A (dependency order)
- [ ] Diamond dependency → D before B and C, B and C before A
- [ ] Cycle A → B → A → throws `CircularDependencyException`
- [ ] Self-loop A → A → throws `CircularDependencyException`
- [ ] Indirect cycle A → B → C → A → throws
- [ ] Multiple disconnected subgraphs → all resolved
- [ ] Duplicate deps for same library → handled correctly
- [ ] Implicit deps (never registered) → included in output

### Thread Safety

- [ ] Concurrent `add()` from multiple threads → no data corruption
- [ ] Concurrent `add()` + `resolveBuildOrder()` → snapshot consistency
- [ ] No deadlocks (read lock is reentrant, write lock is exclusive)
- [ ] Lock acquired/released in try-finally blocks

### Edge Cases

- [ ] `hasCircularDependency()` returns `false` for DAG
- [ ] `hasCircularDependency()` returns `true` for any cycle type
- [ ] `hasCircularDependency()` does not throw — catches and returns boolean
- [ ] Result contains ALL nodes (not just explicitly registered ones)
- [ ] Build order respects ALL dependency constraints

---

## 9. Extension Points (Bonus Discussion)

If you finish early, mention these to stand out:

1. **Cycle path reporting** — "Instead of a generic message, I'd trace back using DFS to report `A → B → C → A` so the
   caller knows exactly what to fix"
2. **Version conflict resolution** — "Real package managers handle versions. I'd augment nodes with
   `Map<String, List<Version>>` and implement nearest-wins strategy"
3. **Parallel resolve** — "Since Kahn's processes independent nodes in parallel, I'd return `Set<String>` per level for
   parallel build execution"
4. **Graph immutability** — "In production, I'd make the graph immutable after construction for thread safety without
   locks — just pass-by-copy"
5. **Exclusions / overrides** — "Support excluding transitive dependencies (like Maven's `<exclusions>`) and version
   pinning"
6. **Streaming input** — "If the dependency list is large (thousands of nodes), I'd stream the input and use a more
   memory-efficient adjacency representation"

---

## 10. Production References

| Resource                                                                                                              | Why It Matters                                         |
|-----------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------|
| [Guava Graph API](https://github.com/google/guava/wiki/GraphsExplained)                                               | Google's graph library — production graph abstractions |
| [Topological Sort (CLRS ch. 22)](https://mitpress.mit.edu/9780262046305/)                                             | Standard reference for algorithm correctness           |
| [Maven Dependency Resolution](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html) | Real-world dependency resolution strategy              |
| [tsort command (Unix)](https://man7.org/linux/man-pages/man1/tsort.1.html)                                            | Unix topological sort — battle-tested CLI              |

---

*This guideline follows the challenge template: presentation → edge cases → chain of thinking → communication →
implementation → pro tips → mistakes → verification.*
