package com.danipl.development.leetcode.backtracking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Backtracking patterns for permutations, combinations, and subsets.
 * <p>
 * Backtracking systematically explores all possible solutions by building
 * candidates incrementally and abandoning paths that cannot lead to valid solutions.
 * </p>
 * <p>
 * Time Complexity varies by problem:
 * - Permutations: O(n × n!)
 * - Combinations: O(C(n, k)) - binomial coefficient
 * - Subsets: O(2^n)
 * </p>
 * <p>
 * Space Complexity: O(n) for recursion stack + O(result size) for output
 * </p>
 *
 * @see com.danipl.BACKTRACKING_GUIDE.md
 */
public class BacktrackingPatterns {

    /**
     * Generates all permutations of an array.
     * <p>
     * Uses a boolean array to track which elements are currently in the path.
     * Each permutation is a complete ordering of all elements.
     * </p>
     *
     * @param nums input array of distinct integers
     * @return list of all possible permutations
     * @apiNote Time: O(n × n!), Space: O(n) excluding result
     * @implNote LeetCode 46: Permutations
     */
    public List<List<Integer>> permute(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        boolean[] used = new boolean[nums.length];
        backtrackPermute(nums, new ArrayList<>(), used, result);
        return result;
    }

    /**
     * Backtracking helper for permutations.
     *
     * @param nums   input array
     * @param path   current permutation being built
     * @param used   tracks which elements are in current path
     * @param result collects complete permutations
     */
    private void backtrackPermute(int[] nums, List<Integer> path, boolean[] used, List<List<Integer>> result) {
        // Base case: path contains all elements
        if (path.size() == nums.length) {
            result.add(new ArrayList<>(path));
            return;
        }

        // Try each unused element
        for (int i = 0; i < nums.length; i++) {
            if (used[i]) {
                continue;
            }

            // Make choice
            path.add(nums[i]);
            used[i] = true;

            // Recurse
            backtrackPermute(nums, path, used, result);

            // Undo choice (backtrack)
            used[i] = false;
            path.removeLast();
        }
    }

    /**
     * Generates all combinations of k numbers chosen from 1 to n.
     * <p>
     * Order does not matter: [1,2] = [2,1], so we use start index
     * to ensure each combination is generated only once.
     * </p>
     * <p>
     * Includes pruning: stops early when not enough remaining elements
     * to fill the required k positions.
     * </p>
     *
     * @param n upper bound of range (1 to n)
     * @param k size of each combination
     * @return list of all possible k-sized combinations
     * @apiNote Time: O(C(n,k)), Space: O(k) excluding result
     * @implNote LeetCode 77: Combinations
     */
    public List<List<Integer>> combine(int n, int k) {
        List<List<Integer>> result = new ArrayList<>();
        backtrackCombine(n, k, 1, new ArrayList<>(), result);
        return result;
    }

    /**
     * Backtracking helper for combinations.
     *
     * @param n     upper bound of range
     * @param k     target combination size
     * @param start current number to consider (ensures increasing order)
     * @param path  current combination being built
     * @param result collects complete combinations
     */
    private void backtrackCombine(int n, int k, int start, List<Integer> path, List<List<Integer>> result) {
        // Base case: combination complete
        if (path.size() == k) {
            result.add(new ArrayList<>(path));
            return;
        }

        // Pruning: only iterate while we can still fill k elements
        // Need (k - path.size()) more, stop when i > n - (k - path.size()) + 1
        for (int i = start; i <= n - (k - path.size()) + 1; i++) {
            // Make choice
            path.add(i);

            // Recurse: next must be larger (i + 1)
            backtrackCombine(n, k, i + 1, path, result);

            // Undo choice (backtrack)
            path.removeLast();
        }
    }

    /**
     * Generates all subsets (power set) of an array.
     * <p>
     * Unlike permutations and combinations, subsets can be any size from 0 to n.
     * Each element is either included or excluded, giving 2^n total subsets.
     * </p>
     *
     * @param nums input array of distinct integers
     * @return list of all possible subsets including empty set
     * @apiNote Time: O(2^n), Space: O(n) excluding result
     * @implNote LeetCode 78: Subsets
     */
    public List<List<Integer>> subsets(int[] nums) {
        List<List<Integer>> result = new ArrayList<>();
        backtrackSubsets(nums, 0, new ArrayList<>(), result);
        return result;
    }

    /**
     * Backtracking helper for subsets.
     *
     * @param nums   input array
     * @param start  current index to consider
     * @param path   current subset being built
     * @param result collects all subsets
     */
    private void backtrackSubsets(int[] nums, int start, List<Integer> path, List<List<Integer>> result) {
        // Add current subset (valid at every step, including empty)
        result.add(new ArrayList<>(path));

        // Include each remaining element
        for (int i = start; i < nums.length; i++) {
            // Make choice: include nums[i]
            path.add(nums[i]);

            // Recurse: consider next elements
            backtrackSubsets(nums, i + 1, path, result);

            // Undo choice (backtrack): exclude nums[i]
            path.removeLast();
        }
    }
}
