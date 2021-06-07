package com.airtasker.ratelimiter.jaxrs.sample;

public interface CommandLineParser {

    String APP_NAME = "jaxrs-sample-app";

    ApplicationOptions parseCommandLine(String[] args) throws InvalidCommandLineException;

    void printHelp();

}
