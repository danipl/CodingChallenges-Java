package com.danipl.development.leetcode.funwitharrays;

/**
 * https://leetcode.com/explore/learn/card/fun-with-arrays/511/in-place-operations/3256/
 */
public class ABetterRepeatedDeletionAlgorithm {

    public int removeDuplicates(int[] nums) {
        if (nums.length < 2) {
            return nums.length;
        }
        int length = 1;
        int currentPos = 1;
        for (int pos = 1; pos < nums.length; pos++) {
            if (nums[pos - 1] != nums[pos]) {
                nums[currentPos] = nums[pos];
                currentPos++;
                length++;
            }
        }
        return length;
    }

}
