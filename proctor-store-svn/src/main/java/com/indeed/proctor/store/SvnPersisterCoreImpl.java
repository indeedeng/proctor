package com.indeed.proctor.store;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.indeed.util.varexport.Export;
import com.indeed.proctor.common.Serializers;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.tmatesoft.svn.core.SVNAuthenticationException;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNDirEntry;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNRevisionProperty;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.io.ISVNSession;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
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

    private final SVNClientManager clientManager;
    private final SVNRepository repo;


    // A flag indicating if the WorkspaceProvider is managed by this instance and should be shutdown
    private final boolean shutdownProvider;
    private final SvnWorkspaceProvider workspaceProvider;
    // The template svn directory used as a source when creating and copying tep-user directories
    private final File templateSvnDir;

    private final AtomicBoolean shutdown = new AtomicBoolean(false);

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

    public SvnPersisterCoreImpl(final String svnPath, final String username, final String password, final File tempDir) {
        this(svnPath, username, password, new SvnWorkspaceProviderImpl(tempDir, TimeUnit.DAYS.toMillis(1)), true);
    }

    /**
     * @param svnPath
     * @param username
     * @param password
     * @param workspaceProvider
     * @param shutdownProvider
     * @param
     */
    public SvnPersisterCoreImpl(final String svnPath, final String username, final String password,
                                final SvnWorkspaceProvider workspaceProvider, boolean shutdownProvider) {
        try {
            final boolean isFileSystemRepo = svnPath.startsWith("file://");
            final SVNURL url = SVNURL.parseURIDecoded(svnPath);
            this.svnUrl = url;
            if (isFileSystemRepo) {
                FSRepositoryFactory.setup();
                repo = FSRepositoryFactory.create(url, ISVNSession.KEEP_ALIVE);
                clientManager = SVNClientManager.newInstance();
            } else {
                DAVRepositoryFactory.setup();
                repo = SVNRepositoryFactory.create(url, ISVNSession.KEEP_ALIVE);
                final BasicAuthenticationManager authManager = new BasicAuthenticationManager(username, password);
                repo.setAuthenticationManager(authManager);
                clientManager = SVNClientManager.newInstance(null, authManager);
            }
        } catch (final SVNException e) {
            throw new RuntimeException("Unable to connect to SVN repo at " + svnPath, e);
        }

        this.workspaceProvider = Preconditions.checkNotNull(workspaceProvider, "SvnWorkspaceProvider should not be null");
        this.shutdownProvider = shutdownProvider;
        try {
            this.templateSvnDir = workspaceProvider.createWorkspace(TEMPLATE_DIR_SUFFIX, true);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not create template directory", e);
        }
    }

    @Override
    public FileBasedProctorStore.TestVersionResult determineVersions(final long fetchRevision) throws StoreException.ReadException {
        checkShutdownState();
        try {
            final String testDefPath = FileBasedProctorStore.TEST_DEFINITIONS_DIRECTORY;
            /*
            final SVNDirEntry info = repo.info(testDefPath, 2);
            if (info == null) {
                LOGGER.warn("No test matrix found in " + testDefPath + " under " + svnPath);
                return null;
            }
            */

            final SVNRevision svnRevision = fetchRevision > 0 ? SVNRevision.create(fetchRevision) : SVNRevision.HEAD;
            final SVNLogClient logClient = clientManager.getLogClient();
            final FilterableSVNDirEntryHandler handler = new FilterableSVNDirEntryHandler();
            final SVNURL url = this.svnUrl.appendPath(testDefPath, false);
            logClient.doList(url,
                             svnRevision,
                             svnRevision,
                             /* fetchlocks */false,
                             SVNDepth.IMMEDIATES,
                             SVNDirEntry.DIRENT_KIND | SVNDirEntry.DIRENT_CREATED_REVISION,
                             handler);


            final SVNDirEntry logEntry = handler.getParent();
            final long revision = logEntry.getRevision();

            final List<FileBasedProctorStore.TestVersionResult.Test> tests = Lists.newArrayListWithExpectedSize(handler.getChildren().size());
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
                final SVNLogEntry log = getMostRecentLogEntry(testDefPath + "/" + testDefFile.getRelativePath(), svnRevision);
                if (log != null && log.getRevision() != testDefFile.getRevision()) {
                    // The difference in the log.revision and the list.revision can occur during an ( svn cp )
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("svn log r" + log.getRevision() + " is different than svn list r" + testDefFile.getRevision() + " for " + testDefFile.getURL());
                    }
                    testRevision = log.getRevision();
                } else {
                    testRevision = testDefFile.getRevision();
                }

                tests.add(new FileBasedProctorStore.TestVersionResult.Test(testName, testRevision));
            }

            return new FileBasedProctorStore.TestVersionResult(
                tests,
                logEntry.getDate(),
                logEntry.getAuthor(),
                revision,
                logEntry.getCommitMessage()
            );

        } catch (final SVNException e) {
            LOGGER.error("Unable to read from SVN", e);
            return null;
        }
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
    private SVNLogEntry getMostRecentLogEntry(final String path, final SVNRevision startRevision) throws SVNException {
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
    public <C> C getFileContents(final Class<C> c, final String[] path_parts, final C defaultValue, final long revision) throws StoreException.ReadException, JsonProcessingException {
        checkShutdownState();
        final String path = Joiner.on("/").join(path_parts);
        try {
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
        } catch (SVNException e) {
            throw new StoreException.ReadException("Error reading " + path + " from svn", e);
        } catch (IOException e) {
            throw new StoreException.ReadException("Error reading " + path + " from svn", e);
        }
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
    }

    @Override
    public void doInWorkingDirectory(final String username,
                                     final String password,
                                     final String comment,
                                     final long previousVersion,
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
        return new SvnDirectoryRefresher(shutdown, templateSvnDir, svnUrl, clientManager);
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
        }
    }

    private void checkShutdownState() {
        if (shutdown.get()) {
            throw new RuntimeException("SvnPersisterCore is shutdown");
        }
    }

    @Override
    public SVNRepository getRepo() {
        checkShutdownState();
        return repo;
    }

    @Override
    public SVNClientManager getClientManager() {
        checkShutdownState();
        return clientManager;
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
}
