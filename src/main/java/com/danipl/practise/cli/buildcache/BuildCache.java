package com.danipl.practise.cli.buildcache;

import java.nio.file.Path;
import java.util.Optional;

/**
 * A file-backed build cache: stores build artifacts by key so developers
 * don't recompile unchanged inputs.
 *
 * <p>Entries are stored as {@code <cacheDir>/<key>.bin}. The cache must be
 * safe, correct, and honest about its own failures — a build cache that
 * silently returns stale or wrong artifacts is worse than no cache at all.
 *
 * Requirements:
 *   - Keys are validated: non-null, non-blank, and safe for use as a file
 *     name ({@code [a-zA-Z0-9._-]+}) — no path traversal
 *   - {@code get} of an absent key returns {@code Optional.empty()}
 *   - A corrupt or unreadable entry is a {@link CacheException} with the
 *     key in the message — never a silent miss
 *   - {@code put} failures surface as {@link CacheException} — never a
 *     swallowed write
 *   - Stats are accurate under concurrent access (no lost updates)
 */
public interface BuildCache {

    /**
     * Factory method backed by the given cache directory.
     *
     * @param cacheDir the cache directory; never null
     * @return a BuildCache rooted at cacheDir
     */
    static BuildCache of(final Path cacheDir) {
        return new BuildCacheImpl(cacheDir);
    }

    /**
     * Reads a cached entry.
     *
     * @param key the cache key
     * @return the cached bytes, or empty if the key is absent
     * @throws IllegalArgumentException if key is null, blank, or unsafe
     * @throws CacheException if the entry exists but cannot be read
     *         (corrupt/unreadable) — message contains the key
     */
    Optional<byte[]> get(String key);

    /**
     * Stores a cache entry.
     *
     * @param key the cache key
     * @param content the artifact bytes; never null
     * @throws IllegalArgumentException if key is null, blank, or unsafe
     * @throws CacheException if the entry cannot be written — message
     *         contains the key
     */
    void put(String key, byte[] content);

    /**
     * Returns cumulative cache statistics.
     *
     * @return hits and misses so far
     */
    Stats stats();

    /**
     * Cumulative cache statistics.
     *
     * @param hits number of successful reads
     * @param misses number of absent/corrupt reads
     */
    record Stats(long hits, long misses) {
    }

    /**
     * Thrown when a cache entry exists but cannot be read or written.
     * The message is what a developer sees — it must name the key.
     */
    class CacheException extends RuntimeException {
        public CacheException(String message) {
            super(message);
        }

        public CacheException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
