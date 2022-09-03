package com.danipl.leetcode;

/**
 * https://leetcode.com/problems/number-of-steps-to-reduce-a-number-to-zero/
 */
public class NumberOfStepsToReduceANumberToZero {

    public int numberOfSteps(int num) {
        int steps = 0;
        while(num != 0){
            if(num % 2 == 0){
                num = num / 2;
            } else {
                num--;
            }
            steps++;
        }
        return steps;
    }

    public int numberOfStepsImproved(int num) {
        // We need to handle this as a special case, otherwise it'll return -1.
        if (num == 0) return 0;
        int steps = 0;
        for (int powerOfTwo = 1; powerOfTwo <= num; powerOfTwo = powerOfTwo * 2) {
            // Apply the bit mask to check if the bit at "powerOfTwo" is a 1.
            if ((powerOfTwo & num) != 0) {
                steps = steps + 2;
            } else {
                steps = steps + 1;
            }
        }
        // We need to subtract 1, because the last bit was over-counted.
        return steps - 1;
    }

}
