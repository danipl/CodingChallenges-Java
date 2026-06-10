package com.danipl.platform.resilience.retry;

import java.util.List;

public record RetryResult<T>(T value, int attemptsMade, long totalWaitMs, List<Long> delays) {
}
