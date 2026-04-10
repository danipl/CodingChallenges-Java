package com.danipl.development.leetcode.funwitharrays;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * https://leetcode.com/explore/learn/card/fun-with-arrays/523/conclusion/3270/
 */
public class FindAllNumbersDisappearedInAnArray {

    public List<Integer> findDisappearedNumbers(int[] nums) {
        final int maximun = nums.length;
        final Set<Integer> present = new HashSet<>();
        for (int pos = 0; pos < maximun; pos++) {
            present.add(nums[pos]);
        }
        final List<Integer> list = new ArrayList<>();
        for (int val = 1; val <= maximun; val++) {
            if (!present.contains(val)) {
                list.add(val);
            }
        }
        return list;
    }

}
