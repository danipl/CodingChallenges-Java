package com.danipl.recursion;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.danipl.recursion.HowSum.memo;
import static com.danipl.recursion.HowSum.normal;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HowSumTest {

    @Nested
    class Normal {

        @Test
        public void test1HasSum() {
            assertTrue(normal(1, new int[]{1}).length > 0);
        }

        @Test
        public void test5HasSum() {
            assertTrue(normal(5, new int[]{1, 2, 6, 3}).length > 0);
        }

        @Test
        public void test7HasSum() {
            assertTrue(normal(7, new int[]{2, 3}).length > 0);
        }

        @Test
        public void test17HasSumV2() {
            assertTrue(normal(7, new int[]{5, 3, 4, 7}).length > 0);
        }

        @Test
        public void test1HasNotSum() {
            assertTrue(normal(7, new int[]{2, 4}).length == 0);
        }

        @Test
        public void test275HasNotSum() {
            assertTrue(normal(275, new int[]{7, 14}).length == 0);
        }

    }

    @Nested
    class Memoization {

        @Test
        public void test1HasSum() {
            assertTrue(memo(1, new int[]{1}).length > 0);
        }

        @Test
        public void test5HasSum() {
            assertTrue(memo(5, new int[]{1, 2, 6, 3}).length > 0);
        }

        @Test
        public void test7HasSum() {
            assertTrue(memo(7, new int[]{2, 3}).length > 0);
        }

        @Test
        public void test17HasSumV2() {
            assertTrue(memo(7, new int[]{5, 3, 4, 7}).length > 0);
        }

        @Test
        public void test1HasNotSum() {
            assertTrue(memo(7, new int[]{2, 4}).length == 0);
        }

        @Test
        public void test275HasNotSum() {
            assertTrue(memo(275, new int[]{7, 14}).length == 0);
        }

    }

}
