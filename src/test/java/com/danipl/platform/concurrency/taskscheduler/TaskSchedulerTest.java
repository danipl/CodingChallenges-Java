package com.danipl.platform.concurrency.taskscheduler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("TaskScheduler tests")
class TaskSchedulerTest {

    private TaskScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = TaskScheduler.of(4);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        scheduler.shutdown();
        assertTrue(scheduler.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Nested
    @DisplayName("Basic delayed execution")
    class BasicDelayedExecution {

        @Test
        @DisplayName("task executes after the specified delay")
        void executesAfterDelay() throws Exception {
            AtomicLong executeTime = new AtomicLong(0);
            long startTime = System.currentTimeMillis();
            Future<?> future = scheduler.submit(() -> {
                executeTime.set(System.currentTimeMillis());
                return "done";
            }, 100, TimeUnit.MILLISECONDS);

            assertNotNull(future.get(2, TimeUnit.SECONDS));
            long elapsed = executeTime.get() - startTime;
            assertTrue(elapsed >= 80, "Task should not execute before delay expires");
        }

        @Test
        @DisplayName("very short delay (0ms) executes almost immediately")
        void zeroDelay() throws Exception {
            AtomicBoolean executed = new AtomicBoolean(false);
            scheduler.schedule(() -> executed.set(true), 0, TimeUnit.MILLISECONDS);
            Thread.sleep(200);
            assertTrue(executed.get());
        }
    }

    @Nested
    @DisplayName("Multiple tasks ordering")
    class TaskOrdering {

        @Test
        @DisplayName("tasks with different delays execute in order")
        void differentDelaysInOrder() throws Exception {
            List<String> executionOrder = Collections.synchronizedList(new ArrayList<>());

            scheduler.submit(() -> {
                executionOrder.add("task2");
                return null;
            }, 100, TimeUnit.MILLISECONDS);

            scheduler.submit(() -> {
                executionOrder.add("task1");
                return null;
            }, 50, TimeUnit.MILLISECONDS);

            scheduler.submit(() -> {
                executionOrder.add("task3");
                return null;
            }, 150, TimeUnit.MILLISECONDS);

            Thread.sleep(500);
            assertEquals(List.of("task1", "task2", "task3"), executionOrder);
        }
    }

    @Nested
    @DisplayName("Pending task count")
    class PendingCount {

        @Test
        @DisplayName("pendingTaskCount reflects scheduled but not started tasks")
        void pendingCountAccuracy() throws Exception {
            CountDownLatch blockTask = new CountDownLatch(1);

            scheduler.schedule(() -> {
                try {
                    blockTask.await();
                } catch (InterruptedException ignored) {
                }
            }, 0, TimeUnit.MILLISECONDS);

            scheduler.schedule(() -> {
            }, 500, TimeUnit.MILLISECONDS);
            scheduler.schedule(() -> {
            }, 1000, TimeUnit.MILLISECONDS);

            Thread.sleep(100);
            int pending = scheduler.pendingTaskCount();
            assertTrue(pending >= 2, "Should have at least 2 pending tasks");

            blockTask.countDown();
        }
    }

    @Nested
    @DisplayName("Shutdown behavior")
    class ShutdownBehavior {

        @Test
        @DisplayName("shutdown stops accepting new tasks")
        void shutdownRejectsNewTasks() {
            scheduler.shutdown();
            assertThrows(IllegalStateException.class,
                    () -> scheduler.schedule(() -> {
                    }, 100, TimeUnit.MILLISECONDS));
        }

        @Test
        @DisplayName("awaitTermination waits for pending tasks")
        void awaitTerminationWaits() throws Exception {
            CountDownLatch taskDone = new CountDownLatch(1);
            scheduler.schedule(() -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
                taskDone.countDown();
            }, 0, TimeUnit.MILLISECONDS);

            scheduler.shutdown();
            boolean completed = scheduler.awaitTermination(5, TimeUnit.SECONDS);
            assertTrue(completed);
            assertEquals(0, taskDone.getCount());
        }

        @Test
        @DisplayName("task scheduled after shutdown throws exception")
        void scheduleAfterShutdownThrows() {
            scheduler.shutdown();
            assertThrows(IllegalStateException.class,
                    () -> scheduler.submit(() -> "should not run", 0, TimeUnit.MILLISECONDS));
        }
    }

    @Nested
    @DisplayName("Error handling")
    class ErrorHandling {

        @Test
        @DisplayName("task exception doesn't crash scheduler")
        void exceptionDoesntCrash() throws Exception {
            scheduler.schedule(() -> {
                throw new RuntimeException("intentional failure");
            }, 0, TimeUnit.MILLISECONDS);

            AtomicBoolean secondTask = new AtomicBoolean(false);
            scheduler.schedule(() -> secondTask.set(true), 50, TimeUnit.MILLISECONDS);

            Thread.sleep(300);
            assertTrue(secondTask.get(), "Scheduler should continue after task exception");
        }
    }

    @Nested
    @DisplayName("Concurrent scheduling")
    class ConcurrentScheduling {

        @Test
        @DisplayName("concurrent scheduling from many threads is safe")
        void concurrentSchedule() throws InterruptedException {
            int threadCount = 20;
            int tasksPerThread = 50;
            AtomicInteger completed = new AtomicInteger(0);
            ExecutorService workers = Executors.newFixedThreadPool(threadCount);
            CountDownLatch start = new CountDownLatch(1);
            CountDownLatch done = new CountDownLatch(threadCount);

            for (int t = 0; t < threadCount; t++) {
                workers.submit(() -> {
                    try {
                        start.await();
                        for (int i = 0; i < tasksPerThread; i++) {
                            scheduler.schedule(completed::incrementAndGet, i, TimeUnit.MILLISECONDS);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }

            start.countDown();
            assertTrue(done.await(30, TimeUnit.SECONDS));
            workers.shutdownNow();

            Thread.sleep(2000);
            assertEquals(threadCount * tasksPerThread, completed.get(),
                    "All concurrently-scheduled tasks should complete");
        }
    }

    @Nested
    @DisplayName("Cancellation")
    class Cancellation {

        @Test
        @DisplayName("long delay task can be cancelled")
        void cancelLongDelayTask() throws Exception {
            AtomicBoolean ran = new AtomicBoolean(false);
            Future<String> future = scheduler.submit(() -> {
                ran.set(true);
                return "should not happen";
            }, 10, TimeUnit.SECONDS);

            boolean cancelled = future.cancel(false);
            assertTrue(cancelled, "cancel() should return true for pending task");

            Thread.sleep(200);
            assertFalse(ran.get(), "Cancelled task should not have run");
        }
    }
}
