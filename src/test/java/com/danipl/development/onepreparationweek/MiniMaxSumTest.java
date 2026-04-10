package com.danipl.development.onepreparationweek;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.danipl.development.onepreparationweek.MiniMaxSum.calculate;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class MiniMaxSumTest {

    @Test
    public void testSimple() {
        assertArrayEquals(
                new Long[]{10L, 14L},
                calculate(List.of(1, 2, 3, 4, 5))
        );
    }

    @Test
    public void testSimple02() {
        assertArrayEquals(
                new Long[]{4L, 8L},
                calculate(List.of(1, 2, 2, 2, 2, 1, 1, 1))
        );
    }

    @Test
    public void testSimple03() {
        assertArrayEquals(
                new Long[]{4L, 200L},
                calculate(List.of(1, 100, 50, 2, 2, 1, 25, 1, 25, 1))
        );
    }

}
