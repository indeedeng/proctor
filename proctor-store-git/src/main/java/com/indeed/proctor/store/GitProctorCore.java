package com.indeed.proctor.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.indeed.proctor.common.Serializers;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GitProctorCore implements FileBasedPersisterCore {
    private static final Logger LOGGER = Logger.getLogger(GitProctorCore.class);
    private static final TextProgressMonitor PROGRESS_MONITOR = new TextProgressMonitor(new LoggerPrintWriter(LOGGER, Level.DEBUG));
    private static final long GC_INTERVAL_IN_DAY = 1;
    private static final boolean DEFAULT_CLEAN_INITIALIZATION = false;

    private final String username;
    private final String password;
    private final String testDefinitionsDirectory;

    private Git git;
    private final String gitUrl;
    private final String refName;
    @Nullable private final String branchName;
    private final GitWorkspaceProvider workspaceProvider;
    private final ScheduledExecutorService gcExecutor;
    private final UsernamePasswordCredentialsProvider user;
    private final GitAPIExceptionWrapper gitAPIExceptionWrapper;

    private final int pullPushTimeoutSeconds;
    private final int cloneTimeoutSeconds;

    public GitProctorCore(final String gitUrl,
                          final String username,
                          final String password,
                          final String testDefinitionsDirectory,
                          final File tempDir) {
        this(gitUrl, username, password, testDefinitionsDirectory, new GitWorkspaceProviderImpl(tempDir));
    }

    public GitProctorCore(final String gitUrl,
                          final String username,
                          final String password,
                          final String testDefinitionsDirectory,
                          final File tempDir,
                          final String branchName) {
        this(gitUrl, username, password, testDefinitionsDirectory, new GitWorkspaceProviderImpl(tempDir),
                GitProctorUtils.DEFAULT_GIT_PULL_PUSH_TIMEOUT_SECONDS, GitProctorUtils.DEFAULT_GIT_CLONE_TIMEOUT_SECONDS,
                DEFAULT_CLEAN_INITIALIZATION, branchName);

    }

    /**
     * @param gitUrl
     * @param username
     * @param password
     * @param testDefinitionsDirectory
     * @param workspaceProvider
     */
    public GitProctorCore(final String gitUrl,
                          final String username,
                          final String password,
                          final String testDefinitionsDirectory,
                          final GitWorkspaceProvider workspaceProvider) {
        this(gitUrl, username, password, testDefinitionsDirectory, workspaceProvider,
                GitProctorUtils.DEFAULT_GIT_PULL_PUSH_TIMEOUT_SECONDS, GitProctorUtils.DEFAULT_GIT_CLONE_TIMEOUT_SECONDS);
    }

    public GitProctorCore(final String gitUrl,
                          final String username,
                          final String password,
                          final String testDefinitionsDirectory,
                          final GitWorkspaceProvider workspaceProvider,
                          final int pullPushTimeoutSeconds,
                          final int cloneTimeoutSeconds) {
        this(gitUrl, username, password, testDefinitionsDirectory, workspaceProvider,
                pullPushTimeoutSeconds, cloneTimeoutSeconds, DEFAULT_CLEAN_INITIALIZATION);
    }

    public GitProctorCore(final String gitUrl,
                          final String username,
                          final String password,
                          final String testDefinitionsDirectory,
                          final GitWorkspaceProvider workspaceProvider,
                          final int pullPushTimeoutSeconds,
                          final int cloneTimeoutSeconds,
                          final boolean cleanInitialization) {
        this(gitUrl, username, password, testDefinitionsDirectory, workspaceProvider,
                pullPushTimeoutSeconds, cloneTimeoutSeconds, cleanInitialization, null);
    }

    public GitProctorCore(final String gitUrl,
                          final String username,
                          final String password,
                          final String testDefinitionsDirectory,
                          final GitWorkspaceProvider workspaceProvider,
                          final int pullPushTimeoutSeconds,
                          final int cloneTimeoutSeconds,
                          final boolean cleanInitialization,
                          @Nullable final String branchName) {
        this.gitUrl = gitUrl;
        this.refName = Constants.HEAD;
        this.workspaceProvider = Preconditions
                .checkNotNull(workspaceProvider, "GitWorkspaceProvider should not be null");
        this.username = username;
        this.password = password;
        user = new UsernamePasswordCredentialsProvider(username, password);
        this.testDefinitionsDirectory = testDefinitionsDirectory;
        gcExecutor = Executors.newSingleThreadScheduledExecutor();
        this.pullPushTimeoutSeconds = pullPushTimeoutSeconds;
        this.cloneTimeoutSeconds = cloneTimeoutSeconds;
        this.branchName = branchName;
        this.gitAPIExceptionWrapper = new GitAPIExceptionWrapper();
        this.gitAPIExceptionWrapper.setGitUrl(gitUrl);
        initializeRepository(cleanInitialization);
    }

    private Git pullRepository(final File workingDir) throws GitAPIException, IOException {
        final Git git = Git.open(workingDir);
        git.pull().setProgressMonitor(PROGRESS_MONITOR)
                .setRebase(true)
                .setCredentialsProvider(user)
                .setTimeout(pullPushTimeoutSeconds)
                .call();
        return git;
    }

    private Git cloneRepository(final File workingDir) throws GitAPIException {
        final CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(gitUrl)
                .setDirectory(workingDir)
                .setProgressMonitor(PROGRESS_MONITOR)
                .setCredentialsProvider(user)
                .setTimeout(cloneTimeoutSeconds);

        if (StringUtils.isNotEmpty(branchName)) {
            cloneCommand.setBranchesToClone(ImmutableSet.of(branchName));
        }

        return cloneCommand.call();
    }

    void initializeRepository(final boolean cleanInitialization) {
        final File workingDir = workspaceProvider.getRootDirectory();
        final File gitDirectory = new File(workingDir, ".git");
        LOGGER.info("Initializing repository " + gitUrl + " in working dir " + workingDir.getAbsolutePath());

        workspaceProvider.synchronizedOperation(new Callable<Void>() {
            @Override
            public Void call() {
                try {
                    if (!gitDirectory.exists()) {
                        LOGGER.info("Local repository not found, creating a new clone...");
                        git = cloneRepository(workingDir);
                    } else if(cleanInitialization) {
                        LOGGER.info("Existing local repository found, but creating a new clone to clean up working directory...");
                        workspaceProvider.cleanWorkingDirectory();
                        git = cloneRepository(workingDir);
                    } else {
                        LOGGER.info("Existing local repository found, pulling latest changes...");
                        try {
                            git = pullRepository(workingDir);
                        } catch (final Exception e) {
                            LOGGER.error("Could not update existing local repository, creating a new clone...", e);
                            workspaceProvider.cleanWorkingDirectory();
                            git = cloneRepository(workingDir);
                        }
                    }
                } catch (final GitAPIException e) {
                    LOGGER.error("Unable to clone git repository at " + gitUrl, e);
                }
                return null;
            }
        });

        try {
            git.fetch()
                    .setProgressMonitor(PROGRESS_MONITOR)
                    .setCredentialsProvider(user)
                    .setTimeout(pullPushTimeoutSeconds)
                    .call();
        } catch (GitAPIException e) {
            LOGGER.error("Unable to fetch from " + gitUrl, e);
        }
        gcExecutor.scheduleAtFixedRate(new GitGcTask(), GC_INTERVAL_IN_DAY, GC_INTERVAL_IN_DAY, TimeUnit.DAYS);
    }

    @Override
    public <C> C getFileContents(final Class<C> c,
            final java.lang.String[] path,
            final C defaultValue,
            final String revision) throws StoreException.ReadException, JsonProcessingException {
        try {
            if (!ObjectId.isId(revision)) {
                throw new StoreException.ReadException("Malformed id " + revision);
            }
            final ObjectId blobOrCommitId = ObjectId.fromString(revision);

            final ObjectLoader loader = git.getRepository().open(blobOrCommitId);

            if (loader.getType() == Constants.OBJ_COMMIT) {
                // look up the file at this revision
                final RevCommit commit = RevCommit.parse(loader.getCachedBytes());

                final TreeWalk treeWalk2 = new TreeWalk(git.getRepository());
                treeWalk2.addTree(commit.getTree());
                treeWalk2.setRecursive(true);
                //final String joinedPath = "matrices" + "/" + Joiner.on("/").join(path);
                final String joinedPath = Joiner.on("/").join(path);
                treeWalk2.setFilter(PathFilter.create(joinedPath));

                if (!treeWalk2.next()) {
                    throw new StoreException.ReadException("Did not find expected file '" + joinedPath + "'");
                }
                final ObjectId blobId = treeWalk2.getObjectId(0);
                return getFileContents(c, blobId);
            } else if (loader.getType() == Constants.OBJ_BLOB) {
                return getFileContents(c, blobOrCommitId);
            } else {
                throw new StoreException.ReadException("Invalid Object Type " + loader.getType() + " for id " + revision);
            }
        } catch (IOException e) {
            throw new StoreException.ReadException(e);
        }
    }

    private <C> C getFileContents(final Class<C> c,
            final ObjectId blobId) throws IOException {
        final ObjectLoader loader = git.getRepository().open(blobId);
        final ObjectMapper mapper = Serializers.lenient();
        return mapper.readValue(loader.getBytes(), c);
    }

    public boolean cleanWorkingDirectory() {
        return workspaceProvider.cleanWorkingDirectory();
    }

    /**
     * @deprecated We don't need to specify username/password.
     * Replaced by {@link #createRefresherTask()}
     */
    @Deprecated
    public GitDirectoryRefresher createRefresherTask(String username, String password) {
        return new GitDirectoryRefresher(workspaceProvider, this, username, password);
    }

    /**
     * Creates a background task that can be scheduled to refresh a template directory used to
     * seed each user workspace during a commit.
     * @return
     */
    public GitDirectoryRefresher createRefresherTask() {
        return new GitDirectoryRefresher(workspaceProvider, this, username, password);
    }

    static class GitRcsClient implements FileBasedProctorStore.RcsClient {
        private final Git git;
        private final String testDefinitionsDirectory;

        public GitRcsClient(final Git git, final String testDefinitionsDirectory) {
            this.git = git;
            this.testDefinitionsDirectory = testDefinitionsDirectory;
        }

        @Override
        public void add(final File file) throws Exception {
            git.add().addFilepattern(testDefinitionsDirectory + "/" + file.getAbsoluteFile().getParentFile().getName() + "/" +
                    file.getName()).call();
        }

        @Override
        public void delete(File testDefinitionDirectory) throws Exception {
            for (File file : testDefinitionDirectory.listFiles()) {
                git.rm().addFilepattern(testDefinitionsDirectory + "/" + testDefinitionDirectory.getName()
                        + "/" + file.getName()).call();
            }
        }

        @Override
        public String getRevisionControlType() {
            return "git";
        }
    }

    @Override
    public void doInWorkingDirectory(final String username,
                                     final String password,
                                     final String author,
                                     final String comment,
                                     final String previousVersion,
                                     final FileBasedProctorStore.ProctorUpdater updater) throws StoreException.TestUpdateException {
        final UsernamePasswordCredentialsProvider user = new UsernamePasswordCredentialsProvider(username, password);
        final File workingDir = workspaceProvider.getRootDirectory();

        workspaceProvider.synchronizedUpdateOperation(new GitProctorCallable<Void>() {
            @Override
            public Void call() throws StoreException.TestUpdateException {
                try {
                    git = Git.open(workingDir);
                    final PullResult pullResult = git.pull()
                            .setProgressMonitor(PROGRESS_MONITOR)
                            .setRebase(true)
                            .setCredentialsProvider(user)
                            .setTimeout(pullPushTimeoutSeconds)
                            .call();
                    if (!pullResult.isSuccessful()) {
                        LOGGER.info("Failed to pull from the remote repository. Running undo local changes");
                        undoLocalChanges();
                    }
                    final FileBasedProctorStore.RcsClient rcsClient = new GitProctorCore.GitRcsClient(git, testDefinitionsDirectory);
                    final boolean thingsChanged;
                    thingsChanged = updater.doInWorkingDirectory(rcsClient, workingDir);
                    if (thingsChanged) {
                        final Set<String> stagedTests = parseStagedTestNames();
                        LOGGER.debug("Staged tests are " + Joiner.on(",").join(stagedTests));
                        if (stagedTests != null && stagedTests.size() >= 2) {
                            LOGGER.error("Multiple tests are going to be modified at the one commit : " + Joiner.on(",").join(stagedTests));
                            throw new IllegalStateException("Another test are staged unintentionally due to invalid local git state");
                        } else if (stagedTests != null && stagedTests.isEmpty()) {
                            LOGGER.warn("Failed to parse staged test names or no test files aren't staged");
                        }

                        git.commit().setCommitter(username, username).setAuthor(author, author).setMessage(comment).call();
                        final Iterable<PushResult> pushResults = git.push().setProgressMonitor(PROGRESS_MONITOR).setCredentialsProvider(user)
                                .setTimeout(pullPushTimeoutSeconds)
                                .call();
                        // jgit doesn't throw an exception for certain kinds of push failures - explicitly check the result
                        for (final PushResult pushResult : pushResults) {
                            for (final RemoteRefUpdate remoteRefUpdate : pushResult.getRemoteUpdates()) {
                                switch (remoteRefUpdate.getStatus()) {
                                    case OK:
                                        continue;
                                    case REJECTED_NONFASTFORWARD:
                                        throw new IllegalStateException("Non-fast-forward push - there have likely been other commits made since starting. Confirm the latest state and try again.");
                                    default:
                                        final String message;
                                        if (StringUtils.isNotEmpty(remoteRefUpdate.getMessage())) {
                                            message = remoteRefUpdate.getMessage();
                                        } else {
                                            message = "Non-success push status: " + remoteRefUpdate.getStatus().toString();
                                        }
                                        throw new IllegalStateException(message);
                                }
                            }
                        }
                    }
                } catch (final GitAPIException e) {
                    undoLocalChanges();
                    throw gitAPIExceptionWrapper.wrapException(new StoreException.TestUpdateException("Unable to commit/push changes", e));
                } catch (final IllegalStateException e) {
                    undoLocalChanges();
                    throw gitAPIExceptionWrapper.wrapException(new StoreException.TestUpdateException("Unable to push changes", e));
                } catch (final Exception e) {
                    undoLocalChanges();
                    throw new StoreException.TestUpdateException("Unable to perform operation", e);
                }
                return null;
            }
        });
    }

    @Override
    public void doInWorkingDirectory(final String username,
                                     final String password,
                                     final String comment,
                                     final String previousVersion,
                                     final FileBasedProctorStore.ProctorUpdater updater) throws StoreException.TestUpdateException {
        doInWorkingDirectory(username, password, username, comment, previousVersion, updater);
    }

    @Nullable
    @VisibleForTesting
    static String parseTestName(final String testDefinitionsDirectory, final String filePath) {
        final String prefix = (Strings.isNullOrEmpty(testDefinitionsDirectory) ? "" : testDefinitionsDirectory + File.separator);
        if (filePath != null && filePath.startsWith(prefix)) {
            final String[] directories = filePath.substring(prefix.length()).split(File.separator);
            return directories.length > 1 ? directories[0] : null;
        } else {
            return null;
        }
    }

    @Nullable
    private Set<String> parseStagedTestNames() {
        try {
            final Status status = git.status().call();
            final Iterable<String> stagedFileNames = Iterables.concat(
                    status.getAdded(),
                    status.getChanged(),
                    status.getRemoved()
            );
            return FluentIterable.from(stagedFileNames)
                    .transform(new Function<String, String>() {
                        @Nullable
                        @Override
                        public String apply(@Nullable final String s) {
                            return parseTestName(testDefinitionsDirectory, s);
                        }
                    })
                    .filter(Predicates.notNull())
                    .toSet();
        } catch (final GitAPIException | NoWorkTreeException e) {
            LOGGER.warn("Failed to call git status", e);
            return null;
        }
    }

    /**
     * Performs git reset --hard and git clean -fd to undo local changes.
     */
    void undoLocalChanges() {

        workspaceProvider.synchronizedOperation(new Callable<Void>() {
            @Override
            public Void call() {
                try {
                    LOGGER.info("Undo local changes due to failure of git operations");
                    try {
                        git.rebase().setOperation(RebaseCommand.Operation.ABORT).call();
                    } catch (WrongRepositoryStateException e) {
                        // ignore rebasing exception when in wrong state
                    }
                    final String remoteBranch = Constants.R_REMOTES + Constants.DEFAULT_REMOTE_NAME + '/' + git.getRepository().getBranch();
                    git.reset().setMode(ResetType.HARD).setRef(remoteBranch).call();
                    git.clean().setCleanDirectories(true).call();
                    try {
                        final ObjectId head = git.getRepository().resolve(Constants.HEAD);
                        LOGGER.info("Undo local changes completed. HEAD is " + head.getName());
                    } catch (final Exception e) {
                        LOGGER.warn("Failed to fetch HEAD", e);
                    }
                } catch (final Exception e) {
                    LOGGER.error("Unable to undo changes", e);
                }
                return null;
            }
        });
    }

    @Override
    public TestVersionResult determineVersions(final String fetchRevision) throws StoreException.ReadException {
        try {
            final RevWalk walk = new RevWalk(git.getRepository());
            final ObjectId commitId = ObjectId.fromString(fetchRevision);
            final RevCommit headTree = walk.parseCommit(commitId);
            final RevTree tree = headTree.getTree();

            // now use a TreeWalk to iterate over all files in the Tree recursively
            // you can set Filters to narrow down the results if needed
            TreeWalk treeWalk = new TreeWalk(git.getRepository());
            treeWalk.addTree(tree);
            treeWalk.setFilter(AndTreeFilter
                .create(PathFilter.create(testDefinitionsDirectory), PathSuffixFilter.create("definition.json")));
            treeWalk.setRecursive(true);

            final List<TestVersionResult.Test> tests = Lists.newArrayList();
            while (treeWalk.next()) {
                final ObjectId id = treeWalk.getObjectId(0);
                // final RevTree revTree = walk.lookupTree(id);

                final String path = treeWalk.getPathString();
                final String[] pieces = path.split("/");
                final String testname = pieces[pieces.length - 2]; // tree / parent directory name

                // testname, blobid pair
                // note this is the blobid hash - not a commit hash
                // RevTree.id and RevBlob.id
                tests.add(new TestVersionResult.Test(testname, id.name()));
            }

            walk.dispose();
            return new TestVersionResult(
                    tests,
                    new Date(Long.valueOf(headTree.getCommitTime()) * 1000 /* convert seconds to milliseconds */),
                    headTree.getAuthorIdent().toExternalString(),
                    headTree.toObjectId().getName(),
                    headTree.getFullMessage()
            );
        } catch (IOException e) {
            throw new StoreException.ReadException(e);
        }
    }

    public void checkoutBranch(final String branchName) {
        Preconditions.checkArgument(
                StringUtils.isEmpty(this.branchName) || this.branchName.equals(branchName),
                "Unable to checkout branch %s because this repository cloned only the branch %s",
                branchName,
                this.branchName
        );

        workspaceProvider.synchronizedOperation(new Callable<Void>() {
            @Override
            public Void call() {
                try {
                    git.branchCreate()
                            .setName(branchName)
                            .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.SET_UPSTREAM)
                            .setStartPoint("origin/" + branchName)
                            .setForce(true)
                            .call();
                    git.checkout().setName(branchName).call();
                } catch (final GitAPIException e) {
                    LOGGER.error("Unable to create/checkout branch " + branchName, e);
                }
                return null;
            }
        });
    }

    Git getGit() {
        return git;
    }

    String getRefName() {
        return refName;
    }

    String getGitUrl() {
        return gitUrl;
    }

    @Override
    public void close() throws IOException {
        // Is this ThreadSafe ?
        git.getRepository().close();
    }

    @Override
    public String getAddTestRevision() {
        return ObjectId.zeroId().name();
    }

    public class GitGcTask implements Runnable {

        @Override
        public void run() {

            workspaceProvider.synchronizedOperation(new Callable<Void>() {
                @Override
                public Void call() {
                    try {
                        LOGGER.info("Start running `git gc` command to clean up git garbage");
                        final Properties call = getGit().gc().call();
                        LOGGER.info("`git gc` has been completed " + call.toString());
                    } catch (final GitAPIException e) {
                        LOGGER.error("Failed to run `git gc` command.", e);
                    }
                    return null;
                }
            });
        }
    }

    public void refresh() {
        workspaceProvider.synchronizedOperation(new Callable<Void>() {
            @Override
            public Void call() {
                try {
                    /** git pull is preferable since it's more efficient **/
                    LOGGER.debug("Started refresh with git pull");
                    final PullResult result = getGit().pull().setProgressMonitor(PROGRESS_MONITOR).setRebase(true).setCredentialsProvider(user).call();
                    if (!result.isSuccessful()) {
                        /** if git pull failed, use git reset **/
                        LOGGER.info("refresh failed. Running undo local changes");
                        undoLocalChanges();
                    }
                    LOGGER.debug("Finished refresh");
                } catch (final Exception e) {
                    LOGGER.error("Error when refreshing git directory " + workspaceProvider.getRootDirectory(), e);
                }
                return null;
            }
        });
    }
}