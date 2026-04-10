package com.danipl.development.leetcode.linkedlist;

import java.util.HashSet;

/**
 * https://leetcode.com/explore/learn/card/linked-list/214/two-pointer-technique/1214/
 */
public class LinkedListCycleII {

    public ListNode detectCycle(ListNode head) {
        final HashSet<ListNode> nodes = new HashSet<>();
        ListNode curent = head;
        while (curent != null) {
            if (nodes.contains(curent)) {
                return curent;
            } else {
                nodes.add(curent);
            }
            curent = curent.next;
        }
        return null;
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
