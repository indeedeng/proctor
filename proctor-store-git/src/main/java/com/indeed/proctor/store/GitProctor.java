package com.indeed.proctor.store;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.Files;
import com.indeed.proctor.common.model.TestMatrixDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.LogCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                      final String password,
                      final String testDefinitionsDirectory,
                      final String branchName) {
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
    public boolean cleanUserWorkspace(final String username) {
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

    @Override
    public Map<String, List<Revision>> getAllHistories() throws StoreException {
        final TestMatrixDefinition testMatrixDefinition = getCurrentTestMatrix().getTestMatrixDefinition();
        if (testMatrixDefinition == null) {
            return Collections.emptyMap();
        }
        final Set<String> activeTests = testMatrixDefinition.getTests().keySet();

        final Repository repository = git.getRepository();
        try {
            final ObjectId head = repository.resolve(Constants.HEAD);
            final RevWalk revWalk = new RevWalk(repository);
            final DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE);
            df.setRepository(git.getRepository());
            df.setDiffComparator(RawTextComparator.DEFAULT);

            final HistoryParser historyParser = new HistoryParser(revWalk, df, getTestDefinitionsDirectory(), activeTests);
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

    public static class HistoryParser {
        final RevWalk revWalk;
        final DiffFormatter df;
        final Pattern testNamePattern;
        final Set<String> activeTests;

        public HistoryParser(final RevWalk revWalk,
                             final DiffFormatter df,
                             final String definitionDirectory,
                             final Set<String> activeTests) {
            this.revWalk = revWalk;
            this.df = df;
            testNamePattern = compileTestNamePattern(definitionDirectory);
            this.activeTests = activeTests;
        }

        public Map<String, List<Revision>> parseFromHead(final ObjectId head) throws IOException {
            final Map<String, List<Revision>> histories = Maps.newHashMap();
            final Set<ObjectId> visited = Sets.newHashSet();
            final Queue<RevCommit> queue = new LinkedList<RevCommit>();
            queue.add(revWalk.parseCommit(head));
            while (!queue.isEmpty()) {
                parseCommit(queue.poll(), histories, visited, queue);
            }

            final long start = System.currentTimeMillis();
            sortByDate(histories);
            final long end = System.currentTimeMillis();
            LOGGER.info(String.format("Took %d ms to sort revisions in chronological order", end - start));
            return histories;
        }

        private void parseCommit(final RevCommit commit,
                                 final Map<String, List<Revision>> histories,
                                 final Set<ObjectId> visited,
                                 final Queue<RevCommit> queue) throws IOException {
            if (visited.contains(commit.getId())) {
                return;
            }
            visited.add(commit.getId());

            final RevCommit[] parents = commit.getParents();
            if (parents.length == 1) {
                final RevCommit parent = revWalk.parseCommit(parents[0].getId());
                final List<DiffEntry> diffs = df.scan(parent.getTree(), commit.getTree());
                for (final DiffEntry diff : diffs) {
                    final String changePath = diff.getChangeType().equals(ChangeType.DELETE) ? diff.getOldPath() : diff.getNewPath();
                    final Matcher testNameMatcher = testNamePattern.matcher(changePath);

                    if (testNameMatcher.matches()) {
                        final String testName = testNameMatcher.group(1);
                        if (activeTests.contains(testName)) {
                            List<Revision> history = histories.get(testName);
                            if (history == null) {
                                history = Lists.newArrayList();
                                histories.put(testName, history);
                            }

                            history.add(new Revision(
                                    commit.getName(),
                                    commit.getAuthorIdent().toExternalString(),
                                    new Date(Long.valueOf(commit.getCommitTime()) * 1000 /* convert seconds to milliseconds */),
                                    commit.getFullMessage()
                            ));
                        }
                    }
                }

                queue.add(parent);
            } else if (parents.length == 2) {
                /** this is a merge commit, should be skipped **/
                for (final RevCommit parent : parents) {
                    queue.add(revWalk.parseCommit(parent.getId()));
                }
            }
        }

        @VisibleForTesting
        static void sortByDate(final Map<String, List<Revision>> histories) {
            final Comparator<Revision> comparator = new Comparator<Revision>() {
                @Override
                public int compare(final Revision o1, final Revision o2) {
                    return o2.getDate().compareTo(o1.getDate());
                }
            };
            for (final List<Revision> revisions : histories.values()) {
                Collections.sort(revisions, comparator);
            }
        }


        @VisibleForTesting
        public static Pattern compileTestNamePattern(final String definitionDirectory) {
            return Pattern.compile(definitionDirectory +
                    File.separator + "(\\w+)" + File.separator + FileBasedProctorStore.TEST_DEFINITION_FILENAME);
        }
    }
}
