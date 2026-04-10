package com.danipl.platform.challenge06;

import java.util.List;

public record RetryResult<T>(T value, int attemptsMade, long totalWaitMs, List<Long> delays) {
}
