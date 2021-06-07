package com.airtasker.ratelimiter.jaxrs.sample.filters;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import java.util.Optional;

public class DummyAuthenticationFilter implements ContainerRequestFilter {

    public static final String API_KEY_PROPERTY = DummyAuthenticationFilter.class.getName() + ".API_KEY";
    private static final String API_KEY_HEADER = "Api-Key";

    @Override
    public void filter(ContainerRequestContext requestContext) {
        Optional.ofNullable(requestContext.getHeaders().get(API_KEY_HEADER))
                .filter(l -> l.size() == 1)
                .map(l -> l.get(0))
                .ifPresentOrElse(
                       apiKey -> requestContext.setProperty(API_KEY_PROPERTY, apiKey),
                        () -> requestContext.abortWith(unauthorizedResponse())
                );
    }

    public static String extractApiKey(ContainerRequestContext context) {
        return (String) context.getProperty(API_KEY_PROPERTY);
    }

    private Response unauthorizedResponse() {
        return Response.status(Response.Status.FORBIDDEN)
                .build();
    }

}
