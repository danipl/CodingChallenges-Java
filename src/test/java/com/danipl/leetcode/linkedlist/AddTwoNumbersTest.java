package com.danipl.leetcode.linkedlist;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

public class AddTwoNumbersTest {

    private static final AddTwoNumbers addTwoNumbers = new AddTwoNumbers();

    @Test
    public void testSimple() {
        AddTwoNumbers.ListNode listNode = addTwoNumbers.addTwoNumbers(
                buildListNodeFromCollection(List.of(9, 9, 9, 9, 9, 9, 9)),
                buildListNodeFromCollection(List.of(9, 9, 9, 9))
        );
        while (listNode != null) {
            System.out.println(listNode.val);
            listNode = listNode.next;
        }
    }

    private static AddTwoNumbers.ListNode buildListNodeFromCollection(final Collection<Integer> list) {
        final AddTwoNumbers.ListNode listNode = new AddTwoNumbers.ListNode(0);
        AddTwoNumbers.ListNode head = listNode;
        for (final Integer val : list) {
            head.next = new AddTwoNumbers.ListNode(val);
            head = head.next;
        }
        return listNode.next;
    }

}
