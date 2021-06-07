package com.airtasker.ratelimiter.core;

import com.airtasker.ratelimiter.core.internal.DefaultCleanupThreadFactory;

import java.time.Clock;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

/**
 * Builders methods for creation of different rate limiters implementation
 * based on default values.
 */
public final class AirtaskerRateLimiters {

    private AirtaskerRateLimiters() { /* Not to be instantiated */ }

    /**
     * Creates a new instance of {@link SlidingLogBlockingWithCleanupThreadRateLimiter} with the provided rate
     *
     * This methods spawns one thread for the cleanup process
     * @param rate The rate limit
     * @param <T> Type of request to limit
     * @return A rate limiter based on {@link SlidingLogBlockingWithCleanupThreadRateLimiter}
     */
    public static <T> RateLimiter<T> slidingLogBlockingWithCleanup(Rate rate) {
        return new SlidingLogBlockingWithCleanupThreadRateLimiter<>(
                rate, Clock.systemDefaultZone(), createDefaultExecutor());
    }

    /**
     * Creates a new instance of {@link SlidingLogBlockingWithCleanupThreadRateLimiter} with the provided rate
     * and executor service
     *
     * The returning rate limiting will schedule cleanup tasks in the executor service. Note that if the task
     * does not run, then the rate limiter could not make the cleanup possible causing false rejections
     *
     * @param rate The rate limit
     * @param <T> Type of request to limit
     * @return A rate limiter based on {@link SlidingLogBlockingWithCleanupThreadRateLimiter}
     */
    public static <T> RateLimiter<T> slidingLogBlockingWithCleanup(Rate rate, ScheduledExecutorService executorService) {
        return new SlidingLogBlockingWithCleanupThreadRateLimiter<>(
                rate, Clock.systemDefaultZone(), executorService);
    }

    /**
     * Creates a new instance of {@link KeyBasedRateLimiter} with the provided rate and key provider
     *
     * This methods spawns one thread for the cleanup process for all the rate limiters.
     *
     * The returned rate limiter will maintain different limits based on the keys, each of the keys
     * will have associated a default rate limiter implementation based on
     * {@link SlidingLogBlockingWithCleanupThreadRateLimiter}, all of them with the same rate
     *
     * @param rate The rate limit
     * @param <T> Type of request to limit
     * @return A rate limiter based on {@link SlidingLogBlockingWithCleanupThreadRateLimiter}
     */
    public static <T, K> RateLimiter<T> defaultKeyBasedRateLimiter(Rate rate, Function<T, K> keyProvider) {
        final var executorService = createDefaultExecutor();
        return new KeyBasedRateLimiter<>(keyProvider, ignored -> slidingLogBlockingWithCleanup(rate, executorService),
                executorService);
    }

    private static ScheduledExecutorService createDefaultExecutor() {
        return Executors.newSingleThreadScheduledExecutor(DefaultCleanupThreadFactory.INSTANCE);
    }


}
