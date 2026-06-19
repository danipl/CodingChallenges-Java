package com.danipl.platform.resilience.idempotency;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * Implementation of {@link IdempotencyManager}.
 * <p>
 * Thread-safety: All state transitions use ConcurrentHashMap's atomic operations.
 * No explicit locks needed - relies on computeIfAbsent and compute for atomic compound operations.
 */
public final class IdempotencyManagerImpl implements IdempotencyManager {

    // === Fields ===
    private final Config config;
    private final Clock clock;
    private final ConcurrentHashMap<String, CompletableFuture<IdempotencyEntry<?>>> cache = new ConcurrentHashMap<>();

    // === Constructors ===

    public IdempotencyManagerImpl(final Config config) {
        this(config, Clock.systemDefaultZone());
    }

    public IdempotencyManagerImpl(final Config config, final Clock clock) {
        this.config = config;
        this.clock = clock;
    }

    // === Public methods ===

    @Override
    public <T> T execute(final String idempotencyKey, final Supplier<T> operation) throws IdempotencyException {
        if (this.size() >= this.config.maxCacheSize()) {
            this.cleanup();
            if (this.size() >= this.config.maxCacheSize()) {
                throw new IdempotencyException("Max operations exceeded");
            }
        }
        final CompletableFuture<IdempotencyEntry<?>> completableFuture = this.cache.computeIfAbsent(idempotencyKey, currentKey -> {
            final CompletableFuture<IdempotencyEntry<?>> future = new CompletableFuture<>();
            CompletableFuture.runAsync(() -> {
                try {
                    future.complete(new IdempotencyEntry<T>(
                            State.SUCCESS,
                            operation.get(),
                            null,
                            this.clock.instant()
                    ));
                } catch (final Exception ex) {
                    future.complete(new IdempotencyEntry<T>(
                            State.FAILED,
                            null,
                            ex,
                            this.clock.instant()
                    ));
                }
            });
            return future;
        });

        try {
            final IdempotencyEntry<?> entry = completableFuture.get();
            if (entry.state == State.SUCCESS) {
                return (T) entry.result;
            } else if (entry.state == State.FAILED) {
                throw new IdempotencyException("Operation failed", entry.error);
            } else {
                throw new IdempotencyException("Unexpected state: " + entry.state);
            }
        } catch (final InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IdempotencyException("Interrupted", ie);
        } catch (final Exception ex) {
            throw new IdempotencyException("Execution failed", ex);
        }
    }

    @Override
    public void executeVoid(final String idempotencyKey, final Runnable operation) throws IdempotencyException {
        final CompletableFuture<IdempotencyEntry<?>> completableFuture = this.cache.computeIfAbsent(idempotencyKey, currentKey -> {
            final CompletableFuture<IdempotencyEntry<?>> future = new CompletableFuture<>();
            CompletableFuture.runAsync(() -> {
                try {
                    operation.run();
                    future.complete(new IdempotencyEntry<>(
                            State.SUCCESS,
                            null,
                            null,
                            this.clock.instant()
                    ));
                } catch (final Exception ex) {
                    future.complete(new IdempotencyEntry<>(
                            State.FAILED,
                            null,
                            ex,
                            this.clock.instant()
                    ));
                }
            });
            return future;
        });

        try {
            final IdempotencyEntry<?> entry = completableFuture.get();
            if (entry.state == State.FAILED) {
                throw new IdempotencyException("Operation failed", entry.error);
            } else if (entry.state != State.SUCCESS) {
                throw new IdempotencyException("Unexpected state: " + entry.state);
            }
        } catch (final InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IdempotencyException("Interrupted", ie);
        } catch (final Exception ex) {
            throw new IdempotencyException("Execution failed", ex);
        }
    }


    @Override
    public <T> Optional<T> getCachedResult(final String idempotencyKey) {
        try {
            final CompletableFuture<IdempotencyEntry<?>> idempotencyEntryFuture = this.cache.get(idempotencyKey);
            if (idempotencyEntryFuture == null) {
                return Optional.empty();
            }
            return Optional.ofNullable((T) idempotencyEntryFuture.get().result);
        } catch (final ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IdempotencyException("Interrupted", e);
        }
    }

    @Override
    public boolean isInProgress(final String idempotencyKey) {
        final CompletableFuture<IdempotencyEntry<?>> idempotencyEntryFuture = this.cache.get(idempotencyKey);
        if (idempotencyEntryFuture == null) {
            return false;
        }
        return !idempotencyEntryFuture.isDone();
    }

    @Override
    public Optional<State> getState(final String idempotencyKey) {
        try {
            final CompletableFuture<IdempotencyEntry<?>> idempotencyEntryFuture = this.cache.get(idempotencyKey);
            if (idempotencyEntryFuture == null) {
                return Optional.empty();
            }
            if (!idempotencyEntryFuture.isDone()) {
                return Optional.of(State.IN_PROGRESS);
            }
            return Optional.ofNullable(this.cache.get(idempotencyKey).get().state);
        } catch (final ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IdempotencyException("Interrupted", e);
        }
    }

    @Override
    public int cleanup() {
        final Instant cutoff = this.clock.instant().minus(this.config.ttl());
        int[] cleaned = {0};
        this.cache.entrySet().removeIf(entry -> {
            try {
                final CompletableFuture<IdempotencyEntry<?>> idempotencyEntryFuture = entry.getValue();
                final IdempotencyEntry<?> idempotencyEntry = idempotencyEntryFuture.get();
                if (idempotencyEntryFuture.isDone() && idempotencyEntry.createdAt.isBefore(cutoff)) {
                    cleaned[0]++;
                    return true;
                }
            } catch (final ExecutionException | InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IdempotencyException("Interrupted", e);
            }
            return false;
        });

        return cleaned[0];
    }

    @Override
    public int size() {
        return this.cache.size();
    }

    // === Private helper class ===

    /**
     * Internal entry representing an idempotency operation.
     */
    private static final class IdempotencyEntry<T> {
        private final State state;
        private final T result;
        private final Throwable error;
        private final Instant createdAt;

        IdempotencyEntry(State state, T result, Throwable error, Instant createdAt) {
            this.state = state;
            this.result = result;
            this.error = error;
            this.createdAt = createdAt;
        }

        State state() {
            return state;
        }

        T result() {
            return result;
        }

        Throwable error() {
            return error;
        }

        Instant createdAt() {
            return createdAt;
        }
    }
}
