package com.danipl.development.leetcode.unionfind;

/**
 * Union-Find (Disjoint Set Union) - efficient connected components tracking.
 * <p>
 * Implements two key optimizations:
 * - Path compression: flatten tree structure during find()
 * - Union by rank: attach smaller tree under larger tree
 * </p>
 * <p>
 * Time Complexity (amortized):
 * - find: O(α(n)) ≈ O(1) where α is inverse Ackermann function
 * - union: O(α(n)) ≈ O(1)
 * - isConnected: O(α(n)) ≈ O(1)
 * </p>
 * <p>
 * Space Complexity: O(n)
 * </p>
 * <p>
 * Use cases: connected components, Kruskal's MST, cycle detection in undirected graphs,
 * dynamic connectivity problems
 * </p>
 *
 * @see com.danipl.GRAPH_GUIDE.md#5-union-find-disjoint-set-union---dsu
 */
public class UnionFind {

    /** Parent pointer for each element */
    private final int[] parent;

    /** Rank (approximate depth) of each tree */
    private final int[] rank;

    /** Number of disjoint sets (components) */
    private int components;

    /**
     * Initializes Union-Find with n elements.
     * <p>
     * Each element starts as its own component (self-loop parent).
     * </p>
     *
     * @param n number of elements (indexed 0 to n-1)
     * @apiNote Time: O(n), Space: O(n)
     */
    public UnionFind(int n) {
        parent = new int[n];
        rank = new int[n];
        components = n;
        // Initialize each element as its own parent
        for (int i = 0; i < n; i++) {
            parent[i] = i;
            rank[i] = 1;  // Initial rank (height) is 1
        }
    }

    /**
     * Finds the root/representative of element x with path compression.
     * <p>
     * Path compression flattens the tree by making all nodes on the path
     * point directly to the root, reducing future lookup times.
     * </p>
     *
     * @param x the element to find root for
     * @return the root/representative of x's component
     * @apiNote Time: O(α(n)) ≈ O(1) amortized
     */
    public int find(int x) {
        // Path compression: make all nodes point to root
        if (parent[x] != x) {
            parent[x] = find(parent[x]);
        }
        return parent[x];
    }

    /**
     * Unions the sets containing x and y using union by rank.
     * <p>
     * Attaches the shorter tree under the taller tree to minimize height.
     * If ranks are equal, arbitrarily choose one as parent and increment its rank.
     * </p>
     *
     * @param x first element
     * @param y second element
     * @return true if union was performed, false if already in same set
     * @apiNote Time: O(α(n)) ≈ O(1) amortized
     */
    public boolean union(int x, int y) {
        int rootX = find(x);
        int rootY = find(y);

        // Already in same component
        if (rootX == rootY) {
            return false;
        }

        // Union by rank: attach smaller tree under larger
        if (rank[rootX] < rank[rootY]) {
            parent[rootX] = rootY;
        } else if (rank[rootX] > rank[rootY]) {
            parent[rootY] = rootX;
        } else {
            // Equal ranks: attach one to other, increment rank
            parent[rootY] = rootX;
            rank[rootX]++;
        }

        // Decreased component count
        components--;
        return true;
    }

    /**
     * Checks if x and y are in the same connected component.
     *
     * @param x first element
     * @param y second element
     * @return true if connected, false otherwise
     * @apiNote Time: O(α(n)) ≈ O(1)
     */
    public boolean isConnected(int x, int y) {
        return find(x) == find(y);
    }

    /**
     * Returns the number of disjoint sets (connected components).
     *
     * @return count of components
     * @apiNote Time: O(1)
     */
    public int getComponentCount() {
        return components;
    }
}
