package com.danipl.platform.datastructures.bloomfilter;

/**
 * A thread-safe Bloom Filter implementing a space-efficient probabilistic data structure.
 *
 * <p>A Bloom Filter is used to test whether an element is a member of a set. It can return
 * false positives (reporting an element is in the set when it is not) but never false negatives
 * (reporting an element is not in the set when it actually is). This makes it ideal for
 * caching layers, database query optimization, and CDN routing where occasional false positives
 * are acceptable but false negatives are not.</p>
 *
 * <p>Key behaviors:</p>
 * <ul>
 *   <li>{@code add(item)} — inserts an element into the filter by setting bits at positions
 *       determined by multiple hash functions</li>
 *   <li>{@code mightContain(item)} — returns {@code true} if the element might be in the set
 *       (possible false positive), {@code false} if definitely not</li>
 *   <li>Configurable false-positive probability — the filter size and number of hash functions
 *       are computed from the expected number of insertions and desired accuracy</li>
 * </ul>
 *
 * <p>Thread-safety: All operations are thread-safe. Reads and writes are protected by a
 * {@link java.util.concurrent.locks.ReentrantReadWriteLock}, allowing concurrent reads while
 * serializing writes.</p>
 */
public interface BloomFilter<T> {

    /**
     * Creates a new Bloom Filter with the given configuration.
     *
     * @param config the configuration parameters (expected insertions, false positive probability)
     * @param <T>    the type of elements stored in the filter
     * @return a new BloomFilter instance
     */
    static <T> BloomFilter<T> of(Config config) {
        return new BloomFilterImpl<>(config);
    }

    // === Domain methods ===

    /**
     * Adds an element to the Bloom Filter.
     *
     * <p>This operation computes multiple hash values for the element and sets the
     * corresponding bits in the underlying bit array. After this call, {@link #mightContain(Object)}
     * will return {@code true} for this element.</p>
     *
     * @param item the element to add; must not be {@code null}
     * @throws NullPointerException if {@code item} is {@code null}
     */
    void add(T item);

    /**
     * Tests whether an element might be contained in the Bloom Filter.
     *
     * <p>This operation computes the same hash values used during {@link #add(Object)} and
     * checks if all corresponding bits are set. If any bit is not set, the element is
     * definitely not in the set. If all bits are set, the element might be in the set
     * (with a configurable false-positive probability).</p>
     *
     * @param item the element to test; must not be {@code null}
     * @return {@code true} if the element might be in the set (possible false positive),
     *         {@code false} if the element is definitely not in the set
     * @throws NullPointerException if {@code item} is {@code null}
     */
    boolean mightContain(T item);

    /**
     * Returns the approximate number of elements that have been added to the filter.
     *
     * <p>Note: Bloom Filters do not natively track the count of distinct elements. This
     * method returns the number of {@link #add(Object)} calls made, which may include
     * duplicates.</p>
     *
     * @return the number of elements added to the filter
     */
    long size();

    /**
     * Returns the expected false-positive probability based on the current number of
     * elements in the filter.
     *
     * <p>This value starts at the configured {@link Config#falsePositiveProbability()}
     * and increases as more elements are added. When the number of elements exceeds the
     * expected insertions, the actual false-positive rate will exceed the configured rate.</p>
     *
     * @return the current expected false-positive probability (between 0.0 and 1.0)
     */
    double expectedFalsePositiveProbability();

    /**
     * Removes all elements from the Bloom Filter, resetting it to its initial empty state.
     *
     * <p>After this call, {@link #mightContain(Object)} will return {@code false} for all
     * elements, and {@link #size()} will return 0.</p>
     */
    void clear();

    /**
     * Returns the configuration used to create this Bloom Filter.
     *
     * @return the configuration record
     */
    Config config();

    // === Nested types ===

    /**
     * Configuration record for a Bloom Filter.
     *
     * <p>The filter size (number of bits) and number of hash functions are computed from
     * these parameters using optimal formulas:</p>
     * <ul>
     *   <li>Bit array size: {@code m = -(n * ln(p)) / (ln(2)^2)}</li>
     *   <li>Number of hash functions: {@code k = (m/n) * ln(2)}</li>
     * </ul>
     *
     * @param expectedInsertions       the expected number of distinct elements to be added (must be &gt;= 1)
     * @param falsePositiveProbability the desired false-positive probability (must be between 0.0 exclusive and 1.0 exclusive)
     */
    record Config(int expectedInsertions, double falsePositiveProbability) {
        public Config {
            if (expectedInsertions < 1) {
                throw new IllegalArgumentException("expectedInsertions must be >= 1, got: " + expectedInsertions);
            }
            if (falsePositiveProbability <= 0.0 || falsePositiveProbability >= 1.0) {
                throw new IllegalArgumentException(
                        "falsePositiveProbability must be between 0.0 exclusive and 1.0 exclusive, got: "
                                + falsePositiveProbability);
            }
        }

        /**
         * Computes the optimal bit array size for this configuration.
         *
         * @return the number of bits in the filter
         */
        public int bitsetSize() {
            double n = expectedInsertions;
            double p = falsePositiveProbability;
            double m = -(n * Math.log(p)) / (Math.log(2) * Math.log(2));
            return (int) Math.ceil(m);
        }

        /**
         * Computes the optimal number of hash functions for this configuration.
         *
         * @return the number of hash functions to use
         */
        public int numHashFunctions() {
            double m = bitsetSize();
            double n = expectedInsertions;
            double k = (m / n) * Math.log(2);
            return Math.max(1, (int) Math.round(k));
        }
    }
}
