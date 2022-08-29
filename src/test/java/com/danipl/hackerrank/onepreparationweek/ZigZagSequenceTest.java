package com.danipl.hackerrank.onepreparationweek;

import org.junit.jupiter.api.Test;

import static com.danipl.hackerrank.onepreparationweek.ZigZagSequence.findZigZagSequence;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class ZigZagSequenceTest {

    @Test
    public void testSimple() {
        final int[] arr = new int[]{2, 3, 5, 1, 4};
        findZigZagSequence(arr, arr.length);
        assertArrayEquals(new int[]{1, 2, 5, 4, 3}, arr);
    }

    @Test
    public void testSimple02() {
        final int[] arr = new int[]{1, 2, 3, 4, 5, 6, 7};
        findZigZagSequence(arr, arr.length);
        assertArrayEquals(new int[]{1, 2, 3, 7, 6, 5, 4}, arr);
    }

}
