package com.danipl.leetcode.linkedlist;

public class RemoveNthNodeFromEndOfList {

    public ListNode removeNthFromEnd(ListNode head, int n) {
        ListNode fastPointer = head;
        for (int pos = 0; pos < n; pos++) {
            fastPointer = fastPointer.next;
        }
        if (fastPointer == null) {
            return head.next;
        }
        ListNode removePointer = head;
        while (fastPointer.next != null) { // .next because we need to choose the node before the node to remove.
            fastPointer = fastPointer.next;
            removePointer = removePointer.next;
        }
        removePointer.next = removePointer.next.next;
        return head;
    }

    class ListNode {
        int val;
        ListNode next;

        ListNode(int x) {
            val = x;
            next = null;
        }
    }

}
