package com.airtasker.ratelimiter.core;

import java.time.Duration;
import java.util.Objects;

public class Rate {

    private final long requests;
    private final Duration window;

    public Rate(long requests, Duration window) {
        if (requests < 0) {
            throw new IllegalArgumentException("Requests should be bigger than 0");
        }
        if(window.isNegative() || window.isZero()) {
            throw new IllegalArgumentException("Time duration should be positive");
        }
        this.requests = requests;
        this.window = window;
    }

    public static Rate of(long requests, Duration window) {
        return new Rate(requests, window);
    }

    public long requests() {
        return requests;
    }

    public Duration window() {
        return window;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rate rate = (Rate) o;
        return requests == rate.requests && Objects.equals(window, rate.window);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requests, window);
    }
}
