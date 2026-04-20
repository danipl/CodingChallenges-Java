package com.danipl.development.leetcode.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Dijkstra's Algorithm - shortest path in weighted graph with non-negative edges.
 * <p>
 * Uses a priority queue (min-heap) to always process the node with minimum
 * distance first, guaranteeing optimal path discovery.
 * </p>
 * <p>
 * Time Complexity: O((V + E) log V) with binary heap PriorityQueue
 * Space Complexity: O(V + E) for adjacency list + O(V) for distance array
 * </p>
 * <p>
 * Important: Only works with non-negative edge weights. For negative weights,
 * use Bellman-Ford algorithm instead.
 * </p>
 *
 * @see com.danipl.GRAPH_GUIDE.md#3-dijkstras-shortest-path-weighted-graphs
 */
public class Dijkstra {

    /**
     * Finds shortest path distances from source to all other nodes.
     * <p>
     * Builds adjacency list from edge array, then runs Dijkstra's algorithm.
     * Unreachable nodes will have distance Integer.MAX_VALUE.
     * </p>
     *
     * @param n      number of nodes (indexed 0 to n-1)
     * @param edges  2D array where each row is [from, to, weight]
     * @param source source node index
     * @return array where result[i] = shortest distance from source to i
     *         (Integer.MAX_VALUE if unreachable)
     * @apiNote Time: O((V+E) log V), Space: O(V + E)
     */
    public int[] shortestPath(int n, int[][] edges, int source) {
        // Build adjacency list: adj.get(u) = List of int[]{v, weight}
        List<List<int[]>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>());
        }
        for (int[] edge : edges) {
            int u = edge[0], v = edge[1], w = edge[2];
            adj.get(u).add(new int[]{v, w});
        }

        // Distance array initialized to infinity
        int[] dist = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        dist[source] = 0;

        // Min-heap: [node, distance] - processes smallest distance first
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
        pq.offer(new int[]{source, 0});

        while (!pq.isEmpty()) {
            int[] current = pq.poll();
            int u = current[0];
            int d = current[1];

            // Skip if we found better path already (stale entry)
            if (d > dist[u]) {
                continue;
            }

            // Relax all neighbors
            for (int[] edge : adj.get(u)) {
                int v = edge[0];
                int weight = edge[1];

                // Relaxation: found shorter path to v through u
                if (dist[u] != Integer.MAX_VALUE && dist[u] + weight < dist[v]) {
                    dist[v] = dist[u] + weight;
                    pq.offer(new int[]{v, dist[v]});
                }
            }
        }

        return dist;
    }

    /**
     * Finds shortest path from source to destination with path reconstruction.
     * <p>
     * Maintains parent pointers to reconstruct the actual path sequence,
     * not just the distance.
     * </p>
     *
     * @param n      number of nodes (indexed 0 to n-1)
     * @param edges  2D array where each row is [from, to, weight]
     * @param source source node index
     * @param dest   destination node index
     * @return array of node IDs representing path from source to dest
     *         (empty array if no path exists)
     * @apiNote Time: O((V+E) log V), Space: O(V + E)
     */
    public int[] shortestPathWithReconstruction(int n, int[][] edges, int source, int dest) {
        // Build adjacency list
        List<List<int[]>> adj = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            adj.add(new ArrayList<>());
        }
        for (int[] edge : edges) {
            int u = edge[0], v = edge[1], w = edge[2];
            adj.get(u).add(new int[]{v, w});
        }

        // Distance and parent arrays
        int[] dist = new int[n];
        int[] parent = new int[n];
        Arrays.fill(dist, Integer.MAX_VALUE);
        Arrays.fill(parent, -1);
        dist[source] = 0;

        // Min-heap: [node, distance]
        PriorityQueue<int[]> pq = new PriorityQueue<>(Comparator.comparingInt(a -> a[1]));
        pq.offer(new int[]{source, 0});

        while (!pq.isEmpty()) {
            int[] current = pq.poll();
            int u = current[0];
            int d = current[1];

            // Skip stale entry
            if (d > dist[u]) {
                continue;
            }

            // Early exit if reached destination
            if (u == dest) {
                break;
            }

            // Relax neighbors
            for (int[] edge : adj.get(u)) {
                int v = edge[0];
                int weight = edge[1];

                if (dist[u] != Integer.MAX_VALUE && dist[u] + weight < dist[v]) {
                    dist[v] = dist[u] + weight;
                    parent[v] = u;  // Track path
                    pq.offer(new int[]{v, dist[v]});
                }
            }
        }

        // No path exists
        if (dist[dest] == Integer.MAX_VALUE) {
            return new int[0];
        }

        // Reconstruct path by following parent pointers
        List<Integer> pathList = new ArrayList<>();
        for (int at = dest; at != -1; at = parent[at]) {
            pathList.add(at);
        }

        // Reverse to get source -> dest order
        int[] path = new int[pathList.size()];
        for (int i = 0; i < pathList.size(); i++) {
            path[i] = pathList.get(pathList.size() - 1 - i);
        }

        return path;
    }
}
