package com.danipl.development.leetcode.funwitharrays;

import java.util.Arrays;

/**
 * https://leetcode.com/explore/learn/card/fun-with-arrays/525/inserting-items-into-an-array/3253/
 */
public class MergeSortedArray {

    public void merge(int[] nums1, int m, int[] nums2, int n) {
        for (int pos = 0; pos < (nums1.length - m); pos++) {
            if (pos > n) {
                break;
            }
            nums1[m + pos] = nums2[pos];
        }
        Arrays.sort(nums1);
    }

}
