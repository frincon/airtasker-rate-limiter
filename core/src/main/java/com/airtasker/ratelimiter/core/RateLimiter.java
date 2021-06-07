package com.airtasker.ratelimiter.core;

import java.time.Duration;
import java.util.Optional;

/**
 * Interface that all RateLimiters should implement
 *
 * @param <R> The type of request RateLimiter limits
 */
public interface RateLimiter<R> {

    /**
     * Function that needs to be called for each request
     *
     * @param request The request to check whether limit or not
     * @return Empty if the request is accepted, otherwise a optional with the duration
     * that at least needs to be elapsed until a request can be accepted
     */
    Optional<Duration> accept(R request);

    /**
     * Indicates the rate limiter does not have any data and can be recycled
     * @return true when can be recycled
     */
    boolean isEmpty();
}
