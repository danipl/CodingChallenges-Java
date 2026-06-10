package com.danipl.platform.concurrency.taskscheduler;

import java.time.Clock;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public final class TaskSchedulerImpl implements TaskScheduler {

    private static final long DISPATCHER_JOIN_TIMEOUT_MS = 1000;

    private final ExecutorService executor;
    private final PriorityQueue<ScheduledTask<?>> queue;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Clock clock;
    private final Consumer<Throwable> errorHandler;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final Thread dispatcher;
    private final AtomicLong taskIdCounter = new AtomicLong(0);
    private final AtomicInteger pendingTaskCount = new AtomicInteger(0);

    public TaskSchedulerImpl(int poolSize) {
        this(poolSize, Clock.systemUTC(), defaultErrorHandler());
    }

    public TaskSchedulerImpl(int poolSize, Clock clock) {
        this(poolSize, clock, defaultErrorHandler());
    }

    public TaskSchedulerImpl(int poolSize, Clock clock, Consumer<Throwable> errorHandler) {
        if (poolSize < 1) {
            throw new IllegalArgumentException("Pool size must be >= 1");
        }
        this.clock = Objects.requireNonNull(clock, "Clock must not be null");
        this.errorHandler = Objects.requireNonNull(errorHandler, "Error handler must not be null");
        this.executor = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "TaskScheduler-worker");
            t.setDaemon(false);
            return t;
        });
        this.queue = new PriorityQueue<>();
        this.dispatcher = new Thread(this::dispatchLoop, "TaskScheduler-dispatcher");
        this.dispatcher.setDaemon(true);
        this.dispatcher.start();
    }

    private static Consumer<Throwable> defaultErrorHandler() {
        return ex -> System.err.println("[TaskScheduler] Task execution failed: " + ex);
    }

    @Override
    public void schedule(Runnable task, long delay, TimeUnit unit) {
        validateTask(task, delay, unit);
        if (shutdown.get()) {
            throw new IllegalStateException("Scheduler has been shut down");
        }
        long executionTime = clock.millis() + unit.toMillis(delay);
        ScheduledTask<?> scheduledTask = new ScheduledTask<>(executionTime, task, taskIdCounter.incrementAndGet());
        lock.lock();
        try {
            queue.add(scheduledTask);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
        pendingTaskCount.incrementAndGet();
    }

    @Override
    public <V> ScheduledFuture<V> submit(CallableWithException<V> task, long delay, TimeUnit unit) {
        validateTask(task, delay, unit);
        if (shutdown.get()) {
            throw new IllegalStateException("Scheduler has been shut down");
        }
        long executionTime = clock.millis() + unit.toMillis(delay);
        long id = taskIdCounter.incrementAndGet();
        ScheduledFutureImpl<V> future = new ScheduledFutureImpl<>();
        Runnable wrapper = () -> {
            try {
                V result = task.call();
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        };
        ScheduledTask<V> scheduledTask = new ScheduledTask<>(executionTime, wrapper, future, id);
        future.setTask(scheduledTask);
        lock.lock();
        try {
            queue.add(scheduledTask);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
        pendingTaskCount.incrementAndGet();
        return future;
    }

    private void validateTask(Object task, long delay, TimeUnit unit) {
        Objects.requireNonNull(task, "Task must not be null");
        Objects.requireNonNull(unit, "TimeUnit must not be null");
        if (delay < 0) {
            throw new IllegalArgumentException("Delay must be non-negative: " + delay);
        }
    }

    @Override
    public void shutdown() {
        if (!shutdown.compareAndSet(false, true)) {
            return;
        }
        lock.lock();
        try {
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
        dispatcher.interrupt();
        try {
            dispatcher.join(DISPATCHER_JOIN_TIMEOUT_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ScheduledTask<?> task;
        lock.lock();
        try {
            while ((task = queue.poll()) != null) {
                if (task.isCancelled()) {
                    pendingTaskCount.decrementAndGet();
                    continue;
                }
                long remainingDelay = Math.max(0, task.executionTime - clock.millis());
                final ScheduledTask<?> t = task;
                if (remainingDelay > 0) {
                    executor.execute(() -> {
                        try {
                            Thread.sleep(remainingDelay);
                            if (!t.isCancelled()) {
                                t.run();
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } catch (Exception e) {
                            errorHandler.accept(e);
                        } finally {
                            pendingTaskCount.decrementAndGet();
                        }
                    });
                } else {
                    final ScheduledTask<?> due = task;
                    executor.execute(() -> {
                        try {
                            due.run();
                        } catch (Exception e) {
                            errorHandler.accept(e);
                        } finally {
                            pendingTaskCount.decrementAndGet();
                        }
                    });
                }
            }
        } finally {
            lock.unlock();
        }
        executor.shutdown();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);

        long dispatcherTimeoutMs = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
        if (dispatcherTimeoutMs > 0) {
            dispatcher.join(dispatcherTimeoutMs);
        }

        long remainingMs = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
        if (remainingMs <= 0) {
            return false;
        }
        return executor.awaitTermination(remainingMs, TimeUnit.MILLISECONDS);
    }

    @Override
    public int pendingTaskCount() {
        return pendingTaskCount.get();
    }

    private void dispatchLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            ScheduledTask<?> task = null;
            lock.lock();
            try {
                while (queue.isEmpty() && !shutdown.get()) {
                    notEmpty.await();
                }
                if (shutdown.get() && queue.isEmpty()) {
                    break;
                }
                task = queue.peek();
                if (task == null) {
                    continue;
                }
                if (task.isCancelled()) {
                    queue.poll();
                    pendingTaskCount.decrementAndGet();
                    continue;
                }
                long now = clock.millis();
                if (task.executionTime <= now) {
                    queue.poll();
                } else {
                    long waitMs = task.executionTime - now;
                    notEmpty.awaitNanos(TimeUnit.MILLISECONDS.toNanos(waitMs));
                    continue;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } finally {
                lock.unlock();
            }
            if (task != null && !task.isCancelled()) {
                submitTask(task);
            }
        }
    }

    private void submitTask(ScheduledTask<?> task) {
        executor.execute(() -> {
            if (task.isCancelled()) {
                pendingTaskCount.decrementAndGet();
                return;
            }
            task.setExecutingThread(Thread.currentThread());
            try {
                task.run();
            } catch (Exception e) {
                errorHandler.accept(e);
            } finally {
                task.clearExecutingThread();
                pendingTaskCount.decrementAndGet();
            }
        });
    }

    private static class ScheduledTask<V> implements Comparable<ScheduledTask<?>> {
        final long executionTime;
        private final Runnable runnable;
        private final ScheduledFutureImpl<V> future;
        final long id;
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private volatile Thread executingThread;

        ScheduledTask(long executionTime, Runnable runnable, ScheduledFutureImpl<V> future, long id) {
            this.executionTime = executionTime;
            this.runnable = runnable;
            this.future = future;
            this.id = id;
        }

        ScheduledTask(long executionTime, Runnable runnable, long id) {
            this.executionTime = executionTime;
            this.runnable = runnable;
            this.future = null;
            this.id = id;
        }

        void setExecutingThread(Thread thread) {
            this.executingThread = thread;
        }

        void clearExecutingThread() {
            this.executingThread = null;
        }

        void run() {
            if (cancelled.get()) {
                if (future != null) {
                    future.cancel(false);
                }
                return;
            }
            try {
                if (runnable != null) {
                    runnable.run();
                }
            } catch (Exception e) {
                if (future != null) {
                    future.completeExceptionally(e);
                }
            }
        }

        boolean cancel(boolean mayInterruptIfRunning) {
            boolean result = cancelled.compareAndSet(false, true);
            if (result) {
                if (mayInterruptIfRunning) {
                    Thread thread = executingThread;
                    if (thread != null) {
                        thread.interrupt();
                    }
                }
                if (future != null) {
                    future.cancel(false);
                }
            }
            return result;
        }

        boolean isCancelled() {
            return cancelled.get();
        }

        @Override
        public int compareTo(ScheduledTask<?> o) {
            return Long.compare(this.executionTime, o.executionTime);
        }
    }

    private static class ScheduledFutureImpl<V> implements TaskScheduler.ScheduledFuture<V> {
        private final CompletableFuture<V> delegate = new CompletableFuture<>();
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private ScheduledTask<V> task;

        ScheduledFutureImpl() {
        }

        void setTask(ScheduledTask<V> task) {
            this.task = task;
        }

        void complete(V result) {
            delegate.complete(result);
        }

        void completeExceptionally(Throwable ex) {
            delegate.completeExceptionally(ex);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (cancelled.compareAndSet(false, true)) {
                if (task != null) {
                    task.cancel(mayInterruptIfRunning);
                }
                return delegate.cancel(mayInterruptIfRunning);
            }
            return false;
        }

        @Override
        public boolean isCancelled() {
            return cancelled.get() || delegate.isCancelled();
        }

        @Override
        public boolean isDone() {
            return delegate.isDone();
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            return delegate.get();
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.get(timeout, unit);
        }
    }
}
