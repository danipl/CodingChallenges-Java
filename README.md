# CodingChallenges-Java

Java coding challenges and interview preparation guides for senior software engineering roles at top-tier tech.

---

## Role-Specific Preparation Guides

### Development Role (DSA & Algorithms)

**Guide**: [src/main/java/com/danipl/development/PREPARATION.md](src/main/java/com/danipl/development/PREPARATION.md)

Covers data structures, algorithms, and problem-solving patterns tested in development engineering interviews:

- Java Collections decision tree and time complexity reference
- Essential Java features: Generics, Streams, Records, Optional, Math precision
- Interview patterns: two pointers, sliding window, BFS/DFS, dynamic programming, Union-Find, topological sort
- Challenge reference by category linking all existing code

### Platform Engineering Role (Concurrency & Systems)

**Guide**: [src/main/java/com/danipl/platform/PREPARATION.md](src/main/java/com/danipl/platform/PREPARATION.md)

Covers concurrency, resilience patterns, and distributed systems concepts tested in platform engineering interviews:

- Java concurrency fundamentals: ExecutorService, CompletableFuture, Locks, Atomic types
- Resilience patterns: Circuit Breaker, Rate Limiter, Retry with backoff, Load Balancing
- Metrics & Observability: sliding windows, percentiles, streaming aggregation
- System design concepts: distributed locking, backpressure, idempotency, graceful shutdown

---

## Challenge Structure

### Development Challenges

```
src/main/java/com/danipl/development/
├── leetcode/           # LeetCode-style problems
│   ├── FizzBuzz.java
│   ├── funwitharrays/
│   ├── linkedlist/
│   ├── MiddleOfTheLinkedList.java
│   ├── NumberOfStepsToReduceANumberToZero.java
│   ├── RansomNote.java
│   ├── RichestCustomerWealth.java
│   └── SumOf1DArray.java
├── onepreparationweek/ # HackerRank one-week-preparation-kit
│   ├── CountingSortOne.java
│   ├── DiagonalDifference.java
│   ├── LonelyInteger.java
│   ├── MiniMaxSum.java
│   ├── PlusMinus.java
│   ├── TimeConversion.java
│   └── ZigZagSequence.java
├── recursion/          # Dynamic programming & recursion
│   ├── AllConstruct.java
│   ├── BestSum.java
│   ├── CanConstruct.java
│   ├── CanSum.java
│   ├── Fibonacci.java
│   ├── GridTraveler.java
│   └── HowSum.java
└── util/
    └── TimeUtils.java
```

### Platform Engineering Challenges

```
src/main/java/com/danipl/platform/
├── challenge01/  Circuit Breaker       (resilience, state machine, ReentrantLock)
├── challenge02/  Load Balancer         (routing algorithms)
├── challenge03/  Dependency Resolver   (topological sort, graph algorithms)
├── challenge04/  Rate Limiter          (token bucket, throughput control)
├── challenge05/  Metrics Aggregator    (sliding windows, P95 percentiles)
├── challenge06/  Retry                 (exponential backoff, jitter)
├── challenge07/  LRU Cache             (doubly-linked list + hash map, TTL)
├── challenge08/  Resource Pool         (blocking acquire, factory pattern)
├── challenge09/  Config Merger         (tree structures, hierarchical config)
└── challenge10/  Task Scheduler        (DelayQueue, thread pool, Future)
```

Each platform challenge provides an interface and a skeleton implementation. The goal is to complete the implementations
while maintaining thread-safety and correct behavior.

---

## Third-Party Library Policy

### For Coding Challenges

**No third-party libraries allowed.** Use only the JDK standard libraries:

- `java.util.*` - Collections, concurrent utilities, Optional
- `java.util.concurrent.*` - ExecutorService, locks, atomic types, queues
- `java.util.concurrent.atomic.*` - AtomicInteger, AtomicLong, AtomicReference
- `java.math.*` - BigInteger, BigDecimal for precision
- `java.time.*` - Instant, Duration, Clock for testable time handling
- `java.io.*` / `java.nio.*` - I/O operations

### For Production / Real-World Use

Use established, battle-tested libraries but understand their internals:

- **Resilience**: Resilience4j (circuit breaker, rate limiter, retry)
- **Caching**: Caffeine (high-performance caching), Guava Cache
- **Metrics**: Micrometer, Prometheus
- **Concurrency**: Guava (ListeningExecutorService, RateLimiter)

The preparation guides explain both the native JDK approach and when to prefer third-party solutions.

---

## How to Use This Repository

1. **Read the preparation guide** for your target role first
2. **Study the existing challenge interfaces** to understand requirements
3. **Complete the skeleton implementations** in `*Impl.java` files
4. **Run the tests** in `src/test/java/` to verify correctness
5. **Review your solutions** for edge cases, thread-safety, and performance

---

## Key Java Concepts by Role

| Topic           | Development Role                    | Platform Role                                |
|-----------------|-------------------------------------|----------------------------------------------|
| Data Structures | Collections, HashMap, PriorityQueue | DelayQueue, BlockingQueue, ConcurrentHashMap |
| Concurrency     | Basic threading                     | Advanced: locks, atomics, CompletableFuture  |
| Algorithms      | Recursion, DP, BFS/DFS, sorting     | Topological sort, graph algorithms           |
| Patterns        | Two pointers, sliding window        | Circuit breaker, rate limiter, retry         |
| Time Handling   | Basic                               | Clock abstraction for testability            |
| Thread Safety   | Not always required                 | Critical - every platform challenge          |
