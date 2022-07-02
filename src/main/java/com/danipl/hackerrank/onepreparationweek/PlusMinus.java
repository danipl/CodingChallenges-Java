package com.danipl.hackerrank.onepreparationweek;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static java.lang.System.out;

/**
 * @see https://www.hackerrank.com/challenges/one-week-preparation-kit-plus-minus/problem?h_l=interview&isFullScreen=true&playlist_slugs%5B%5D%5B%5D=preparation-kits&playlist_slugs%5B%5D%5B%5D=one-week-preparation-kit&playlist_slugs%5B%5D%5B%5D=one-week-day-one
 */
public class PlusMinus {

    /*
     * Complete the 'plusMinus' function below.
     *
     * The function accepts INTEGER_ARRAY arr as parameter.
     */
    public static void plusMinus(List<Integer> arr) {
        for (final BigDecimal value : calculation(arr)) {
            out.println(value);
        }
    }

    public static BigDecimal[] calculation(List<Integer> arr) {
        final BigDecimal total = new BigDecimal(arr.size());
        final BigDecimal one = new BigDecimal(1);

        BigDecimal positive = new BigDecimal(0);
        BigDecimal negative = new BigDecimal(0);
        BigDecimal zero = new BigDecimal(0);

        for (final Integer value : arr) {
            if (value > 0) {
                positive = positive.add(one);
            } else if (value < 0) {
                negative = negative.add(one);
            } else {
                zero = zero.add(one);
            }
        }

        return new BigDecimal[]{
                positive.divide(total, 6, RoundingMode.DOWN),
                negative.divide(total, 6, RoundingMode.DOWN),
                zero.divide(total, 6, RoundingMode.DOWN)
        };
    }

}
