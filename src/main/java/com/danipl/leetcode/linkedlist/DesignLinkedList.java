package com.danipl.leetcode.linkedlist;

/**
 * https://leetcode.com/explore/learn/card/linked-list/209/singly-linked-list/1290/
 */
public class DesignLinkedList {

    public static class MyLinkedList {

        Node head;
        Node tail;

        int size = 0;

        public MyLinkedList() {
            head = new Node(0);
            tail = new Node(0);
            head.next = tail;
            tail.prev = head;
        }

        public int get(int index) {
            // Allow fetching from index 0 (head > *) to index as the first iteration is from head.
            if (index < 0 || index >= size) {
                return -1;
            }
            Node current = head;
            for (int pos = 0; pos < index + 1; pos++) {
                current = current.next;
            }
            return current.val;
        }

        public void addAtHead(int val) {
            addAtIndex(0, val);
        }

        public void addAtTail(int val) {
            addAtIndex(size, val);
        }

        public void addAtIndex(int index, int val) {
            // Allow fetching from index 0 (head > *) as the first iteration is from head to index + 1, right after the last item.
            if (index < 0 || index > size) {
                return;
            }
            Node node = new Node(val);
            Node current = head;
            for (int pos = 0; pos < index + 1; pos++) {
                current = current.next;
            }
            node.next = current;
            node.prev = current.prev;
            current.prev.next = node;
            current.prev = node;
            size++;
        }

        public void deleteAtIndex(int index) {
            // Allow deletions from index 0 (head > *) to index as the first iteration is from head.
            if (index < 0 || index >= size) {
                return;
            }
            Node current = head;
            for (int pos = 0; pos < index + 1; pos++) {
                current = current.next;
            }
            current.prev.next = current.next;
            if (current.next != null) {
                current.next.prev = current.prev;
            }
            current = null;
            size--;
        }

        @Override
        public String toString() {
            final StringBuffer buffer = new StringBuffer("MyLinkedList[");
            if (size > 0) {
                Node current = head.next;
                for (int pos = 1; pos < size + 1; pos++) {
                    buffer.append(current.val);
                    if (pos != size) {
                        buffer.append(", ");
                    }
                    current = current.next;
                }
            }
            buffer.append("]");
            return buffer.toString();
        }

        class Node {

            int val;
            Node prev;
            Node next;

            public Node(int val) {
                this.val = val;
            }

        }

    }

}
