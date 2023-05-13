package com.danipl.leetcode.linkedlist;

public class MyLinkedList {

    private int size = 0;
    private Node head = new Node(0);

    public MyLinkedList() {
    }

    public int get(int index) {
        if (index < 0 || index >= size) return -1;
        Node curr = head;
        for (int pos = 0; pos <= index; pos++) curr = curr.next;
        return curr.val;
    }

    public void addAtHead(int val) {
        addAtIndex(0, val);
    }

    public void addAtTail(int val) {
        addAtIndex(size, val);
    }

    public void addAtIndex(int index, int val) {
        if (index > size) return; // only if > because on the contrary of deleteAtIndex we cannot reach npe.
        if (index < 0) index = 0;
        Node toAdd = new Node(val);
        Node curr = head;
        for (int pos = 0; pos < index; pos++) curr = curr.next;
        toAdd.next = curr.next;
        curr.next = toAdd;
        size++;
    }

    public void deleteAtIndex(int index) {
        if (index < 0 || index >= size) return; // > or = to avoid npe later on when next = next.next.
        Node curr = head;
        for (int pos = 0; pos < index; pos++) curr = curr.next;
        curr.next = curr.next.next;
        size--;
    }

    static class Node {
        int val;
        Node next, prev;

        public Node(int val) {
            this.val = val;
        }

        @Override
        public String toString() {
            return "Node{" +
                    "val=" + val +
                    ", next=" + ((next == null) ? "null" : next.val) +
                    ", prev=" + ((prev == null) ? "null" : prev.val) +
                    '}';
        }
    }

}
