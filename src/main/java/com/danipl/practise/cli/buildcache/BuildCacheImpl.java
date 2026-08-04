package com.danipl.practise.cli.buildcache;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/**
 * Implementation of {@link BuildCache}.
 */
public final class BuildCacheImpl implements BuildCache {

    private final Path cacheDir;

    private long hits = 0;
    private long misses = 0;

    private final static String RAW_KEY_PATTERN = "[a-zA-Z0-9._-]+";
    private final static Pattern KEY_PATTERN = Pattern.compile(RAW_KEY_PATTERN);

    private final ReentrantLock lock = new ReentrantLock(true);

    public BuildCacheImpl(final Path cacheDir) {
        // Deliberate deferral: construction must not throw — callers build the                                                                                                                                                                          █
        // cache up-front (locked in by the test contract).
        try {
            Files.createDirectories(cacheDir);
        } catch (final IOException e) {
            // Intentional - see above
        }
        this.cacheDir = cacheDir;
    }

    @Override
    public Optional<byte[]> get(final String key) {
        checkKey(key);
        final Path path = cacheDir.resolve(key + ".bin");
        lock.lock();
        try {
            if (!Files.exists(path)) {
                misses++;
                return Optional.empty();
            }
            final byte[] content = Files.readAllBytes(path);
            hits++;
            return Optional.of(content);
        } catch (final IOException e) {
            throw new CacheException("Corrupted entry: " + key, e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void put(final String key, final byte[] content) {
        checkKey(key);
        final Path path = cacheDir.resolve(key + ".bin");
        lock.lock();
        try {
            Files.write(path, content);
        } catch (final IOException e) {
            throw new CacheException("Unexpected error saving the entry: " + key, e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Stats stats() {
        lock.lock();
        try {
            return new Stats(hits, misses);
        } finally {
            lock.unlock();
        }
    }

    private void checkKey(final String key) {
        if (key == null) {
            throw new IllegalArgumentException("The key argument is null");
        } else if (key.trim().isEmpty()) {
            throw new IllegalArgumentException("The key is effectively empty");
        } else if (key.contains("\\") || key.contains("/")) {
            throw new IllegalArgumentException("The key contains path separators");
        } else if (!KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException("The key does not satisfy the required pattern: " + RAW_KEY_PATTERN);
        }
    }

}
