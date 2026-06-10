# CodingChallenges-Java

> **Master Java for Senior Engineering Interviews** — from whiteboard algorithms to production-grade concurrency.

A curated collection of **coding challenges**, **interview preparation guides**, and **AI-powered coaching agents** designed to prepare you for senior/staff engineering interviews at top-tier tech companies (Amazon, Revolut, RevenueCat, GitHub, Netflix, Datadog, and more).

---

## Two Interview Tracks

### Track 1: Development — DSA & Algorithms

**Guide**: [`src/main/java/com/danipl/algo/PREPARATION.md`](src/main/java/com/danipl/algo/PREPARATION.md)

Classic data structures and algorithms — the kind tested in whiteboard screens and LeetCode-style rounds:

- **Java Collections Decision Tree** — when to use `List`, `Set`, `Map`, `Queue` and why
- **Time Complexity Reference** — Big-O for every Java collection operation
- **Interview Patterns** — two pointers, sliding window, BFS/DFS, dynamic programming, Union-Find, topological sort
- **Essential Java** — Generics, Streams, Records, Optional, `var`, `Math` precision

**AI Coach**: `/whiteboard-algo-coach` — generates exercises with cheat-sheets, skeleton code, and JUnit 5 tests across the "Big 8": Arrays/Strings, Linked Lists, Trees, Graphs, Heaps, Hashing, Recursion, Sorting. Tracks your progress with a dual-gate progression system.

### Track 2: Platform Engineering — Concurrency & Systems

**Guide**: [`src/main/java/com/danipl/platform/PREPARATION.md`](src/main/java/com/danipl/platform/PREPARATION.md)

Concurrency, resilience patterns, and distributed systems — the kind tested in platform/infrastructure engineering interviews:

- **Java Concurrency Fundamentals** — `ExecutorService`, `CompletableFuture`, `ReentrantLock`, `Atomic` types, `volatile`, virtual threads (Java 21)
- **Resilience Patterns** — Circuit Breaker, Rate Limiter, Retry with exponential backoff & jitter
- **Metrics & Observability** — sliding windows, P95 percentiles, streaming aggregation
- **System Design Concepts** — distributed locking, backpressure, idempotency, graceful shutdown
- **Data Structures for Platform** — `DelayQueue`, `BlockingQueue`, `ConcurrentHashMap`, `CopyOnWriteArrayList`

**AI Coach**: `/platform-challenge-coach` — generates production-quality challenges with interface + implementation skeleton + JUnit 5 test suites + live-coding GUIDELINES.md.

---

## Challenge Structure

### Platform Engineering Challenges

```
src/main/java/com/danipl/platform/
├── resilience/
│   └── <challenge>/    CircuitBreaker, RateLimiter, Retry, ...
├── concurrency/
│   └── <challenge>/    ResourcePool, TaskScheduler, ...
├── datastructures/
│   └── <challenge>/    LoadBalancer, DependencyResolver, LruCache, ConfigMerger, ...
└── observability/
    └── <challenge>/    MetricsAggregator, ...
```

### Algorithm Challenges

```
src/main/java/com/danipl/algo/
├── arrays/
│   └── <difficulty>/   <ChallengeName>.java    (easy, medium, hard, very_hard)
├── strings/
│   └── <difficulty>/   <ChallengeName>.java
├── lists/
│   └── <difficulty>/   <ChallengeName>.java
├── trees/
│   └── <difficulty>/   <ChallengeName>.java
├── graphs/
│   └── <difficulty>/   <ChallengeName>.java
├── heaps/
│   └── <difficulty>/   <ChallengeName>.java
├── hashing/
│   └── <difficulty>/   <ChallengeName>.java
├── recursion/
│   └── <difficulty>/   <ChallengeName>.java
└── sorting/
    └── <difficulty>/   <ChallengeName>.java
```

### What Each Challenge Provides

**Platform challenges** (`src/main/java/com/danipl/platform/<topic>/<challenge>/`):
- **Interface** — the contract with full Javadoc, nested types (Config, State, Exceptions), and a `static of()` factory
- **Implementation skeleton** — `*Impl.java` with proper locking structure; you fill in the logic
- **GUIDELINES.md** — live-coding interview prep: state diagrams, edge cases, chain of thinking, communication tips, common mistakes
- **JUnit 5 test suite** — `@Nested` groups covering basic behavior, edge cases, and thread safety

**Algorithm challenges** (`src/main/java/com/danipl/algo/<topic>/<difficulty>/`):
- **Challenge class** — problem description, Java cheat-sheet in Javadoc, and skeleton method
- **JUnit 5 test suite** — parameterized tests with edge cases

Use `/platform-challenge-coach` or `/whiteboard-algo-coach` to generate new challenges — they follow these conventions automatically.

---

## Reference Guides

Located at `src/main/java/com/danipl/`, these are deep-dive guides shared across both tracks:

| Guide | Covers |
|---|---|
| [`LIST_GUIDE.md`](src/main/java/com/danipl/LIST_GUIDE.md) | ArrayList, LinkedList, CopyOnWriteArrayList |
| [`SET_GUIDE.md`](src/main/java/com/danipl/SET_GUIDE.md) | HashSet, LinkedHashSet, TreeSet, EnumSet |
| [`MAP_GUIDE.md`](src/main/java/com/danipl/MAP_GUIDE.md) | HashMap, LinkedHashMap, TreeMap, ConcurrentHashMap |
| [`QUEUE_GUIDE.md`](src/main/java/com/danipl/QUEUE_GUIDE.md) | ArrayDeque, PriorityQueue, all BlockingQueue types, DelayQueue |
| [`HEAP_GUIDE.md`](src/main/java/com/danipl/HEAP_GUIDE.md) | PriorityBlockingQueue, Top-K patterns |
| [`GRAPH_GUIDE.md`](src/main/java/com/danipl/GRAPH_GUIDE.md) | BFS/DFS, Union-Find, topological sort |
| [`TREE_GUIDE.md`](src/main/java/com/danipl/TREE_GUIDE.md) | Trie, BST, tree traversal patterns |
| [`INTERVAL_GUIDE.md`](src/main/java/com/danipl/INTERVAL_GUIDE.md) | Merge intervals, sliding window boundaries |
| [`MONOTONIC_GUIDE.md`](src/main/java/com/danipl/MONOTONIC_GUIDE.md) | Monotonic stack/queue, sliding window max/min |
| [`SORT_SEARCH_GUIDE.md`](src/main/java/com/danipl/SORT_SEARCH_GUIDE.md) | Sorting algorithms, binary search patterns |
| [`TWO_POINTERS_GUIDE.md`](src/main/java/com/danipl/TWO_POINTERS_GUIDE.md) | Two pointers, fast/slow pointers |
| [`STRING_GUIDE.md`](src/main/java/com/danipl/STRING_GUIDE.md) | String manipulation, pattern matching |
| [`DP_PATTERNS_GUIDE.md`](src/main/java/com/danipl/DP_PATTERNS_GUIDE.md) | Dynamic programming patterns (memoization, tabulation) |
| [`BACKTRACKING_GUIDE.md`](src/main/java/com/danipl/BACKTRACKING_GUIDE.md) | Backtracking patterns (subsets, permutations, combinations) |
| [`BIT_MANIPULATION_GUIDE.md`](src/main/java/com/danipl/BIT_MANIPULATION_GUIDE.md) | Bit manipulation tricks and patterns |
| [`CP_IO_GUIDE.md`](src/main/java/com/danipl/CP_IO_GUIDE.md) | Competitive programming I/O patterns |

---

## AI-Powered Coaching Agents

This repository includes **three specialized agents** that turn it into an interactive interview preparation system:

| Agent | Trigger | What It Does |
|---|---|---|
| **`/whiteboard-algo-coach`** | "give me a problem", "interview practice" | Generates DSA exercises with cheat-sheets, skeleton code, and JUnit 5 tests. Tracks your progress across the "Big 8" topics with a dual-gate progression system. |
| **`/platform-challenge-coach`** | "platform challenge", "concurrency exercise" | Generates production-quality platform engineering challenges with interface + skeleton + tests + GUIDELINES.md. Covers resilience, concurrency, data structures, and observability. |
| **`/interview-evaluate`** | After you submit a solution | Evaluates your code as a Staff-level interviewer would: correctness, code quality, complexity, engineering practices, and communication. Produces a severity-graded report with pass/fail decision. |

**How to use**: Just ask in natural language. Examples:
- *"Give me a medium arrays problem"* → algo coach generates exercise
- *"Platform challenge: circuit breaker"* → platform coach generates challenge
- *"Evaluate my solution"* → evaluator grades your code

---

## How to Use This Repository

### For Interview Preparation

1. **Read the preparation guide** for your target role (`algo/PREPARATION.md` or `platform/PREPARATION.md`)
2. **Study the reference guides** for patterns you want to master
3. **Ask an AI coach** for a challenge at your level
4. **Implement the solution** in the `*Impl.java` file
5. **Run the tests** to verify correctness
6. **Ask the evaluator** to grade your solution

### Running Tests

```bash
# Run all tests
./gradlew test

# Run a specific challenge's tests
./gradlew test --tests "com.danipl.platform.resilience.circuitbreaker.CircuitBreakerTest"

# Run all platform tests
./gradlew test --tests "com.danipl.platform.*"

# Clean and rebuild
./gradlew clean build
```

---

## Third-Party Library Policy

### For Coding Challenges

**No third-party libraries.** Use only the JDK standard libraries:

- `java.util.*` — Collections, concurrent utilities, Optional
- `java.util.concurrent.*` — ExecutorService, locks, atomic types, queues
- `java.util.concurrent.atomic.*` — AtomicInteger, AtomicLong, AtomicReference
- `java.math.*` — BigInteger, BigDecimal for precision
- `java.time.*` — Instant, Duration, Clock for testable time handling
- `java.io.*` / `java.nio.*` — I/O operations

### For Production / Real-World Use

Use established, battle-tested libraries but understand their internals:

- **Resilience**: Resilience4j (circuit breaker, rate limiter, retry)
- **Caching**: Caffeine (high-performance caching), Guava Cache
- **Metrics**: Micrometer, Prometheus
- **Concurrency**: Guava (ListeningExecutorService, RateLimiter)

The preparation guides explain both the native JDK approach and when to prefer third-party solutions.

---

## Key Java Concepts by Track

| Topic           | Development Track                     | Platform Track                               |
|-----------------|---------------------------------------|----------------------------------------------|
| Data Structures | Collections, HashMap, PriorityQueue   | DelayQueue, BlockingQueue, ConcurrentHashMap |
| Concurrency     | Basic threading                       | Advanced: locks, atomics, CompletableFuture  |
| Algorithms      | Recursion, DP, BFS/DFS, sorting       | Topological sort, graph algorithms           |
| Patterns        | Two pointers, sliding window          | Circuit breaker, rate limiter, retry         |
| Time Handling   | Basic                                 | Clock abstraction for testability            |
| Thread Safety   | Not always required                   | Critical — every platform challenge          |
| Java Version    | Java 21 (Records, var, pattern matching) | Java 21 (Virtual threads, Structured concurrency) |

---

## Tech Stack

- **Java 21** — Records, `var`, pattern matching, virtual threads, structured concurrency
- **JUnit Jupiter 5.10.2** — `@Nested` groups, parameterized tests, display names
- **Gradle** — `./gradlew test`, `./gradlew clean build`
- **No external dependencies** in challenges — pure JDK
