package com.airtasker.ratelimiter.jaxrs.sample.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/sample")
public class SampleResource {

    @GET
    @Produces({MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON})
    public Response get() {
        return Response.status(Response.Status.OK)
                .entity("This is an example")
                .build();
    }

}
