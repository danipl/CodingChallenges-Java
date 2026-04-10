package com.danipl.development.leetcode;

/**
 * https://leetcode.com/problems/running-sum-of-1d-array/
 */
public class SumOf1DArray {

    public static int[] runningSum(int[] nums) {
        for (int pos = 1; pos < nums.length; pos++) {
            nums[pos] = nums[pos] + nums[pos - 1];
        }
        return nums;
    }

}
