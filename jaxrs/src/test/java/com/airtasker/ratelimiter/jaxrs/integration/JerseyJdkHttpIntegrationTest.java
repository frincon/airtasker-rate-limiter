package com.airtasker.ratelimiter.jaxrs.integration;


import com.airtasker.ratelimiter.jaxrs.integration.resources.SampleResource;
import com.sun.net.httpserver.HttpServer;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server .ResourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class JerseyJdkHttpIntegrationTest {

    private static final URI SERVER_URI = UriBuilder.fromUri("http://localhost/").port(0).build();
    private static final ResourceConfig SAMPLE_RESOURCE_CONFIG = new ResourceConfig().register(SampleResource.class);
    private static final Executor CLIENT_EXECUTOR = Executors.newFixedThreadPool(2);
    private static final String RATE_LIMITED_PATH = SampleResource.SAMPLE_PATH + SampleResource.RATE_LIMITED_SUBPATH;
    private static final String NOT_RATE_LIMITED_PATH = SampleResource.SAMPLE_PATH + SampleResource.NOT_RATE_LIMITED_SUBPATH;

    private HttpServer httpServer;
    private HttpClient httpClient;

    @BeforeEach
    public void createHttpClientAndServer() {
        this.httpServer = JdkHttpServerFactory.createHttpServer(SERVER_URI, SAMPLE_RESOURCE_CONFIG);
    }

    @BeforeEach
    public void createHttpClient() {
        this.httpClient = HttpClient.newBuilder().executor(CLIENT_EXECUTOR).build();
    }

    @Test
    public void testRateLimitEndpoint() {
        final var request = buildRequest(RATE_LIMITED_PATH);

        final var initTime = System.currentTimeMillis();

        // first nth request should pass
        assertThat(sendRequest(request, SampleResource.RATE_LIMIT_REQUEST))
                .allSatisfy(response -> assertThat(response.statusCode()).isEqualTo(200));

        //Rest in the same time window should be 429
        var rejected = 0L;
        while(System.currentTimeMillis() - initTime < SampleResource.RATE_LIMIT_TIME_MILLIS) {
            assertThat(unsafeSend(request, BodyHandlers.ofString()).statusCode()).isEqualTo(429);
            rejected++;
        }
        assertThat(rejected).isGreaterThan(0L);

        // Now, at least after the time window elapsed
        await().atMost(SampleResource.RATE_LIMIT_TIME_MILLIS, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> assertThat(
                        unsafeSend(request, BodyHandlers.ofString()).statusCode()).isEqualTo(200));

    }

    @Test
    public void testRateLimitEndpointDoesNotBlockOtherEndpoints() {
        final var rateLimitedRequest = buildRequest(RATE_LIMITED_PATH);
        final var notRateLimitedRequest = buildRequest(NOT_RATE_LIMITED_PATH);

        assertThat(sendRequest(rateLimitedRequest, SampleResource.RATE_LIMIT_REQUEST))
                .allSatisfy(response -> assertThat(response.statusCode()).isEqualTo(200));
        assertThat(unsafeSend(rateLimitedRequest, BodyHandlers.ofString()).statusCode()).isEqualTo(429);

        assertThat(sendRequest(notRateLimitedRequest, SampleResource.RATE_LIMIT_REQUEST + 1))
            .allSatisfy(response -> assertThat(response.statusCode()).isEqualTo(200));
    }

    private HttpRequest buildRequest(String path) {
        return HttpRequest.newBuilder()
                .uri(UriBuilder.fromUri(SERVER_URI).port(httpServer.getAddress().getPort()).path(path).build())
                .GET()
                .build();
    }

    private List<HttpResponse<String>> sendRequest(HttpRequest request, long times) {
        return Stream
                .generate(() -> unsafeSend(request, BodyHandlers.ofString()))
                .limit(times)
                .collect(Collectors.toList());
    }

    private <T> HttpResponse<T> unsafeSend(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
        try {
            return httpClient.send(request, responseBodyHandler);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }


}
