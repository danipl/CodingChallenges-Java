package com.danipl.development.recursion;

import java.util.HashMap;
import java.util.Map;

/**
 * Determine how many paths there are until the goal.
 */
public class GridTraveler {
    public static long normal(int x, int y) {
        if (x == 1 && y == 1) return 1;
        if (x == 0 || y == 0) return 0;
        return normal(x - 1, y) + normal(x, y - 1);
    }

    public static long memo(int x, int y) {
        return memo(x, y, new HashMap<>());
    }

    public static long memo(int x, int y, Map<String, Long> memo) {
        if (x == 1 && y == 1) return 1;
        if (x == 0 || y == 0) return 0;

        final String pair = x + "," + y;

        if (memo.containsKey(pair)) {
            return memo.get(pair);
        }

        final long result = memo(x - 1, y, memo) + memo(x, y - 1, memo);
        memo.put(pair, result);

        return result;
    }

}
