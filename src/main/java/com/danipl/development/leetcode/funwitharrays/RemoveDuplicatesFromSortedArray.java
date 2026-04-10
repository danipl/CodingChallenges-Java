package com.danipl.development.leetcode.funwitharrays;

import java.util.HashSet;
import java.util.Set;

/**
 * https://leetcode.com/explore/learn/card/fun-with-arrays/526/deleting-items-from-an-array/3248/
 */
public class RemoveDuplicatesFromSortedArray {

    public int removeDuplicates(int[] nums) {
        if (nums.length == 0) {
            return 0;
        }
        int length = nums.length;
        final Set<Integer> set = new HashSet<>();
        for (int pos = nums.length - 1; pos >= 0; pos--) {
            if (set.contains(nums[pos])) {
                for (int current = pos; current < (length - 1); current++) {
                    nums[current] = nums[current + 1];
                }
                length--;
                nums[length] = 0;
            }
            set.add(nums[pos]);
        }
        return length;
    }

}
