package com.indeed.proctor.builder;

import com.indeed.proctor.common.IncompatibleTestMatrixException;
import com.indeed.proctor.store.LocalDirectoryStore;
import com.indeed.proctor.store.StoreException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.log4j.Logger;

import java.io.*;

public class LocalProctorBuilder extends ProctorBuilder {

    private static final Logger LOGGER = Logger.getLogger(LocalProctorBuilder.class);

    public LocalProctorBuilder(File inputDir, Writer outputSink) {
        super(new LocalDirectoryStore(inputDir), outputSink);
    }

    public LocalProctorBuilder(File inputDir, Writer outputSink, String author) {
        super(new LocalDirectoryStore(inputDir), outputSink, author);
    }

    public LocalProctorBuilder(File inputDir, Writer outputSink, String author, long version) {
        super(new LocalDirectoryStore(inputDir), outputSink, author, version);
    }

    private static class LocalProctorBuilderArgs extends ProctorBuilderArgs {
        private String inputdir;

        private LocalProctorBuilderArgs() {
            options.addOption(OptionBuilder.hasArg(true)
                                  .isRequired()
                                  .withLongOpt("input")
                                  .withArgName("input directory")
                                  .withDescription("The directory to read from.")
                                  .create("i"));
        }

        @Override
        protected void extract(CommandLine results)  {
            super.extract(results);
            this.inputdir = results.getOptionValue("input");
        }

        public String getInputdir() {
            return inputdir;
        }
    }

    public static void main(final String[] args) throws IOException, StoreException, IncompatibleTestMatrixException {
        final LocalProctorBuilderArgs arguments = new LocalProctorBuilderArgs();
        arguments.parse(args);

        try {
            new LocalProctorBuilder(
                    new File(arguments.getInputdir()),
                    "-".equals(arguments.getOutputdir()) ?
                        new PrintWriter(System.out) :
                        new FileWriter(new File(arguments.getOutputdir(), arguments.getFilename())),
                    arguments.getAuthor(),
                    arguments.getVersion()).execute();
        } catch (Exception e) {
            LOGGER.error("Failed to generates proctor artifact from " + arguments.getInputdir(), e);
            System.exit(1);
        }
    }
}
