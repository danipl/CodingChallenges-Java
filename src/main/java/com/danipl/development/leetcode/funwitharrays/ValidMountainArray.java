package com.danipl.development.leetcode.funwitharrays;

/**
 * https://leetcode.com/explore/learn/card/fun-with-arrays/527/searching-for-items-in-an-array/3251/
 */
public class ValidMountainArray {

    public boolean validMountainArray(int[] arr) {
        if (arr.length < 3) {
            return false;
        }
        int climb = 0;
        int climbPos = 0;
        for (int pos = 0; pos < arr.length; pos++) {
            if (arr[pos] > climb) {
                climbPos = pos;
                climb = arr[pos];
            }
        }
        if (climbPos == 0 || climbPos == arr.length - 1) {
            return false;
        }
        for (int pos = 0; pos < climbPos; pos++) {
            if (arr[pos] >= arr[pos + 1]) {
                return false;
            }
        }
        for (int pos = climbPos; pos < arr.length - 1; pos++) {
            if (arr[pos] <= arr[pos + 1]) {
                return false;
            }
        }
        return true;
    }

}
