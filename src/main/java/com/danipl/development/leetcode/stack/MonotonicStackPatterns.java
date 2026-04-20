package com.danipl.development.leetcode.stack;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/**
 * Monotonic Stack patterns for next-greater/smaller element problems.
 * <p>
 * A monotonic stack maintains elements in sorted order (increasing or decreasing),
 * enabling efficient solutions to "next greater/smaller element" problems.
 * Each element is pushed and popped at most once, yielding O(n) time.
 * </p>
 * <p>
 * Key patterns:
 * - Decreasing stack: pop when current > top (find next greater)
 * - Increasing stack: pop when current < top (find next smaller)
 * </p>
 * <p>
 * Time Complexity: O(n) for all methods
 * Space Complexity: O(n) for stack storage
 * </p>
 *
 * @see com.danipl.MONOTONIC_GUIDE.md
 */
public class MonotonicStackPatterns {

    /**
     * Finds the next greater element to the right for each element.
     * <p>
     * Uses a decreasing stack to track indices. When current element is
     * greater than stack top, we found the next greater for the popped index.
     * </p>
     *
     * @param nums input array
     * @return array where result[i] = next greater element to right of nums[i],
     *         or -1 if no greater element exists
     * @apiNote Time: O(n), Space: O(n)
     * @implNote Classic LeetCode "Next Greater Element" pattern
     */
    public int[] nextGreaterElement(int[] nums) {
        int n = nums.length;
        int[] result = new int[n];
        Arrays.fill(result, -1);

        // Stores indices; maintains decreasing sequence of values
        Deque<Integer> stack = new ArrayDeque<>();

        for (int i = 0; i < n; i++) {
            // Pop elements that found their next greater
            while (!stack.isEmpty() && nums[i] > nums[stack.peek()]) {
                int idx = stack.pop();
                result[idx] = nums[i];
            }
            stack.push(i);
        }

        return result;
    }

    /**
     * Calculates days until a warmer temperature for each day.
     * <p>
     * Variant of next greater element: returns index difference instead of value.
     * Uses decreasing stack to track temperatures waiting for a warmer day.
     * </p>
     *
     * @param temperatures array of daily temperatures
     * @return array where result[i] = number of days until warmer temperature,
     *         or 0 if no warmer day exists
     * @apiNote Time: O(n), Space: O(n)
     * @implNote LeetCode 739: Daily Temperatures
     */
    public int[] dailyTemperatures(int[] temperatures) {
        int n = temperatures.length;
        int[] result = new int[n];

        // Stores indices of days waiting for warmer temperature
        Deque<Integer> stack = new ArrayDeque<>();

        for (int i = 0; i < n; i++) {
            // Current temp warmer than days on stack
            while (!stack.isEmpty() && temperatures[i] > temperatures[stack.peek()]) {
                int prevIdx = stack.pop();
                result[prevIdx] = i - prevIdx;  // Days difference
            }
            stack.push(i);
        }

        return result;
    }

    /**
     * Finds the largest rectangular area in a histogram.
     * <p>
     * Uses an increasing stack to track bar heights. When a shorter bar is
     * encountered, we calculate areas for all taller bars that end at this position.
     * Width is determined by current position and previous stack element.
     * </p>
     *
     * @param heights array of bar heights
     * @return maximum rectangular area
     * @apiNote Time: O(n), Space: O(n)
     * @implNote LeetCode 84: Largest Rectangle in Histogram
     */
    public int largestRectangleArea(int[] heights) {
        int maxArea = 0;

        // Stores indices; maintains increasing sequence of heights
        Deque<Integer> stack = new ArrayDeque<>();

        // Process all bars plus sentinel (height 0) to flush remaining stack
        for (int i = 0; i <= heights.length; i++) {
            int currentHeight = (i == heights.length) ? 0 : heights[i];

            // Pop taller bars and calculate their areas
            while (!stack.isEmpty() && currentHeight < heights[stack.peek()]) {
                int height = heights[stack.pop()];
                // Width: from previous index (exclusive) to current (exclusive)
                int left = stack.isEmpty() ? 0 : stack.peek() + 1;
                int right = i - 1;
                int width = right - left + 1;
                maxArea = Math.max(maxArea, height * width);
            }

            stack.push(i);
        }

        return maxArea;
    }
}
