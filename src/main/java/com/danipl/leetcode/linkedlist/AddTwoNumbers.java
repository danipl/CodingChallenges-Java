package com.danipl.leetcode.linkedlist;

public class AddTwoNumbers {

    public ListNode addTwoNumbers(ListNode l1, ListNode l2) {
        ListNode l1Head = l1;
        ListNode l2Head = l2;
        final ListNode result = new ListNode();
        ListNode resultHead = result;
        int carr = 0;
        while (l1Head != null || l2Head != null || carr != 0) {
            int val = 0;
            if (l1Head != null) val += l1Head.val;
            if (l2Head != null) val += l2Head.val;
            val += carr;
            if (val >= 10) {
                carr = (val - (val % 10)) / 10;
                val = val % 10;
            } else {
                carr = 0;
            }
            resultHead.next = new ListNode(val);
            resultHead = resultHead.next;
            if (l1Head != null) l1Head = l1Head.next;
            if (l2Head != null) l2Head = l2Head.next;
        }
        return result.next;
    }

    public static class ListNode {
        int val;
        ListNode next;

        ListNode() {
        }

        ListNode(int val) {
            this.val = val;
        }

        ListNode(int val, ListNode next) {
            this.val = val;
            this.next = next;
        }
    }

}
