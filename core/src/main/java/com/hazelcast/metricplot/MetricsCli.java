package com.hazelcast.metricplot;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static java.lang.String.format;

public class MetricsCli {

    private final OptionParser parser = new OptionParser();

    private final OptionSpec<Long> fromEpochSpec = parser.accepts("from",
            "The epoch time to start from.")
            .withRequiredArg().ofType(Long.class).defaultsTo(0L);

    private final OptionSpec<Long> toEpochSpec = parser.accepts("to",
            "The epoch time to end.")
            .withRequiredArg().ofType(Long.class).defaultsTo(Long.MAX_VALUE);

    private final OptionSpec<String> resultSpec = parser.accepts("result",
            "The epoch time to end.")
            .withRequiredArg().ofType(String.class).defaultsTo("result.html");

    private final OptionSpec<String> filterSpec = parser.accepts("filter",
            "The filter on the metrics to select. E.g. 'foo.val*,bar.*'.")
            .withRequiredArg().ofType(String.class).defaultsTo("*");

//    private final NonOptionArgumentSpec<String> nonOptionArgumentSpec = parser.nonOptions().ofType(String.class);

    private final String[] args;
    private final OptionSet optionSet;

    public MetricsCli(String[] args) {
        this.args = args;
        optionSet = parser.parse(args);
    }

    public long fromEpoch() {
        return optionSet.valueOf(fromEpochSpec);
    }

    public long toEpoch() {
        return optionSet.valueOf(toEpochSpec);
    }

    public MetricsFilter filter(){
        return new MetricsFilter(optionSet.valueOf(filterSpec));
    }

    public File inputFile() throws IOException {
        List nonOptionArguments = optionSet.nonOptionArguments();
        if (nonOptionArguments.size() != 1) {
            throw new IllegalArgumentException("One argument expected");
        }

        File file = new File((String)nonOptionArguments.get(0));
        if (!file.exists()) {
            throw new IOException(format("Input file '%s' doesn't exist", file));
        }

        return file;
    }

    public File outputFile() throws IOException {
        File file = new File(optionSet.valueOf(resultSpec));
        if (!file.exists()) {
            file.createNewFile();
        }
        return file;
    }
}
