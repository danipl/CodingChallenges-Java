package com.danipl.leetcode.funwitharrays;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * https://leetcode.com/explore/learn/card/fun-with-arrays/523/conclusion/3231/
 */
public class ThirdMaximumNumber {

    public int thirdMax(int[] nums) {
        if (nums.length == 0) {
            return 0;
        } else if (nums.length == 1) {
            return nums[0];
        }
        final Set<Integer> maximuns = new HashSet<>();
        for (final int candidate : nums) {
            maximuns.add(candidate);
            if (maximuns.size() > 3) {
                maximuns.remove(Collections.min(maximuns));
            }
        }
        return (maximuns.size() == 3) ? Collections.min(maximuns) : Collections.max(maximuns);
    }

}
