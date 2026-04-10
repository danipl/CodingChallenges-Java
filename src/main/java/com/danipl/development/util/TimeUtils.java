package com.danipl.development.util;

public class TimeUtils {

    public static void timed(final Runnable runnable) {
        timed(runnable, ">");
    }

    public static void timed(final Runnable runnable, final String reason) {
        final long initial = System.currentTimeMillis();
        runnable.run();
        System.out.println(reason + " " + (System.currentTimeMillis() - initial) + " ms.");
    }

}
