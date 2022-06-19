package com.danipl.recursion;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.danipl.recursion.BestSum.memo;
import static com.danipl.recursion.BestSum.normal;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BestSumTest {

    @Nested
    class Normal {

        @Test
        public void test7Is1() {
            assertEquals(1, normal(7, new int[]{5, 3, 4, 7}).length);
        }

        @Test
        public void test8Is2() {
            assertEquals(2, normal(8, new int[]{2, 3, 5}).length);
        }

        @Test
        public void test8Is2V2() {
            assertEquals(2, normal(8, new int[]{1, 4, 5}).length);
        }

        @Test
        public void test250Is1() {
            assertEquals(1, normal(25, new int[]{1, 2, 2, 5, 25}).length);
        }

    }

    @Nested
    class Memoization {

        @Test
        public void test7Is1() {
            assertEquals(1, memo(7, new int[]{5, 3, 4, 7}).length);
        }

        @Test
        public void test8Is2() {
            assertEquals(2, memo(8, new int[]{2, 3, 5}).length);
        }

        @Test
        public void test8Is2V2() {
            assertEquals(2, memo(8, new int[]{1, 4, 5}).length);
        }

        @Test
        public void test250Is1() {
            assertEquals(1, memo(25, new int[]{1, 2, 2, 5, 25}).length);
        }

    }

}
