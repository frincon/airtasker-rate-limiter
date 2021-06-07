package com.airtasker.ratelimiter.core.internal;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultCleanupThreadFactory implements ThreadFactory {

    public static final DefaultCleanupThreadFactory INSTANCE = new DefaultCleanupThreadFactory();

    private static final String THREAD_TEMPLATE = "airtasker-ratelimiter-cleanup-thread-%d";

    private final ThreadFactory delegate = Executors.defaultThreadFactory();
    private final AtomicLong counter = new AtomicLong(0);

    private DefaultCleanupThreadFactory() {}

    @Override
    public Thread newThread(Runnable r) {

        final var current = counter.incrementAndGet();
        final var threadName = String.format(THREAD_TEMPLATE, current);

        final var thread = delegate.newThread(r);
        thread.setName(threadName);
        thread.setDaemon(true);
        return thread;
    }
}
