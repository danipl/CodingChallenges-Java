package com.danipl.development.leetcode.linkedlist;

/**
 * https://leetcode.com/explore/learn/card/linked-list/219/classic-problems/1207/
 */
public class RemoveLinkedListElements {

    public ListNode removeElements(ListNode head, int val) {
        if(head == null) return head;
        ListNode sentinel = new ListNode(0);
        sentinel.next = head;
        ListNode prev = sentinel, curr = head;
        while(curr != null){
            if(curr.val == val){
                prev.next = curr.next;
            } else {
                prev = curr;
            }
            curr = curr.next;
        }
        head = sentinel.next;
        sentinel.next = null;
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
