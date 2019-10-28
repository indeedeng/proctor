package com.indeed.proctor.builder;

import com.indeed.proctor.common.IncompatibleTestMatrixException;
import com.indeed.proctor.store.FileBasedProctorStore;
import com.indeed.proctor.store.LocalDirectoryStore;
import com.indeed.proctor.store.StoreException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.log4j.Logger;

import java.io.*;

public class LocalProctorBuilder extends ProctorBuilder {

    private static final Logger LOGGER = Logger.getLogger(LocalProctorBuilder.class);

    public LocalProctorBuilder(final File inputDir,
                               final String testDefinitionsDirectory,
                               final Writer outputSink) {
        super(new LocalDirectoryStore(inputDir, testDefinitionsDirectory), outputSink);
    }

    public LocalProctorBuilder(final File inputDir,
                               final Writer outputSink) {
        this(inputDir, FileBasedProctorStore.DEFAULT_TEST_DEFINITIONS_DIRECTORY, outputSink);
    }

    public LocalProctorBuilder(final File inputDir,
                               final String testDefinitionsDirectory,
                               final Writer outputSink,
                               final String author) {
        super(new LocalDirectoryStore(inputDir, testDefinitionsDirectory), outputSink, author);
    }

    public LocalProctorBuilder(final File inputDir,
                               final Writer outputSink,
                               final String author) {
        this(inputDir, FileBasedProctorStore.DEFAULT_TEST_DEFINITIONS_DIRECTORY, outputSink, author);
    }

    public LocalProctorBuilder(final File inputDir,
                               final String testDefinitionsDirectory,
                               final Writer outputSink,
                               final String author,
                               final String version) {
        super(new LocalDirectoryStore(inputDir, testDefinitionsDirectory), outputSink, author, version);
    }

    public LocalProctorBuilder(final File inputDir,
                               final Writer outputSink,
                               final String author,
                               final String version) {
        this(inputDir, FileBasedProctorStore.DEFAULT_TEST_DEFINITIONS_DIRECTORY, outputSink, author, version);
    }

    private static class LocalProctorBuilderArgs extends ProctorBuilderArgs {
        private String inputdir;
        private String testDefinitionsDirectory = FileBasedProctorStore.DEFAULT_TEST_DEFINITIONS_DIRECTORY;

        private LocalProctorBuilderArgs() {
            options.addOption(OptionBuilder.hasArg(true)
                .isRequired()
                .withLongOpt("input")
                .withArgName("base input directory")
                .withDescription("The base directory to read from.")
                .create("i"));
            options.addOption(OptionBuilder.hasArg(true)
                .withLongOpt("test-definitions-directory")
                .withArgName("test-definitions directory")
                .withDescription("test-definitions directory, relative to the base directory.")
                .create("d"));
        }

        @Override
        protected void extract(final CommandLine results)  {
            super.extract(results);
            this.inputdir = results.getOptionValue("input");
            if (results.hasOption("test-definitions-directory")) {
                this.testDefinitionsDirectory = results.getOptionValue("test-definitions-directory");
            }
        }

        public String getInputdir() {
            return inputdir;
        }

        public String getTestDefinitionsDirectory() {
            return testDefinitionsDirectory;
        }
    }

    public static void main(final String[] args) throws IOException, StoreException, IncompatibleTestMatrixException {
        final LocalProctorBuilderArgs arguments = new LocalProctorBuilderArgs();
        arguments.parse(args);

        try {
            new LocalProctorBuilder(
                    new File(arguments.getInputdir()),
                    arguments.getTestDefinitionsDirectory(),
                    "-".equals(arguments.getOutputdir()) ?
                        new PrintWriter(System.out) :
                        new FileWriter(new File(arguments.getOutputdir(), arguments.getFilename())),
                    arguments.getAuthor(),
                    arguments.getVersion()).execute();
        } catch (final Exception e) {
            LOGGER.error("Failed to generates proctor artifact from " + arguments.getInputdir(), e);
            System.exit(1);
        }
    }
}
