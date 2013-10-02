package com.indeed.proctor.builder;

import com.indeed.proctor.store.LocalDirectoryStore;
import com.indeed.proctor.store.ProcterReader;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.log4j.Logger;

import java.io.File;

public class LocalProctorBuilder {
    private static final Logger LOGGER = Logger.getLogger(LocalProctorBuilder.class);

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

    public static void main(final String[] args) {
        final LocalProctorBuilderArgs arguments = new LocalProctorBuilderArgs();
        arguments.parse(args);

        final ProcterReader proctorPersister = new LocalDirectoryStore(new File(arguments.getInputdir()));

        try {
            ProctorBuilderUtils.generateArtifact(proctorPersister, arguments);
        } catch (Exception e) {
            LOGGER.error("Failed to generates proctor artifact from " + arguments.getInputdir(), e);
            System.exit(1);
        }
    }
}
