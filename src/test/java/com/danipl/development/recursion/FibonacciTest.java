package com.danipl.development.recursion;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.danipl.development.recursion.Fibonacci.memo;
import static com.danipl.development.recursion.Fibonacci.normal;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FibonacciTest {

    @Nested
    class Normal {

        @Test
        public void test1Equals1() {
            assertEquals(1, normal(1));
        }

        @Test
        public void test5Equals5() {
            assertEquals(5, normal(5));
        }

        @Test
        public void test8Equals21() {
            assertEquals(21, normal(8));
        }

        @Test
        public void test45Equals1134903170() {
            assertEquals(1134903170L, normal(45));
        }
    }

    @Nested
    class Memoization {

        @Test
        public void test1Equals1() {
            assertEquals(1, memo(1));
        }

        @Test
        public void test5Equals5() {
            assertEquals(5, memo(5));
        }

        @Test
        public void test8Equals21() {
            assertEquals(21, memo(8));
        }

        @Test
        public void test45Equals1134903170() {
            assertEquals(1134903170L, memo(45));
        }
    }

}
