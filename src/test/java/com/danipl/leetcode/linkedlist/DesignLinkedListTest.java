package com.danipl.leetcode.linkedlist;

import org.junit.jupiter.api.Test;

import static com.danipl.leetcode.linkedlist.DesignLinkedList.MyLinkedList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DesignLinkedListTest {

    @Test
    public void simple() {
        final MyLinkedList myLinkedList = new MyLinkedList();
        myLinkedList.addAtHead(1);
        myLinkedList.addAtTail(3);
        myLinkedList.addAtIndex(1, 2);
        assertEquals(2, myLinkedList.get(1));
        myLinkedList.deleteAtIndex(1);
        assertEquals(3, myLinkedList.get(1));
    }

    @Test
    public void simple02() {
        final MyLinkedList myLinkedList = new MyLinkedList();
        myLinkedList.addAtHead(1);
        myLinkedList.addAtTail(3);
        myLinkedList.addAtIndex(1, 2);
        assertEquals(2, myLinkedList.get(1));
        myLinkedList.deleteAtIndex(0);
        assertEquals(2, myLinkedList.get(0));
    }

    @Test
    public void simple03() {
        final MyLinkedList myLinkedList = new MyLinkedList();
        myLinkedList.addAtIndex(0, 10);
        myLinkedList.addAtIndex(0, 20);
        myLinkedList.addAtIndex(1, 30);
        assertEquals(20, myLinkedList.get(0));
    }

    @Test
    public void simple04() {
        final MyLinkedList myLinkedList = new MyLinkedList();
        myLinkedList.addAtHead(7);
        myLinkedList.addAtHead(2);
        myLinkedList.addAtHead(1);
        myLinkedList.addAtIndex(3, 0);
        myLinkedList.deleteAtIndex(2);
        myLinkedList.addAtHead(6);
        myLinkedList.addAtTail(4);
        assertEquals(4, myLinkedList.get(4));
        myLinkedList.addAtHead(4);
        myLinkedList.addAtIndex(5, 0);
        myLinkedList.addAtHead(6);
    }

    @Test
    public void simple05() {
        final MyLinkedList myLinkedList = new MyLinkedList();
        myLinkedList.addAtHead(2);
        myLinkedList.deleteAtIndex(1);
        myLinkedList.addAtHead(2);
        myLinkedList.addAtHead(7);
        myLinkedList.addAtHead(3);
        myLinkedList.addAtHead(2);
        myLinkedList.addAtHead(5);
        myLinkedList.addAtTail(5);
        assertEquals(2, myLinkedList.get(5));
        myLinkedList.deleteAtIndex(6);
        myLinkedList.deleteAtIndex(4);
    }

}
