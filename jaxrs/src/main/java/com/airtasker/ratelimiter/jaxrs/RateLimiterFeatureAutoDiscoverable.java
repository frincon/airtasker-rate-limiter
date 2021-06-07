package com.airtasker.ratelimiter.jaxrs;

import org.glassfish.jersey.internal.spi.AutoDiscoverable;

import javax.annotation.Priority;
import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.FeatureContext;

@ConstrainedTo(RuntimeType.SERVER)
@Priority(AutoDiscoverable.DEFAULT_PRIORITY)
public class RateLimiterFeatureAutoDiscoverable implements AutoDiscoverable {
    @Override
    public void configure(FeatureContext context) {
        if (!context.getConfiguration().isRegistered(RateLimiterAnnotationFeature.class)) {
            context.register(RateLimiterAnnotationFeature.class);
        }
    }
}
