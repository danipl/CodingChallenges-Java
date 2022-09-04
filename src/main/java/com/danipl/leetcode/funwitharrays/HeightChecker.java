package com.danipl.leetcode.funwitharrays;

import java.util.Arrays;

/**
 * https://leetcode.com/explore/learn/card/fun-with-arrays/523/conclusion/3228/
 */
public class HeightChecker {

    public int heightChecker(int[] heights) {
        final int[] ordered = Arrays.copyOf(heights, heights.length);
        Arrays.sort(ordered);
        int changes = 0;
        for (int pos = 0; pos < heights.length; pos++) {
            if (ordered[pos] != heights[pos]) {
                changes++;
            }
        }
        return changes;
    }

}
