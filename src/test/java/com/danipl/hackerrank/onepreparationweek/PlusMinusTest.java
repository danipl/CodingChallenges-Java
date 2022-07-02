package com.danipl.hackerrank.onepreparationweek;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static com.danipl.hackerrank.onepreparationweek.PlusMinus.calculation;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class PlusMinusTest {

    @Test
    public void testSimple() {
        assertArrayEquals(
                new BigDecimal[]{
                        new BigDecimal("0.400000"),
                        new BigDecimal("0.400000"),
                        new BigDecimal("0.200000")},
                calculation(List.of(1, 1, 0, -1, -1)));
    }

}
