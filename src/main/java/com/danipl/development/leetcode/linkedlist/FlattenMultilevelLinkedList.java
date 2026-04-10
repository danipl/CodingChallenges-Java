package com.danipl.development.leetcode.linkedlist;

/**
 * https://leetcode.com/problems/flatten-a-multilevel-doubly-linked-list/description/
 */
public class FlattenMultilevelLinkedList {

    public Node flatten(final Node head) {
        if (head == null) return head;
        Node sentinel = new Node();
        sentinel.next = head;
        flattenByDFS(sentinel, head);
        sentinel.next.prev = null;
        return sentinel.next;
    }

    private Node flattenByDFS(Node curr, Node child) {
        if (child == null) return curr;
        child.prev = curr;
        curr.next = child;
        final Node next = child.next;
        final Node tail = flattenByDFS(child, child.child);
        child.child = null;
        return flattenByDFS(tail, next);
    }

    static class Node {
        public int val;
        public Node prev;
        public Node next;
        public Node child;

        public Node() {
        }

        public Node(int val, Node prev, Node next, Node child) {
            this.val = val;
            this.prev = prev;
            this.next = next;
            this.child = child;
        }

    }

}
