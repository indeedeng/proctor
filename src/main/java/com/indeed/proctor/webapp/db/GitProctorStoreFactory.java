package com.indeed.proctor.webapp.db;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.indeed.util.varexport.VarExporter;
import com.indeed.proctor.store.*;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GitProctorStoreFactory implements StoreFactory {
    private static final Logger LOGGER = Logger.getLogger(GitProctorStoreFactory.class);

    final ScheduledExecutorService executor;

    private String gitUrl;
    private String gitUsername;
    private String gitPassword;
    private String testDefinitionsDirectory = FileBasedProctorStore.DEFAULT_TEST_DEFINITIONS_DIRECTORY;

    /* The root directory into which we should put the "qa-matrices" or "trunk-matrices"
     * If not set - a the temp directory will be used
     * */
    File tempRoot;

    private final File implicitTempRoot;

    // The period to use when scheduling a refresh of the git directory
    private long gitRefreshMillis = TimeUnit.SECONDS.toMillis(300);

    public GitProctorStoreFactory(final ScheduledExecutorService executor,
                                  final long gitRefreshSeconds,
                                  final String gitUrl,
                                  final String gitUsername,
                                  final String gitPassword,
                                  final String testDefinitionsDirectory) throws IOException, ConfigurationException {
        this.executor = executor;
        this.gitUrl = gitUrl;
        this.gitUsername = gitUsername;
        this.gitPassword = gitPassword;
        this.testDefinitionsDirectory = testDefinitionsDirectory;
        this.gitRefreshMillis = TimeUnit.SECONDS.toMillis(gitRefreshSeconds);
        this.implicitTempRoot = identifyImplicitTempRoot();
    }

    public ProctorStore getTrunkStore() {
        return createStore("proctor/git/trunk");
    }

    public ProctorStore getQaStore() {
        return createStore("proctor/git/qa");
    }

    public ProctorStore getProductionStore() {
        return createStore("proctor/git/production");
    }
    
    public ProctorStore createStore(final String relativePath) {

        final File tempDirectory = createTempDirectoryForPath(relativePath);

        Preconditions.checkArgument(!CharMatcher.WHITESPACE.matchesAllOf(Strings.nullToEmpty(gitUrl)), "scm.path property cannot be empty");

        final GitWorkspaceProviderImpl provider = new GitWorkspaceProviderImpl(tempDirectory);
        final GitProctorCore gitCore = new CachedGitProctorCore(gitUrl, gitUsername, gitPassword, testDefinitionsDirectory, provider);

        if(gitRefreshMillis > 0) {
            final GitDirectoryRefresher refresher = gitCore.createRefresherTask
                    (gitUsername, gitPassword);
            LOGGER.info("Scheduling GitDirectoryRefresher every " + gitRefreshMillis + " milliseconds for dir: " + refresher.getDirectoryPath());
            executor.scheduleWithFixedDelay(refresher, TimeUnit.SECONDS.toMillis(60), gitRefreshMillis, TimeUnit.MILLISECONDS);
        }

        final String branchName = relativePath.substring(relativePath.lastIndexOf("/")+1);
        final GitProctor store = new GitProctor(gitCore, testDefinitionsDirectory, branchName);
        final String prefix = relativePath.replace('/', '-');
        final VarExporter exporter = VarExporter.forNamespace(GitProctor.class.getSimpleName()).includeInGlobal();
        exporter.export(store, prefix + "-");

        return store;
    }

    private File createTempDirectoryForPath(final String relativePath) {
        // replace "/" with "-" omit first "/" but omitEmptyStrings
        final String dirName = CharMatcher.is(File.separatorChar).trimAndCollapseFrom(relativePath, '-');
        final File parent = tempRoot != null ? tempRoot : implicitTempRoot;
        final File temp = new File(parent, dirName);
        if(temp.exists()) {
           if(!temp.isDirectory()) {
               throw new IllegalStateException(temp + " exists but is not a directory");
           }
        } else {
            if(!temp.mkdir()) {
                throw new IllegalStateException("Could not create directory : " + temp);
            }
        }
        return temp;
    }

    /**
     * Identify the root-directory for TempFiles
     * @return
     */
    private File identifyImplicitTempRoot() throws IOException {
        final File tempFile = File.createTempFile("implicit", GitProctorStoreFactory.class.getSimpleName());

        tempFile.delete();
        return tempFile.getParentFile();
    }

    public File getTempRoot() {
        return tempRoot;
    }

    public void setTempRoot(File tempRoot) {
        this.tempRoot = tempRoot;
    }
}
