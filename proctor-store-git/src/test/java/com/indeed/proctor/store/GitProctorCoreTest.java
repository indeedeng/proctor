package com.indeed.proctor.store;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.junit.RepositoryTestCase;
import org.junit.Test;

import java.io.File;

import static com.google.common.io.Files.createTempDir;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class GitProctorCoreTest extends RepositoryTestCase {

    private static final String GIT_USERNAME = "username";
    private static final String GIT_PASSWORD = "password";
    private static final String TEST_DEFINITION_DIRECTORY = "matrices/test-definitions";
    private static final String COMMIT_MESSAGE = "Initial commit";

    private Git remoteGit;

    private String gitUrl;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        remoteGit = new Git(db);

        gitUrl = "file://" + remoteGit.getRepository().getWorkTree().getAbsolutePath();

        writeTrashFile("GitProctorCoreTest.txt", "GitProctorCoreTest");
        remoteGit.add().addFilepattern("GitProctorCoreTest.txt").call();
        remoteGit.commit().setMessage(COMMIT_MESSAGE).call();
    }

    @Test
    public void testParseTestName() {
        assertEquals(
                "testname",
                GitProctorCore.parseTestName("matrices/test-definitions", "matrices/test-definitions/testname/definition.json")
        );
        assertEquals(
                "testname",
                GitProctorCore.parseTestName("matrices/test-definitions", "matrices/test-definitions/testname/metadata.json")
        );
        assertEquals(
                "testname",
                GitProctorCore.parseTestName("test-definitions", "test-definitions/testname/definition.json")
        );
        assertEquals(
                "testname",
                GitProctorCore.parseTestName("", "testname/definition.json")
        );
        assertNull(GitProctorCore.parseTestName("matrices/test-definitions", ""));
        assertNull(GitProctorCore.parseTestName("matrices/test-definitions", "some_file.txt"));
        assertEquals(
                "testname",
                GitProctorCore.parseTestName("matrices/test-definitions", "matrices/test-definitions/testname/some/file")
        );
        assertEquals(
                null,
                GitProctorCore.parseTestName("matrices/test-definitions", "matrices/test-definitions/filename")
        );
    }

    @Test
    public void testCloneRepository() throws Exception {
        final File workingDir = createTempDir();

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

    @Test
    public void testCloneSingleRepository() throws Exception {
        final File workingDir = createTempDir();
        final String branchName = "test";

        remoteGit.checkout().setCreateBranch(true).setName(branchName).call();
        writeTrashFile("testCloneSingleRepository.txt", "test");
        remoteGit.add().addFilepattern("testCloneSingleRepository.txt").call();

        final String commitMessage = "Create a new branch";
        remoteGit.commit().setMessage(commitMessage).call();

        // Run cloneRepository with single branch
        final GitProctorCore gitProctorCore = new GitProctorCore(
                gitUrl,
                GIT_USERNAME,
                GIT_PASSWORD,
                TEST_DEFINITION_DIRECTORY,
                workingDir,
                branchName
        );

        final Git git = gitProctorCore.getGit();

        assertNotNull(git);
        assertEquals(branchName, git.getRepository().getBranch());

        final String localCommitMessage = git.log().call().iterator().next().getFullMessage();
        assertEquals(commitMessage, localCommitMessage);

        try {
            gitProctorCore.checkoutBranch(branchName);
        } catch (final Exception unexpected) {
            fail("checkoutBranch should not throw any exceptions");
        }
    }
}