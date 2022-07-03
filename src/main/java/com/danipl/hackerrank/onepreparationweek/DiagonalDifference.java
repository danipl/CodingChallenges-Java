package com.danipl.hackerrank.onepreparationweek;

import java.util.List;

import static java.lang.Math.abs;

/**
 * @see https://www.hackerrank.com/challenges/one-week-preparation-kit-diagonal-difference/problem?isFullScreen=true&h_l=interview&playlist_slugs%5B%5D=preparation-kits&playlist_slugs%5B%5D=one-week-preparation-kit&playlist_slugs%5B%5D=one-week-day-two&h_r=next-challenge&h_v=zen
 */
public class DiagonalDifference {

    /*
     * Complete the 'diagonalDifference' function below.
     *
     * The function is expected to return an INTEGER.
     * The function accepts 2D_INTEGER_ARRAY arr as parameter.
     */
    public static int diagonalDifference(List<List<Integer>> arr) {
        int rightPos = 0, leftPos = arr.size() - 1;
        int rightSum = 0, leftSum = 0;
        for (final List<Integer> numbers : arr) {
            int pos = 0;
            for (final Integer number : numbers) {
                if (pos == rightPos) {
                    rightSum += number;
                }
                if (pos == leftPos) {
                    leftSum += number;
                }
                pos++;
            }
            rightPos++;
            leftPos--;
        }

        return abs(rightSum - leftSum);
    }

}
