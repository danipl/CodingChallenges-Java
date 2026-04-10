package com.danipl.development.leetcode.doublelinkedlist;

import com.danipl.development.leetcode.linkedlist.MyLinkedList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MyLinkedListTest {

    @Test
    public void testSimple() {
        MyLinkedList myLinkedList = new MyLinkedList();
        myLinkedList.addAtHead(1);
        assertEquals(1, myLinkedList.get(0));
        myLinkedList.addAtTail(3);
        assertEquals(1, myLinkedList.get(0));
        assertEquals(3, myLinkedList.get(1));
        myLinkedList.addAtIndex(1, 2);
        assertEquals(1, myLinkedList.get(0));
        assertEquals(2, myLinkedList.get(1));
        assertEquals(3, myLinkedList.get(2));
        myLinkedList.deleteAtIndex(1);
        assertEquals(1, myLinkedList.get(0));
        assertEquals(3, myLinkedList.get(1));
    }

    @Test
    public void testSimpleTwo() {
        MyLinkedList myLinkedList = new MyLinkedList();
        myLinkedList.addAtHead(7); // 7
        myLinkedList.addAtHead(2); // 2-7
        myLinkedList.addAtHead(1); // 1-2-7
        myLinkedList.addAtIndex(3, 0); // 1-2-7-0
        myLinkedList.deleteAtIndex(2); // 1-7-0
        myLinkedList.addAtHead(6); // 6-1-7-0
        myLinkedList.addAtTail(4); // 6-1-7-0-4
        assertEquals(4, myLinkedList.get(4));
        myLinkedList.addAtHead(4); // 4-6-1-7-0-4
        myLinkedList.addAtIndex(5, 0); // 4-6-1-7-0-0-4
        myLinkedList.addAtHead(6); // 6-4-6-1-7-0-0-4
    }

}
