package com.danipl.development.recursion;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.danipl.development.recursion.GridTraveler.memo;
import static com.danipl.development.recursion.GridTraveler.normal;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Calculate all possible paths to reach the goal > cell(1, 1).
 */
public class GridTravelerTest {

    @Nested
    class Normal {

        @Test
        public void test1_1Equals1() {
            assertEquals(1, normal(1, 1));
        }

        @Test
        public void test2_1Equals2() {
            assertEquals(1, normal(2, 1));
        }

        @Test
        public void test2_3Equals3() {
            assertEquals(3, normal(2, 3));
        }

        @Test
        public void test18_18Equals23333606220() {
            assertEquals(3, normal(2, 3));
        }

        @Test
        public void test16_16Equals155117520() {
            assertEquals(155117520, normal(16, 16));
        }
    }

    @Nested
    class Memoization {

        @Test
        public void test1_1Equals1() {
            assertEquals(1, memo(1, 1));
        }

        @Test
        public void test2_1Equals2() {
            assertEquals(1, memo(2, 1));
        }

        @Test
        public void test2_3Equals3() {
            assertEquals(3, memo(2, 3));
        }

        @Test
        public void test16_16Equals155117520() {
            assertEquals(155117520, memo(16, 16));
        }
    }

}
