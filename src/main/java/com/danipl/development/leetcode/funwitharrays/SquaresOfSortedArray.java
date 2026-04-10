package com.danipl.development.leetcode.funwitharrays;

import java.util.Arrays;

/**
 * https://leetcode.com/explore/learn/card/fun-with-arrays/521/introduction/3240/
 */
public class SquaresOfSortedArray {

    public int[] sortedSquares(int[] nums) {
        for(int pos = 0; pos < nums.length; pos++){
            nums[pos] *= nums[pos];
        }
        Arrays.sort(nums);
        return nums;
    }

}
