package com.danipl.leetcode.funwitharrays;

/**
 * https://leetcode.com/explore/learn/card/fun-with-arrays/521/introduction/3237/
 */
public class EvenNumberOfDigits {

    public int findNumbers(int[] nums) {
        int evenNumbers = 0;

        for (int number : nums) {
            int numOfDigits = 0;
            while (number != 0) {
                numOfDigits++;
                number /= 10;
            }
            if (numOfDigits % 2 == 0) {
                evenNumbers++;
            }
        }

        return evenNumbers;
    }

}
