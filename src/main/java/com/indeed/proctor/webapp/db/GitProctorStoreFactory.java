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

    /* The root directory into which we should put the "qa-matrices" or "trunk-matrices"
     * If not set - a the temp directory will be used
     * */
    File tempRoot;

    private final File implicitTempRoot;

    // The age (in milliseconds) to use when identifying temp directories that can be cleaned up
    private long tempDirCleanupAgeMillis = TimeUnit.DAYS.toMillis(1);

    // The period to use when scheduling a refresh of the svn directory
    private long gitRefreshMillis = TimeUnit.MINUTES.toMillis(5);

    public GitProctorStoreFactory(final ScheduledExecutorService executor, final long gitRefreshMinutes, final String gitUrl, final String gitUsername, final String gitPassword) throws IOException, ConfigurationException {
        this.executor = executor;
        this.gitUrl = gitUrl;
        this.gitUsername = gitUsername;
        this.gitPassword = gitPassword;
        this.gitRefreshMillis= TimeUnit.MINUTES.toMillis(gitRefreshMinutes);
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

        final GitWorkspaceProviderImpl provider = new GitWorkspaceProviderImpl(tempDirectory, tempDirCleanupAgeMillis);
        final GitProctorCore gitCore = new GitProctorCore(gitUrl, gitUsername, gitPassword, provider);
        
        // actively clean up directories every hour: (not relying on cache eviction)
        final long cleanupScheduleMillis = Math.min(TimeUnit.HOURS.toMillis(1), tempDirCleanupAgeMillis);
        LOGGER.info("Scheduling GitWorkspaceProvider every " + cleanupScheduleMillis + " milliseconds for dir: " +
                tempDirectory + " with age millis " + tempDirCleanupAgeMillis);
        executor.scheduleWithFixedDelay(provider, cleanupScheduleMillis, cleanupScheduleMillis, TimeUnit.MILLISECONDS);

        if(gitRefreshMillis > 0) {
            final GitDirectoryRefresher refresher = gitCore.createRefresherTask
                    (gitUsername, gitPassword);
            LOGGER.info("Scheduling GitDirectoryRefresher every " + gitRefreshMillis + " milliseconds for dir: " + refresher.getDirectoryPath());
            executor.scheduleWithFixedDelay(refresher, gitRefreshMillis, gitRefreshMillis, TimeUnit.MILLISECONDS);
        }

        final String branchName = relativePath.substring(relativePath.lastIndexOf("/")+1);
        final GitProctor store = new GitProctor(gitCore, branchName);
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
