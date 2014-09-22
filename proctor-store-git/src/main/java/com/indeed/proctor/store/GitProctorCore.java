package com.indeed.proctor.store;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.TextProgressMonitor;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.indeed.proctor.common.Serializers;

public class GitProctorCore implements FileBasedPersisterCore {
    private static final Logger LOGGER = Logger.getLogger(GitProctorCore.class);
    private final String username;
    private final String password;

    private Git git;
    private final String gitUrl;
    private final String refName;
    private final GitWorkspaceProvider workspaceProvider;

    public GitProctorCore(final String gitUrl, final String username, final String password,
            final File tempDir) {
        this(gitUrl, username, password, new GitWorkspaceProviderImpl(tempDir, TimeUnit.DAYS.toMillis(1)));
    }

    /**
     * @param gitUrl
     * @param username
     * @param password
     * @param
     */
    public GitProctorCore(final String gitUrl, final String username, final String password,
            final GitWorkspaceProviderImpl workspaceProvider) {
        this.gitUrl = gitUrl;
        this.refName = Constants.HEAD;
        this.workspaceProvider = Preconditions
                .checkNotNull(workspaceProvider, "GitWorkspaceProvider should not be null");
        this.username = username;
        this.password = password;

        initializeRepository();
    }

    void initializeRepository() {
        UsernamePasswordCredentialsProvider user = new UsernamePasswordCredentialsProvider(username, password);
        File workingDir = workspaceProvider.getRootDirectory();
        File gitDirectory = new File(workingDir, ".git");
        LOGGER.info("working dir: " + workingDir.getAbsolutePath());

        try {
            if (gitDirectory.exists()) {
                try {
                    git = Git.open(workingDir);
                    git.pull().setCredentialsProvider(user).call();
                } catch (Exception e) {
                    workspaceProvider.cleanWorkingDirectory();
                    CloneCommand gitCommand = Git.cloneRepository()
                            .setURI(gitUrl)
                            .setDirectory(workingDir)
                            .setProgressMonitor(new TextProgressMonitor())
                            .setCredentialsProvider(user);
                    git = gitCommand.call();
                }
            } else {
                git = Git.cloneRepository()
                        .setURI(gitUrl)
                        .setDirectory(workingDir)
                        .setProgressMonitor(new TextProgressMonitor())
                        .setCredentialsProvider(user)
                        .call();
            }
        } catch (GitAPIException e) {
            LOGGER.error("Unable to clone git repository at " + gitUrl);
        }

        try {
            git.fetch()
                    .setProgressMonitor(new TextProgressMonitor())
                    .setCredentialsProvider(user)
                    .call();
        } catch (GitAPIException e) {
            LOGGER.error("Unable to fetch from " + gitUrl);
        }
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
        ObjectLoader loader = git.getRepository().open(blobId);
        final ObjectMapper mapper = Serializers.lenient();
        return mapper.readValue(loader.getBytes(), c);
    }

    public boolean cleanUserWorkspace(String user) {
        return workspaceProvider.deleteWorkspaceQuietly(user);
    }

    /**
     * Creates a background task that can be scheduled to refresh a template directory used to
     * seed each user workspace during a commit.
     * @return
     */
    public GitDirectoryRefresher createRefresherTask(String username, String password) {
        return new GitDirectoryRefresher(workspaceProvider.getRootDirectory(), git, username, password);
    }

    static class GitRcsClient implements FileBasedProctorStore.RcsClient {
        private final Git git;

        public GitRcsClient(final Git git) {
            this.git = git;
        }

        @Override
        public void add(final File file) throws Exception {
            git.add().addFilepattern("test-definitions/" + file.getAbsoluteFile().getParentFile().getName() + "/" +
                    file.getName()).call();
        }

        @Override
        public void delete(File testDefinitionDirectory) throws Exception {
            for (File file : testDefinitionDirectory.listFiles()) {
                git.rm().addFilepattern("test-definitions/" + testDefinitionDirectory.getName()
                        + "/" + file.getName()).call();
            }
        }

        @Override
        public String getRevisionControlType() {
            return "git";
        }
    }

    @Override
    public void doInWorkingDirectory(String username,
                                     String password,
                                     String comment,
                                     String previousVersion,
                                     FileBasedProctorStore.ProctorUpdater updater) throws StoreException.TestUpdateException {
        UsernamePasswordCredentialsProvider user = new UsernamePasswordCredentialsProvider(username, password);

        final FileBasedProctorStore.RcsClient rcsClient = new GitProctorCore.GitRcsClient(git);
        final File workingDir = workspaceProvider.getRootDirectory();
        final boolean thingsChanged;
        try {
            thingsChanged = updater.doInWorkingDirectory(rcsClient, workingDir);
            if (thingsChanged) {
                git.commit().setMessage(comment).call();
                git.push().setCredentialsProvider(user).call();
            }
        } catch (final GitAPIException e) {
            throw new StoreException.TestUpdateException("Unable to commit/push changes", e);
        } catch (final Exception e) {
            throw new StoreException.TestUpdateException("Unable to perform operation", e);
        }
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
                    .create(PathFilter.create("test-definitions"), PathSuffixFilter.create("definition.json")));
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
}