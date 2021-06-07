package com.airtasker.ratelimiter.jaxrs;

import com.airtasker.ratelimiter.core.RateLimiter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import java.time.Duration;

public class RateLimiterRequestFilter implements ContainerRequestFilter {

    private static final String RATE_MESSAGE_TEMPLATE = "Rate limit exceeded. Try again in %d seconds";

    private final RateLimiter<ContainerRequestContext> rateLimiter;

    public RateLimiterRequestFilter(RateLimiter<ContainerRequestContext> rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    public void filter(ContainerRequestContext requestContext) {
        final var maybeNextChance = rateLimiter.accept(requestContext);
        maybeNextChance.ifPresent(timeToWait -> requestContext.abortWith(rateLimitReachedResponse(timeToWait)));
    }

    private Response rateLimitReachedResponse(Duration timeToWait) {
        final var durationInSeconds = Math.max(1, timeToWait.toSeconds());
        return Response.status(Response.Status.TOO_MANY_REQUESTS)
                .entity(String.format(RATE_MESSAGE_TEMPLATE, durationInSeconds))
                .build();
    }
}
