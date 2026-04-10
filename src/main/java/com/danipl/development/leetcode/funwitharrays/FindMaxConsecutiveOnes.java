package com.danipl.development.leetcode.funwitharrays;

/**
 * https://leetcode.com/explore/learn/card/fun-with-arrays/521/introduction/3238/
 */
public class FindMaxConsecutiveOnes {

    public int findMaxConsecutiveOnes(int[] nums) {
        if (nums.length == 0) {
            return 0;
        } else if (nums.length == 1) {
            return nums[0];
        }

        int maxConsecutives = 0;

        for (int iP = 0; iP < nums.length; iP++) {
            if (nums[iP] == 0) {
                continue;
            }
            int currentConsecutives = 1;
            for (int eP = iP + 1; eP < nums.length; eP++) {
                if (iP == eP || nums[eP] == 0) {
                    break;
                }
                currentConsecutives++;
            }
            maxConsecutives = Math.max(maxConsecutives, currentConsecutives);
        }

        return maxConsecutives;
    }

}
