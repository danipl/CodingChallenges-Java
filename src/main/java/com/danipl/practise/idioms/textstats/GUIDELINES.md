# Challenge: TextStats - Guidelines

## 1. Challenge Presentation
### What You're Building
A text statistics calculator that computes word counts, unique words, longest word, and average word length. This is a common pattern in text processing, analytics dashboards, and content validation.

### Core Contract
- Input: raw text string (may be null or empty)
- Output: statistics (counts, sets, optionals)
- Words are whitespace-separated, case-insensitive for uniqueness

### Interface Summary
| Method | Purpose |
|--------|---------|
| `wordCount` | Total number of words |
| `uniqueWords` | Set of unique words (lowercase) |
| `longestWord` | Longest word (Optional) |
| `averageWordLength` | Average length (Optional) |

---

## 2. Edge & Corner Cases
### How to Identify Them
Think about: null input, empty string, whitespace-only, single word, all same words, ties for longest.

| # | Edge Case | How It Surfaces | How to Handle |
|---|-----------|-----------------|---------------|
| 1 | null input | NPE risk | Return 0 / empty set / empty Optional |
| 2 | empty string | No words | Return 0 / empty set / empty Optional |
| 3 | whitespace only | No words after split | Return 0 / empty set / empty Optional |
| 4 | single word | Boundary | Return 1 / set with one word / that word |
| 5 | case variations | "Hello" vs "hello" | Normalize to lowercase for uniqueness |
| 6 | ties for longest | Multiple words same length | Return any one (first encountered) |

---

## 3. First Approach - Chain of Thinking
- **Minute 0-2**: Clarify requirements. What's a "word"? How to handle null/empty?
- **Minute 2-5**: Sketch helper to split text into word stream. Handle null/empty upfront.
- **Minute 5-10**: Implement each method using Streams. Use `Optional` for methods that may have no result.
- **Minute 10-20**: Refine with idiomatic Java: `Collectors.toSet()`, `Comparator.comparingInt()`, `Collectors.averagingInt()`.

---

## 4. Communication Approach
Explain design choices out loud:
- Why split into a helper method? (DRY, testable)
- Why `Optional` instead of returning null or throwing? (Explicit absence)
- Why `Set<String>` instead of `List<String>` for unique words? (Semantic correctness)
- Why normalize to lowercase? (Case-insensitive comparison)

---

## 5. Implementation Structure
```java
public final class TextStatsImpl implements TextStats {
    // Helper: split text into stream of lowercase words
    private Stream<String> words(String text) {
        // handle null/empty, split by whitespace, lowercase
    }

    @Override
    public int wordCount(String text) {
        // words(text).count()
    }

    @Override
    public Set<String> uniqueWords(String text) {
        // words(text).collect(Collectors.toUnmodifiableSet())
    }

    @Override
    public Optional<String> longestWord(String text) {
        // words(text).max(Comparator.comparingInt(String::length))
    }

    @Override
    public Optional<Double> averageWordLength(String text) {
        // words(text).mapToInt(String::length).average()
    }
}
```

---

## 6. Technical Pro Tips
- Use `Stream<String>` as the core abstraction for all methods.
- Prefer `Optional` over null for methods that may have no result.
- Use `Collectors.toUnmodifiableSet()` for immutable results.
- Handle null/empty input once in the helper, not in every method.
- Use `Comparator.comparingInt(String::length)` for clean max/min.

---

## 7. Common Mistakes to Avoid
- Manually checking for null/empty in every method (extract to helper).
- Returning null instead of `Optional.empty()`.
- Mutating the input string or returning mutable collections.
- Forgetting to normalize case for uniqueness.
- Using `split("\\s+")` without handling empty input (returns array with one empty string).

---

## 8. Verification Checklist
- [ ] null input returns 0 / empty set / empty Optional
- [ ] empty string returns 0 / empty set / empty Optional
- [ ] whitespace-only returns 0 / empty set / empty Optional
- [ ] single word works correctly
- [ ] case-insensitive uniqueness ("Hello" and "hello" are same)
- [ ] longest word returns first encountered on tie
- [ ] average word length is precise (double, not int)

---

## 9. Extension Points
- How would you make this thread-safe for concurrent access?
- How to support custom word delimiters (e.g., punctuation)?
- How to add more stats (median word length, most frequent word)?

---

## 10. Production References
- Effective Java (Joshua Bloch) - Item 55: Return Optionals
- Java Streams API documentation
- Clean Code (Robert C. Martin) - Chapter 3: Functions
