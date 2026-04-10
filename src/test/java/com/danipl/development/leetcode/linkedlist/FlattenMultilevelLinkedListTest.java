package com.danipl.development.leetcode.linkedlist;

import com.danipl.development.leetcode.linkedlist.FlattenMultilevelLinkedList;
import com.danipl.development.leetcode.linkedlist.FlattenMultilevelLinkedList.Node;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FlattenMultilevelLinkedListTest {

    final FlattenMultilevelLinkedList flattenMultilevelLinkedList =
            new FlattenMultilevelLinkedList();

    @Test
    public void testSimple() {
        final Node node2 = new Node(2, null, null, null);
        final Node node1 = new Node(1, null, node2, null);
        node2.prev = node1;
        final Node node3 = new Node(3, null, null, null);
        final Node head = new Node(0, null, node3, node1);
        node3.prev = head;
        Node curr = flattenMultilevelLinkedList.flatten(head);
        for (int value = 0; value < 4; value++) {
            assertEquals(value, curr.val);
            curr = curr.next;
        }
    }

}
