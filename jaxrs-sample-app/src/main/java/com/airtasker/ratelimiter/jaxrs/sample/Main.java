package com.airtasker.ratelimiter.jaxrs.sample;

import org.slf4j.bridge.SLF4JBridgeHandler;

public class Main {

    private static final int EXIT_ERROR = 1;

    public static void main(String[] args) {
        bootstrapLog();
        final var app = createApp(args);
        try {
            app.run();
        } catch (Exception ex) {
            System.err.printf("Exception caught, exiting: %s%n", ex.getMessage());
            ex.printStackTrace(System.err);
            System.exit(EXIT_ERROR);
        }
    }

    private static void bootstrapLog() {
        SLF4JBridgeHandler.removeHandlersForRootLogger();  // (since SLF4J 1.6.5)
        SLF4JBridgeHandler.install();
    }

    private static Application createApp(String[] args) {
        final var parser = new CommonsCliParser();
        try {
            final var options = new CommonsCliParser().parseCommandLine(args);
            return new Application(options);
        } catch (InvalidCommandLineException ex) {
            System.err.println(ex.getMessage());
            parser.printHelp();
            System.exit(EXIT_ERROR);
            throw new RuntimeException("This never happen");
        }
    }
}
