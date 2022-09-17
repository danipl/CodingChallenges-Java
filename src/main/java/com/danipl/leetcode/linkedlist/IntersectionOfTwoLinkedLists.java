package com.danipl.leetcode.linkedlist;

/**
 * https://leetcode.com/explore/learn/card/linked-list/214/two-pointer-technique/1215/
 */
public class IntersectionOfTwoLinkedLists {

    public ListNode getIntersectionNode(ListNode headA, ListNode headB) {
        ListNode currA = headA;
        ListNode currB = headB;
        while (currA != currB) {
            currA = currA == null ? headB : headA.next;
            currB = currB == null ? headA : headB.next;
        }
        return currA;
    }

    public class ListNode {
        int val;
        ListNode next;

        ListNode(int x) {
            val = x;
            next = null;
        }

        @Override
        public String toString() {
            return "ListNode{" +
                    "val=" + val +
                    '}';
        }
    }

}
