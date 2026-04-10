package com.danipl.development.leetcode.linkedlist;

/**
 * https://leetcode.com/explore/learn/card/linked-list/219/classic-problems/1209/
 */
public class PalindromeLinkedList {

    public boolean isPalindrome(ListNode head) {
        ListNode slow = head, fast = head;
        while (fast.next != null && fast.next.next != null) {
            slow = slow.next;
            fast = fast.next.next;
        }
        // 1>2>[3]>2>[1]
        ListNode finalNode = reverseFromNode(slow);
        ListNode current = head;
        // [1]>2>3>[1]>2>3
        while (finalNode != null && current != null) {
            if (current.val != finalNode.val) {
                return false;
            }
            finalNode = finalNode.next;
            current = current.next;
        }
        return true;
    }

    private ListNode reverseFromNode(final ListNode root) {
        ListNode previous = null;
        ListNode curr = root;
        while (curr != null) {
            final ListNode temp = curr.next;
            curr.next = previous;
            previous = curr;
            curr = temp;
        }
        return previous;
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
