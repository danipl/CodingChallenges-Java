package com.danipl.leetcode.linkedlist;

/**
 * https://leetcode.com/explore/learn/card/linked-list/219/classic-problems/1208/
 */
public class OddEvenLinkedList {

    public ListNode oddEvenList(ListNode head) {
        if(head == null) return head;
        final ListNode oddS = new ListNode(0), evenS = new ListNode(0);
        ListNode oddSP = oddS, evenSP = evenS, curr = head;
        int index = 0;
        while(curr != null){
            if(index % 2 == 0){
                evenSP.next = curr;
                evenSP = evenSP.next;
                curr = curr.next;
                evenSP.next = null;
            } else {
                oddSP.next = curr;
                oddSP = oddSP.next;
                curr = curr.next;
                oddSP.next = null;
            }
            index++;
        }
        if(evenS.next == null){
            return oddS.next;
        }
        evenSP.next = oddS.next;
        return evenS.next;
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
