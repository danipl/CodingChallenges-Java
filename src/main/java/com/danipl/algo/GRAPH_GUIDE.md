# Java 21 Graph Algorithms Guide

## Quick-Reference: Graph Representation Selection Matrix

| Use Case                         | Representation         | Space    | Add Edge       | Check Edge | Iterate Neighbors | When to Use                           |
|----------------------------------|------------------------|----------|----------------|------------|-------------------|---------------------------------------|
| **Sparse graphs (E << V²)**      | Adjacency List         | O(V + E) | O(1)           | O(degree)  | O(degree)         | Default for interviews, most problems |
| **Dense graphs (E ≈ V²)**        | Adjacency Matrix       | O(V²)    | O(1)           | O(1)       | O(V)              | Small V, need O(1) edge lookup        |
| **Edge list processing**         | Edge List              | O(E)     | O(1)           | O(E)       | O(E)              | Kruskal's, edge-based algorithms      |
| **Dynamic graphs**               | Adjacency List + Map   | O(V + E) | O(1) amortized | O(degree)  | O(degree)         | Node labels not integers              |
| **Undirected graphs**            | Adjacency List (both)  | O(V + E) | O(1)           | O(degree)  | O(degree)         | Store both directions                 |
| **Weighted graphs**              | Adjacency List + Pair  | O(V + E) | O(1)           | O(degree)  | O(degree)         | Store (neighbor, weight) pairs        |
| **Multi-graph (parallel edges)** | Adjacency List + Count | O(V + E) | O(1)           | O(degree)  | O(degree)         | Multiple edges between same nodes     |

### At-A-Glance Decision Flow

```
Need to represent a graph?
├─ Sparse graph (E << V²)?
│   └─ YES → Adjacency List (default choice)
│             ├─ Integer nodes 0..n-1 → List<List<Integer>> or List<List<int[]>> for weighted
│             └─ String/complex nodes → Map<String, List<String>> (DependencyResolverImpl pattern)
├─ Dense graph (E ≈ V²) AND need O(1) edge lookup?
│   └─ YES → Adjacency Matrix boolean[][] or int[][]
├─ Edge-based algorithm (Kruskal's)?
│   └─ YES → Edge List: List<int[]> where int[] = {u, v, weight}
└─ Need both directions (undirected)?
    └─ Add edge in BOTH directions: adj[u].add(v); adj[v].add(u)
```

---

## Overview

Graph problems in interviews fall into recognizable patterns: shortest path, connectivity, topological ordering, cycle
detection, and component counting. The right representation + algorithm choice is half the solution.

**Core graph problem types:**

- **Shortest Path**: BFS (unweighted), Dijkstra (weighted non-negative), Bellman-Ford (negative weights)
- **Connectivity**: Union-Find, DFS/BFS component counting
- **Topological Sort**: Kahn's algorithm (BFS), DFS with post-order
- **Cycle Detection**: 3-color DFS, topological sort failure
- **Component Analysis**: Connected components, strongly connected components

**Java graph representation patterns:**

- `Map<String, List<String>>` — string-labeled nodes (DependencyResolverImpl)
- `List<List<Integer>>` — integer nodes 0 to n-1
- `List<List<int[]>>` — weighted edges (neighbor, weight)
- `Map<Integer, Map<Integer, Integer>>` — adjacency matrix as nested map

---

## 1. BFS on Graphs (Shortest Path Unweighted)

```java
Queue<String> queue = new ArrayDeque<>();
Set<String> visited = new HashSet<>();
```

### Characteristics

| Property           | Value                               |
|--------------------|-------------------------------------|
| **Ordering**       | Layer-by-layer, shortest unweighted |
| **Data Structure** | ArrayDeque for queue                |
| **Visited Track**  | HashSet or boolean[]                |
| **Complexity**     | O(V + E) time, O(V) space           |
| **Level Tracking** | Queue size trick or distance map    |

### Complexity

| Operation            | Time     | Space |
|----------------------|----------|-------|
| BFS traversal        | O(V + E) | O(V)  |
| Shortest path        | O(V + E) | O(V)  |
| Cycle detection      | O(V + E) | O(V)  |
| Connected components | O(V + E) | O(V)  |

### When to Use

- **Shortest path in unweighted graph** — minimum edges between nodes
- **Level-order traversal** — tree/graph level extraction
- **Minimum transformations** — word ladder, state transitions
- **Cycle detection in directed graph** — with visited + recursion stack
- **Connected components** — count/isolate components

### Magic Methods/Patterns

```java
// Standard BFS with visited set
public List<String> bfs(Map<String, List<String>> graph, String start) {
    Deque<String> queue = new ArrayDeque<>();
    Set<String> visited = new HashSet<>();
    List<String> result = new ArrayList<>();

    queue.offer(start);
    visited.add(start);

    while (!queue.isEmpty()) {
        String node = queue.poll();
        result.add(node);

        for (String neighbor : graph.getOrDefault(node, List.of())) {
            if (visited.add(neighbor)) {  // Returns false if already present
                queue.offer(neighbor);
            }
        }
    }

    return result;
}

// BFS with level tracking (Queue size trick)
public List<List<Integer>> levelOrder(Map<Integer, List<Integer>> graph, int start) {
    List<List<Integer>> levels = new ArrayList<>();
    Deque<Integer> queue = new ArrayDeque<>();
    Set<Integer> visited = new HashSet<>();

    queue.offer(start);
    visited.add(start);

    while (!queue.isEmpty()) {
        int levelSize = queue.size();  // Capture current level size
        List<Integer> level = new ArrayList<>();

        for (int i = 0; i < levelSize; i++) {
            Integer node = queue.poll();
            level.add(node);

            for (Integer neighbor : graph.getOrDefault(node, List.of())) {
                if (visited.add(neighbor)) {
                    queue.offer(neighbor);
                }
            }
        }

        levels.add(level);
    }

    return levels;
}

// BFS with distance tracking
public Map<String, Integer> distances(Map<String, List<String>> graph, String start) {
    Map<String, Integer> dist = new HashMap<>();
    Deque<String> queue = new ArrayDeque<>();

    queue.offer(start);
    dist.put(start, 0);

    while (!queue.isEmpty()) {
        String node = queue.poll();
        int d = dist.get(node);

        for (String neighbor : graph.getOrDefault(node, List.of())) {
            if (!dist.containsKey(neighbor)) {
                dist.put(neighbor, d + 1);
                queue.offer(neighbor);
            }
        }
    }

    return dist;
}
```

**Practical BFS — DependencyResolverImpl pattern (Kahn's algorithm):**

```java
// From DependencyResolverImpl.java — Kahn's algorithm for topological sort
public List<String> resolveBuildOrder() throws CircularDependencyException {
    // Take snapshot under read lock
    LinkedHashMap<String, List<String>> snapshot;
    try {
        readLock.lock();
        snapshot = new LinkedHashMap<>(libraryMap);
    } finally {
        readLock.unlock();
    }

    // Compute in-degrees
    Map<String, Integer> inDegree = new HashMap<>();
    Map<String, List<String>> reverseGraph = new LinkedHashMap<>();

    for (var entry : snapshot.entrySet()) {
        String library = entry.getKey();
        inDegree.putIfAbsent(library, 0);

        for (String dep : entry.getValue()) {
            inDegree.merge(library, 1, Integer::sum);
            inDegree.putIfAbsent(dep, 0);
            reverseGraph.computeIfAbsent(dep, k -> new ArrayList<>()).add(library);
        }
    }

    // Initialize queue with zero in-degree nodes
    Deque<String> queue = new ArrayDeque<>();
    for (var entry : inDegree.entrySet()) {
        if (entry.getValue() == 0) {
            queue.add(entry.getKey());
        }
    }

    // Process layer by layer
    List<String> order = new ArrayList<>();
    while (!queue.isEmpty()) {
        String node = queue.poll();
        order.add(node);

        for (String neighbor : reverseGraph.getOrDefault(node, List.of())) {
            if (inDegree.merge(neighbor, -1, Integer::sum) == 0) {
                queue.add(neighbor);
            }
        }
    }

    // Cycle detection: not all nodes processed
    if (order.size() != inDegree.size()) {
        throw new CircularDependencyException("Circular dependency detected");
    }

    return order;
}
```

---

## 2. DFS on Graphs (Recursive and Iterative)

```java
// Recursive DFS
void dfs(Map<String, List<String>> graph, String node, Set<String> visited) {
    if (visited.add(node)) {
        for (String neighbor : graph.getOrDefault(node, List.of())) {
            dfs(graph, neighbor, visited);
        }
    }
}

// Iterative DFS with explicit stack
void dfsIterative(Map<String, List<String>> graph, String start) {
    Deque<String> stack = new ArrayDeque<>();
    Set<String> visited = new HashSet<>();

    stack.push(start);
    while (!stack.isEmpty()) {
        String node = stack.pop();
        if (!visited.add(node)) continue;

        // Process node here

        // Push neighbors (reverse order to match recursive order)
        List<String> neighbors = graph.getOrDefault(node, List.of());
        for (int i = neighbors.size() - 1; i >= 0; i--) {
            stack.push(neighbors.get(i));
        }
    }
}
```

### Characteristics

| Property            | Value                                       |
|---------------------|---------------------------------------------|
| **Ordering**        | Depth-first, explore fully before backtrack |
| **Data Structure**  | Recursion stack or ArrayDeque               |
| **Visited Track**   | HashSet, boolean[], or 3-color state        |
| **Complexity**      | O(V + E) time, O(V) space (recursion)       |
| **Cycle Detection** | 3-color DFS (NEW, VISITING, VISITED)        |

### Complexity

| Operation                 | Time     | Space | Notes                       |
|---------------------------|----------|-------|-----------------------------|
| DFS traversal             | O(V + E) | O(V)  | Recursion depth can be O(V) |
| Connected components      | O(V + E) | O(V)  | Count via DFS calls         |
| Cycle detection (3-color) | O(V + E) | O(V)  | WHITE, GRAY, BLACK states   |
| Path finding              | O(V + E) | O(V)  | With path reconstruction    |

### When to Use

- **Connected components** — count/isolate disconnected subgraphs
- **Cycle detection** — 3-color DFS for directed graphs
- **Path existence** — any path from A to B
- **Topological sort** — DFS with post-order (reverse finishing times)
- **Strongly connected components** — Kosaraju's, Tarjan's algorithms

### 3-Color DFS Cycle Detection

```java
// States for directed graph cycle detection
enum State {NEW, VISITING, VISITED}

// From DependencyResolverImpl.java — 3-color DFS
public boolean hasCircularDependency() {
    LinkedHashMap<String, List<String>> snapshot;
    try {
        readLock.lock();
        snapshot = new LinkedHashMap<>(libraryMap);
    } finally {
        readLock.unlock();
    }

    Map<String, State> visitState = new HashMap<>();

    // Start DFS from each unvisited node (handles disconnected graphs)
    for (String library : snapshot.keySet()) {
        if (hasCycleDfs(snapshot, library, visitState)) {
            return true;
        }
    }

    return false;
}

private boolean hasCycleDfs(
        LinkedHashMap<String, List<String>> graph,
        String node,
        Map<String, State> visitState
) {
    State state = visitState.getOrDefault(node, State.NEW);

    if (state == State.VISITED) {
        return false;  // Already fully explored, no cycle through here
    }

    if (state == State.VISITING) {
        return true;   // Back edge found — cycle detected!
    }

    // Mark as being explored
    visitState.put(node, State.VISITING);

    // Explore all neighbors
    for (String neighbor : graph.getOrDefault(node, List.of())) {
        if (hasCycleDfs(graph, neighbor, visitState)) {
            return true;
        }
    }

    // Mark as fully explored
    visitState.put(node, State.VISITED);
    return false;
}
```

**Practical connected components:**

```java
// Count connected components in undirected graph
public int countComponents(int n, int[][] edges) {
    // Build adjacency list
    Map<Integer, List<Integer>> graph = new HashMap<>();
    for (int i = 0; i < n; i++) {
        graph.put(i, new ArrayList<>());
    }
    for (int[] edge : edges) {
        graph.get(edge[0]).add(edge[1]);
        graph.get(edge[1]).add(edge[0]);  // Undirected
    }

    Set<Integer> visited = new HashSet<>();
    int components = 0;

    // DFS from each unvisited node
    for (int i = 0; i < n; i++) {
        if (!visited.contains(i)) {
            components++;
            dfsComponent(graph, i, visited);
        }
    }

    return components;
}

private void dfsComponent(Map<Integer, List<Integer>> graph, int node, Set<Integer> visited) {
    if (!visited.add(node)) return;

    for (int neighbor : graph.get(node)) {
        dfsComponent(graph, neighbor, visited);
    }
}
```

---

## 3. Dijkstra's Shortest Path (Weighted Graphs)

```java
PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
// int[] = {node, distance}
```

### Characteristics

| Property           | Value                               |
|--------------------|-------------------------------------|
| **Ordering**       | Extract minimum distance node first |
| **Data Structure** | PriorityQueue + distance array/map  |
| **Visited Track**  | Distance comparison (skip if worse) |
| **Complexity**     | O((V + E) log V) with binary heap   |
| **Constraints**    | Non-negative edge weights only      |

### Complexity

| Operation            | Time           | Space |
|----------------------|----------------|-------|
| Dijkstra             | O((V+E) log V) | O(V)  |
| With Fibonacci heap  | O(E + V log V) | O(V)  |
| Dense graph (matrix) | O(V²)          | O(V)  |

### When to Use

- **Shortest path in weighted graph** — non-negative edge weights
- **Network latency** — minimum cost path
- **Pathfinding with costs** — terrain costs, toll roads
- **Multi-source shortest path** — initialize multiple sources at distance 0

### Magic Methods/Patterns

```java
// Compute if absent pattern for distance map
Map<Integer, Integer> dist = new HashMap<>();
dist.

computeIfAbsent(node, k ->Integer.MAX_VALUE);

// Relaxation pattern
        if(dist.

get(u) +weight <dist.

get(v)){
        dist.

put(v, dist.get(u) +weight);
        pq.

offer(new int[] {
    v, dist.get(v)
});
        }
```

**Practical Dijkstra implementation:**

```java
public int[] shortestPath(int n, List<List<int[]>> graph, int start) {
    // graph.get(u) = List of int[]{v, weight}
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

        // Relax all neighbors
        for (int[] edge : graph.get(u)) {
            int v = edge[0], weight = edge[1];

            if (dist[u] + weight < dist[v]) {
                dist[v] = dist[u] + weight;
                pq.offer(new int[]{v, dist[v]});
            }
        }
    }

    return dist;  // dist[i] = shortest distance from start to i
}

// Dijkstra with path reconstruction
public List<Integer> shortestPathWithReconstruction(
        int n,
        List<List<int[]>> graph,
        int start,
        int end
) {
    int[] dist = new int[n];
    int[] parent = new int[n];
    Arrays.fill(dist, Integer.MAX_VALUE);
    Arrays.fill(parent, -1);
    dist[start] = 0;

    PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
    pq.offer(new int[]{start, 0});

    while (!pq.isEmpty()) {
        int[] current = pq.poll();
        int u = current[0], d = current[1];

        if (d > dist[u]) continue;
        if (u == end) break;  // Early exit

        for (int[] edge : graph.get(u)) {
            int v = edge[0], weight = edge[1];
            if (dist[u] + weight < dist[v]) {
                dist[v] = dist[u] + weight;
                parent[v] = u;
                pq.offer(new int[]{v, dist[v]});
            }
        }
    }

    // Reconstruct path
    if (dist[end] == Integer.MAX_VALUE) {
        return List.of();  // No path
    }

    List<Integer> path = new ArrayList<>();
    for (int at = end; at != -1; at = parent[at]) {
        path.add(at);
    }
    Collections.reverse(path);
    return path;
}
```

---

## 4. Topological Sort (DAG Linearization)

```java
// Kahn's algorithm (BFS-based)
// DFS-based with post-order
```

### Characteristics

| Property            | Value                           |
|---------------------|---------------------------------|
| **Ordering**        | Dependencies before dependents  |
| **Prerequisite**    | Graph must be a DAG (no cycles) |
| **Approaches**      | Kahn's (BFS) or DFS post-order  |
| **Complexity**      | O(V + E) time, O(V) space       |
| **Cycle Detection** | Built-in (Kahn's: count < V)    |

### Complexity

| Operation            | Time     | Space |
|----------------------|----------|-------|
| Kahn's algorithm     | O(V + E) | O(V)  |
| DFS topological sort | O(V + E) | O(V)  |
| Cycle detection      | O(V + E) | O(V)  |

### When to Use

- **Build order** — DependencyResolverImpl pattern
- **Task scheduling** — prerequisite constraints
- **Course schedule** — curriculum dependencies
- **Deadlock detection** — wait-for graph cycle detection

### Kahn's Algorithm (BFS-Based) — See DependencyResolverImpl.java

```java
public List<String> topologicalSortKahn(Map<String, List<String>> graph) {
    // Compute in-degrees
    Map<String, Integer> inDegree = new HashMap<>();

    for (String node : graph.keySet()) {
        inDegree.putIfAbsent(node, 0);
        for (String neighbor : graph.get(node)) {
            inDegree.merge(neighbor, 1, Integer::sum);
            inDegree.putIfAbsent(neighbor, 0);
        }
    }

    // Initialize queue with zero in-degree nodes
    Deque<String> queue = new ArrayDeque<>();
    for (var entry : inDegree.entrySet()) {
        if (entry.getValue() == 0) {
            queue.add(entry.getKey());
        }
    }

    // Process nodes
    List<String> order = new ArrayList<>();
    while (!queue.isEmpty()) {
        String node = queue.poll();
        order.add(node);

        for (String neighbor : graph.getOrDefault(node, List.of())) {
            if (inDegree.merge(neighbor, -1, Integer::sum) == 0) {
                queue.add(neighbor);
            }
        }
    }

    // Cycle detection: not all nodes processed
    if (order.size() != inDegree.size()) {
        throw new IllegalStateException("Cycle detected — not a DAG");
    }

    return order;
}
```

### DFS-Based Topological Sort

```java
public List<String> topologicalSortDfs(Map<String, List<String>> graph) {
    Set<String> visited = new HashSet<>();
    Set<String> recStack = new HashSet<>();  // For cycle detection
    List<String> result = new ArrayList<>();

    for (String node : graph.keySet()) {
        if (!visited.contains(node)) {
            if (!topologicalSortDfsHelper(graph, node, visited, recStack, result)) {
                throw new IllegalStateException("Cycle detected — not a DAG");
            }
        }
    }

    Collections.reverse(result);  // Reverse post-order
    return result;
}

private boolean topologicalSortDfsHelper(
        Map<String, List<String>> graph,
        String node,
        Set<String> visited,
        Set<String> recStack,
        List<String> result
) {
    if (recStack.contains(node)) {
        return false;  // Cycle detected
    }
    if (visited.contains(node)) {
        return true;   // Already processed
    }

    visited.add(node);
    recStack.add(node);

    for (String neighbor : graph.getOrDefault(node, List.of())) {
        if (!topologicalSortDfsHelper(graph, neighbor, visited, recStack, result)) {
            return false;
        }
    }

    recStack.remove(node);
    result.add(node);  // Add to result after all children processed
    return true;
}
```

---

## 5. Union-Find (Disjoint Set Union - DSU)

```java
class UnionFind {
    private final int[] parent;
    private final int[] rank;

    public UnionFind(int n) {
        parent = new int[n];
        rank = new int[n];
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
    }

    public int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]);  // Path compression
        }
        return parent[x];
    }

    public boolean union(int x, int y) {
        int px = find(x), py = find(y);
        if (px == py) return false;

        // Union by rank
        if (rank[px] < rank[py]) {
            parent[px] = py;
        } else if (rank[px] > rank[py]) {
            parent[py] = px;
        } else {
            parent[py] = px;
            rank[px]++;
        }
        return true;
    }

    public boolean isConnected(int x, int y) {
        return find(x) == find(y);
    }
}
```

### Characteristics

| Property          | Value                                  |
|-------------------|----------------------------------------|
| **Operations**    | find, union, isConnected               |
| **Optimizations** | Path compression + union by rank       |
| **Complexity**    | O(α(n)) ≈ O(1) amortized per operation |
| **Space**         | O(n)                                   |
| **Use Case**      | Connected components, Kruskal's MST    |

### Complexity

| Operation   | Time (amortized) | Space |
|-------------|------------------|-------|
| find        | O(α(n)) ≈ O(1)   | O(n)  |
| union       | O(α(n)) ≈ O(1)   | O(n)  |
| isConnected | O(α(n)) ≈ O(1)   | O(n)  |

> α(n) is the inverse Ackermann function — for all practical purposes, O(1).

### When to Use

- **Connected components** — more efficient than DFS for dynamic connectivity
- **Kruskal's MST** — edge-based minimum spanning tree
- **Cycle detection in undirected graph** — union until edge connects same component
- **Dynamic connectivity** — adding edges, querying connectivity online

### Magic Methods/Patterns

```java
// Path compression (iterative version)
public int find(int x) {
    int root = x;
    while (parent[root] != root) {
        root = parent[root];
    }
    // Second pass: compress all nodes on path
    while (x != root) {
        int next = parent[x];
        parent[x] = root;
        x = next;
    }
    return root;
}

// Union by size (alternative to union by rank)
class UnionFindBySize {
    private final int[] parent;
    private final int[] size;

    public UnionFindBySize(int n) {
        parent = new int[n];
        size = new int[n];
        Arrays.fill(size, 1);
        for (int i = 0; i < n; i++) {
            parent[i] = i;
        }
    }

    public int find(int x) {
        if (parent[x] != x) {
            parent[x] = find(parent[x]);
        }
        return parent[x];
    }

    public boolean union(int x, int y) {
        int px = find(x), py = find(y);
        if (px == py) return false;

        // Attach smaller tree under larger tree
        if (size[px] < size[py]) {
            parent[px] = py;
            size[py] += size[px];
        } else {
            parent[py] = px;
            size[px] += size[py];
        }
        return true;
    }
}
```

**Practical connected components with Union-Find:**

```java
// Count connected components — Union-Find approach
public int countComponentsUnionFind(int n, int[][] edges) {
    UnionFind uf = new UnionFind(n);
    int components = n;  // Start with n components

    for (int[] edge : edges) {
        if (uf.union(edge[0], edge[1])) {
            components--;  // Merged two components
        }
    }

    return components;
}

// Kruskal's MST using Union-Find
public int kruskalMST(int n, int[][] edges) {
    // Sort edges by weight
    Arrays.sort(edges, Comparator.comparingInt(e -> e[2]));

    UnionFind uf = new UnionFind(n);
    int mstWeight = 0;
    int edgesAdded = 0;

    for (int[] edge : edges) {
        int u = edge[0], v = edge[1], weight = edge[2];

        if (uf.union(u, v)) {  // Edge connects different components
            mstWeight += weight;
            edgesAdded++;

            if (edgesAdded == n - 1) {
                break;  // MST complete
            }
        }
    }

    return edgesAdded == n - 1 ? mstWeight : -1;  // -1 if graph not connected
}
```

---

## Graph Representation Patterns

### Adjacency List (Integer Nodes 0 to n-1)

```java
// Unweighted directed graph
List<List<Integer>> adj = new ArrayList<>();
for(
int i = 0;
i<n;i++){
        adj.

add(new ArrayList<>());
        }
        adj.

get(u).

add(v);  // Add edge u -> v

// Undirected: add both directions
adj.

get(u).

add(v);
adj.

get(v).

add(u);

// Weighted directed graph
List<List<int[]>> adj = new ArrayList<>();
for(
int i = 0;
i<n;i++){
        adj.

add(new ArrayList<>());
        }
        adj.

get(u).

add(new int[] {
    v, weight
});  // Edge u -> v with weight

// computeIfAbsent pattern
Map<Integer, List<Integer>> adj = new HashMap<>();
adj.

computeIfAbsent(u, k ->new ArrayList<>()).

add(v);
```

### Adjacency List (String/Complex Nodes) — See DependencyResolverImpl.java

```java
// DependencyResolverImpl pattern
Map<String, List<String>> graph = new LinkedHashMap<>();
graph.

computeIfAbsent(library, k ->new ArrayList<>()).

add(dependency);

// Weighted with string nodes
Map<String, List<Pair<String, Integer>>> graph = new HashMap<>();
graph.

computeIfAbsent(u, k ->new ArrayList<>()).

add(new Pair<>(v, weight));
```

### Adjacency Matrix

```java
// Unweighted — boolean matrix
boolean[][] matrix = new boolean[n][n];
matrix[u][v]=true;  // Edge exists
// Check: matrix[u][v]

// Weighted — int matrix (use MAX_VALUE for no edge)
int[][] matrix = new int[n][n];
for(
int[] row :matrix){
        Arrays.

fill(row, Integer.MAX_VALUE);
}
matrix[u][v]=weight;
// Check: matrix[u][v] != Integer.MAX_VALUE

// Undirected: mirror across diagonal
matrix[u][v]=weight;
matrix[v][u]=weight;
```

### Edge List

```java
// List of edges — useful for Kruskal's, edge processing
List<int[]> edges = new ArrayList<>();
edges.

add(new int[] {
    u, v, weight
});

// Sort by weight for Kruskal's
        edges.

sort(Comparator.comparingInt(e ->e[2]));
```

---

## Magic Methods/Patterns Reference

### Adjacency List Construction

```java
// computeIfAbsent for edge addition
adj.computeIfAbsent(u, k ->new ArrayList<>()).

add(v);

// getOrDefault for iteration
for(
String neighbor :graph.

getOrDefault(node, List.of())){
        // Process neighbor
        }

// merge for in-degree counting (Kahn's algorithm)
        inDegree.

merge(neighbor, -1,Integer::sum);
if(inDegree.

get(neighbor) ==0){
        queue.

add(neighbor);
}
```

### Visited Tracking Patterns

```java
// HashSet (flexible for any node type)
Set<String> visited = new HashSet<>();
if(visited.

add(node)){
        // First visit — process
        }

// boolean[] (integer nodes 0 to n-1)
boolean[] visited = new boolean[n];
if(!visited[node]){
visited[node]=true;
        // Process
        }

// 3-color DFS (cycle detection in directed graphs)
enum State {NEW, VISITING, VISITED}

Map<String, State> state = new HashMap<>();
```

### Queue Size Trick for BFS Levels

```java
while(!queue.isEmpty()){
int levelSize = queue.size();  // Capture current level
List<Integer> level = new ArrayList<>();
    
    for(
int i = 0;
i<levelSize;i++){
Integer node = queue.poll();
        level.

add(node);
// Add neighbors...
    }

            levels.

add(level);
}
```

---

## Complexity Summary

| Algorithm                   | Time Complexity | Space Complexity | Best For                   |
|-----------------------------|-----------------|------------------|----------------------------|
| BFS (unweighted)            | O(V + E)        | O(V)             | Shortest path (unweighted) |
| DFS                         | O(V + E)        | O(V)             | Traversal, components      |
| Dijkstra                    | O((V+E) log V)  | O(V)             | Shortest path (weighted)   |
| Kahn's Topological Sort     | O(V + E)        | O(V)             | Build order, DAG           |
| DFS Topological Sort        | O(V + E)        | O(V)             | DAG linearization          |
| Union-Find (m operations)   | O(m α(n))       | O(n)             | Dynamic connectivity       |
| 3-Color DFS Cycle Detection | O(V + E)        | O(V)             | Directed graph cycles      |
| Kruskal's MST               | O(E log E)      | O(V)             | Minimum spanning tree      |

---

## Common Gotchas

1. **Directed vs undirected graphs** — Undirected graphs require edges in BOTH directions. Forgetting `adj[v].add(u)`
   causes traversal to miss half the graph.

   ```java
   // WRONG: missing reverse edge
   adj.get(u).add(v);
   
   // CORRECT for undirected
   adj.get(u).add(v);
   adj.get(v).add(u);
   ```

2. **Visited set vs array** — Use `boolean[]` for integer nodes 0 to n-1 (O(1), lower overhead). Use `HashSet` for
   string/complex nodes. Mixing them up causes bugs.

3. **Disconnected graphs** — Not all graphs are connected. Always iterate over ALL nodes to handle disconnected
   components:

   ```java
   // WRONG: assumes connected graph
   dfs(graph, start, visited);
   
   // CORRECT: handles disconnected graphs
   for (int i = 0; i < n; i++) {
       if (!visited[i]) {
           dfs(graph, i, visited);
       }
   }
   ```

4. **Negative weights break Dijkstra** — Dijkstra's algorithm FAILS with negative edge weights. Use Bellman-Ford
   instead (outside scope, but know the limitation).

5. **Stack overflow on deep DFS** — Recursive DFS can overflow on deep graphs (V > ~10000). Use iterative DFS with
   explicit stack for large graphs:

   ```java
   // Recursive DFS — may overflow on deep graphs
   dfs(graph, node, visited);
   
   // Iterative DFS — safe for any depth
   Deque<Integer> stack = new ArrayDeque<>();
   stack.push(start);
   while (!stack.isEmpty()) {
       Integer node = stack.pop();
       // Process...
   }
   ```

6. **BFS visited check timing** — Add to visited set when ENQUEUING, not when dequeuing. Otherwise, same node may be
   added multiple times.

   ```java
   // WRONG: visited check on dequeue
   while (!queue.isEmpty()) {
       String node = queue.poll();
       if (visited.contains(node)) continue;  // Too late — duplicates enqueued
       visited.add(node);
   }
   
   // CORRECT: visited check on enqueue
   visited.add(start);
   queue.offer(start);
   while (!queue.isEmpty()) {
       String node = queue.poll();
       for (String neighbor : neighbors) {
           if (visited.add(neighbor)) {  // Returns false if already present
               queue.offer(neighbor);
           }
       }
   }
   ```

7. **Topological sort only works on DAGs** — Directed Acyclic Graphs only. If graph has cycle, topological sort is
   undefined. Both Kahn's and DFS approaches detect cycles.

8. **Union-Find for undirected cycles only** — Union-Find detects cycles in undirected graphs by checking if edge
   endpoints are already in same component. For directed graphs, use 3-color DFS.

---

## See Also

- **QUEUE_GUIDE.md** — `ArrayDeque` for BFS queues and DFS stacks; `PriorityQueue` for Dijkstra's algorithm frontier
  management.

- **HEAP_GUIDE.md** — Priority queue patterns for Dijkstra's shortest path, Top-K graph queries, and merging sorted
  graph traversals.

- **platform/challenge03/DependencyResolverImpl.java** — Reference implementation of Kahn's algorithm (BFS topological
  sort) and 3-color DFS cycle detection with thread-safe snapshot pattern.

- **platform/challenge09/ConfigMergerImpl.java** — Tree-based configuration merging; contrasts graph vs tree
  structures (trees are acyclic connected graphs with parent-child hierarchy).

- **development/PREPARATION.md** — Graph patterns section with Java Collections decision tree; union-find alternative to
  DFS for connected components.

- **development/recursion/GridTraveler.java** — DFS on grid graphs; memoization pattern for counting paths in DAG.

---

## Java 21 Graph Patterns

### Record-Based Graph Nodes

```java
// Immutable graph edge
record Edge(int from, int to, int weight) {
}

// Immutable adjacency entry
record AdjEntry(int node, int weight) {
}

// Usage
List<List<AdjEntry>> graph = new ArrayList<>();
graph.

get(u).

add(new AdjEntry(v, weight));
```

### Pattern Matching for instanceof

```java
// Before Java 21
if(obj instanceof Node){
Node node = (Node) obj;

process(node.value);
}

// Java 21 — pattern matching
        if(obj instanceof
Node node){

process(node.value);  // No explicit cast
}
```

### Switch Expressions on Graph State

```java
// 3-color DFS with switch expression
State state = visitState.get(node);
boolean shouldProcess = switch (state) {
    case NEW -> true;
    case VISITING -> false;  // Cycle detected
    case VISITED -> false;
};
```

(End of file - total ~720 lines)
