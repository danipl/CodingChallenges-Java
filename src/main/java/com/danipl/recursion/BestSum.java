package com.danipl.recursion;

import java.util.HashMap;
import java.util.Map;

public class BestSum {

    public static int[] normal(int targetSum, int[] values) {
        return normal(targetSum, values, new int[0]);
    }

    public static int[] normal(int targetSum, int[] values, int[] sumValues) {
        if (targetSum == 0) return sumValues;
        int[] shortest = new int[0];
        if (targetSum < 0) return shortest;

        for (final int candidate : values) {
            final int result = targetSum - candidate;

            final int[] newArray = new int[sumValues.length + 1];
            if (sumValues.length > 0) {
                System.arraycopy(sumValues, 0, newArray, 0, sumValues.length);
            }
            newArray[newArray.length - 1] = candidate;

            final int[] resultValues = normal(result, values, newArray);

            if (resultValues.length > 0 && (shortest.length == 0 || resultValues.length < shortest.length)) {
                shortest = resultValues;
            }
        }

        return shortest;
    }

    public static Integer[] memo(int targetSum, int[] values) {
        return memo(targetSum, values, new HashMap<>());
    }

    public static Integer[] memo(int targetSum, int[] values, Map<Integer, Integer[]> memo) {
        if (targetSum == 0) return new Integer[0];
        if (targetSum < 0) return null;

        if (memo.containsKey(targetSum)) {
            return memo.get(targetSum);
        }

        Integer[] shortest = new Integer[0];

        for (final int candidate : values) {
            final int result = targetSum - candidate;
            final Integer[] resultValues = memo(result, values, memo);

            if (resultValues == null) continue;

            final Integer[] newArray = new Integer[resultValues.length + 1];
            if (resultValues.length > 0) {
                System.arraycopy(resultValues, 0, newArray, 0, resultValues.length);
            }
            newArray[newArray.length - 1] = candidate;

            if (shortest.length == 0 || newArray.length < shortest.length) {
                shortest = newArray;
            }
        }

        memo.put(targetSum, shortest);

        return shortest;
    }

}
