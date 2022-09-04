package com.danipl.leetcode.funwitharrays;

/**
 * https://leetcode.com/explore/learn/card/fun-with-arrays/523/conclusion/3230/
 */
public class MaxConsecutiveOnes {

    public int findMaxConsecutiveOnes(int[] nums) {
        if (nums.length == 0) {
            return 0;
        } else if (nums.length == 1) {
            return 1;
        }
        int maxConsecutiveOnes = 0;
        int left = 0;
        int skippedZeroPos = -1;
        boolean changed = false;
        for (int right = 0; right < nums.length; right++) {
            if (nums[right] == 0) {
                if (changed) {
                    left = skippedZeroPos + 1;
                }
                skippedZeroPos = right;
                changed = true;
            }
            maxConsecutiveOnes = Math.max(maxConsecutiveOnes, (right - left) + 1);
        }
        return maxConsecutiveOnes;
    }

}
