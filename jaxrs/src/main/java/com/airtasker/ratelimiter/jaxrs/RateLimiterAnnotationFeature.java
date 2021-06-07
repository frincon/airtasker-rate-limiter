package com.airtasker.ratelimiter.jaxrs;

import com.airtasker.ratelimiter.core.AirtaskerRateLimiters;
import com.airtasker.ratelimiter.core.Rate;
import com.airtasker.ratelimiter.core.SlidingLogBlockingWithCleanupThreadRateLimiter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public class RateLimiterAnnotationFeature implements DynamicFeature {

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        final var maybeRateLimited = Optional.ofNullable(
                resourceInfo.getResourceMethod().getAnnotation(RateLimited.class));

        maybeRateLimited.ifPresent(rateLimited ->
            context.register(createRateLimiter(rateLimited))
        );
    }

    private RateLimiterRequestFilter createRateLimiter(RateLimited rateLimited) {
        return new RateLimiterRequestFilter(
                AirtaskerRateLimiters.slidingLogBlockingWithCleanup(
                        Rate.of(rateLimited.requests(),  Duration.ofMillis(rateLimited.timeMillis()))));
    }
}
