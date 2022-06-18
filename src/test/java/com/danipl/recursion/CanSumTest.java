package com.danipl.recursion;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.danipl.recursion.CanSum.memo;
import static com.danipl.recursion.CanSum.normal;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CanSumTest {

    @Nested
    class Normal {

        @Test
        public void test2isFalse() {
            assertFalse(normal(5, new int[]{7, 2}));
        }

        @Test
        public void test4isTrue() {
            assertTrue(normal(4, new int[]{1, 2, 2}));
        }

        @Test
        public void test4isTrueV2() {
            assertTrue(normal(4, new int[]{1, 2}));
        }

        @Test
        public void test5isTrue() {
            assertTrue(normal(5, new int[]{2, 2, 4, 4, 2, 1}));
        }

        @Test
        public void test300isFalse() {
            assertFalse(normal(300, new int[]{7, 14}));
        }

    }

    @Nested
    class Memoization {

        @Test
        public void test2isFalse() {
            assertFalse(memo(5, new int[]{7, 2}));
        }

        @Test
        public void test4isTrue() {
            assertTrue(memo(4, new int[]{1, 2, 2}));
        }

        @Test
        public void test4isTrueV2() {
            assertTrue(memo(4, new int[]{1, 2}));
        }

        @Test
        public void test5isTrue() {
            assertTrue(memo(5, new int[]{2, 2, 4, 4, 2, 1}));
        }

        @Test
        public void test300isFalse() {
            assertFalse(memo(300, new int[]{7, 14}));
        }

    }

}
