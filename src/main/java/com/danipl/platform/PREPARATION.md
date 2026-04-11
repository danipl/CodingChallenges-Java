# Java Platform Engineering Interview Preparation Guide

A comprehensive guide for senior platform engineering interviews at top-tier companies: Revolut, Deel, RevenueCat,
GitHub, Docker, Datadog.

This guide covers concurrency patterns, resilience mechanisms, system design, and distributed systems concepts essential
for platform roles.

---

## Platform Engineering Challenge Map

| Challenge   | Concept             | Key Java Features                                     |
|-------------|---------------------|-------------------------------------------------------|
| challenge01 | Circuit Breaker     | State machine, ReentrantLock, volatile counters       |
| challenge02 | Load Balancer       | Distribution algorithms, server selection             |
| challenge03 | Dependency Resolver | Topological sort, graph algorithms                    |
| challenge04 | Rate Limiter        | Token bucket, throughput control, concurrency         |
| challenge05 | Metrics Aggregator  | Sliding windows, percentiles, streaming data          |
| challenge06 | Retry               | Exponential backoff, jitter, result tracking          |
| challenge07 | LRU Cache           | Doubly-linked list + hash map, TTL, Clock abstraction |
| challenge08 | Resource Pool       | Blocking acquire, factory pattern, AutoCloseable      |
| challenge09 | Config Merger       | Tree structures, hierarchical configuration           |
| challenge10 | Task Scheduler      | DelayQueue, thread pool, Future, graceful shutdown    |

---

## A. Concurrency Fundamentals

### Thread Creation: Thread vs Runnable vs Callable

```java
// Thread class - use only when you need to override run() behavior
Thread thread = new Thread(() -> {
            System.out.println("Running in: " + Thread.currentThread().getName());
        });
thread.

start();

// Runnable - preferred for simple async tasks
Runnable task = () -> System.out.println("Runnable task");
new

Thread(task).

start();

// Callable - when you need to return a value or throw checked exceptions
Callable<String> callable = () -> {
    Thread.sleep(100);
    return "Result from " + Thread.currentThread().getName();
};

// ExecutorService with Callable
ExecutorService executor = Executors.newSingleThreadExecutor();
Future<String> future = executor.submit(callable);
String result = future.get(); // Blocks until result available
```

**When to use each:**

- **Thread**: Rarely. Only when you need thread-specific behavior beyond just running code.
- **Runnable**: Fire-and-forget async tasks where you don't need a return value.
- **Callable**: Any task that produces a result or may throw checked exceptions.

### ExecutorService Framework

```java
// Fixed thread pool - bounded concurrency, predictable resource usage
ExecutorService fixedPool = Executors.newFixedThreadPool(4);

// Cached thread pool - unbounded growth, reuses idle threads
ExecutorService cachedPool = Executors.newCachedThreadPool();

// Scheduled executor - delayed and periodic execution
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
scheduler.

schedule(task, 5,TimeUnit.SECONDS);
scheduler.

scheduleAtFixedRate(task, 0,1,TimeUnit.SECONDS);

// Work stealing pool (Java 8+) - ForkJoinPool for compute-intensive tasks
ExecutorService workStealing = Executors.newWorkStealingPool();

// Custom thread factory for named threads
ThreadFactory namedFactory = r -> {
    Thread t = new Thread(r);
    t.setName("platform-worker-" + t.getId());
    t.setDaemon(false);
    return t;
};
ExecutorService customPool = Executors.newFixedThreadPool(4, namedFactory);
```

**Pool selection guide:**

- **FixedThreadPool**: I/O-bound tasks, known concurrency limits.
- **CachedThreadPool**: Many short-lived tasks, but watch memory (unbounded queue).
- **ScheduledExecutorService**: Delays, timeouts, heartbeats, cron-like tasks.
- **WorkStealingPool**: CPU-intensive parallel computations.

### CompletableFuture: Async Pipelines

```java
// Basic async execution
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            return fetchFromService();
        });

// Chaining operations
CompletableFuture<String> result = CompletableFuture.supplyAsync(this::fetchUser)
        .thenApply(user -> user.getId())           // Transform result
        .thenCompose(this::fetchOrders)            // FlatMap - returns another CF
        .thenApply(orders -> orders.stream()
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
        .orTimeout(5, TimeUnit.SECONDS)            // Timeout handling
        .exceptionally(ex -> {
            log.error("Failed to calculate total", ex);
            return BigDecimal.ZERO;
        });

// Parallel composition
CompletableFuture<String> userFuture = CompletableFuture.supplyAsync(this::fetchUser);
CompletableFuture<String> configFuture = CompletableFuture.supplyAsync(this::fetchConfig);

CompletableFuture<Void> both = CompletableFuture.allOf(userFuture, configFuture);
both.

thenRun(() ->{
// Both completed
String user = userFuture.join();
String config = configFuture.join();
});

// AnyOf - proceed when first completes
CompletableFuture<Object> first = CompletableFuture.anyOf(userFuture, configFuture);
```

**Key patterns:**

- `thenApply`: Synchronous transformation.
- `thenCompose`: Async transformation (flatMap).
- `thenCombine`: Merge two futures.
- `allOf`: Wait for all, get individual results via `join()`.
- `orTimeout`: Essential for production - prevents indefinite waits.

### Locks: ReentrantLock vs synchronized vs ReadWriteLock

```java
// synchronized - simple, JVM-managed, no timeout capability
public synchronized void method() {
}

// ReentrantLock - explicit control, timeouts, fairness, conditions
public class CircuitBreaker {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition stateChanged = lock.newCondition();
    private State state = State.CLOSED;

    public boolean tryAcquire(long timeoutMs) throws InterruptedException {
        if (!lock.tryLock(timeoutMs, TimeUnit.MILLISECONDS)) {
            return false; // Timeout - fail fast
        }
        try {
            if (state == State.OPEN) {
                return false;
            }
            // Execute protected operation
            return true;
        } finally {
            lock.unlock();
        }
    }
}

// ReadWriteLock - read-heavy scenarios
public class ConfigurationStore {
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();
    private Map<String, String> config = new HashMap<>();

    public String get(String key) {
        readLock.lock();
        try {
            return config.get(key);
        } finally {
            readLock.unlock();
        }
    }

    public void update(Map<String, String> newConfig) {
        writeLock.lock();
        try {
            this.config = new HashMap<>(newConfig); // Copy-on-write pattern
        } finally {
            writeLock.unlock();
        }
    }
}

// StampedLock - optimistic reads for extreme performance (Java 8+)
public class OptimisticCache {
    private final StampedLock lock = new StampedLock();
    private volatile String value;

    public String read() {
        long stamp = lock.tryOptimisticRead();
        String current = value;
        if (!lock.validate(stamp)) {
            // Conflict detected, fallback to read lock
            stamp = lock.readLock();
            try {
                current = value;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return current;
    }
}
```

**Lock selection:**

- **synchronized**: Simple cases, no timeout needed, code brevity matters.
- **ReentrantLock**: Need tryLock with timeout, fairness, multiple conditions, interruptible locks.
- **ReadWriteLock**: Read-heavy workload (10:1 ratio or higher).
- **StampedLock**: Extreme read performance, optimistic concurrency acceptable.

### Atomic Types: Lock-Free Concurrency

```java
// AtomicInteger - counters, sequence generators
AtomicInteger counter = new AtomicInteger(0);
int newValue = counter.incrementAndGet();     // ++i
int oldValue = counter.getAndIncrement();     // i++
int updated = counter.addAndGet(5);           // i += 5

// CAS loop for custom logic
AtomicInteger state = new AtomicInteger(0);

public boolean transition(int expected, int newState) {
    return state.compareAndSet(expected, newState);
}

// AtomicLong - for metrics, timestamps
AtomicLong lastFailureTime = new AtomicLong(0);
lastFailureTime.

updateAndGet(current ->
current >System.

currentTimeMillis() ?current :System.

currentTimeMillis()
);

// AtomicReference - for object updates
AtomicReference<Connection> connectionRef = new AtomicReference<>();
Connection old = connectionRef.getAndSet(newConnection);
if(old !=null){
        old.

close(); // Cleanup old connection
}

// LongAdder - high-contention counters (better than AtomicLong)
LongAdder requestCount = new LongAdder();
requestCount.

increment();

long total = requestCount.sum();

// LongAccumulator - custom accumulation logic
LongAccumulator maxLatency = new LongAccumulator(Long::max, 0);
maxLatency.

accumulate(latency);
```

**Atomic vs Lock trade-offs:**

- **Atomic types**: Better for simple operations, no blocking, retry on contention.
- **Locks**: Better for complex critical sections, guaranteed progress.
- **LongAdder**: Use instead of AtomicLong when many threads update concurrently.

### volatile: Visibility Guarantee

```java
public class CircuitBreakerImpl {
    // volatile ensures visibility across threads
    private volatile State state = State.CLOSED;
    private volatile long consecutiveFailures = 0;

    public State getState() {
        // Reads see the latest write from any thread
        return state;
    }

    public void recordFailure() {
        // Writes are immediately visible to other threads
        consecutiveFailures++;
        if (consecutiveFailures >= threshold) {
            state = State.OPEN;
        }
    }
}
```

**When volatile is sufficient:**

- Single field writes (independent updates).
- State flags (boolean running = true/false).
- Reference publication (final fields of referenced object are safe).

**When volatile is NOT sufficient:**

- Compound operations (i++ is read-modify-write).
- Multiple field consistency (need synchronized or locks).
- Check-then-act sequences (need atomic operations).

### ConcurrentHashMap vs Collections.synchronizedMap

```java
// Collections.synchronizedMap - coarse-grained locking
Map<String, String> syncMap = Collections.synchronizedMap(new HashMap<>());
// Every operation locks the entire map

// ConcurrentHashMap - fine-grained locking (segment-based)
ConcurrentHashMap<String, String> concurrentMap = new ConcurrentHashMap<>();

// ConcurrentHashMap specific operations
concurrentMap.

putIfAbsent("key","value");
concurrentMap.

computeIfAbsent("key",k ->

expensiveCompute(k));
        concurrentMap.

merge("key","value",String::concat);

// Atomic compound operations
concurrentMap.

compute("counter",(k, v) ->{
        if(v ==null)return"1";
        return String.

valueOf(Integer.parseInt(v) +1);
        });
```

**Performance comparison:**

- `synchronizedMap`: Single lock, poor concurrency, simple implementation.
- `ConcurrentHashMap`: Multiple locks (default 16 segments), high concurrency, more memory.

### BlockingQueue: Producer-Consumer Pattern

```java
// ArrayBlockingQueue - bounded, pre-allocated, FIFO
BlockingQueue<Task> queue = new ArrayBlockingQueue<>(1000);

// LinkedBlockingQueue - optionally bounded, linked nodes
BlockingQueue<Task> linkedQueue = new LinkedBlockingQueue<>(); // Unbounded - careful!
BlockingQueue<Task> boundedLinked = new LinkedBlockingQueue<>(1000);

// Producer
queue.

put(task);              // Blocks if full

boolean offered = queue.offer(task, 5, TimeUnit.SECONDS); // Timeout

// Consumer
Task task = queue.take();     // Blocks if empty
Task polled = queue.poll(5, TimeUnit.SECONDS); // Timeout

// Resource pool implementation using BlockingQueue
public class ResourcePool<T extends AutoCloseable> {
    private final BlockingQueue<T> available;
    private final int maxSize;

    public ResourcePool(int size, Supplier<T> factory) {
        this.maxSize = size;
        this.available = new ArrayBlockingQueue<>(size);
        for (int i = 0; i < size; i++) {
            available.offer(factory.get());
        }
    }

    public T acquire(long timeoutMs) throws InterruptedException {
        T resource = available.poll(timeoutMs, TimeUnit.MILLISECONDS);
        if (resource == null) {
            throw new PoolExhaustedException("Timeout waiting for resource");
        }
        return resource;
    }

    public void release(T resource) {
        available.offer(resource);
    }
}
```

### Coordination Primitives

```java
// CountDownLatch - one-shot barrier
CountDownLatch latch = new CountDownLatch(3);
// Each worker calls: latch.countDown();
latch.

await(); // Main thread waits for all 3

// CyclicBarrier - reusable barrier
CyclicBarrier barrier = new CyclicBarrier(4);
// Each thread calls: barrier.await();
// All 4 threads meet at barrier, then continue together

// Semaphore - limit concurrent access
Semaphore semaphore = new Semaphore(10); // Max 10 concurrent
semaphore.

acquire();   // Decrement, block if 0
semaphore.

release();   // Increment

// Rate limiter using Semaphore
public class SemaphoreRateLimiter {
    private final Semaphore semaphore;

    public SemaphoreRateLimiter(int permitsPerSecond) {
        this.semaphore = new Semaphore(permitsPerSecond);
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            semaphore.release(permitsPerSecond - semaphore.availablePermits());
        }, 1, 1, TimeUnit.SECONDS);
    }

    public boolean tryAcquire() {
        return semaphore.tryAcquire();
    }
}
```

### ThreadLocal: Use Cases and Warnings

```java
// ThreadLocal for per-thread context
public class RequestContext {
    private static final ThreadLocal<String> currentUser = new ThreadLocal<>();

    public static void setUser(String user) {
        currentUser.set(user);
    }

    public static String getUser() {
        return currentUser.get();
    }

    public static void clear() {
        currentUser.remove(); // CRITICAL: Always clean up
    }
}

// Use with try-finally to prevent leaks
public void handleRequest(Request req) {
    RequestContext.setUser(req.getUser());
    try {
        processRequest(req);
    } finally {
        RequestContext.clear(); // Prevent memory leak
    }
}
```

**ThreadLocal pitfalls:**

- **Memory leaks**: Values not cleaned up when using thread pools (threads reused).
- **Hidden coupling**: Makes code harder to test and reason about.
- **Async propagation**: CompletableFuture switches threads - ThreadLocal values don't follow.

---

## B. Data Structures for Platform Engineering

### DelayQueue: Time-Ordered Elements

```java
// TaskScheduler challenge pattern
public class ScheduledTask implements Delayed {
    private final Runnable task;
    private final long executeAt;

    public ScheduledTask(Runnable task, long delayMs) {
        this.task = task;
        this.executeAt = System.currentTimeMillis() + delayMs;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long diff = executeAt - System.currentTimeMillis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed other) {
        return Long.compare(this.executeAt, ((ScheduledTask) other).executeAt);
    }

    public void execute() {
        task.run();
    }
}

// Usage
DelayQueue<ScheduledTask> queue = new DelayQueue<>();
queue.

put(new ScheduledTask(() ->System.out.

println("Hello"), 5000));

// Consumer thread
ScheduledTask task = queue.take(); // Blocks until delay expired
task.

execute();
```

### PriorityQueue: Min-Heap for Scheduling

```java
// Custom priority for task scheduling
public class PriorityTask implements Comparable<PriorityTask> {
    private final int priority; // Lower = higher priority
    private final Runnable task;
    private final long timestamp;

    @Override
    public int compareTo(PriorityTask other) {
        int priorityCompare = Integer.compare(this.priority, other.priority);
        if (priorityCompare != 0) return priorityCompare;
        // FIFO for same priority
        return Long.compare(this.timestamp, other.timestamp);
    }
}

PriorityQueue<PriorityTask> pq = new PriorityQueue<>();
pq.

offer(new PriorityTask(1, task1));
        pq.

offer(new PriorityTask(3, task2));
        pq.

offer(new PriorityTask(1, task3));

PriorityTask next = pq.poll(); // task1 (priority 1, older)
```

### ConcurrentHashMap: Thread-Safe Maps

```java
// Compute patterns for atomic updates
ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();

// Option 1: Using atomic operations on value
counters.

computeIfAbsent("key",k ->new

AtomicInteger(0))
        .

incrementAndGet();

// Option 2: Using ConcurrentHashMap's compute
counters.

compute("key",(k, v) ->{
        if(v ==null)return new

AtomicInteger(1);
    v.

incrementAndGet();
    return v;
});

// ConcurrentHashMap.newKeySet() - concurrent Set
Set<String> concurrentSet = ConcurrentHashMap.newKeySet();
```

### CopyOnWriteArrayList: Read-Heavy Scenarios

```java
// Configuration listeners - write rarely, read often
public class ConfigNotifier {
    private final List<Consumer<Config>> listeners = new CopyOnWriteArrayList<>();

    public void addListener(Consumer<Config> listener) {
        listeners.add(listener); // Creates copy
    }

    public void notify(Config config) {
        // Iterates over snapshot - no ConcurrentModificationException
        for (Consumer<Config> listener : listeners) {
            listener.accept(config);
        }
    }
}
```

### ConcurrentLinkedQueue: Non-Blocking Queue

```java
// High-throughput, low-latency scenarios
ConcurrentLinkedQueue<Event> eventQueue = new ConcurrentLinkedQueue<>();

// Lock-free operations
eventQueue.

offer(event);

Event polled = eventQueue.poll(); // null if empty

// Note: size() is O(n) - avoid in hot paths
```

---

## C. Resilience Patterns

### Circuit Breaker (challenge01)

```java
public class CircuitBreaker {
    public enum State {CLOSED, OPEN, HALF_OPEN}

    private final int failureThreshold;
    private final long timeoutMs;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile State state = State.CLOSED;
    private volatile long consecutiveFailures = 0;
    private volatile long lastFailureTime = 0;

    public <T> T execute(Callable<T> operation) throws Exception {
        if (!acquirePermission()) {
            throw new CircuitBreakerOpenException("Circuit is OPEN");
        }

        try {
            T result = operation.call();
            onSuccess();
            return result;
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }

    private boolean acquirePermission() {
        lock.lock();
        try {
            if (state == State.OPEN) {
                if (System.currentTimeMillis() - lastFailureTime > timeoutMs) {
                    state = State.HALF_OPEN;
                    return true;
                }
                return false;
            }
            return true;
        } finally {
            lock.unlock();
        }
    }

    private void onSuccess() {
        lock.lock();
        try {
            consecutiveFailures = 0;
            if (state == State.HALF_OPEN) {
                state = State.CLOSED;
            }
        } finally {
            lock.unlock();
        }
    }

    private void onFailure() {
        lock.lock();
        try {
            consecutiveFailures++;
            lastFailureTime = System.currentTimeMillis();
            if (consecutiveFailures >= failureThreshold) {
                state = State.OPEN;
            }
        } finally {
            lock.unlock();
        }
    }
}
```

**Why circuit breakers matter:**

- Prevent cascading failures when downstream services degrade.
- Fail fast instead of waiting for timeouts.
- Give failing services time to recover (cool-down period).

**Third-party options:** Resilience4j (recommended), Hystrix (deprecated).

### Rate Limiter (challenge04)

```java
public class TokenBucketRateLimiter {
    private final long capacity;
    private final double refillRatePerMs;
    private final ReentrantLock lock = new ReentrantLock();

    private double tokens;
    private long lastRefillTime;

    public TokenBucketRateLimiter(long capacity, long refillRatePerSecond) {
        this.capacity = capacity;
        this.refillRatePerMs = refillRatePerSecond / 1000.0;
        this.tokens = capacity;
        this.lastRefillTime = System.currentTimeMillis();
    }

    public boolean tryAcquire(int tokensRequested) {
        return tryAcquire(tokensRequested, 0, TimeUnit.MILLISECONDS);
    }

    public boolean tryAcquire(int tokensRequested, long timeout, TimeUnit unit) {
        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);

        lock.lock();
        try {
            while (true) {
                refill();

                if (tokens >= tokensRequested) {
                    tokens -= tokensRequested;
                    return true;
                }

                if (timeout == 0) {
                    return false;
                }

                long waitTime = calculateWaitTime(tokensRequested);
                if (System.currentTimeMillis() + waitTime > deadline) {
                    return false;
                }

                lock.unlock();
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
                lock.lock();
            }
        } finally {
            lock.unlock();
        }
    }

    private void refill() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRefillTime;
        tokens = Math.min(capacity, tokens + elapsed * refillRatePerMs);
        lastRefillTime = now;
    }

    private long calculateWaitTime(int tokensNeeded) {
        return (long) Math.ceil((tokensNeeded - tokens) / refillRatePerMs);
    }
}
```

**Rate limiting algorithms:**

- **Token bucket**: Allows bursts up to capacity, smooths sustained rate. Best for APIs.
- **Sliding window log**: Precise, memory intensive. Stores timestamps of each request.
- **Fixed window**: Simple, allows burst at window boundaries. Good for basic protection.

**Third-party options:** Bucket4j, Resilience4j RateLimiter.

### Retry with Backoff (challenge06)

```java
public class RetryExecutor {
    private final int maxRetries;
    private final long baseDelayMs;
    private final double backoffMultiplier;
    private final double jitterFactor;

    public <T> T executeWithRetry(Callable<T> operation) throws Exception {
        int attempt = 0;
        long delay = baseDelayMs;

        while (true) {
            try {
                return operation.call();
            } catch (Exception e) {
                attempt++;
                if (attempt >= maxRetries) {
                    throw new MaxRetriesExceededException("Failed after " + maxRetries + " attempts", e);
                }

                if (!isRetryable(e)) {
                    throw e;
                }

                long jitter = (long) (delay * jitterFactor * (Math.random() - 0.5));
                Thread.sleep(delay + jitter);

                delay = (long) (delay * backoffMultiplier);
            }
        }
    }

    private boolean isRetryable(Exception e) {
        // Don't retry client errors (4xx), only server errors (5xx) and IO exceptions
        return !(e instanceof ClientException);
    }
}

// Usage with exponential backoff: base=100ms, multiplier=2, max=5 retries
// Delays: 100ms, 200ms, 400ms, 800ms, 1600ms (plus/minus jitter)
```

**Why jitter matters:**

- Prevents thundering herd when many clients retry simultaneously after a service recovers.
- Distributes retry load over time.

**Third-party options:** Resilience4j Retry, Spring Retry.

### Load Balancing (challenge02)

```java
public interface LoadBalancer {
    Server select(List<Server> servers);
}

// Round-robin
public class RoundRobinBalancer implements LoadBalancer {
    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public Server select(List<Server> servers) {
        if (servers.isEmpty()) return null;
        int index = counter.getAndIncrement() % servers.size();
        return servers.get(index);
    }
}

// Weighted round-robin
public class WeightedRoundRobinBalancer implements LoadBalancer {
    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public Server select(List<Server> servers) {
        int totalWeight = servers.stream().mapToInt(Server::getWeight).sum();
        int index = counter.getAndIncrement() % totalWeight;

        int currentWeight = 0;
        for (Server server : servers) {
            currentWeight += server.getWeight();
            if (index < currentWeight) {
                return server;
            }
        }
        return servers.get(0);
    }
}

// Consistent hashing - important for caching scenarios
public class ConsistentHashBalancer implements LoadBalancer {
    private final TreeMap<Integer, Server> ring = new TreeMap<>();
    private final int virtualNodes = 150; // Virtual nodes improve distribution

    public ConsistentHashBalancer(List<Server> servers) {
        for (Server server : servers) {
            for (int i = 0; i < virtualNodes; i++) {
                int hash = hash(server.getId() + i);
                ring.put(hash, server);
            }
        }
    }

    @Override
    public Server select(List<Server> servers, String key) {
        if (ring.isEmpty()) return null;
        int hash = hash(key);
        Map.Entry<Integer, Server> entry = ring.ceilingEntry(hash);
        return entry != null ? entry.getValue() : ring.firstEntry().getValue();
    }

    private int hash(String key) {
        return key.hashCode(); // Use better hash in production
    }
}
```

**Consistent hashing benefits:**

- When servers added/removed, only 1/N keys need to remap (N = server count).
- Virtual nodes improve distribution and handle heterogeneous server capacities.

### Resource Pool (challenge08)

```java
public class ResourcePool<T extends AutoCloseable> implements AutoCloseable {
    private final BlockingQueue<T> available;
    private final List<T> allResources;
    private final Function<T, Boolean> healthCheck;
    private final Supplier<T> factory;
    private volatile boolean closed = false;

    public ResourcePool(int size, Supplier<T> factory, Function<T, Boolean> healthCheck) {
        this.factory = factory;
        this.healthCheck = healthCheck;
        this.available = new ArrayBlockingQueue<>(size);
        this.allResources = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            T resource = factory.get();
            allResources.add(resource);
            available.offer(resource);
        }
    }

    public T acquire(long timeoutMs) throws InterruptedException, PoolExhaustedException {
        if (closed) {
            throw new IllegalStateException("Pool is closed");
        }

        T resource = available.poll(timeoutMs, TimeUnit.MILLISECONDS);
        if (resource == null) {
            throw new PoolExhaustedException("Timeout waiting for resource");
        }

        if (!healthCheck.apply(resource)) {
            // Replace unhealthy resource
            T replacement = factory.get();
            allResources.remove(resource);
            allResources.add(replacement);
            resource = replacement;
        }

        return resource;
    }

    public void release(T resource) {
        if (!closed && resource != null) {
            available.offer(resource);
        }
    }

    @Override
    public void close() {
        closed = true;
        for (T resource : allResources) {
            try {
                resource.close();
            } catch (Exception e) {
                // Log but continue closing others
            }
        }
        allResources.clear();
        available.clear();
    }
}

// Usage with try-with-resources pattern
public void doWork() throws Exception {
    Connection conn = pool.acquire(5000);
    try {
        conn.executeQuery("SELECT * FROM users");
    } finally {
        pool.release(conn);
    }
}
```

---

## D. Time & Scheduling

### ScheduledExecutorService Patterns

```java
ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

// One-shot delay
scheduler.

schedule(() ->System.out.

println("Delayed"), 5,TimeUnit.SECONDS);

// Fixed rate - executes at consistent intervals (may overlap)
        scheduler.

scheduleAtFixedRate(() ->

heartbeat(), 0,30,TimeUnit.SECONDS);

// Fixed delay - waits specified time between completions
        scheduler.

scheduleWithFixedDelay(() ->

cleanup(), 0,60,TimeUnit.SECONDS);

// Graceful shutdown
public void shutdownGracefully() {
    scheduler.shutdown();
    try {
        if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
            scheduler.shutdownNow();
        }
    } catch (InterruptedException e) {
        scheduler.shutdownNow();
        Thread.currentThread().interrupt();
    }
}
```

### DelayQueue Implementation (challenge10)

```java
public class TaskScheduler {
    private final DelayQueue<ScheduledTask> queue = new DelayQueue<>();
    private final ExecutorService executor;
    private volatile boolean running = true;

    public TaskScheduler(int threadPoolSize) {
        this.executor = Executors.newFixedThreadPool(threadPoolSize);
        startDispatcher();
    }

    private void startDispatcher() {
        Thread dispatcher = new Thread(() -> {
            while (running) {
                try {
                    ScheduledTask task = queue.take(); // Blocks until ready
                    executor.submit(task::execute);
                } catch (InterruptedException e) {
                    if (!running) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        });
        dispatcher.setDaemon(true);
        dispatcher.start();
    }

    public void schedule(Runnable task, long delayMs) {
        queue.put(new ScheduledTask(task, delayMs));
    }

    public <T> Future<T> submit(Callable<T> task) {
        return executor.submit(task);
    }

    public void shutdown() {
        running = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
}
```

### java.time API for Testability

```java
// Use Clock abstraction for testable time-dependent code
public class LruCache<K, V> {
    private final Clock clock;
    private final long ttlMs;

    public LruCache(long ttlMs) {
        this(ttlMs, Clock.systemUTC());
    }

    public LruCache(long ttlMs, Clock clock) {
        this.ttlMs = ttlMs;
        this.clock = clock;
    }

    private boolean isExpired(Node<K, V> node) {
        return clock.millis() - node.timestamp > ttlMs;
    }
}

// Testing with fixed clock
@Test
public void testExpiration() {
    Instant now = Instant.now();
    Clock fixedClock = Clock.fixed(now, ZoneId.systemDefault());

    LruCache<String, String> cache = new LruCache<>(1000, fixedClock);
    cache.put("key", "value");

    assertNotNull(cache.get("key"));

    // Advance time
    Clock advancedClock = Clock.offset(fixedClock, Duration.ofMillis(1500));
    LruCache<String, String> cache2 = new LruCache<>(1000, advancedClock);
    // Test with advanced time...
}
```

---

## E. Metrics & Observability

### Sliding Window Algorithms (challenge05)

```java
public class SlidingWindowMetrics {
    private final long windowSizeMs;
    private final Queue<LogEntry> window;
    private final ReentrantLock lock = new ReentrantLock();

    public SlidingWindowMetrics(long windowSizeMs) {
        this.windowSizeMs = windowSizeMs;
        this.window = new ConcurrentLinkedQueue<>();
    }

    public void record(LogEntry entry) {
        lock.lock();
        try {
            evictExpired();
            window.offer(entry);
        } finally {
            lock.unlock();
        }
    }

    public double getErrorRate() {
        lock.lock();
        try {
            evictExpired();
            long errors = window.stream()
                    .filter(e -> e.getLevel() == LogLevel.ERROR || e.getLevel() == LogLevel.FATAL)
                    .count();
            return window.isEmpty() ? 0.0 : (double) errors / window.size();
        } finally {
            lock.unlock();
        }
    }

    public long getP95ResponseTime() {
        lock.lock();
        try {
            evictExpired();
            List<Long> sorted = window.stream()
                    .mapToLong(LogEntry::getResponseTimeMs)
                    .sorted()
                    .boxed()
                    .collect(Collectors.toList());

            if (sorted.isEmpty()) return 0;

            int index = (int) Math.ceil(sorted.size() * 0.95) - 1;
            return sorted.get(Math.max(0, index));
        } finally {
            lock.unlock();
        }
    }

    private void evictExpired() {
        long cutoff = System.currentTimeMillis() - windowSizeMs;
        while (!window.isEmpty() && window.peek().getTimestamp() < cutoff) {
            window.poll();
        }
    }
}
```

**Why P95 over mean:**

- Mean hides tail latency issues that affect real users.
- P95 shows what 95% of users experience.
- P99 for critical paths where even rare slowness matters.

### Moving Averages

```java
public class EWMARate {
    private final double alpha; // Smoothing factor (0 < alpha < 1)
    private volatile double average = 0;
    private final AtomicLong count = new AtomicLong(0);

    public EWMARate(double alpha) {
        this.alpha = alpha;
    }

    public void update(double value) {
        long n = count.incrementAndGet();
        if (n == 1) {
            average = value;
        } else {
            average = alpha * value + (1 - alpha) * average;
        }
    }

    public double getAverage() {
        return average;
    }
}

// Usage: alpha = 2/(N+1) for N-period EMA
// For 60-second window with 1s samples: alpha = 2/61 ≈ 0.033
```

---

## F. Third-Party Library Policy

### Coding Challenge Constraints

**In coding challenges: NO third-party libraries.**

Allowed packages:

- `java.util.*`
- `java.util.concurrent.*`
- `java.util.concurrent.atomic.*`
- `java.time.*`
- `java.io.*`, `java.nio.*`

**Why:** Interviewers want to see you understand the underlying patterns, not just API familiarity.

### Production Platform Engineering

**USE established libraries BUT know what they do internally.**

| Concern         | Recommended Library        | Native JDK Alternative         |
|-----------------|----------------------------|--------------------------------|
| Circuit Breaker | Resilience4j               | ReentrantLock + volatile state |
| Rate Limiter    | Resilience4j, Bucket4j     | Semaphore + ScheduledExecutor  |
| Retry           | Resilience4j, Spring Retry | Loop with Thread.sleep         |
| Caching         | Caffeine                   | ConcurrentHashMap + TTL        |
| Connection Pool | HikariCP                   | BlockingQueue + factory        |
| Metrics         | Micrometer                 | Sliding window implementation  |
| Logging         | SLF4J + Logback            | java.util.logging              |

**Why use libraries:**

- Battle-tested edge cases you haven't thought of.
- Optimized implementations (lock-free where possible).
- Active maintenance and bug fixes.
- Standardization across teams.

**Why know internals:**

- Interviewers ask "how would you implement..."
- Debugging production issues requires understanding.
- Tuning requires knowing what the library is doing.

---

## G. System Design Concepts

### Distributed Locking Patterns

```java
// Redis-based distributed lock (Redisson library)
public class DistributedLock {
    private final RedissonClient redisson;

    public boolean tryLock(String resource, long waitTime, long leaseTime) {
        RLock lock = redisson.getLock("lock:" + resource);
        return lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
    }
}

// Key considerations:
// - Lock expiration (prevent indefinite locks on crash)
// - Lock renewal (while operation in progress)
// - Redlock algorithm for Redis cluster
```

### Backpressure Patterns

```java
public class BoundedProcessor {
    private final BlockingQueue<Task> queue;
    private final ExecutorService executor;

    public BoundedProcessor(int capacity, int threads) {
        // ArrayBlockingQueue provides backpressure
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(this::processLoop);
        }
    }

    public boolean submit(Task task, long timeoutMs) throws InterruptedException {
        // Blocks or times out when queue full - backpressure
        return queue.offer(task, timeoutMs, TimeUnit.MILLISECONDS);
    }

    private void processLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Task task = queue.take();
                process(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
```

### Idempotency

```java
// Idempotency key pattern for retries
public class IdempotentClient {
    public Response call(Request request, String idempotencyKey) {
        // Server uses idempotencyKey to deduplicate
        // Same key = same response (cached or re-executed with same result)
        return httpClient.post("/api/orders")
                .header("Idempotency-Key", idempotencyKey)
                .body(request)
                .execute();
    }
}

// Generate deterministic key from request content
String key = hash(userId + ":" + orderDetails + ":" + timestampBucket);
```

**Why idempotency matters:**

- Retries REQUIRE idempotent operations to be safe.
- Network timeouts are indistinguishable from slow successes.
- Without idempotency: duplicate charges, duplicate orders.

### Graceful Shutdown Pattern

```java
public class GracefulService {
    private final ExecutorService executor;
    private final BlockingQueue<Task> queue;
    private volatile boolean accepting = true;

    public void shutdown() {
        // 1. Stop accepting new work
        accepting = false;

        // 2. Wait for queue to drain (with timeout)
        long deadline = System.currentTimeMillis() + 30000;
        while (!queue.isEmpty() && System.currentTimeMillis() < deadline) {
            Thread.sleep(100);
        }

        // 3. Shutdown executor
        executor.shutdown();
        try {
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
```

---

## H. Java 17+ Features for Platform Engineering

### Records: Immutable Data Carriers

```java
// Circuit breaker configuration
public record CircuitBreakerConfig(
                int failureThreshold,
                long timeoutDurationMs,
                int halfOpenMaxCalls
        ) {
    // Compact constructor for validation
    public CircuitBreakerConfig {
        if (failureThreshold <= 0) {
            throw new IllegalArgumentException("Threshold must be positive");
        }
    }

    // Factory method
    public static CircuitBreakerConfig defaults() {
        return new CircuitBreakerConfig(5, 60000, 3);
    }
}

// Log entry for metrics
public record LogEntry(
        LogLevel level,
        long timestamp,
        long responseTimeMs,
        String endpoint
) {
}
```

### Switch Expressions

```java
// Cleaner state machines
public State nextState(State current, Event event) {
    return switch (current) {
        case CLOSED -> switch (event) {
            case FAILURE -> consecutiveFailures >= threshold ? State.OPEN : State.CLOSED;
            case SUCCESS -> State.CLOSED;
        };
        case OPEN -> event == Event.TIMEOUT_ELAPSED ? State.HALF_OPEN : State.OPEN;
        case HALF_OPEN -> switch (event) {
            case FAILURE -> State.OPEN;
            case SUCCESS -> successCount >= halfOpenMax ? State.CLOSED : State.HALF_OPEN;
        };
    };
}
```

### Pattern Matching for instanceof

```java
// Cleaner exception handling
public void handleException(Throwable t) {
    if (t instanceof IOException io) {
        // io is already cast to IOException
        log.warn("IO error: {}", io.getMessage());
    } else if (t instanceof SQLException sql && sql.getErrorCode() == 1040) {
        // Pattern matching with condition
        handleTooManyConnections(sql);
    }
}
```

### Virtual Threads (Java 21)

```java
// Project Loom - lightweight threads
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

// Millions of concurrent tasks possible
for(
int i = 0;
i< 1_000_000;i++){
        executor.

submit(() ->{
        // Lightweight - doesn't consume OS thread when blocked
        Thread.

sleep(1000);
        return"Done";
                });
                }

// When to use:
// - High concurrency (10k+ concurrent tasks)
// - I/O-bound workloads
// - Traditional blocking code you can't rewrite

// When NOT to use:
// - CPU-intensive work (pin carrier thread)
// - Heavy synchronization (pin carrier thread)
// - Low concurrency (overhead not worth it)
```

---

## Company-Specific Focus

### Revolut

- **Emphasis**: High-throughput trading systems, low-latency requirements.
- **Key topics**: Lock-free algorithms, memory barriers, CPU cache optimization.
- **Expect**: Questions on StampedLock, LongAdder, false sharing.

### Deel

- **Emphasis**: Platform reliability, global payroll API resilience.
- **Key topics**: Circuit breakers, idempotency, distributed transactions.
- **Expect**: Design for eventual consistency, retry patterns.

### RevenueCat

- **Emphasis**: Subscription infrastructure, webhook reliability.
- **Key topics**: Idempotency, exponential backoff, delivery guarantees.
- **Expect**: Queue-based architectures, at-least-once delivery.

### GitHub

- **Emphasis**: Large-scale systems, Git internals, concurrency at scale.
- **Key topics**: ReadWriteLock, copy-on-write, concurrent data structures.
- **Expect**: Questions on scaling read-heavy workloads.

### Docker

- **Emphasis**: Container orchestration, resource management.
- **Key topics**: Resource pools, bounded queues, backpressure.
- **Expect**: Memory-efficient data structures, graceful degradation.

### Datadog

- **Emphasis**: Observability, metrics pipelines, streaming data.
- **Key topics**: Sliding windows, percentiles, streaming algorithms.
- **Expect**: Time-series data structures, efficient aggregation.

---

## Quick Reference: Code Patterns

### Thread-Safe Singleton

```java
public class Singleton {
    private static volatile Singleton instance;
    private static final Object lock = new Object();

    public static Singleton getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new Singleton();
                }
            }
        }
        return instance;
    }
}

// Or use enum (preferred)
public enum SingletonEnum {
    INSTANCE;
    // methods...
}
```

### Producer-Consumer with BlockingQueue

```java
BlockingQueue<Task> queue = new ArrayBlockingQueue<>(100);

// Producer
queue.

put(task); // Blocks if full

// Consumer
Task task = queue.take(); // Blocks if empty
```

### Graceful Shutdown

```java
public void shutdown() {
    acceptingNewWork = false;
    executor.shutdown();
    if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
        executor.shutdownNow();
    }
}
```

### CompletableFuture Pipeline

```java
CompletableFuture.supplyAsync(this::fetch)
    .

thenApply(this::transform)
    .

thenCompose(this::asyncEnrich)
    .

orTimeout(5,TimeUnit.SECONDS)
    .

exceptionally(this::handleError);
```

---

## Challenge Implementation Notes

### challenge01: CircuitBreaker

- Use `ReentrantLock` for state transitions.
- Use `volatile` for counters accessed without lock.
- State machine: CLOSED → OPEN → HALF_OPEN → CLOSED.

### challenge04: RateLimiter

- Token bucket: track `tokens` and `lastRefillTime`.
- Calculate tokens to add: `elapsed * refillRate`.
- Support `tryAcquire(int tokens, long timeout, TimeUnit)`.

### challenge05: MetricsAggregator

- Sliding window: `Queue<LogEntry>` with eviction.
- P95: Sort response times, pick index at 95%.
- Error rate: Count ERROR + FATAL / total.

### challenge07: LruCache

- Doubly-linked list + HashMap (NOT LinkedHashMap).
- Node: `K key, V value, Node prev, Node next`.
- Move to head on access, evict from tail.

### challenge08: ResourcePool

- BlockingQueue for available resources.
- Health check on acquire.
- `AutoCloseable` for cleanup.

### challenge10: TaskScheduler

- DelayQueue with `Delayed` tasks.
- Fixed thread pool for execution.
- `Future` for result tracking.
- Graceful shutdown: drain queue, await termination.

---

**Good luck with your platform engineering interviews!**
