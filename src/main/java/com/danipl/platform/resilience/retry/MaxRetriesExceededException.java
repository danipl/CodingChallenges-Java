package com.danipl.platform.resilience.retry;

public class MaxRetriesExceededException extends RuntimeException {

    private final int attemptsMade;

    public MaxRetriesExceededException(String message, Exception cause, int attemptsMade) {
        super(message, cause);
        this.attemptsMade = attemptsMade;
    }

    public int getAttemptsMade() {
        return attemptsMade;
    }
}
