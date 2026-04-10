package com.danipl.development.onepreparationweek;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.danipl.development.onepreparationweek.CountingSortOne.countingSort;
import static java.util.Arrays.asList;
import static java.util.Arrays.fill;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CountingSortOneTest {

    @Test
    public void testSimple() {

        final Integer[] arr = new Integer[100];
        fill(arr, 0);
        arr[10] = 1;
        arr[20] = 1;
        arr[25] = 50;
        arr[55] = 50;
        arr[99] = 99;

        final Integer[] result = new Integer[100];
        fill(result, 0);
        result[0] = 95;
        result[1] = 2;
        result[50] = 2;
        result[99] = 1;

        assertEquals(Arrays.asList(result), countingSort(asList(arr)));
    }

}
