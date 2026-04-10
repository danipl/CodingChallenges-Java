package com.danipl.development.leetcode.funwitharrays;

/**
 * https://leetcode.com/explore/learn/card/fun-with-arrays/526/deleting-items-from-an-array/3247/
 */
public class RemoveElement {

    public int removeElement(int[] nums, int val) {
        if (nums.length == 0) {
            return 0;
        }
        int length = nums.length;
        for (int pos = nums.length - 1; pos >= 0; pos--) {
            if (nums[pos] == val) {
                int tmp = nums[length - 1];
                nums[length - 1] = nums[pos];
                nums[pos] = tmp;
                length--;
            }
        }
        return length;
    }

}
