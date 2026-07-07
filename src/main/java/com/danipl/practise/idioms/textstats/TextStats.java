package com.danipl.practise.idioms.textstats;

import java.util.Optional;
import java.util.Set;

/**
 * Computes statistics from a text string using modern Java idioms.
 *
 * Requirements:
 *   - Words are separated by whitespace
 *   - Words are case-insensitive for uniqueness (normalize to lowercase)
 *   - Empty/null input returns zero counts and empty optionals
 */
public interface TextStats {

    /**
     * Factory method to create a default implementation.
     */
    static TextStats of() {
        return new TextStatsImpl();
    }

    /**
     * Counts the total number of words in the text.
     *
     * @param text the input text (may be null or empty)
     * @return the word count (0 if null/empty)
     */
    int wordCount(String text);

    /**
     * Returns the set of unique words (case-insensitive, normalized to lowercase).
     *
     * @param text the input text (may be null or empty)
     * @return immutable set of unique lowercase words
     */
    Set<String> uniqueWords(String text);

    /**
     * Finds the longest word in the text.
     *
     * @param text the input text (may be null or empty)
     * @return Optional containing the longest word, or empty if no words
     */
    Optional<String> longestWord(String text);

    /**
     * Computes the average word length.
     *
     * @param text the input text (may be null or empty)
     * @return Optional containing the average length, or empty if no words
     */
    Optional<Double> averageWordLength(String text);
}
