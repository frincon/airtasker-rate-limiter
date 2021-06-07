package com.airtasker.ratelimiter.jaxrs.sample;

public class InvalidCommandLineException extends Exception {
    public InvalidCommandLineException(String message) {
        super(message);
    }

    public InvalidCommandLineException(String message, Throwable cause) {
        super(message, cause);
    }
}
