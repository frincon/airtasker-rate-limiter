package com.airtasker.ratelimiter.core;

import org.apache.commons.lang3.RandomUtils;
import org.awaitility.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;

public class KeyBasedRateLimiterTest {

    private static final ScheduledExecutorService EXECUTOR_SERVICE = mock(ScheduledExecutorService.class);

    @BeforeEach
    public void resetMocks() {
        reset(EXECUTOR_SERVICE);
    }

    @Test
    public void rateLimiterShouldOnlyBeCreatedOnceAndReused() {

        final var created = new AtomicBoolean(false);
        final var requestedKey = RandomUtils.nextInt();

        final var unit = new KeyBasedRateLimiter<Integer, Integer>(
                Function.identity(),
                key -> {
                    assertThat(created).isFalse();
                    assertThat(key).isEqualTo(requestedKey);
                    created.set(true);
                    return mock(RateLimiter.class);
                },
                EXECUTOR_SERVICE
        );

        unit.accept(requestedKey);
        unit.accept(requestedKey);
    }

    @Test
    public void shouldReturnResultFromDelegated() {

        final var requestedKey = RandomUtils.nextInt();
        final RateLimiter<Integer> delegated = mock(RateLimiter.class);

        final var unit = new KeyBasedRateLimiter<Integer, Integer>(
                Function.identity(),
                ignored -> delegated,
                EXECUTOR_SERVICE
        );

        when(delegated.accept(requestedKey)).thenReturn(Optional.empty());
        assertThat(unit.accept(requestedKey)).isEmpty();

        final var returnValue = Optional.of(java.time.Duration.ofMillis(1000));

        when(delegated.accept(requestedKey)).thenReturn(returnValue);
        assertThat(unit.accept(requestedKey)).isSameAs(returnValue);
    }

    @Test
    public void cleanupTaskShouldCleanReferences() {

        final var keys = 1000;

        // Weak references does not work with mocks as most probably
        // the library maintains some reference from static variables
        final var rateLimiters = Stream.generate(() -> new RateLimiter<String>() {

            @Override
            public Optional<java.time.Duration> accept(String request) {
                return Optional.of(java.time.Duration.ofSeconds(1));
            }

            @Override
            public boolean isEmpty() {
                return true;
            }})
                .limit(keys)
                .collect(Collectors.toList());

        final var unit = new KeyBasedRateLimiter<String, String>(
                Function.identity(),
                key -> rateLimiters.get(Integer.parseInt(key)),
                EXECUTOR_SERVICE
        );

        IntStream.range(0, keys).mapToObj(Integer::toString).forEach(unit::accept);

        final var weakReferences = rateLimiters
                .stream().map(WeakReference::new).collect(Collectors.toList());
        rateLimiters.clear();

        final var executorInvocations = mockingDetails(EXECUTOR_SERVICE).getInvocations();
        final var cleanupTask = executorInvocations.iterator().next().getArgument(0, Runnable.class);

        assertThat(weakReferences).allSatisfy(ref -> assertThat(ref.get()).isNotNull());

        cleanupTask.run();
        assertThat(unit.isEmpty()).isTrue();

        await().atMost(Duration.TEN_SECONDS)
                .untilAsserted(() -> {
                    System.gc();
                    assertThat(weakReferences).allSatisfy(ref -> assertThat(ref.get()).isNull());
                });
    }


}
