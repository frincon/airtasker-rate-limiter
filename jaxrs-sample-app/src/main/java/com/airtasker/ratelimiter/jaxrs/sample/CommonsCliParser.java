package com.airtasker.ratelimiter.jaxrs.sample;

import com.airtasker.ratelimiter.core.Rate;
import org.apache.commons.cli.*;

import java.time.Duration;
import java.util.Optional;

public class CommonsCliParser implements CommandLineParser {

    private static final long DEFAULT_REQUESTS = 100;
    private static final long DEFAULT_TIME_MILLIS = Duration.ofHours(1).toMillis();

    private static final String OPT_PORT = "p";
    private static final String OPT_REQUESTS = "r";
    private static final String OPT_TIME = "t";

    private static final Options options = createOptions();

    @Override
    public ApplicationOptions parseCommandLine(String[] args) throws InvalidCommandLineException {
        final var parser = new DefaultParser();
        try {
            final var commandLine = parser.parse(options, args);
            final var maybePort = readPort(commandLine);
            final var requests = readRequests(commandLine);
            final var time = readTime(commandLine);

            return new ApplicationOptions(maybePort, new Rate(requests, Duration.ofMillis(time)));
        } catch (ParseException e) {
            throw new InvalidCommandLineException(
                    String.format("Error in command line options: %s%n", e.getMessage()), e);
        }
    }

    @Override
    public void printHelp() {
        final var formatter = new HelpFormatter();
        formatter.printHelp(APP_NAME, options);
    }

    private static Optional<Integer> readPort(CommandLine commandLine) throws ParseException {
        if (commandLine.hasOption(OPT_PORT)) {
            final var port = ((Number) commandLine.getParsedOptionValue(OPT_PORT)).intValue();
            if (port < 0 || port > 65535) {
                throw new ParseException("The port should be a valid port");
            }
            return Optional.of(port);
        }
        return Optional.empty();
    }

    private static long readRequests(CommandLine commandLine) throws ParseException {
        if (commandLine.hasOption(OPT_REQUESTS)) {
            final var requests = ((Number) commandLine.getParsedOptionValue(OPT_REQUESTS)).longValue();
            if (requests < 0) {
                throw new ParseException("Number of request should not be negative");
            }
            return requests;
        }
        return DEFAULT_REQUESTS;
    }

    private static long readTime(CommandLine commandLine) throws ParseException {
        if (commandLine.hasOption(OPT_TIME)) {
            final var time = ((Number) commandLine.getParsedOptionValue(OPT_TIME)).longValue();
            if (time <= 0) {
                throw new ParseException("Time in millis should not be natural number");
            }
            return time;
        }
        return DEFAULT_TIME_MILLIS;
    }

    private static Options createOptions() {
        final var options = new Options();
        options.addOption(portOption());
        options.addOption(requestOption());
        options.addOption(timeInMillisOption());
        return options;
    }

    private static Option portOption() {
        return Option.builder("p")
                .longOpt("port")
                .desc("Port to listen")
                .type(Number.class)
                .hasArg()
                .argName("NUMBER")
                .build();
    }

    private static Option requestOption() {
        return Option.builder("r")
                .longOpt("requests")
                .desc(String.format("Number of requests per time frame for the rate limit, Default: %d",
                        DEFAULT_REQUESTS))
                .type(Number.class)
                .hasArg()
                .argName("NUMBER")
                .build();
    }

    private static Option timeInMillisOption() {
        return Option.builder("t")
                .longOpt("time")
                .desc(String.format(
                        "Millisecond for calculating the rate limit based on the number of requests, Default: %d",
                        DEFAULT_TIME_MILLIS))
                .type(Number.class)
                .hasArg()
                .argName("NUMBER")
                .build();
    }

}
