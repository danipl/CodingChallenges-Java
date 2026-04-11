package com.danipl.platform.experiment;

import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class ThreadPoolExperiment {

    private static final int CORE_POOL_SIZE = 4;
    private static final int MAX_POOL_SIZE = 10;
    private static final int QUEUE_CAPACITY = 10;
    private static final int TASK_COUNT = 100;
    private static final long TASK_MIN_DURATION_MS = 500;
    private static final long TASK_MAX_DURATION_MS = 1500;
    private static final long FUTURE_TIMEOUT_MS = 3000;
    private static final long POOL_SHUTDOWN_TIMEOUT_S = 60;

    public static void main(final String[] args) {
        validateArgs(args);

        final String handlerName = parseHandler(args.length > 0 ? args[0] : null);
        System.out.println("[POOL-MONITOR] Using rejection handler: " + handlerName);

        final RejectedExecutionHandler handler = createHandler(handlerName);
        final ThreadPoolExecutor fixedPool = createThreadPool(handler);

        final TaskResultCollector collector = new TaskResultCollector();
        final Collection<Future<?>> tasks = submitTasks(fixedPool, TASK_COUNT, collector);

        fixedPool.shutdown();

        final ExecutorService monitorPool = Executors.newVirtualThreadPerTaskExecutor();
        try {
            monitorResults(tasks, (int) FUTURE_TIMEOUT_MS, collector, monitorPool);
        } finally {
            shutdownGracefully(fixedPool, monitorPool);
        }
    }

    private static void validateArgs(final String[] args) {
        if (args == null) {
            throw new IllegalArgumentException("Args array must not be null");
        }
        if (CORE_POOL_SIZE > MAX_POOL_SIZE) {
            throw new IllegalStateException("Core pool size must be <= max pool size");
        }
        if (QUEUE_CAPACITY <= 0) {
            throw new IllegalStateException("Queue capacity must be positive");
        }
        if (TASK_COUNT <= 0) {
            throw new IllegalStateException("Task count must be positive");
        }
    }

    private static String parseHandler(final String handlerName) {
        if (handlerName == null || handlerName.isBlank()) {
            return "caller";
        }
        return switch (handlerName.toLowerCase()) {
            case "abort", "discard", "discardoldest", "caller" -> handlerName;
            default -> throw new IllegalArgumentException(
                    "Unknown handler: " + handlerName + ". Valid: abort, discard, discardoldest, caller"
            );
        };
    }

    private static RejectedExecutionHandler createHandler(final String handlerName) {
        return switch (handlerName) {
            case "abort" -> new ThreadPoolExecutor.AbortPolicy();
            case "discard" -> new ThreadPoolExecutor.DiscardPolicy();
            case "discardoldest" -> new ThreadPoolExecutor.DiscardOldestPolicy();
            default -> new ThreadPoolExecutor.CallerRunsPolicy();
        };
    }

    private static ThreadPoolExecutor createThreadPool(final RejectedExecutionHandler handler) {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                handler
        );
        System.out.println("[POOL-MONITOR] Created pool: core=" + CORE_POOL_SIZE + ", max=" + MAX_POOL_SIZE + ", queue=" + QUEUE_CAPACITY);
        return executor;
    }

    private static Collection<Future<?>> submitTasks(final ThreadPoolExecutor executor, final int taskCount, final TaskResultCollector collector) {
        return IntStream.range(0, taskCount).mapToObj(i -> executor.submit(() -> {
            System.out.println("[TASK-EXEC] Task " + i + " started by " + Thread.currentThread().getName());
            if ((int) (Math.random() * 10) == 1) {
                collector.recordFailed(new RuntimeException("Simulated failure for task " + i));
                throw new RuntimeException("Simulated failure for task " + i);
            }
            try {
                Thread.sleep(TASK_MIN_DURATION_MS + (long) (Math.random() * (TASK_MAX_DURATION_MS - TASK_MIN_DURATION_MS)));
                collector.recordCompleted();
            } catch (final InterruptedException e) {
                collector.recordFailed(e);
                Thread.currentThread().interrupt();
            }
        })).collect(toList());
    }

    private static void monitorResults(
            final Collection<Future<?>> tasks, final int timeoutMs,
            final TaskResultCollector collector, final ExecutorService monitorPool
    ) {
        System.out.println("[POOL-MONITOR] Monitoring " + tasks.size() + " tasks with timeout " + timeoutMs + "ms");

        try {
            final Collection<Future<?>> checkers = tasks.stream().map(future -> monitorPool.submit(() -> {
                if (future.isCancelled()) {
                    collector.recordCancelled();
                } else {
                    try {
                        future.get();
                    } catch (final Exception e) {
                        collector.recordFailed(e);
                    }
                }
            })).collect(toList());

            for (final Future<?> checker : checkers) {
                try {
                    checker.get(timeoutMs, TimeUnit.MILLISECONDS);
                } catch (final Exception e) {
                    System.out.println("[POOL-MONITOR] Task check timed out or failed: " + e.getMessage());
                }
            }

            if (monitorPool.awaitTermination(1, TimeUnit.MINUTES)) {
                System.out.println("[POOL-MONITOR] All tasks completed successfully");
            }
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (!monitorPool.isShutdown()) {
                monitorPool.shutdown();
            }
        }

        System.out.println("\nCompleted tasks: " + collector.getCompletedCount());
        System.out.println("Cancelled tasks: " + collector.getCancelledCount());
        System.out.println("Failed tasks: " + collector.getFailedCount());
    }

    private static void shutdownGracefully(final ExecutorService... executors) {
        System.out.println("[SHUTDOWN] Initiating graceful shutdown for " + executors.length + " executor(s)");
        for (final ExecutorService executor : executors) {
            if (executor == null || executor.isShutdown()) {
                continue;
            }
            try {
                if (!executor.awaitTermination(POOL_SHUTDOWN_TIMEOUT_S, TimeUnit.SECONDS)) {
                    System.out.println("[SHUTDOWN] Executor did not terminate, forcing shutdown");
                    executor.shutdownNow();
                }
            } catch (final InterruptedException e) {
                System.out.println("[SHUTDOWN] Shutdown interrupted, forcing shutdownNow");
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            } finally {
                if (!executor.isTerminated()) {
                    executor.close();
                }
            }
        }
        System.out.println("[SHUTDOWN] All executors shut down");
    }

    static class TaskResultCollector {

        private final AtomicLong completedCount = new AtomicLong(0);
        private final AtomicLong cancelledCount = new AtomicLong(0);
        private final AtomicLong failedCount = new AtomicLong(0);

        void recordCompleted() {
            completedCount.incrementAndGet();
        }

        void recordFailed(final Throwable error) {
            failedCount.incrementAndGet();
        }

        void recordCancelled() {
            cancelledCount.incrementAndGet();
        }

        long getCompletedCount() {
            return completedCount.get();
        }

        long getCancelledCount() {
            return cancelledCount.get();
        }

        long getFailedCount() {
            return failedCount.get();
        }

    }

}
