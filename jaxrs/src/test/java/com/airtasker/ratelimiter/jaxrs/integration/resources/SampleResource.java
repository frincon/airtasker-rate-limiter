package com.airtasker.ratelimiter.jaxrs.integration.resources;

import com.airtasker.ratelimiter.jaxrs.RateLimited;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

@Path(SampleResource.SAMPLE_PATH)
public class SampleResource {

    public static final long RATE_LIMIT_REQUEST = 10L;
    public static final long RATE_LIMIT_TIME_MILLIS = 1000L;

    public static final String SAMPLE_PATH = "/sample";
    public static final String RATE_LIMITED_SUBPATH = "/rate-limited";
    public static final String NOT_RATE_LIMITED_SUBPATH = "/not-rate-limited";


    @GET
    @Path(RATE_LIMITED_SUBPATH)
    @RateLimited(requests = RATE_LIMIT_REQUEST, timeMillis = RATE_LIMIT_TIME_MILLIS)
    public Response rateLimited() {
        return Response.status(Response.Status.OK).build();
    }

    @GET
    @Path(NOT_RATE_LIMITED_SUBPATH)
    public Response notRateLimited() {
        return Response.status(Response.Status.OK).build();
    }

}
