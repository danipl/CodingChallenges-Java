package com.danipl.recursion;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.lang.System.arraycopy;

/**
 * Calculates the values to sum the target if they exist.
 */
public class HowSum {

    public static int[] normal(int targetSum, int[] values) {
        return normal(targetSum, values, new int[0]);
    }

    public static int[] normal(int targetSum, int[] values, int[] sumValues) {
        if (targetSum == 0) return sumValues;
        if (targetSum < 0) return new int[0];

        for (final int candidate : values) {
            final int result = targetSum - candidate;

            final int[] newArray = new int[sumValues.length + 1];
            if (sumValues.length > 0) {
                arraycopy(sumValues, 0, newArray, 0, sumValues.length);
            }
            newArray[newArray.length - 1] = candidate;

            final int[] resultValues = normal(result, values, newArray);

            if (resultValues.length > 0) {
                return resultValues;
            }
        }

        return new int[0];
    }

    public static int[] memo(int targetSum, int[] values) {
        return memo(targetSum, values, new int[0], new HashMap<>());
    }

    public static int[] memo(int targetSum, int[] values, int[] sumValues, Map<Integer, Integer[]> memo) {
        if (targetSum == 0) return sumValues;
        if (targetSum < 0) return new int[0];

        if (memo.containsKey(targetSum)) {
            return Arrays.stream(memo.get(targetSum)).mapToInt(value -> value).toArray();
        }

        for (final int candidate : values) {
            final int result = targetSum - candidate;

            final int[] newArray = new int[sumValues.length + 1];
            if (sumValues.length > 0) {
                arraycopy(sumValues, 0, newArray, 0, sumValues.length);
            }
            newArray[newArray.length - 1] = candidate;

            final int[] resultValues = memo(result, values, newArray, memo);

            memo.put(targetSum, Arrays.stream(resultValues).boxed().toArray(value -> new Integer[resultValues.length]));

            if (resultValues.length > 0) {
                return resultValues;
            }
        }

        return new int[0];
    }

}
