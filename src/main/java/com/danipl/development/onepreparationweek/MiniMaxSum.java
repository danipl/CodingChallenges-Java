package com.danipl.development.onepreparationweek;


import java.util.List;
import java.util.function.BiFunction;

import static java.lang.Long.MAX_VALUE;
import static java.lang.Long.MIN_VALUE;
import static java.lang.System.out;

/**
 * The original problem might be solved easier, just calculating the sum of all of them, subtracting
 * the max a min values, the array also is only 5 items length, but the exercise tried to be successful
 * with wider more complex scenarios.
 *
 * @see https://www.hackerrank.com/challenges/one-week-preparation-kit-mini-max-sum/problem?h_l=interview&isFullScreen=true&playlist_slugs%5B%5D%5B%5D=preparation-kits&playlist_slugs%5B%5D%5B%5D=one-week-preparation-kit&playlist_slugs%5B%5D%5B%5D=one-week-day-one&h_r=next-challenge&h_v=zen
 */
public class MiniMaxSum {

    /*
     * Complete the 'miniMaxSum' function below.
     *
     * The function accepts INTEGER_ARRAY arr as parameter.
     */
    public static void miniMaxSum(final List<Integer> list) {
        final Long[] calculations = calculate(list);
        out.print(calculations[0] + " " + calculations[1]);
    }

    public static Long[] calculate(final List<Integer> list) {
        final Integer[] arr = list.toArray(new Integer[0]);
        return new Long[]{
                calculate(arr, 0, 0, 0, new Calculation() {
                    @Override
                    public long defaultValue() {
                        return MAX_VALUE;
                    }

                    @Override
                    public BiFunction<Long, Long, Long> function() {
                        return Math::min;
                    }
                }),
                calculate(arr, 0, 0, 0, new Calculation() {
                    @Override
                    public long defaultValue() {
                        return MIN_VALUE;
                    }

                    @Override
                    public BiFunction<Long, Long, Long> function() {
                        return Math::max;
                    }
                })};
    }

    public static long calculate(final Integer[] arr, final long value, final int amount, final int pos, final Calculation calculation) {
        if (amount == 4) return value;
        if (pos == arr.length) return calculation.defaultValue();

        long calculated = calculation.defaultValue();

        for (int cPos = pos; cPos < arr.length; cPos++) {
            final long withMe = calculate(arr, value + arr[cPos], amount + 1, cPos + 1, calculation);
            final long withoutMe = calculate(arr, value, amount, cPos + 1, calculation);
            calculated = calculation.function().apply(calculated, calculation.function().apply(withMe, withoutMe));
        }

        return calculated;
    }

    interface Calculation {

        long defaultValue();

        BiFunction<Long, Long, Long> function();

    }

}
