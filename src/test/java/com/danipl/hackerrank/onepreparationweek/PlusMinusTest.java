package com.danipl.hackerrank.onepreparationweek;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

public class PlusMinusTest {

    @Test
    public void testSimple() {
        Assertions.assertArrayEquals(
                new BigDecimal[]{
                        new BigDecimal("0.400000"),
                        new BigDecimal("0.400000"),
                        new BigDecimal("0.200000")},
                PlusMinus.calculation(List.of(1, 1, 0, -1, -1)));
    }

}
