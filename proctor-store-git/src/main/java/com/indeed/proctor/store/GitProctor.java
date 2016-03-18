package com.indeed.proctor.store;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.indeed.proctor.common.model.TestMatrixVersion;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public class GitProctor extends FileBasedProctorStore {
    private static final Logger LOGGER = Logger.getLogger(GitProctor.class);

    /* Storage Schema:
        ${gitPath}/
            ${testDefinitionsDirectory}/
                test-name-one/
                    definition.json
                    metadata.json
                test-name-two/
                    definition.json
                    metadata.json
    */

    private final Git git;
    private String branchName;

    public GitProctor(final String gitPath,
                      final String username,
                      final String password,
                      final String testDefinitionsDirectory) {
        this(new GitProctorCore(gitPath, username, password, testDefinitionsDirectory, Files.createTempDir()), testDefinitionsDirectory);
    }

    public GitProctor(final String gitPath,
                      final String username,
                      final String password) {
        this(gitPath, username, password, DEFAULT_TEST_DEFINITIONS_DIRECTORY);
    }

    public GitProctor(final GitProctorCore core) {
        this(core, DEFAULT_TEST_DEFINITIONS_DIRECTORY);
    }

    public GitProctor(final GitProctorCore core, final String testDefinitionsDirectory) {
        super(core, testDefinitionsDirectory);
        this.git = core.getGit();
    }

    public GitProctor(final GitProctorCore core, final String testDefinitionsDirectory, final String branchName) {
        super(core, testDefinitionsDirectory);
        this.git = core.getGit();
        this.branchName = branchName;
        checkoutBranch(branchName);
    }

    public static void main(String args[]) throws IOException {
        final String gitUrl = System.console().readLine("git url: ");
        final String gituser = System.console().readLine("user: ");
        final String password = new String(System.console().readPassword("password: "));
        final int num_revisions = Integer.parseInt(System.console().readLine("number of histories: "));
        final String testDefinitionDirectory = System.console().readLine("test definitions directory: ");

        final File tempDir = Files.createTempDir();
        try {
            final GitProctor client = new GitProctor(gitUrl, gituser, password, testDefinitionDirectory);

            System.out.println("Running load matrix for last " + num_revisions + " revisions");
            final long start = System.currentTimeMillis();
            final List<Revision> revisions = client.getMatrixHistory(0, num_revisions);
            for(final Revision rev : revisions) {
                final TestMatrixVersion matrix = client.getTestMatrix(rev.getRevision());
            }
            final long elapsed = System.currentTimeMillis() - start;
            System.out.println("Finished reading matrix history (" + revisions.size() + ") in " + elapsed + " ms");
            client.close();
        } catch (StoreException e) {
            LOGGER.error(e);
        } finally {
            System.out.println("Deleting temp dir : " + tempDir);
            FileUtils.deleteDirectory(tempDir);
        }
    }

    @Override
    public void verifySetup() throws StoreException {
        final String refName = getGitCore().getRefName();
        try {
            final ObjectId branchHead = git.getRepository().resolve(refName);
            if (branchHead == null) {
                throw new StoreException("git repository couldn't resolve " + refName);
            }
        } catch (IncorrectObjectTypeException e) {
            throw new StoreException("Could get resolve " + refName);
        } catch (AmbiguousObjectException e) {
            throw new StoreException("Could get resolve " + refName);
        } catch (IOException e) {
            throw new StoreException("Could get resolve " + refName);
        }
    }

    protected GitProctorCore getGitCore() {
        return (GitProctorCore) core;
    }

    @Override
    public boolean cleanUserWorkspace(String username) {
        getGitCore().undoLocalChanges();
        getGitCore().initializeRepository();
        checkoutBranch(this.branchName);
        return true;
    }

    @Override
    public String getLatestVersion() throws StoreException {
        try {
            final Ref branch = git.getRepository().getRef(getGitCore().getRefName());
            return branch.getObjectId().name();
        } catch (IOException e) {
            throw new StoreException(e);
        }
    }

    @Override
    public List<Revision> getMatrixHistory(final int start,
                                                   final int limit) throws StoreException {
        final LogCommand logCommand;
        try {
            final ObjectId branchHead = git.getRepository().resolve(getGitCore().getRefName());
            logCommand = git.log()
                .add(branchHead)
                .setSkip(start)
                .setMaxCount(limit);
            return getHistoryFromLogCommand(logCommand);
        } catch (MissingObjectException e) {
            throw new StoreException("Could not get history for starting at " + getGitCore().getRefName(), e);
        } catch (IncorrectObjectTypeException e) {
            throw new StoreException("Could not get history for starting at " + getGitCore().getRefName(), e);
        } catch (AmbiguousObjectException e) {
            throw new StoreException("Could not get history for starting at " + getGitCore().getRefName(), e);
        } catch (IOException e) {
            throw new StoreException("Could not get history for starting at " + getGitCore().getRefName(), e);
        }
    }

    @Override
    public List<Revision> getHistory(final String test,
                                             final int start,
                                             final int limit) throws StoreException {
        return getHistory(test, getLatestVersion(), start, limit);
    }

    @Override
    public List<Revision> getHistory(final String test,
                                             final String revision,
                                             final int start,
                                             final int limit) throws StoreException {
        try {
            final ObjectId commitId = ObjectId.fromString(revision);
            final LogCommand logCommand = git.log()
                // TODO: create path to definition.json file, sanitize test name for invalid / relative characters
                .addPath(getTestDefinitionsDirectory()  + File.separator + test + File.separator + FileBasedProctorStore.TEST_DEFINITION_FILENAME)
                .add(commitId)
                .setSkip(start)
                .setMaxCount(limit);
            return getHistoryFromLogCommand(logCommand);

        } catch (IOException e) {
            throw new StoreException("Could not get history for " + test + " starting at " + getGitCore().getRefName(), e);
        }
    }

    private List<Revision> getHistoryFromLogCommand(final LogCommand command) throws StoreException {
        final List<Revision> versions = Lists.newArrayList();
        final Iterable<RevCommit> commits;
        try {
            commits = command.call();
        } catch (GitAPIException e) {
            throw new StoreException("Could not get history", e);
        }
        for( RevCommit commit : commits) {
            versions.add(new Revision(
                commit.getName(),
                commit.getAuthorIdent().toExternalString(),
                new Date(Long.valueOf(commit.getCommitTime()) * 1000 /* convert seconds to milliseconds */),
                commit.getFullMessage()
            ));
        }
        return versions;
    }

    public void checkoutBranch(String branchName) {
        try {
            git.branchCreate()
                    .setName(branchName)
                    .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                    .setStartPoint("origin/" + branchName)
                    .setForce(true)
                    .call();
            git.checkout().setName(branchName).call();
        } catch (GitAPIException e) {
            LOGGER.error("Unable to create/checkout branch " + branchName, e);
        }
    }
}
