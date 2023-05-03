package com.indeed.proctor.builder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

import com.indeed.proctor.store.FileBasedProctorStore;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.indeed.proctor.common.IncompatibleTestMatrixException;
import com.indeed.proctor.store.GitProctor;
import com.indeed.proctor.store.ProctorReader;
import com.indeed.proctor.store.StoreException;

public class GitLocalProctorBuilder extends ProctorBuilder {

    private static final Logger LOGGER = LogManager.getLogger(GitLocalProctorBuilder.class);

    public GitLocalProctorBuilder(final ProctorReader proctorReader,
                                  final String testDefinitionsDirectory,
                                  final Writer outputSink) {
        super(proctorReader, outputSink);
    }

    public GitLocalProctorBuilder(final ProctorReader proctorReader,
                                  final String testDefinitionsDirectory,
                                  final Writer outputSink,
                                  String author) {
        super(proctorReader, outputSink, author);
    }

    public GitLocalProctorBuilder(final ProctorReader proctorReader,
                                  final String testDefinitionsDirectory,
                                  final Writer outputSink,
                                  final String author,
                                  final String version) {
        super(proctorReader, outputSink, author, version);
    }

    private static class GitLocalProctorBuilderArgs extends ProctorBuilderArgs {
        private String inputGitUrl;
        private String branchName;
        private String username = "";
        private String password = "";
        private String testDefinitionsDirectory = FileBasedProctorStore.DEFAULT_TEST_DEFINITIONS_DIRECTORY;

        private GitLocalProctorBuilderArgs() {
            options.addOption(OptionBuilder.hasArg(true)
                .isRequired()
                .withLongOpt("input")
                .withArgName("input git url")
                .withDescription("The git url to read from.")
                .create("i"));
            options.addOption(OptionBuilder.hasArg(true)
                    .withLongOpt("test-definitions-directory")
                    .withArgName("test-definitions directory")
                    .withDescription("test-definitions directory, relative to the base directory.")
                    .create("d"));
            options.addOption(OptionBuilder.hasArg(true)
                .withLongOpt("branch")
                .withArgName("git branch")
                .withDescription("The git branch to checkout.")
                .create("b"));
            options.addOption(OptionBuilder.hasArg(true)
                .withLongOpt("username")
                .withArgName("git username")
                .withDescription("The git username.")
                .create("u"));
            options.addOption(OptionBuilder.hasArg(true)
                .withLongOpt("password")
                .withArgName("git password")
                .withDescription("The git password.")
                .create("p"));
        }

        @Override
        protected void extract(CommandLine results)  {
            super.extract(results);
            this.inputGitUrl = results.getOptionValue("input");
            if (results.hasOption("branch")) {
                this.branchName = results.getOptionValue("branch");
            }
            if (results.hasOption("username")) {
                this.username = results.getOptionValue("username");
            }
            if (results.hasOption("password")) {
                this.password = results.getOptionValue("password");
            }
            if (results.hasOption("test-definitions-directory")) {
                this.testDefinitionsDirectory = results.getOptionValue("test-definitions-directory");
            }
        }

        public String getInputGitUrl() {
            return inputGitUrl;
        }

        public String getBranchName() {
            return branchName;
        }

        public String getUsername() {
            return username;
        }

        public String getPassword() {
            return password;
        }

        public String getTestDefinitionsDirectory() {
            return testDefinitionsDirectory;
        }
    }

    public static void main(final String[] args) throws IOException, StoreException, IncompatibleTestMatrixException {
        final GitLocalProctorBuilderArgs arguments = new GitLocalProctorBuilderArgs();
        arguments.parse(args);

        try {
            GitProctor proctor = new GitProctor(arguments.getInputGitUrl(), arguments.getUsername(), arguments.getPassword());
            if (arguments.getBranchName() != null && !arguments.getBranchName().isEmpty()) {
                proctor.checkoutBranch(arguments.getBranchName());
            }
            File outputDir = new File(arguments.getOutputdir());
            outputDir.mkdirs();
            File matrixFile = new File(outputDir, arguments.getFilename());
            matrixFile.createNewFile();
            new GitLocalProctorBuilder(
                    proctor,
                    arguments.getTestDefinitionsDirectory(),
                    "-".equals(arguments.getOutputdir()) ?
                            new PrintWriter(System.out) :
                            new FileWriter(matrixFile),
                    arguments.getAuthor(),
                    arguments.getVersion()).execute();
        } catch (Exception e) {
            LOGGER.error("Failed to generates proctor artifact from " + arguments.getInputGitUrl(), e);
            System.exit(1);
        }
    }
}