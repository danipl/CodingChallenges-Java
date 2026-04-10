package com.danipl.development.leetcode.linkedlist;

/**
 * https://leetcode.com/explore/learn/card/linked-list/219/classic-problems/1205/
 */
public class ReverseLinkedList {

    public ListNode reverseList(ListNode head) {
        if (head == null || head.next == null) {
            return head;
        }
        ListNode originalHead = head;
        while (originalHead.next != null) {
            ListNode current = originalHead.next;
            originalHead.next = current.next;
            current.next = head;
            head = current;
        }
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
