package com.danipl.leetcode.funwitharrays;

/**
 * https://leetcode.com/explore/learn/card/fun-with-arrays/525/inserting-items-into-an-array/3245/
 */
public class DuplicateZeros {

    public void duplicateZeros(int[] arr) {
        for (int pos = arr.length - 1; pos >= 0; pos--) {
            if (arr[pos] == 0) {
                for (int curr = arr.length - 1; curr > pos; curr--) {
                    arr[curr] = arr[curr - 1];
                }
            }
        }
    }

}
