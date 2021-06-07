package com.airtasker.ratelimiter.jaxrs.sample;

import com.airtasker.ratelimiter.core.*;
import com.airtasker.ratelimiter.jaxrs.RateLimiterRequestFilter;
import com.airtasker.ratelimiter.jaxrs.sample.filters.DummyAuthenticationFilter;
import com.airtasker.ratelimiter.jaxrs.sample.resources.SampleResource;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.UriBuilder;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class Application {

    private static final Logger LOG = LoggerFactory.getLogger(Application.class);

    private static final String SERVER_URI = "http://localhost/";
    private final Optional<Integer> port;
    private final Rate rate;
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();

    public Application(ApplicationOptions options) {
        this.port = options.getPort();
        this.rate = options.getRate();
    }

    public void run() {
        final var uriBuilder = UriBuilder.fromUri(SERVER_URI);
        port.ifPresent(uriBuilder::port);

        final var uri = uriBuilder.build();
        LOG.info("Starting http server on uri {}", uri);

        final var httpServer = JdkHttpServerFactory.createHttpServer(
                uri, resourceConfiguration());
    }

    private ResourceConfig resourceConfiguration() {
        return new ResourceConfig()
                .register(SampleResource.class)
                .register(DummyAuthenticationFilter.class, Priorities.AUTHENTICATION)
                .register(buildRateLimiter(), Priorities.AUTHORIZATION);
    }

    private RateLimiterRequestFilter buildRateLimiter() {
        final var rateLimiter = AirtaskerRateLimiters.defaultKeyBasedRateLimiter(
                rate, DummyAuthenticationFilter::extractApiKey);

        return new RateLimiterRequestFilter(rateLimiter);
    }

}
