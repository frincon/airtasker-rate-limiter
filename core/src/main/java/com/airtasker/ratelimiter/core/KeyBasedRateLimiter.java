package com.airtasker.ratelimiter.core;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Rate limiter which maintains a map of delegates rate limiters for each key.
 *
 * In order to not make the memory full, this implementation cleanup rate limiters that are empty
 * using a background thread scheduled by every 10 milliseconds (for now this value is fixed and cannot be modified)
 *
 * @param <R> The request type for this rate limiter
 * @param <K> The type of the keys
 */
public class KeyBasedRateLimiter<R, K> implements RateLimiter<R> {

    private static final long CLEANUP_TASK_DELAY_MILLIS = 10L;

    private final Function<R, K> keyProvider;
    private final Function<K, RateLimiter<R>> rateLimiterProvider;
    private final ConcurrentMap<K, RateLimiter<R>> rateLimiterMap = new ConcurrentHashMap<>();

    /**
     * Build a rate limiter using the provided parameters
     *
     * The function {@code rateLimiterProvider} is going to be called in case there is no rate limiter associated
     * to the key. After that, the rate limiter is used until it is cleanup.
     *
     * The rate limiters are only cleaned up in case the method {@code isEmpty} is called
     *
     * @param keyProvider A function to extract the key from the request
     * @param rateLimiterProvider A function to create specific rate limiter for a key
     * @param executor The executor where the cleanup task is going to be scheduled
     */
    public KeyBasedRateLimiter(Function<R, K> keyProvider, Function<K, RateLimiter<R>> rateLimiterProvider,
                               ScheduledExecutorService executor) {
        this.keyProvider = keyProvider;
        this.rateLimiterProvider = rateLimiterProvider;

        executor.scheduleWithFixedDelay(this::cleanup, CLEANUP_TASK_DELAY_MILLIS, CLEANUP_TASK_DELAY_MILLIS,
                TimeUnit.MILLISECONDS);
    }

    @Override
    public Optional<Duration> accept(R request) {
        final var key = keyProvider.apply(request);
        final var rateLimiter = rateLimiterMap.computeIfAbsent(key, rateLimiterProvider);
        return rateLimiter.accept(request);
    }

    @Override
    public boolean isEmpty() {
        return rateLimiterMap.isEmpty();
    }

    private void cleanup() {
        final var keys = rateLimiterMap.keySet();
        keys.forEach(key -> rateLimiterMap.compute(key, (ignored, rateLimiter) -> {
            if(rateLimiter == null || rateLimiter.isEmpty()) {
                return null;
            } else {
                return rateLimiter;
            }
        }));
    }

}
