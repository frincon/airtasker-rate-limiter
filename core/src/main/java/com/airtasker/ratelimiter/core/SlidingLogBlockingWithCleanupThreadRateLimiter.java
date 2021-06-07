package com.airtasker.ratelimiter.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Rate Limiter based on Sliding Log algorithm with blocking operations
 *
 * The accept function runs on constant time, assuming the {@code execute} call of the
 * executor is constant time as well (which usually is the case for most executors), ignoring the time
 * spend blocking
 *
 * This implementation stores in memory a log with all the calls in the provided window of the rate.
 *
 * The cleanup of the request log is scheduled in the provided executor service which makes this implementation
 * not 100% exact, as there is a chance where the cleaning task did not run on time while checking for a new request
 * In that case it is possible that a request is rejected when it was not needed.
 *
 * So caveats:
 *
 * * Accept function is not multi thread, not very suitable for high parallel services
 * * It stores in memory every past request in the given window, which means that can consume a lot of memory
 * * The cleanup task is running in different thread, which means under high load could result in false rejections
 * * The accept() function is O(1) (constant) in time (assuming {@code executor.execute()} call is constant as well
 *   and ignoring the time spending blocking
 *
 * @param <R> The type of requests
 */
public class SlidingLogBlockingWithCleanupThreadRateLimiter<R> implements RateLimiter<R> {

    private static final Logger LOG = LoggerFactory.getLogger(SlidingLogBlockingWithCleanupThreadRateLimiter.class);

    private final Rate rate;
    private final Clock clock;
    private final ScheduledExecutorService executorService;
    private final Deque<Instant> log = new LinkedList<>();
    private final Object headLock = new Object();
    private final Object tailLock = new Object();
    private final Runnable cleanRunnable = this::cleanLog;

    public SlidingLogBlockingWithCleanupThreadRateLimiter(
            Rate rate, Clock clock, ScheduledExecutorService executorService) {
        this.rate = rate;
        this.clock = clock;
        this.executorService = executorService;
    }

    @Override
    public Optional<Duration> accept(R request) {
        synchronized (tailLock) {
            if (log.size() >= rate.requests()) {
                LOG.debug("Rejecting request: {}", request);
                final var head = log.peekFirst();
                if(head == null) {
                    // race condition, should be able now to run
                    return Optional.of(Duration.ZERO);
                } else {
                    return Optional.of(Duration.between(clock.instant(), head.plus(rate.window())));
                }
            }

            log.addLast(clock.instant());
            executorService.execute(() ->
                    executorService.schedule(cleanRunnable, rate.window().toMillis(), TimeUnit.MILLISECONDS));
            LOG.debug("Accepting request: {}", request);
            return Optional.empty();
        }
    }

    @Override
    public boolean isEmpty() {
        return log.isEmpty();
    }

    private void cleanLog() {
        synchronized (headLock) {
            final var limit = clock.instant().minus(rate.window());
            while((!log.isEmpty()) && limit.isAfter(log.getFirst())) {
                log.removeFirst();
            }
        }
    }

}
