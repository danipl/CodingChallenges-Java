package com.danipl.leetcode.funwitharrays;

/**
 * https://leetcode.com/explore/learn/card/fun-with-arrays/511/in-place-operations/3259/
 */
public class ReplaceElementsWithGreatestElementOnRightSide {

    public int[] replaceElements(int[] arr) {
        if (arr.length == 0) {
            return arr;
        }
        int current = arr[arr.length - 1];
        for (int pos = arr.length - 2; pos >= 0; pos--) {
            final int tmp = arr[pos];
            arr[pos] = current;
            current = Math.max(current, tmp);
        }
        arr[arr.length - 1] = -1;
        return arr;
    }

}
