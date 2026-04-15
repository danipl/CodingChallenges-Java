# Challenge 02: Load Balancer - Live Coding Guidelines

## 1. Challenge Presentation

### What You're Building

A **thread-safe weighted load balancer** - routes incoming requests across a pool of backend servers, distributing
traffic proportionally to each server's configured weight. Heavier servers receive more traffic.

### Core Contract

```
add("host1:8080", weight=3)  ─┐
add("host2:8080", weight=1)  ─┼── next() ──▶ 75% host1, 25% host2
                               │  next() ──▶ probabilistic distribution
next() ───────────────────────┘              weighted by server capacity
```

### Interface Summary

| Method                          | Purpose                                               |
|---------------------------------|-------------------------------------------------------|
| `add(Server)`                   | Register a server; duplicate host:port merges weights |
| `remove(String host, int port)` | Remove a server by address (no-op if missing)         |
| `next()`                        | Select the next server to route a request to          |

### Server Record

| Field    | Type   | Meaning                           |
|----------|--------|-----------------------------------|
| `host`   | String | Server hostname or IP             |
| `port`   | int    | Server port                       |
| `weight` | int    | Traffic proportion (must be >= 1) |

### What Interviewers Evaluate

1. **Weight distribution correctness** - servers receive traffic proportional to their weights
2. **Thread safety** - concurrent `add`/`remove`/`next` don't corrupt state or crash
3. **Dynamic reconfiguration** - servers can be added/removed at runtime without disrupting routing
4. **Complexity awareness** - candidate explains time/space trade-offs of their algorithm choice
5. **Edge case handling** - empty pool, invalid weights, duplicate servers, concurrent mutation

---

## 2. Edge & Corner Cases

### How to Identify Them Before Coding

Draw the routing flow first. Every input validation and every state change is a potential edge case.

| #  | Edge Case                             | How It Surfaces                                                  | How to Handle                                                |
|----|---------------------------------------|------------------------------------------------------------------|--------------------------------------------------------------|
| 1  | **Weight < 1**                        | `add(new Server("h", 80, 0))` or negative weight                 | Validate in `add()`, throw `IllegalArgumentException`        |
| 2  | **Empty pool**                        | `next()` called before any `add()` or after removing all servers | Throw `NoSuchElementException`                               |
| 3  | **Duplicate server (same host:port)** | `add("h1:80", w=2)` then `add("h1:80", w=3)`                     | Merge weights: effective weight = 5                          |
| 4  | **Remove non-existent server**        | `remove("unknown", 99)` when it was never added                  | No-op, must not throw                                        |
| 5  | **Remove during active routing**      | Thread A calls `next()`, Thread B removes the selected server    | Lock guards consistency; caller gets a valid server snapshot |
| 6  | **Add during active routing**         | Thread A in `next()`, Thread B adds a new server                 | Write lock serializes; next call sees the new server         |
| 7  | **Single server in pool**             | One server with any weight                                       | `next()` always returns it                                   |
| 8  | **All servers equal weight**          | 3 servers, weight=1 each                                         | Equal 33.3% distribution each                                |
| 9  | **Large weight disparity**            | host1 weight=1000, host2 weight=1                                | Distribution should still be ~1000:1                         |
| 10 | **Concurrent add/remove/next**        | 10 threads × 100 ops, mix of all three operations                | No exceptions, no hangs, all ops complete                    |

### Quick Pre-Implementation Checklist

```
▢ add() validates weight >= 1
▢ add() merges weights when host:port already exists
▢ remove() is idempotent (no-op for missing servers)
▢ next() throws NoSuchElementException when pool is empty
▢ next() distributes traffic proportional to weights
▢ next() handles dynamic pool changes correctly
▢ Thread safety: read-write split for concurrent next() calls
▢ Keys are collision-free (IPv6 hosts with colons)
▢ No memory leaks: removed servers are fully cleaned up
```

---

## 3. First Approach - Chain of Thinking

### Minute 0-2: Clarify Requirements

Ask the interviewer:

- *"Should routing be deterministic (round-robin) or probabilistic (weighted random)?"* → Both are valid; pick one and
  explain trade-offs.
- *"What happens when the same server is added twice with different weights?"* → Merge weights.
- *"Should remove() throw if server doesn't exist?"* → No, make it idempotent.
- *"How should thread safety work?"* → Concurrent `next()` calls should not block each other if possible.

### Minute 2-5: Pick the Algorithm

Two solid options:

**Option A: Weighted Random (recommended for interview)**

```
- Space: O(n) - just a HashMap
- next():  O(n) - scan through weights
- add():   O(1) - put or merge in map
- remove(): O(1) - remove from map

Simple to implement, no pre-computed structures, bounded memory.
```

**Option B: Weighted Round-Robin (expansion list)**

```
- Space:  O(Σ weights) - expanded array
- next():  O(1) - array index lookup
- add():   O(n × max_weight) - rebuild list
- remove(): O(n × max_weight) - rebuild list

Deterministic ordering, but memory explodes with high weights.
```

Say to interviewer: *"I'll go with weighted random selection. It's O(n) space regardless of weights, and O(1) mutations.
The trade-off is O(n) per selection call instead of O(1). For pools under 100 servers this is negligible, and I can
mention optimization to O(log n) with binary search on prefix sums."*

### Minute 5-10: Sketch the Core Flow

```java
// Data structures:
Map<ServerKey, Server> serverMap   // host:port → Server with effective weight
int totalWeight                     // sum of all server weights (cached)
ReentrantReadWriteLock              // ReadLock for next(), WriteLock for add/remove()
```

### Minute 10-20: Implement

```java
Server next() {
    readLock.lock();
    try {
        if (serverMap.isEmpty()) throw NoSuchElementException;

        // Pick random point in [0, totalWeight)
        int position = random.nextInt(totalWeight);

        // Find which server owns this position
        int cumulative = 0;
        for (var server : serverMap.values()) {
            cumulative += server.weight();
            if (position < cumulative) return server;
        }
        return fallback();  // defensive - should not reach
    } finally {
        readLock.unlock();
    }
}
```

### Minute 20-25: Test & Verify

- Single server always returned
- Two servers with 3:1 ratio → ratio between 2.5-3.5 over 4000 calls
- Empty pool throws
- Duplicate adds merge weights

---

## 4. Communication Approach During the Interview

### What to Say Out Loud

| Moment           | Say This                                                                                                                                                                                              |
|------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Starting         | "I need a weighted load balancer with thread-safe add, remove, and next operations. I'll use a HashMap keyed by host:port, protected by a ReadWriteLock to allow concurrent reads."                   |
| Algorithm choice | "I chose weighted random selection over round-robin expansion. Space is O(n) instead of O(Σ weights), mutations are O(1), and the code is simpler. The trade-off is O(n) per next() call."            |
| Before locking   | "I'm using ReentrantReadWriteLock because next() is read-heavy - multiple threads can route simultaneously. Only add/remove need exclusive access."                                                   |
| About weights    | "I cache totalWeight to avoid recalculating it on every next() call. It's updated atomically under the write lock when servers are added or removed."                                                 |
| About keying     | "I'm using a record for the key instead of string concatenation. This avoids issues with IPv6 addresses that contain colons, like [::1]:8080."                                                        |
| About fallback   | "The fallback return after the loop is defensive - mathematically it should never execute because totalWeight equals the sum of all weights. But in production, I prefer a safe fallback over a bug." |

### When Stuck

```
I notice [specific issue]. 
The risk is [consequence]. 
Two options: [A] weighted random with O(n) scan, or [B] prefix-sum array with O(log n) binary search. 
I'll go with [A] for simplicity, with O(log n) as an optimization if the pool grows. Does that align with your expectations?
```

---

## 5. Implementation Structure

### Recommended File Layout

```java
public class LoadBalancerImpl implements LoadBalancer {
    // === Key record ===
    private record ServerKey(String host, int port) {
    }

    // === Fields ===
    private final ReentrantReadWriteLock rwLock;
    private final ReadLock readLock;
    private final WriteLock writeLock;
    private final Map<ServerKey, Server> serverMap;
    private int totalWeight;                      // cached sum of all weights

    // === add(Server) ===                        ← Validate weight, merge or add, update totalWeight
    // === remove(String, int) ===                ← Remove from map, update totalWeight if existed
    // === next() ===                             ← Random selection via cumulative weights
    // === resolveKey(host, port) ===             ← Factory for ServerKey
}
```

### Weighted Random Selection Logic

```
totalWeight = 4
  host1: weight 3  ← owns positions [0, 1, 2]
  host2: weight 1  ← owns position [3]

random(4) = 0,1,2 → host1 (75%)
random(4) = 3     → host2 (25%)
```

This cumulative sum approach naturally handles any weight distribution without pre-computing structures.

---

## 6. Technical Pro Tips

### Algorithm Comparison

| Approach                     | Space        | next()   | add() / remove() | Deterministic?     | Interview Fit     |
|------------------------------|--------------|----------|------------------|--------------------|-------------------|
| **Weighted Random (chosen)** | O(n)         | O(n)     | O(1)             | No (probabilistic) | ✅ Best            |
| Expansion List (round-robin) | O(Σ weights) | O(1)     | O(n × m)         | Yes                | Good, but complex |
| Prefix-Sum + Binary Search   | O(n)         | O(log n) | O(n)             | No                 | Advanced          |
| Smooth Weighted Round-Robin  | O(n)         | O(n)     | O(1)             | Yes                | Production        |

### Nginx Smooth Weighted Round-Robin (mention as production alternative)

Nginx uses effective-current weight math to avoid list expansion:

```
Each server has: weight (configured), effectiveWeight (dynamic), currentWeight (accumulator)
Selection: pick server with highest currentWeight
Update:   selected.currentWeight -= totalWeight; all.servers.currentWeight += effectiveWeight
```

This gives deterministic weighted round-robin in O(n) per selection with O(1) mutations. Worth mentioning.

### ThreadLocalRandom vs Math.random()

| Approach                      | Thread-safe?     | Contention? | Performance            |
|-------------------------------|------------------|-------------|------------------------|
| `Math.random()`               | Yes (sync'd)     | High        | Poor under concurrency |
| `ThreadLocalRandom.current()` | Yes (per-thread) | None        | Best                   |

For production, prefer `ThreadLocalRandom`. In an interview, `Math.random()` is acceptable but mentioning
`ThreadLocalRandom` shows awareness.

### Load Balancing vs Related Patterns

| Pattern               | Problem It Solves                  | Key Question                       |
|-----------------------|------------------------------------|------------------------------------|
| **Load Balancer**     | Distribute traffic across servers  | "Which server should handle this?" |
| **Circuit Breaker**   | Stop routing to unhealthy services | "Is this server down?"             |
| **Health Check**      | Detect server failures proactively | "Is this server healthy?"          |
| **Service Discovery** | Find available servers dynamically | "What servers exist?"              |

**Production pattern**: Service Discovery finds servers → Health Check filters unhealthy ones → Load Balancer picks
among healthy → Circuit Breaker stops routing to degraded ones.

### Testing Strategy

```java
// Statistical test: over N calls, verify ratio is within acceptable range
int totalCalls = 4000;
Map<String, AtomicInteger> counts = new HashMap<>();
for(
int i = 0;
i<totalCalls;i++){
Server s = lb.next();
    counts.

get(s.host()).

incrementAndGet();
}
double ratio = (double) host1Count / host2Count;

assertTrue(ratio >=2.5&&ratio<=3.5, "Expected ~3.0 ratio but was "+ratio);

// Thread safety: concurrent ops complete without exceptions
ExecutorService es = Executors.newFixedThreadPool(10);
CountDownLatch latch = new CountDownLatch(10);

// ... mix of add, remove, next calls ...
assertTrue(latch.await(10, SECONDS));
```

### What Senior Engineers Demonstrate

1. **Complexity trade-off awareness** - "I chose O(n) per call over O(Σ weights) space. If this becomes a bottleneck,
   binary search drops it to O(log n)."
2. **Observability mindset** - "In production, I'd track requests per server, p99 latency per backend, and failed
   routing attempts."
3. **Production hardening** - "I'd add server health tracking - remove unhealthy servers automatically, not just
   manually."
4. **Consistent hashing mention** - "For session-affinity requirements, I'd use consistent hashing instead of random
   selection."
5. **Graceful degradation** - "If totalWeight becomes 0 (all servers removed), I'd throw a specific exception rather
   than generic NoSuchElementException."

---

## 7. Common Mistakes to Avoid

| Mistake                                                      | Why It Fails                                 | Fix                                            |
|--------------------------------------------------------------|----------------------------------------------|------------------------------------------------|
| String key `"host:port"` with hostnames containing colons    | IPv6 addresses like `[::1]:80` break the key | Use a record or tuple as key                   |
| Recalculating totalWeight on every `next()`                  | O(n) overhead, unnecessary                   | Cache totalWeight, update on add/remove        |
| Using `Random` shared instance under lock                    | Contention bottleneck                        | Use `ThreadLocalRandom`                        |
| `add()` replacing existing server instead of merging weights | Loses accumulated capacity                   | Sum weights when key already exists            |
| `remove()` throws for missing servers                        | Forces caller to check before removing       | Make idempotent - no-op for missing            |
| `next()` returns null when pool is empty                     | NullPointerException downstream              | Throw `NoSuchElementException`                 |
| Not updating totalWeight on remove                           | Position calculated beyond actual weights    | Decrement totalWeight by removed server weight |
| Holding write lock during entire `next()`                    | Serializes all reads unnecessarily           | Use ReadLock for `next()`                      |

---

## 8. Verification Checklist

### Functional

- [ ] `add()` with weight < 1 throws `IllegalArgumentException`
- [ ] `add()` with valid weight adds server to pool
- [ ] Duplicate `add()` (same host:port) merges weights
- [ ] `remove()` removes server and updates totalWeight
- [ ] `remove()` of non-existent server does not throw
- [ ] `next()` returns correct server under random distribution
- [ ] `next()` throws `NoSuchElementException` when pool is empty
- [ ] Weight distribution matches configured ratios (within statistical tolerance)
- [ ] Equal weights produce near-equal distribution
- [ ] Dynamic add: new server receives traffic after being added
- [ ] Dynamic remove: removed server receives no more traffic

### Thread Safety

- [ ] Concurrent `add`/`remove`/`next` complete without exceptions
- [ ] `ReadWriteLock` allows concurrent readers (next calls)
- [ ] All mutations (add/remove) protected by WriteLock
- [ ] totalWeight is consistent with actual sum of weights
- [ ] No deadlocks (simple lock hierarchy, single lock)

### Edge Cases

- [ ] Single server always returned
- [ ] Two servers with large weight disparity still distribute correctly
- [ ] Remove during routing doesn't cause ConcurrentModificationException
- [ ] Add during routing immediately affects distribution
- [ ] Removing all servers, then adding new ones works correctly

---

## 9. Extension Points (Bonus Discussion)

If you finish early, mention these to stand out:

1. **Consistent Hashing** - "For sticky sessions or cache-aware routing, I'd implement consistent hashing. Each server
   gets virtual nodes on a hash ring, and requests are routed to the nearest node."
2. **Least Connections** - "Instead of weight-only, I could factor in active connection count to prefer less-loaded
   servers."
3. **Health-Aware Routing** - "Integrate with a health check system. Automatically mark servers as unhealthy and exclude
   them from routing. Re-enable on health recovery."
4. **Prefix-Sum Binary Search** - "If pool grows beyond 100-200 servers, I'd build a sorted prefix-sum array and use
   binary search for O(log n) selection. Rebuilt on add/remove."
5. **Smooth Weighted Round-Robin** - "For deterministic distribution (no statistical variance), Nginx's algorithm uses
   effective-current weight accumulation. O(n) per call, O(1) mutations."
6. **Circuit Breaker Integration** - "Combine with Challenge 01's circuit breaker. Stop routing to servers that have
   tripped their circuit breaker while keeping them in the pool for health checks."
7. **Connection Persistence** - "For TCP/TLS-heavy backends, I'd add connection pooling and route to the server with
   available capacity."
8. **Metrics & Observability** - "Track per-server request count, error rate, and latency. Enable dashboards and
   alerting on routing anomalies."

---

## 10. Production References

| Resource                                                                                                     | Why It Matters                           |
|--------------------------------------------------------------------------------------------------------------|------------------------------------------|
| [Nginx Load Balancing](https://nginx.org/en/docs/http/load_balancing.html)                                   | Industry standard, smooth weighted RR    |
| [Envoy Proxy](https://www.envoyproxy.io/docs/envoy/latest/intro/arch_overview/upstream/load_balancing)       | Modern LB with multiple algorithms       |
| [Kubernetes Services](https://kubernetes.io/docs/concepts/services-networking/service/)                      | kube-proxy load balancing, iptables/IPVS |
| [AWS ELB/ALB](https://docs.aws.amazon.com/elasticloadbalancing/latest/userguide/what-is-load-balancing.html) | Cloud-native load balancing patterns     |
| [Ribbon (Netflix)](https://github.com/Netflix/ribbon) (archived)                                             | Historical client-side LB implementation |
| [Apache Commons LoadBalancer](https://commons.apache.org/proper/commons-pool/)                               | Server object pool with LB capabilities  |

---

*This guideline follows the same structure as Challenge 01. Each challenge provides: presentation → edge cases → chain
of thinking → communication → implementation → pro tips → mistakes → verification.*