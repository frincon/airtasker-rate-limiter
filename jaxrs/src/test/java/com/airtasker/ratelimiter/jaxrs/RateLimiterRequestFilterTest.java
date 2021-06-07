package com.airtasker.ratelimiter.jaxrs;

import com.airtasker.ratelimiter.core.RateLimiter;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.InstanceOfAssertFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Response;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
public class RateLimiterRequestFilterTest {

    private static final Logger LOG = LoggerFactory.getLogger(RateLimiterRequestFilterTest.class);

    @Test
    public void shouldDoNothingIfRateLimiterNotReached() {
        final var rateLimiter = (RateLimiter<ContainerRequestContext>) mock(RateLimiter.class);
        when(rateLimiter.accept(any())).thenReturn(Optional.empty());

        final var context = mock(ContainerRequestContext.class);

        final var unit = new RateLimiterRequestFilter(rateLimiter);
        unit.filter(context);

        verify(rateLimiter, times(1)).accept(same(context));
        verifyNoMoreInteractions(context);
    }

    @Test
    public void shouldAbortWhenReteLimiterReached() {
        final var rateLimiter = (RateLimiter<ContainerRequestContext>) mock(RateLimiter.class);
        when(rateLimiter.accept(any())).thenReturn(Optional.of(Duration.ofSeconds(250)));

        final var context = mock(ContainerRequestContext.class);

        final var unit = new RateLimiterRequestFilter(rateLimiter);
        unit.filter(context);

        verify(rateLimiter, times(1)).accept(same(context));

        final var responseCaptor = ArgumentCaptor.forClass(Response.class);
        verify(context, times(1)).abortWith(responseCaptor.capture());

        final var response = responseCaptor.getValue();

        assertThat(response.getStatus()).isEqualTo(429);
        assertThat(response.getEntity()).asInstanceOf(InstanceOfAssertFactories.STRING)
                .isEqualTo("Rate limit exceeded. Try again in 250 seconds");
    }

}
