package com.danipl.development.onepreparationweek;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Arrays.fill;

/**
 * @see https://www.hackerrank.com/challenges/one-week-preparation-kit-countingsort1/problem?isFullScreen=true&h_l=interview&playlist_slugs%5B%5D=preparation-kits&playlist_slugs%5B%5D=one-week-preparation-kit&playlist_slugs%5B%5D=one-week-day-two&h_r=next-challenge&h_v=zen
 */
public class CountingSortOne {

    /*
     * Complete the 'countingSort' function below.
     *
     * The function is expected to return an INTEGER_ARRAY.
     * The function accepts INTEGER_ARRAY arr as parameter.
     */
    public static List<Integer> countingSort(List<Integer> list) {
        final Integer[] arr = new Integer[100];
        fill(arr, 0);

        for (final Integer number : list) {
            arr[number]++;
        }

        return asList(arr);
    }

}
