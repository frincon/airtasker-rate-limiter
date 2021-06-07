package com.airtasker.ratelimiter.core;

import io.vavr.Tuple;
import io.vavr.Tuple2;
import org.apache.commons.lang3.RandomUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class SlidingLogBlockingWithCleanupThreadRateLimiterTest {

    private static final ScheduledExecutorService EXECUTOR_SERVICE = mock(ScheduledExecutorService.class);
    private static final Clock CLOCK = mock(Clock.class);
    private static final Instant DEFAULT_INSTANT = Instant.EPOCH;

    @BeforeEach
    public void prepareMocks() {
        reset(EXECUTOR_SERVICE, CLOCK);
        when(CLOCK.instant()).thenReturn(DEFAULT_INSTANT);
        doAnswer(invocation -> {
            invocation.getArgument(0, Runnable.class).run();
            return null;
        }).when(EXECUTOR_SERVICE).execute(any());
    }

    @Test
    public void shouldRejectWhenRequestsRateIs0() {
        final var rate = Rate.of(0, randomDuration());
        testSequence(rate, List.of(Tuple.of(DEFAULT_INSTANT, Optional.of(Duration.ZERO))));
    }

    @Test
    public void shouldAcceptWhenNumberOfRequestIsNotReached() {
        final var rate = Rate.of(RandomUtils.nextLong(1, 100),
                Duration.ofMillis(RandomUtils.nextLong(1, Long.MAX_VALUE)));

        final var expectations = LongStream
                .range(0, rate.requests())
                .mapToObj(ignored -> Tuple.of(DEFAULT_INSTANT, Optional.<Duration>empty()))
                .collect(Collectors.toList());

        testSequence(rate, expectations);
    }

    @Test
    public void shouldRejectWhenRateIsReached() {

        final var rate = Rate.of(RandomUtils.nextLong(10, 20),
                Duration.ofMillis(RandomUtils.nextLong(1, 1000)));

        final var initialTime = Instant.now();
        final var maxDuration = rate.window();
        final var endTime = initialTime.plus(maxDuration);

        // Make N request = requests in the rate
        final var expectations = Stream.generate(() -> RandomUtils.nextLong(0, maxDuration.toMillis()))
                .map(millis -> initialTime.plus(millis, ChronoUnit.MILLIS))
                .limit(rate.requests())
                .sorted()
                .map(instant -> Tuple.of(instant, Optional.<Duration>empty()))
                .collect(Collectors.toList());


        final var expectedWaitingTime = Duration.between(initialTime, expectations.get(0)._1);

        expectations.add(Tuple.of(endTime, Optional.of(expectedWaitingTime)));

        testSequence(rate, expectations);
    }

    @Test
    public void shouldAcceptWhenSomeGoesOutOfWindow() {

        final var rate = Rate.of(RandomUtils.nextLong(10, 20),
                Duration.ofMillis(RandomUtils.nextLong(1, 1000)));

        final var initialTime = Instant.now();
        final var maxDuration = rate.window();
        final var endTime = initialTime.plus(maxDuration);

        // Make N request = requests in the rate
        final var expectations = new ArrayList<Tuple2<Instant, Optional<Duration>>>();
        expectations.add(Tuple.of(initialTime, Optional.empty()));

        expectations.addAll(Stream.generate(() -> RandomUtils.nextLong(1, maxDuration.toMillis()))
                .map(millis -> initialTime.plus(millis, ChronoUnit.MILLIS))
                .limit(rate.requests() - 1)
                .map(instant -> Tuple.of(instant, Optional.<Duration>empty()))
                .collect(Collectors.toList()));

        expectations.add(Tuple.of(endTime, Optional.of(Duration.ZERO)));
        expectations.add(Tuple.of(endTime.plus(Duration.ofMillis(1)), Optional.empty()));

        testSequence(rate, expectations);
    }

    @Test
    public void shouldBeEmptyWithNoRequests() {
        final var unit = new SlidingLogBlockingWithCleanupThreadRateLimiter<String>(
                Rate.of(1, Duration.ofMillis(1)), CLOCK, EXECUTOR_SERVICE);

        assertThat(unit.isEmpty()).isTrue();
    }

    @Test
    public void shouldNotBeEmptyWithAtLeastOneRequest() {
        final var unit = new SlidingLogBlockingWithCleanupThreadRateLimiter<String>(
                Rate.of(1, Duration.ofMillis(1)), CLOCK, EXECUTOR_SERVICE);

        unit.accept("request");
        assertThat(unit.isEmpty()).isFalse();

    }

    @Test
    public void shouldBeEmptyAfterCleanup() {
        final var unit = new SlidingLogBlockingWithCleanupThreadRateLimiter<String>(
                Rate.of(1, Duration.ofMillis(1)), CLOCK, EXECUTOR_SERVICE);

        unit.accept("request");

        final var cleanupRunnables = collectScheduledRunnables(DEFAULT_INSTANT);
        when(CLOCK.instant()).thenReturn(DEFAULT_INSTANT.plus(Duration.ofMillis(2)));
        cleanupRunnables.forEach(t -> t._2.run());

        assertThat(unit.isEmpty()).isTrue();

    }


    private void testSequence(Rate rate, List<Tuple2<Instant, Optional<Duration>>> expectations) {

        final var unit = new SlidingLogBlockingWithCleanupThreadRateLimiter<String>(rate, CLOCK, EXECUTOR_SERVICE);

        final var runnables = new LinkedList<Tuple2<Instant, Runnable>>();

        expectations
                .stream()
                .sorted(Comparator.comparing(Tuple2::_1))
                .forEach(expectation -> {
                    final var instant = expectation._1;
                    final var expectedResult = expectation._2;

                    when(CLOCK.instant()).thenReturn(instant);
                    // Run all the pending runnables
                    runnables.stream()
                            .filter(tuple -> tuple._1.isBefore(instant))
                            .map(Tuple2::_2)
                            .forEach(Runnable::run);

                    final var result = unit.accept("request");
                    assertThat(result).isEqualTo(expectedResult);

                    runnables.addAll(collectScheduledRunnables(instant));
                });
    }

    private Collection<Tuple2<Instant, Runnable>> collectScheduledRunnables(Instant refInstant) {
        final var result = mockingDetails(EXECUTOR_SERVICE)
                .getInvocations()
                .stream()
                .filter(invocation -> invocation.getMethod().getName().equals("schedule"))
                .map(invocation -> {
                    final var runnable = invocation.getArgument(0, Runnable.class);
                    final var ammount = invocation.getArgument(1, Long.class);
                    final var timeUnit = invocation.getArgument(2, TimeUnit.class);
                    final var runningInstant = refInstant.plus(ammount, timeUnit.toChronoUnit());
                    return Tuple.of(runningInstant, runnable);
                })
                .collect(Collectors.toList());
        reset(EXECUTOR_SERVICE);
        return result;
    }

    private Duration randomDuration() {
        return Duration.ofMillis(RandomUtils.nextLong(1, Long.MAX_VALUE));
    }

}
