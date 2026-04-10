package com.danipl.development.leetcode.funwitharrays;

import java.util.LinkedList;
import java.util.Queue;

/**
 * https://leetcode.com/explore/learn/card/fun-with-arrays/511/in-place-operations/3157/
 */
public class MoveZeroes {

    public void moveZeroes(int[] nums) {
        final Queue<Integer> queue = new LinkedList<>();
        for (int reader = 0; reader <= nums.length - 1; reader++) {
            if (nums[reader] == 0) {
                queue.add(reader);
            } else if (!queue.isEmpty()) {
                final int writer = queue.poll();
                int tmp = nums[reader];
                nums[reader] = nums[writer];
                nums[writer] = tmp;
                queue.add(reader);
            }
        }
    }

}
