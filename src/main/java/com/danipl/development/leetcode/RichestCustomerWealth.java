package com.danipl.development.leetcode;

/**
 * https://leetcode.com/problems/richest-customer-wealth/
 */
public class RichestCustomerWealth {

    public static int maximumWealth(int[][] accounts) {
        int richest = 0;
        int totalAmount;
        for (int customer = 0; customer < accounts.length; customer++) {
            totalAmount = 0;
            for (int amount = 0; amount < accounts[customer].length; amount++) {
                totalAmount += accounts[customer][amount];
            }
            richest = Math.max(richest, totalAmount);
        }
        return richest;
    }

}
