package com.danipl.platform.challenge05;

public record LogEntry(long timestamp, LogLevel level, String serviceName, long responseTimeMs) {
    public LogEntry {
        if (responseTimeMs < 0) {
            throw new IllegalArgumentException("responseTimeMs must be >= 0");
        }
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName must not be null");
        }
    }
}
