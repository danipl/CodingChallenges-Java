package com.danipl.development.onepreparationweek;

import java.util.Arrays;
import java.util.List;

/**
 * @see https://www.hackerrank.com/challenges/one-week-preparation-kit-lonely-integer/problem?isFullScreen=true&h_l=interview&playlist_slugs%5B%5D=preparation-kits&playlist_slugs%5B%5D=one-week-preparation-kit&playlist_slugs%5B%5D=one-week-day-two
 */
public class LonelyInteger {

    /*
     * Complete the 'lonelyinteger' function below.
     *
     * The function is expected to return an INTEGER.
     * The function accepts INTEGER_ARRAY a as parameter.
     */
    public static int lonelyinteger(List<Integer> list) {
        final Integer[] arr = list.toArray(new Integer[0]);
        Arrays.sort(arr);
        for (int pos = 0; pos < arr.length - 1; pos++) {
            if (arr[pos] != arr[pos + 1] && (pos != 0 && arr[pos] != arr[pos - 1])) {
                return arr[pos];
            }
        }
        return arr[arr.length - 1];
    }

}
