package com.danipl.practise.idioms.textstats;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TextStats tests")
class TextStatsTest {

    private TextStats instance;

    @BeforeEach
    void setUp() {
        instance = TextStats.of();
    }

    @Nested
    @DisplayName("wordCount")
    class WordCount {

        @Test
        @DisplayName("should return 0 for null input")
        void nullInput() {
            assertEquals(0, instance.wordCount(null));
        }

        @Test
        @DisplayName("should return 0 for empty string")
        void emptyString() {
            assertEquals(0, instance.wordCount(""));
        }

        @Test
        @DisplayName("should return 0 for whitespace-only string")
        void whitespaceOnly() {
            assertEquals(0, instance.wordCount("   \t\n  "));
        }

        @Test
        @DisplayName("should count single word")
        void singleWord() {
            assertEquals(1, instance.wordCount("hello"));
        }

        @Test
        @DisplayName("should count multiple words")
        void multipleWords() {
            assertEquals(3, instance.wordCount("hello world foo"));
        }

        @Test
        @DisplayName("should handle multiple spaces between words")
        void multipleSpaces() {
            assertEquals(3, instance.wordCount("hello   world   foo"));
        }
    }

    @Nested
    @DisplayName("uniqueWords")
    class UniqueWords {

        @Test
        @DisplayName("should return empty set for null input")
        void nullInput() {
            assertTrue(instance.uniqueWords(null).isEmpty());
        }

        @Test
        @DisplayName("should return empty set for empty string")
        void emptyString() {
            assertTrue(instance.uniqueWords("").isEmpty());
        }

        @Test
        @DisplayName("should return single word in set")
        void singleWord() {
            assertEquals(Set.of("hello"), instance.uniqueWords("hello"));
        }

        @Test
        @DisplayName("should deduplicate words")
        void deduplicate() {
            assertEquals(Set.of("hello", "world"), instance.uniqueWords("hello world hello"));
        }

        @Test
        @DisplayName("should be case-insensitive")
        void caseInsensitive() {
            assertEquals(Set.of("hello", "world"), instance.uniqueWords("Hello WORLD hello"));
        }

        @Test
        @DisplayName("should return immutable set")
        void immutableSet() {
            Set<String> words = instance.uniqueWords("hello world");
            assertThrows(UnsupportedOperationException.class, () -> words.add("foo"));
        }
    }

    @Nested
    @DisplayName("longestWord")
    class LongestWord {

        @Test
        @DisplayName("should return empty Optional for null input")
        void nullInput() {
            assertTrue(instance.longestWord(null).isEmpty());
        }

        @Test
        @DisplayName("should return empty Optional for empty string")
        void emptyString() {
            assertTrue(instance.longestWord("").isEmpty());
        }

        @Test
        @DisplayName("should return single word")
        void singleWord() {
            assertEquals("hello", instance.longestWord("hello").orElse(null));
        }

        @Test
        @DisplayName("should return longest word")
        void longestWord() {
            assertEquals("world", instance.longestWord("hi world foo").orElse(null));
        }

        @Test
        @DisplayName("should return first encountered on tie")
        void tie() {
            String result = instance.longestWord("hello world foo").orElse(null);
            assertTrue(result.equals("hello") || result.equals("world"));
        }
    }

    @Nested
    @DisplayName("averageWordLength")
    class AverageWordLength {

        @Test
        @DisplayName("should return empty Optional for null input")
        void nullInput() {
            assertTrue(instance.averageWordLength(null).isEmpty());
        }

        @Test
        @DisplayName("should return empty Optional for empty string")
        void emptyString() {
            assertTrue(instance.averageWordLength("").isEmpty());
        }

        @Test
        @DisplayName("should return length for single word")
        void singleWord() {
            assertEquals(5.0, instance.averageWordLength("hello").orElse(0.0));
        }

        @Test
        @DisplayName("should compute average for multiple words")
        void multipleWords() {
            // "hi" (2) + "world" (5) + "foo" (3) = 10 / 3 = 3.333...
            assertEquals(10.0 / 3, instance.averageWordLength("hi world foo").orElse(0.0), 0.001);
        }
    }
}
