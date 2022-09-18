package com.danipl.leetcode.linkedlist;

/**
 * https://leetcode.com/explore/learn/card/linked-list/219/classic-problems/1207/
 */
public class RemoveLinkedListElements {

    public ListNode removeElements(ListNode head, int val) {
        if (head == null) return head;
        ListNode curr = head;
        ListNode prev = null;
        head = null;
        while (curr != null) {
            if (curr.val == val && prev != null) {
                prev.next = curr.next;
            } else if (head == null) {
                head = prev;
            }
            if (curr.val != val) prev = curr;
            curr = curr.next;
        }
        return (head == null) ? prev : head;
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
