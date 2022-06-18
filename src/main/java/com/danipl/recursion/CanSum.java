package com.danipl.recursion;

import java.util.HashMap;
import java.util.Map;

/**
 * Check if given a collection of values, they can sum with them the target.
 */
public class CanSum {

    public static boolean normal(int target, int[] values) {
        if (target == 0) return true;
        if (target < 0) return false;

        for (final int candidate : values) {
            if (candidate != 0 && normal((target - candidate), values)) {
                return true;
            }
        }

        return false;
    }

    public static boolean memo(int target, int[] values) {
        return memo(target, values, new HashMap<>());
    }

    public static boolean memo(int target, int[] values, Map<Integer, Boolean> memo) {
        if (target == 0) return true;
        if (target < 0) return false;

        if (memo.containsKey(target)) return memo.get(target);

        for (final int candidate : values) {
            final boolean result = (candidate != 0 && memo((target - candidate), values, memo));
            memo.put(target, result);
            if (result) {
                return true;
            }
        }

        return false;
    }

}
