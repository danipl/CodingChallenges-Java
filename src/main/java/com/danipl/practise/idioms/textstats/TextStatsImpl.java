package com.danipl.practise.idioms.textstats;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of {@link TextStats}.
 *
 * TODO: Implement all methods using modern Java idioms (Streams, Optional,
 * etc.)
 */
public final class TextStatsImpl implements TextStats {

    @Override
    public int wordCount(final String text) {
        if (!isValid(text)) {
            return 0;
        }
        return (int) streamFromString(text).count();
    }

    @Override
    public Set<String> uniqueWords(final String text) {
        if (!isValid(text)) {
            return Collections.emptySet();
        }
        return streamFromString(text)
                .map(String::toLowerCase)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Optional<String> longestWord(final String text) {
        if (!isValid(text)) {
            return Optional.empty();
        }
        return streamFromString(text)
                .max(Comparator.comparingInt(String::length));
    }

    @Override
    public Optional<Double> averageWordLength(final String text) {
        if (!isValid(text)) {
            return Optional.empty();
        }
        final OptionalDouble averageOp = streamFromString(text)
                .mapToInt(String::length)
                .average();
        return averageOp.isEmpty() ? Optional.empty() : Optional.of(averageOp.getAsDouble());
    }

    private boolean isValid(final String text) {
        return text != null && !text.trim().isEmpty();
    }

    private Stream<String> streamFromString(final String text) {
        return Arrays.stream(text.trim().split(" ")).filter(candidate -> !candidate.isEmpty());
    }

}
