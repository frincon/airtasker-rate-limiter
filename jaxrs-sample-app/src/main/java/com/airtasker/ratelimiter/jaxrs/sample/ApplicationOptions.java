package com.airtasker.ratelimiter.jaxrs.sample;

import com.airtasker.ratelimiter.core.Rate;

import java.util.Optional;

public class ApplicationOptions {

    private final Optional<Integer> port;
    private final Rate rate;


    public ApplicationOptions(Optional<Integer> port, Rate rate) {
        this.port = port;
        this.rate = rate;
    }

    public Optional<Integer> getPort() {
        return port;
    }

    public Rate getRate() {
        return rate;
    }

}
