package com.danipl.recursion;

import java.util.HashMap;
import java.util.Map;

/**
 * Calculate the fibonacci sequence.
 */
public class Fibonacci {
    public static long normal(int num) {
        if (num <= 0) return 0;
        if (num <= 2) return 1;
        return normal(--num) + normal(--num);
    }

    public static long memo(int num) {
        return memo(num, new HashMap<>());
    }

    public static long memo(int num, Map<Integer, Long> memo) {
        if (num <= 0) return 0;
        if (num <= 2) return 1;

        if (memo.containsKey(num)) {
            return memo.get(num);
        }

        final long value = (memo(num - 1, memo) + memo(num - 2, memo));
        memo.put(num, value);

        return value;
    }

}
