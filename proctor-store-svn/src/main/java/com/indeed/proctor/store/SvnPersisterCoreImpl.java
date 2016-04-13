package com.indeed.proctor.store;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.indeed.proctor.common.Serializers;
import com.indeed.util.varexport.Export;
import org.apache.commons.io.FileUtils;
import org.apache.commons.pool2.ObjectPool;
import org.apache.log4j.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNRevisionProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author parker
 */
public class SvnPersisterCoreImpl implements SvnPersisterCore, Closeable {
    private static final Logger LOGGER = Logger.getLogger(SvnPersisterCoreImpl.class);

    final ObjectMapper objectMapper = Serializers.strict();

    // template directory suffix to be used when creating the 'template svn repo'
    private static final String TEMPLATE_DIR_SUFFIX = "template";

    private final SVNURL svnUrl;

    private final ObjectPool<SVNClientManager> clientManagerPool;

    // A flag indicating if the WorkspaceProvider is managed by this instance and should be shutdown
    private final boolean shutdownProvider;
    private final SvnWorkspaceProvider workspaceProvider;
    // The template svn directory used as a source when creating and copying tep-user directories
    private final File templateSvnDir;

    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    private final String testDefinitionsDirectory;

    /* Storage Schema:
        ${svnPath}/
            test-definitions/
                test-name-one/
                    definition.json
                    metadata.json
                test-name-two/
                    definition.json
                    metadata.json
    */

    public SvnPersisterCoreImpl(final String svnPath,
                                final String username,
                                final String password,
                                final File tempDir) {
        this(svnPath, username, password, FileBasedProctorStore.DEFAULT_TEST_DEFINITIONS_DIRECTORY, tempDir);
    }

    public SvnPersisterCoreImpl(final String svnPath,
                                final String username,
                                final String password,
                                final String testDefinitionsDirectory,
                                final File tempDir) {
        this(svnPath, username, password, testDefinitionsDirectory, new SvnWorkspaceProviderImpl(tempDir, TimeUnit.DAYS.toMillis(1)), true);
    }

    /**
     * @param svnPath
     * @param username
     * @param password
     * @param testDefinitionsDirectory
     * @param workspaceProvider
     * @param shutdownProvider
     */
    public SvnPersisterCoreImpl(final String svnPath,
                                final String username,
                                final String password,
                                final String testDefinitionsDirectory,
                                final SvnWorkspaceProvider workspaceProvider,
                                final boolean shutdownProvider) {
        try {
            final boolean isFileSystemRepo = svnPath.startsWith("file://");
            final SVNURL url = SVNURL.parseURIDecoded(svnPath);
            this.svnUrl = url;
            if (isFileSystemRepo) {
                FSRepositoryFactory.setup();
                clientManagerPool = SvnObjectPools.clientManagerPool();
            } else {
                DAVRepositoryFactory.setup();
                clientManagerPool = SvnObjectPools.clientManagerPoolWithAuth(username, password);
            }
        } catch (final SVNException e) {
            throw new RuntimeException("Unable to connect to SVN repo at " + svnPath, e);
        }

        this.workspaceProvider = Preconditions.checkNotNull(workspaceProvider, "SvnWorkspaceProvider should not be null");
        this.shutdownProvider = shutdownProvider;
        this.testDefinitionsDirectory = testDefinitionsDirectory;
        try {
            this.templateSvnDir = workspaceProvider.createWorkspace(TEMPLATE_DIR_SUFFIX, true);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not create template directory", e);
        }
    }

    @Override
    public TestVersionResult determineVersions(final String fetchRevision) throws StoreException.ReadException {
        checkShutdownState();

        return doReadWithClientAndRepository(new SvnOperation<TestVersionResult>() {
            @Override
            public TestVersionResult execute(final SVNRepository repo, final SVNClientManager clientManager) throws Exception {
                final String testDefPath = testDefinitionsDirectory;
                /*
                final SVNDirEntry info = repo.info(testDefPath, 2);
                if (info == null) {
                    LOGGER.warn("No test matrix found in " + testDefPath + " under " + svnPath);
                    return null;
                }
                */
                final Long revision = fetchRevision.length() > 0 ? parseRevisionOrDie(fetchRevision) : Long.valueOf(-1);
                final SVNRevision svnRevision = revision.longValue() > 0 ? SVNRevision.create(revision.longValue()) : SVNRevision.HEAD;
                final SVNLogClient logClient = clientManager.getLogClient();
                final FilterableSVNDirEntryHandler handler = new FilterableSVNDirEntryHandler();
                final SVNURL url = SvnPersisterCoreImpl.this.svnUrl.appendPath(testDefPath, false);
                logClient.doList(url,
                                 svnRevision,
                                 svnRevision,
                                 /* fetchlocks */false,
                                 SVNDepth.IMMEDIATES,
                                 SVNDirEntry.DIRENT_KIND | SVNDirEntry.DIRENT_CREATED_REVISION,
                                 handler);


                final SVNDirEntry logEntry = handler.getParent();

                final List<TestVersionResult.Test> tests = Lists.newArrayListWithExpectedSize(handler.getChildren().size());
                for (final SVNDirEntry testDefFile : handler.getChildren()) {
                    if (testDefFile.getKind() != SVNNodeKind.DIR) {
                        LOGGER.warn(String.format("svn kind (%s) is not SVNNodeKind.DIR, skipping %s", testDefFile.getKind(), testDefFile.getURL()));
                        continue;
                    }
                    final String testName = testDefFile.getName();
                    final long testRevision;

                    /*
                        When a svn directory gets copied using svn cp source-dir destination-dir, the revision
                        returned by svn list --verbose directory is different from that of svn log directory/sub-dir
                        The revision returned by svn list is the revision of the on the source-dir instead of the destination-dir
                        The code below checks to see if the directory at the provided revision exists, if it does it will use this revision.
                        If the directory does does not exist, try and identify the correct revision using svn log.
                     */
                    final SVNLogEntry log = getMostRecentLogEntry(clientManager, testDefPath + "/" + testDefFile.getRelativePath(), svnRevision);
                    if (log != null && log.getRevision() != testDefFile.getRevision()) {
                        // The difference in the log.revision and the list.revision can occur during an ( svn cp )
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("svn log r" + log.getRevision() + " is different than svn list r" + testDefFile.getRevision() + " for " + testDefFile.getURL());
                        }
                        testRevision = log.getRevision();
                    } else {
                        testRevision = testDefFile.getRevision();
                    }

                    tests.add(new TestVersionResult.Test(testName, String.valueOf(testRevision)));
                }

                final String matrixRevision = String.valueOf(logEntry.getRevision());
                return new TestVersionResult(
                    tests,
                    logEntry.getDate(),
                    logEntry.getAuthor(),
                    matrixRevision,
                    logEntry.getCommitMessage()
                );
            }

            @Override
            public StoreException handleException(final Exception e) throws StoreException {
                throw new StoreException.ReadException("Unable to read from SVN", e);
            }
        });
    }

    /**
     * Returns the most recent log entry startRevision.
     * startRevision should not be HEAD because the path @HEAD could be deleted.
     *
     * @param path
     * @param startRevision
     * @return
     * @throws SVNException
     */
    private SVNLogEntry getMostRecentLogEntry(final SVNClientManager clientManager, final String path, final SVNRevision startRevision) throws SVNException {
        final String[] targetPaths = {path};

        final SVNLogClient logClient = clientManager.getLogClient();
        final FilterableSVNLogEntryHandler handler = new FilterableSVNLogEntryHandler();

        final int limit = 1;
        // In order to get history is "descending" order, the startRevision should be the one closer to HEAD
        // The path@head could be deleted - must use 'pegRevision' to get history at a deleted path
        logClient.doLog(svnUrl, targetPaths,
                    /* pegRevision */ startRevision,
                    /* startRevision */ startRevision,
                    /* endRevision */ SVNRevision.create(1),
                    /* stopOnCopy */ false,
                    /* discoverChangedPaths */ false,
                    /* includeMergedRevisions */ false,
                    limit,
                    new String[]{SVNRevisionProperty.AUTHOR}, handler);
        if (handler.getLogEntries().isEmpty()) {
            return null;
        } else {
            return handler.getLogEntries().get(0);
        }
    }


    @Override
    public <C> C getFileContents(final Class<C> c, final String[] path_parts, final C defaultValue, final String version) throws StoreException.ReadException, JsonProcessingException {
        checkShutdownState();
        final String path = Joiner.on("/").join(path_parts);
        return doReadWithClientAndRepository(new SvnOperation<C>() {
            @Override
            public C execute(final SVNRepository repo, final SVNClientManager clientManager) throws Exception {
                final Long revision = parseRevisionOrDie(version);
                // use raw repo to check if the path exists
                final SVNNodeKind nodeType = repo.checkPath(path, revision);
                if (SVNNodeKind.NONE.equals(nodeType)) {
                    LOGGER.warn(repo.getLocation() + "/" + path + " @r" + revision + " is SVNNodeKind.NONE returning " + defaultValue);
                    return defaultValue;
                }

                final SVNWCClient client = clientManager.getWCClient();
                final SVNURL url = svnUrl.appendPath(path, true);
                final SVNRevision svnRevision = SVNRevision.create(revision);

                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                client.doGetFileContents(url, svnRevision, svnRevision, /* expandKeywords */ false, baos);
                final C testDefinition = objectMapper.readValue(baos.toByteArray(), c);
                try {
                    baos.close();
                } catch (IOException e) { /* ignore */ }
                return testDefinition;
            }

            @Override
            public StoreException handleException(final Exception e) throws StoreException {
                throw new StoreException.ReadException("Error reading " + path + " from svn", e);
            }
        });
    }

    static class SvnRcsClient implements FileBasedProctorStore.RcsClient {
        private final SVNWCClient wcClient;

        public SvnRcsClient(final SVNWCClient wcClient) {
            this.wcClient = wcClient;
        }

        @Override
        public void add(final File file) throws Exception {
            wcClient.doAdd(file, false, false, false, SVNDepth.UNKNOWN, false, true);
        }

        @Override
        public void delete(File testDefinitionDirectory) throws Exception {
            wcClient.doDelete(testDefinitionDirectory, true, false);
        }

        @Override
        public String getRevisionControlType() {
            return "svn";
        }
    }

    @Override
    public void doInWorkingDirectory(final String username,
                                     final String password,
                                     final String comment,
                                     final String previousVersion,
                                     final FileBasedProctorStore.ProctorUpdater updater) throws StoreException.TestUpdateException {
        checkShutdownState();

        try {
            final File workingDir = this.getWorkingDirForUser(username);

            SvnProctorUtils.doInWorkingDirectory(LOGGER, workingDir, username, password, svnUrl, updater, comment);
        } catch (final SVNAuthenticationException e) {
            throw new StoreException.TestUpdateException("Invalid credentials provided for " + username, e);
        } catch (final SVNException e) {
            throw new StoreException.TestUpdateException("Unable to check out to working directory", e);
        } catch (final IOException e) {
            throw new StoreException.TestUpdateException("Unable to perform operation", e);
        } catch (final Exception e) {
            throw new StoreException.TestUpdateException("Unable to perform operation", e);
        }
    }

    private File getOrCreateSvnUserDirectory(final String username) throws IOException {
        final File userDirectory = workspaceProvider.createWorkspace(username, false);
        try {
            if (userDirectory.list().length == 0) {
                if(templateSvnDir.exists()) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("copying base svn directory (" + templateSvnDir + ") into user " + username + " directory (" + userDirectory + ")");
                    }
                    final boolean preserveFileDate = true;
                    FileUtils.copyDirectory(templateSvnDir, userDirectory, preserveFileDate);
                } else {
                    LOGGER.warn("skipping copying from base directory (" + templateSvnDir + ") because it does not exist");
                }
            }
        } catch (IOException e) {
            LOGGER.error("Exception during copy from (svn base) " + templateSvnDir + " to (" + username + ") " + userDirectory, e);
        }
        return userDirectory;
    }

    @Override
    public String getAddTestRevision() {
        return Long.valueOf(0).toString();
    }

    /**
     * For each user, SVNProctor maintains a working directory for the repository.
     * Commits and modifications happen on the filesystem.
     * So rather than creating a new temp directory and checking it out from SVN
     * for each modification, maintain a temp-directory per user.
     * <p/>
     * After getting the WorkingDir, cleanUpWorkingDir(userDir) should be called
     * to ensure that it's svn is up to date.
     *
     * @param username
     * @return
     * @throws java.io.IOException
     * @throws SVNException
     */
    private File getWorkingDirForUser(final String username) throws IOException {
        final File userDir = getOrCreateSvnUserDirectory(username);
        LOGGER.info("Using " + userDir + " for user " + username);
        return userDir;
    }

    @Override
    public boolean cleanUserWorkspace(final String username) {
        return workspaceProvider.deleteWorkspaceQuietly(username);
    }

    /**
     * Creates a background task that can be scheduled to refresh a template directory used to
     * seed each user workspace during a commit.
     * @return
     */
    public SvnDirectoryRefresher createRefresherTask() {
        return new SvnDirectoryRefresher(shutdown, templateSvnDir, svnUrl, this);
    }

    // backwards compatible with Terminable interface
    public void shutdown() {
        try {
            close();
        } catch (IOException e) {
            LOGGER.error("Ignored exception during close", e);
        }
    }

    @Override
    public void close() throws IOException{
        if (shutdown.compareAndSet(false, true)) {
            LOGGER.info("[close] Deleting working directories");
            if(this.shutdownProvider) {
                LOGGER.info("[close] workspaceProvider.close()");
                workspaceProvider.close();
            }
            clientManagerPool.close();
        }
    }

    private void checkShutdownState() {
        if (shutdown.get()) {
            throw new RuntimeException("SvnPersisterCore is shutdown");
        }
    }

    static Long parseRevisionOrDie(final String revision) throws StoreException.ReadException {
        try {
            final Long rev = Long.parseLong(revision);
            if (rev.longValue() < 0) {
                throw new StoreException.ReadException("Invalid SVN revision " + revision);
            }
            return rev;
        } catch (NumberFormatException e) {
            throw new StoreException.ReadException("Invalid SVN revision " + revision);
        }
    }

    private <T> T doReadWithClientAndRepository(final SvnOperation<T> operation) throws StoreException.ReadException {
        try {
            return doWithClientAndRepository(operation);
        } catch (StoreException e) {
            Throwables.propagateIfInstanceOf(e, StoreException.ReadException.class);
            throw new StoreException.ReadException(e);
        }
    }

    @Override
    public <T> T doWithClientAndRepository(final SvnOperation<T> operation) throws StoreException {
        checkShutdownState();
        SVNClientManager clientManager = null;
        try {
            clientManager = clientManagerPool.borrowObject();
            if (clientManager == null) {
                throw new StoreException.ReadException("Failed to acquire SVNClientManager");
            }
            // do not explicitly close the session because mayReuse=true
            final SVNRepository repository = clientManager.createRepository(svnUrl, true);

            return operation.execute(repository, clientManager);
        } catch (Exception e) {
            Throwables.propagateIfInstanceOf(e, StoreException.class);
            throw operation.handleException(e);
        } finally {
            if (clientManager != null) {
                try {
                    clientManagerPool.returnObject(clientManager);
                } catch (Exception e) {
                    LOGGER.fatal("Failed to return SVNClientManager", e);
                }
            }
        }
    }

    @Override
    public SVNURL getSvnUrl() {
        return svnUrl;
    }

    @Override
    public String toString() {
        return svnUrl.toString();
    }

    @Export(name = "svn-path", doc = "")
    public String getSvnPath() {
        return svnUrl.toString();
    }

    @Export(name = "svn-client-pool-active")
    public int getClientPoolNumActive() {
        return clientManagerPool.getNumActive();
    }

    @Export(name = "svn-client-pool-idle")
    public int getClientPoolNumIdle() {
        return clientManagerPool.getNumIdle();
    }

}
