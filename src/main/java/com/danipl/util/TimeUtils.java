package com.danipl.util;

public class TimeUtils {

    public static void timed(final Runnable runnable) {
        final long initial = System.currentTimeMillis();
        runnable.run();
        System.out.println((System.currentTimeMillis() - initial) + " ms.");
    }

}
