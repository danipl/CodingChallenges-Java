package com.danipl.platform.challenge08;

/**
 * Thrown when a resource cannot be acquired from the pool
 * because all resources are in use and the acquire timeout expired.
 */
public final class PoolExhaustedException extends RuntimeException {

    public PoolExhaustedException(String message) {
        super(message);
    }
}
