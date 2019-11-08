package com.indeed.proctor.store;

import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.indeed.proctor.store.GitProctorUtils.determineAuthorId;

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
    @Nullable
    private final String branchName;

    public GitProctor(final String gitPath,
                      final String username,
                      final String password,
                      final String testDefinitionsDirectory) {
        this(gitPath, username, password, testDefinitionsDirectory, null);
    }

    public GitProctor(final String gitPath,
                      final String username,
                      final String password,
                      final String testDefinitionsDirectory,
                      @Nullable final String branchName) {
        this(new GitProctorCore(gitPath, username, password, testDefinitionsDirectory, Files.createTempDir()), testDefinitionsDirectory, branchName);
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
        this(core, testDefinitionsDirectory, null);
    }

    /**
     *
     * @param core a core with a defined remote and defined local working directory
     * @param testDefinitionsDirectory where test definitions are located inside the local git repository
     * @param branchName stay on this branch if not null, else default branch from remote
     */
    public GitProctor(final GitProctorCore core, final String testDefinitionsDirectory, @Nullable final String branchName) {
        super(core, testDefinitionsDirectory);
        git = core.getGit();
        this.branchName = branchName;
        if (branchName != null) {
            checkoutBranch(branchName);
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
        } catch (final IOException e) {
            throw new StoreException("Could get resolve " + refName);
        }
    }

    protected GitProctorCore getGitCore() {
        return (GitProctorCore) core;
    }

    @Override
    public boolean cleanUserWorkspace(final String username) {
        getGitCore().undoLocalChanges();
        getGitCore().initializeRepository(false);
        if (this.branchName != null) {
            checkoutBranch(this.branchName);
        }
        return true;
    }

    @Nonnull
    @Override
    public String getLatestVersion() throws StoreException {
        try {
            final Ref branch = git.getRepository().findRef(getGitCore().getRefName());
            return branch.getObjectId().name();
        } catch (final IOException e) {
            throw new StoreException(e);
        }
    }

    @Nonnull
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
        } catch (final IOException e) {
            throw new StoreException("Could not get history for starting at " + getGitCore().getRefName(), e);
        }
    }

    @Nonnull
    @Override
    public List<Revision> getMatrixHistory(final Instant sinceInclusive, final Instant untilExclusive) throws StoreException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Nonnull
    @Override
    public List<Revision> getHistory(final String test,
                                     final int start,
                                     final int limit) throws StoreException {
        return getHistory(test, getLatestVersion(), start, limit);
    }

    @Nonnull
    @Override
    public List<Revision> getHistory(final String test,
                                     final String revision,
                                     final int start,
                                     final int limit) throws StoreException {
        try {
            final ObjectId commitId = ObjectId.fromString(revision);
            final LogCommand logCommand = git.log()
                    // TODO: create path to definition.json file, sanitize test name for invalid / relative characters
                    .addPath(getTestDefinitionsDirectory() + File.separator + test + File.separator + FileBasedProctorStore.TEST_DEFINITION_FILENAME)
                    .add(commitId)
                    .setSkip(start)
                    .setMaxCount(limit);
            return getHistoryFromLogCommand(logCommand);

        } catch (final IOException e) {
            throw new StoreException("Could not get history for " + test + " starting at " + getGitCore().getRefName(), e);
        }
    }

    @CheckForNull
    @Override
    public RevisionDetails getRevisionDetails(final String revisionId) throws StoreException {
        try {
            final ObjectId objectId = git.getRepository().resolve(revisionId);
            if (objectId == null) {
                return null;
            }
            final GitHistoryParser historyParser =
                    GitHistoryParser.fromRepository(git.getRepository(), getTestDefinitionsDirectory());
            return historyParser.parseRevisionDetails(objectId);
        } catch (final MissingObjectException e) {
            LOGGER.debug("unknown revision " + revisionId, e);
            return null;
        } catch (final IOException e) {
            throw new StoreException("Could not get detail for " + revisionId, e);
        }
    }

    @Nonnull
    @Override
    public Map<String, List<Revision>> getAllHistories() throws StoreException {
        final Repository repository = git.getRepository();
        try {
            final ObjectId head = repository.resolve(Constants.HEAD);
            final GitHistoryParser historyParser =
                    GitHistoryParser.fromRepository(git.getRepository(), getTestDefinitionsDirectory());
            return historyParser.parseFromHead(head);
        } catch (final IOException e) {
            throw new StoreException("Could not get history " + getGitCore().getRefName(), e);
        }
    }

    private List<Revision> getHistoryFromLogCommand(final LogCommand command) throws StoreException {
        final List<Revision> versions = Lists.newArrayList();
        final Iterable<RevCommit> commits;
        try {
            commits = command.call();
        } catch (final GitAPIException e) {
            throw new StoreException("Could not get history", e);
        }
        for (final RevCommit commit : commits) {
            versions.add(new Revision(
                    commit.getName(),
                    determineAuthorId(commit),
                    new Date((long) commit.getCommitTime() * 1000 /* convert seconds to milliseconds */),
                    commit.getFullMessage()
            ));
        }
        return versions;
    }

    public void checkoutBranch(final String branchName) {
        getGitCore().checkoutBranch(branchName);
    }

    @Override
    public void refresh() throws StoreException {
        getGitCore().refresh();
    }

    @Override
    public String getName() {
        return "GitProctor-" + branchName;
    }
}
