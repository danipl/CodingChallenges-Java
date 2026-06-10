package com.danipl.platform.concurrency.taskscheduler;

import java.util.concurrent.*;

/**
 * Asynchronous Task Scheduler with delayed execution.
 *
 * Uses a custom priority queue (min-heap by execution time) + fixed thread pool.
 * Tasks execute after the specified delay. Thread-safe for concurrent schedule calls.
 */
public interface TaskScheduler {

    /**
     * Schedules a Runnable task to run after the specified delay.
     *
     * @param task   the task to execute
     * @param delay  the delay before execution
     * @param unit   the time unit of the delay
     * @throws IllegalStateException if the scheduler has been shut down
     */
    void schedule(Runnable task, long delay, TimeUnit unit);

    /**
     * Submits a callable task to run after the specified delay and returns a Future.
     *
     * @param task   the task to execute
     * @param delay  the delay before execution
     * @param unit   the time unit of the delay
     * @param <V>    the result type
     * @return a Future for the task
     * @throws IllegalStateException if the scheduler has been shut down
     */
    <V> ScheduledFuture<V> submit(CallableWithException<V> task, long delay, TimeUnit unit);

    /**
     * Initiates an orderly shutdown in which previously scheduled tasks are
     * executed, but no new tasks will be accepted.
     */
    void shutdown();

    /**
     * Blocks until all tasks have completed execution after a shutdown request,
     * or the timeout occurs, or the current thread is interrupted.
     *
     * @return true if all tasks completed within the timeout, false if timed out
     */
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    /**
     * Returns the number of tasks that have been scheduled but not yet started.
     */
    int pendingTaskCount();

    /**
     * Creates a new TaskScheduler with the given pool size.
     *
     * @param poolSize the number of threads in the executor pool
     * @return a new TaskScheduler instance
     */
    static TaskScheduler of(int poolSize) {
        return new TaskSchedulerImpl(poolSize);
    }

    /**
     * A Callable that may throw a checked Exception.
     *
     * @param <V> the result type
     */
    @FunctionalInterface
    interface CallableWithException<V> {
        V call() throws Exception;
    }

    /**
     * A cancellable scheduled task extending Future.
     *
     * @param <V> the result type
     */
    interface ScheduledFuture<V> extends Future<V> {
        /**
         * Attempts to cancel execution of this task.
         *
         * @param mayInterruptIfRunning true if the thread executing this task should be interrupted
         * @return false if the task could not be cancelled (already completed or already cancelled)
         */
        @Override
        boolean cancel(boolean mayInterruptIfRunning);
    }
}
