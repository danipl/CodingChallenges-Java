package com.danipl.platform.challenge03;

public class CircularDependencyException extends RuntimeException {

    public CircularDependencyException(String message) {
        super(message);
    }

    public CircularDependencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
