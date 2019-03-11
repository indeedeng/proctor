package com.indeed.proctor.store;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GitProctorCoreWithRepositoryTest extends RepositoryTestCase {

    private static final String GIT_USERNAME = "username";
    private static final String GIT_PASSWORD = "password";
    private static final String TEST_DEFINITION_DIRECTORY = "matrices/test-definitions";
    private static final String COMMIT_MESSAGE = "Initial commit";
    private static final String TEST_FILE_NAME = "GitProctorCoreTest.txt";

    private Git remoteGit;

    private String gitUrl;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Override
    public void setUp() throws Exception {
        super.setUp();
        remoteGit = new Git(db);

        gitUrl = "file://" + remoteGit.getRepository().getWorkTree().getAbsolutePath();

        writeTrashFile(TEST_FILE_NAME, "GitProctorCoreTest");
        remoteGit.add().addFilepattern(TEST_FILE_NAME).call();
        remoteGit.commit().setMessage(COMMIT_MESSAGE).call();
    }

    @Test
    public void testCloneRepository() throws Exception {
        final File workingDir = temporaryFolder.newFolder("testCloneRepository");

        // Run cloneRepository
        final GitProctorCore gitProctorCore = new GitProctorCore(
                gitUrl,
                GIT_USERNAME,
                GIT_PASSWORD,
                TEST_DEFINITION_DIRECTORY,
                workingDir
        );

        final Git git = gitProctorCore.getGit();

        assertNotNull(git);
        assertEquals("master", git.getRepository().getBranch());

        final String localCommitMessage = git.log().call().iterator().next().getFullMessage();
        assertEquals(COMMIT_MESSAGE, localCommitMessage);
    }

}
