package com.danipl.leetcode;

import java.util.ArrayList;
import java.util.List;

public class MiddleOfTheLinkedList {

    class Solution {
        public ListNode middleNode(ListNode head) {
            if (head == null) {
                return null;
            }
            final List<ListNode> list = new ArrayList<>();
            ListNode current = head;
            while (current != null) {
                list.add(current);
                current = current.next;
            }
            final int size = list.size();
            return list.get((size % 2 == 0) ? ((size / 2) + 1) : (size / 2));
        }

        public ListNode middleNodeImproved(ListNode head) {
            ListNode middle = head;
            ListNode forward = head;
            while (forward != null && forward.next != null) {
                middle = middle.next;
                forward = forward.next.next;
            }
            return middle;
        }
    }

    public class ListNode {
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
