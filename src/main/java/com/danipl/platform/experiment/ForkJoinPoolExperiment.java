package com.danipl.platform.experiment;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.IntStream;

public class ForkJoinPoolExperiment {

    public static class SumTask extends RecursiveTask<Long> {

        private static final int THRESHOLD = 1_000;

        private final long[] numbers;
        private final int start;
        private final int end;

        public SumTask(final long[] numbers, final int start, final int end) {
            this.numbers = numbers;
            this.start = start;
            this.end = end;
        }

        @Override
        protected Long compute() {
            final int length = end - start;

            // Base case: if the segment is small enough, compute the sum directly
            if (length <= THRESHOLD) {
                long sum = 0;
                for (int i = start; i < end; i++) {
                    sum += numbers[i];
                }
                return sum;
            }

            final int mid = start + length / 2;

            SumTask leftTask = new SumTask(numbers, start, mid);
            SumTask rightTask = new SumTask(numbers, mid, end);

            leftTask.fork();                        // run left asynchronously
            final long rightResult = rightTask.compute(); // compute right in current thread
            final long leftResult = leftTask.join();      // wait for left

            return leftResult + rightResult;
        }
    }

    public static void main(final String... args) {
        final long[] data = IntStream.rangeClosed(1, 10000).mapToLong(i -> i).toArray();

        final ForkJoinPool pool = ForkJoinPool.commonPool();
        final long result = pool.invoke(new SumTask(data, 0, data.length));

        System.out.println("Sum = " + result);
    }

}
