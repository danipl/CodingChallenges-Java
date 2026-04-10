package com.danipl.development.recursion;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.danipl.development.recursion.CanConstruct.memo;
import static com.danipl.development.recursion.CanConstruct.normal;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CanConstructTest {

    @Nested
    class Normal {

        @Test
        public void testTestCanConstruct() {
            assertTrue(normal("test", new String[]{"t", "es"}));
        }

        @Test
        public void testTestCannotConstruct() {
            assertFalse(normal("test", new String[]{"t", "et", "s"}));
        }

        @Test
        public void testAbdcefCanConstruct() {
            assertTrue(normal("abcdef", new String[]{"ab", "abc", "cd", "def", "abcd"}));
        }

        @Test
        public void testSkateboardCannotConstruct() {
            assertFalse(normal("skateboard", new String[]{"bo", "rd", "ate", "t", "ska", "sk", "boar"}));
        }

        @Test
        public void testEeeeeeeeeeeeeeeeeeeeeeeeeeefCannotConstruct() {
            assertFalse(normal("eeeeeeeeeeeeeeeeeeeeeeeeeeef", new String[]{"e", "ee", "eee", "eeee", "eeeee", "eeeeee"}));
        }

    }

    @Nested
    class Memoization {

        @Test
        public void testTestCanConstruct() {
            assertTrue(memo("test", new String[]{"t", "es"}));
        }

        @Test
        public void testTestCannotConstruct() {
            assertFalse(memo("test", new String[]{"t", "et", "s"}));
        }

        @Test
        public void testAbdcefCanConstruct() {
            assertTrue(memo("abcdef", new String[]{"ab", "abc", "cd", "def", "abcd"}));
        }

        @Test
        public void testSkateboardCannotConstruct() {
            assertFalse(memo("skateboard", new String[]{"bo", "rd", "ate", "t", "ska", "sk", "boar"}));
        }

        @Test
        public void testEeeeeeeeeeeeeeeeeeeeeeeeeeefCannotConstruct() {
            assertFalse(memo("eeeeeeeeeeeeeeeeeeeeeeeeeeef", new String[]{"e", "ee", "eee", "eeee", "eeeee", "eeeeee"}));
        }

    }

}
