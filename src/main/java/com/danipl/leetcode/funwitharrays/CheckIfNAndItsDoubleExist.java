package com.danipl.leetcode.funwitharrays;

/**
 * https://leetcode.com/explore/learn/card/fun-with-arrays/527/searching-for-items-in-an-array/3250/
 */
public class CheckIfNAndItsDoubleExist {

    public boolean checkIfExist(int[] arr) {
        if (arr.length == 0) {
            return false;
        }
        for (int pos = 0; pos < arr.length; pos++) {
            final int candidate = arr[pos] * 2;
            for (int check = 0; check < arr.length; check++) {
                if (arr[check] == candidate && pos != check) {
                    return true;
                }
            }
        }
        return false;
    }

}
