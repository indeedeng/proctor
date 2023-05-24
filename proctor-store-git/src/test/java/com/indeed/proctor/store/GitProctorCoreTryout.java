package com.indeed.proctor.store;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/** Helper class to run GitProctor methods against a local folder. */
public class GitProctorCoreTryout {

    private static final Logger LOGGER = LogManager.getLogger(GitProctorCoreTryout.class);

    private static final String GIT_USERNAME = "username";
    private static final String GIT_PASSWORD = "password";
    private static final String TEST_DEFINITION_DIRECTORY = "matrices/test-definitions";

    /** run with folder name of proctor data as first program argument */
    public static void main(String[] args) throws Exception {
        LOGGER.debug("Start, checking/cloning git repo from " + args[0]);
        // use random folder to clone cleanly, but this will take more time for big proctor data
        // final File tempDir = new File("/tmp/" + RandomStringUtils.randomAlphanumeric(10));
        final File tempDir = new File("/tmp/junit");
        tempDir.mkdir();
        final GitProctorCore gitProctorCore =
                new GitProctorCore(
                        args[0],
                        GIT_USERNAME,
                        GIT_PASSWORD,
                        TEST_DEFINITION_DIRECTORY,
                        tempDir,
                        "trunk");
        try (final FileBasedProctorStore store =
                new GitProctor(gitProctorCore, TEST_DEFINITION_DIRECTORY)) {
            LOGGER.debug("Created store, getting history...");
            LOGGER.debug("Histories.size: " + store.getAllHistories().size());
            LOGGER.debug("Histories.size again: " + store.getAllHistories().size());
            LOGGER.debug(
                    "Testmatrix size: "
                            + store.getCurrentTestMatrix()
                                    .getTestMatrixDefinition()
                                    .getTests()
                                    .size());
            LOGGER.debug(
                    "Testmatrix size again: "
                            + store.getCurrentTestMatrix()
                                    .getTestMatrixDefinition()
                                    .getTests()
                                    .size());
        } finally {
            LOGGER.debug("Shutdown");
        }
    }
}
