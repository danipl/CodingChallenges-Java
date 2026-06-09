package com.danipl.algo.graphs.easy;


import java.util.*;

/**
 * JAVA INTERVIEW CHEAT-SHEET: GRAPHS (EASY)
 * ------------------------------------------
 * 1. Map<Integer, List<Integer>> for adjacency list — flexible sparse graph representation.
 * Use HashMap when node IDs are non-contiguous, ArrayList[] when they are 0..n-1.
 * 2. Set<Integer> for visited tracking — O(1) contains() prevents revisiting nodes in BFS/DFS.
 * 3. ArrayDeque as Queue — preferred over LinkedList for BFS (no null-element allowance, faster).
 * 4. Union-Find (Disjoint Set Union) — tracks connected components efficiently.
 * Path compression + union by rank gives ~O(1) amortized per operation.
 * int[] parent + int[] rank arrays; find() with path compression, union() by rank.
 * 5. Enum for node state — WHITE (unvisited), GRAY (visiting), BLACK (visited) for DFS cycle detection.
 */
public class NumberOfConnectedComponents {

    /**
     * PROBLEM: Number of Connected Components in an Undirected Graph
     * <p>
     * Given n nodes labeled from 0 to n - 1 and a list of undirected edges,
     * count the number of connected components in the graph.
     * <p>
     * A connected component is a set of nodes where every node is reachable from
     * every other node in that component, and no node in the component is connected
     * to any node outside of it.
     * <p>
     * REQUIREMENTS:
     * - Return the number of connected components.
     * - Nodes are labeled 0 to n-1. If n = 0, return 0.
     * - Each edge is given as [u, v] meaning an undirected edge between u and v.
     * - No duplicate edges or self-loops in the input.
     * - Time Complexity must be O(n + e) where n = number of nodes, e = number of edges.
     * - Space Complexity must be O(n + e) for graph representation or O(n) for Union-Find.
     *
     * @param n     the number of nodes (labeled 0 to n-1).
     * @param edges a 2D array where each row is an undirected edge [u, v].
     * @return the number of connected components in the graph.
     */
    public int countComponents(int n, int[][] edges) {
        if (edges.length == 0) return n;

        // Space: O(n + e) - adjacency map stores up to n nodes and 2*e edge entries
        // Time: O(e) - iterate each edge once, computeIfAbsent is O(1) amortized
        final var graph = new HashMap<Integer, Collection<Integer>>();
        for (final int[] neighbors : edges) {
            final var a = neighbors[0];
            final var b = neighbors[1];
            graph.computeIfAbsent(a, ArrayList::new).add(b);
            graph.computeIfAbsent(b, ArrayList::new).add(a);
        }

        var components = 0;
        final var queue = new LinkedList<Integer>();
        final var visited = new HashSet<Integer>();

        // Space: O(n) - visited set holds at most n nodes, queue at most n nodes
        // Time: O(n + e) - outer loop visits each node once, inner BFS visits each edge twice
        for (int candidate = 0; candidate < n; candidate++) {
            if (visited.contains(candidate)) continue;
            components++;
            visited.add(candidate);
            queue.add(candidate);
            while (!queue.isEmpty()) {
                final var current = queue.poll();
                for (final var neighbor : graph.getOrDefault(current, List.of())) {
                    if (visited.contains(neighbor)) continue;
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        // Overall Time Complexity: O(n + e) - build adjacency list O(e), BFS visits each node and edge once O(n + e)
        // Overall Space Complexity: O(n + e) - adjacency map O(n + e), visited set O(n), queue O(n)
        return components;
    }
}
