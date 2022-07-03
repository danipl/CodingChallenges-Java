package com.danipl.hackerrank.onepreparationweek;

import org.junit.jupiter.api.Test;

import static com.danipl.hackerrank.onepreparationweek.DiagonalDifference.diagonalDifference;
import static java.util.List.of;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DiagonalDifferenceTest {

    @Test
    public void testSimple() {

        assertEquals(2, diagonalDifference(
                of(
                        of(1, 2, 3),
                        of(4, 5, 6),
                        of(9, 8, 9)
                )));
    }

}
