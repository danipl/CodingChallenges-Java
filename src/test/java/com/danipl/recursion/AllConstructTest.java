package com.danipl.recursion;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.danipl.recursion.AllConstruct.memo;
import static com.danipl.recursion.AllConstruct.normal;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AllConstructTest {

    @Nested
    class Normal {

        @Test
        public void testPurpleAllConstructs() {
            assertEquals(2, normal("purple", new String[]{"purp", "p", "ur", "le", "purpl"}).size());
        }

        @Test
        public void testAbcdefAllConstructs() {
            assertEquals(4, normal("abcdef", new String[]{"ab", "abc", "cd", "def", "abcd", "ef", "c"}).size());
        }


        @Test
        public void testAaaaaaaaaaaaaaaaaaaaafAllConstructs() {
            assertEquals(223317, normal("aaaaaaaaaaaaaaaaaaaaaf", new String[]{"a", "aa", "aaa", "f"}).size());
        }

    }

    @Nested
    class Memoization {

        @Test
        public void testPurpleAllConstructs() {
            assertEquals(2, memo("purple", new String[]{"purp", "p", "ur", "le", "purpl"}).size());
        }

        @Test
        public void testAbcdefAllConstructs() {
            assertEquals(4, memo("abcdef", new String[]{"ab", "abc", "cd", "def", "abcd", "ef", "c"}).size());
        }

        @Test
        public void testAaaaaaaaaaaaaaaaaaaaafAllConstructs() {
            assertEquals(223317, memo("aaaaaaaaaaaaaaaaaaaaaf", new String[]{"a", "aa", "aaa", "f"}).size());
        }

    }

}
