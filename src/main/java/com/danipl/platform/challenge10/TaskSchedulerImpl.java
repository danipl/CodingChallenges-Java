package com.danipl.platform.challenge10;

import java.util.concurrent.*;

/**
 * Skeleton implementation of TaskScheduler.
 * All methods throw UnsupportedOperationException.
 */
public final class TaskSchedulerImpl implements TaskScheduler {

    public TaskSchedulerImpl(int poolSize) {
        // TODO: initialize thread pool and scheduler queue
    }

    @Override
    public void schedule(Runnable task, long delay, TimeUnit unit) {
        // TODO: implement with priority queue and delay queue
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public <V> ScheduledFuture<V> submit(CallableWithException<V> task, long delay, TimeUnit unit) {
        // TODO: implement callable scheduling with future result
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void shutdown() {
        // TODO: implement graceful shutdown
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        // TODO: implement blocked wait for task completion
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public int pendingTaskCount() {
        // TODO: count queued but not started tasks
        throw new UnsupportedOperationException("Not implemented");
    }
}
