package com.airtasker.ratelimiter.core.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultCleanupThreadFactoryTest {

    @Test
    public void newThreadShouldBeDaemon() {
        final var thread = DefaultCleanupThreadFactory.INSTANCE.newThread(() -> {});
        assertThat(thread.isDaemon()).isTrue();
    }

    @Test
    public void newThreadShouldHaveProperName() {
        final var thread1 = DefaultCleanupThreadFactory.INSTANCE.newThread(() -> {});
        final var thread2 = DefaultCleanupThreadFactory.INSTANCE.newThread(() -> {});
        assertThat(thread1.getName()).isEqualTo("airtasker-ratelimiter-cleanup-thread-1");
        assertThat(thread2.getName()).isEqualTo("airtasker-ratelimiter-cleanup-thread-2");
    }

}
