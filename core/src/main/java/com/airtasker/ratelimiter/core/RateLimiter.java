package com.airtasker.ratelimiter.core;

import java.time.Duration;
import java.util.Optional;

public interface RateLimiter<R> {

    Optional<Duration> accept(R request);

    boolean isEmpty();
}
