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

/**
 * @author parker
 */
public class SvnProctorStoreFactory implements StoreFactory {
    private static final Logger LOGGER = Logger.getLogger(SvnProctorStoreFactory.class);

    final ScheduledExecutorService executor;

    private boolean cache;
    private  String svnPath;
    private String svnUsername;
    private String svnPassword;

    /* The root directory into which we should put the "qa-matrices" or "trunk-matrices"
     * If not set - a the temp directory will be used
     * */
    File tempRoot;

    private final File implicitTempRoot;

    // The age (in milliseconds) to use when identifying temp directories that can be cleaned up
    private long tempDirCleanupAgeMillis = TimeUnit.DAYS.toMillis(1);

    // The period to use when scheduling a refresh of the svn directory
    private long svnRefreshMillis = TimeUnit.MINUTES.toMillis(5);

    public SvnProctorStoreFactory(final ScheduledExecutorService executor, final boolean cache, final long tempDirCleanupAgeMinutes,
                                  final long svnRefreshMinutes, final String svnPath, final String svnUsername, final String svnPassword) throws IOException, ConfigurationException {
        this.executor = executor;
        this.cache = cache;
        this.tempDirCleanupAgeMillis = TimeUnit.MINUTES.toMillis(tempDirCleanupAgeMinutes);
        this.svnRefreshMillis= TimeUnit.MINUTES.toMillis(svnRefreshMinutes);
        this.svnPath = svnPath;
        this.svnUsername = svnUsername;
        this.svnPassword = svnPassword;
        this.implicitTempRoot = identifyImplicitTempRoot();
    }

    public ProctorStore getTrunkStore() {
        return createStore("/trunk/matrices");
    }

    public ProctorStore getQaStore() {
        return createStore("/branches/deploy/qa/matrices");
    }

    public ProctorStore getProductionStore() {
        return createStore("/branches/deploy/production/matrices");
    }

    public ProctorStore createStore(final String relativePath) {
        Preconditions.checkArgument(tempDirCleanupAgeMillis > 0, "tempDirCleanupAgeMillis %s must be greater than zero", tempDirCleanupAgeMillis);
        final File tempDirectory = createTempDirectoryForPath(relativePath);

        Preconditions.checkArgument(!CharMatcher.WHITESPACE.matchesAllOf(Strings.nullToEmpty(svnPath)), "svn.path property cannot be empty");
        // TODO (parker) 9/13/12 - sanity check that path + relative path make a valid url
        final String fullPath = svnPath + relativePath;

        final SvnWorkspaceProviderImpl provider = new SvnWorkspaceProviderImpl(tempDirectory, tempDirCleanupAgeMillis);
        final SvnPersisterCoreImpl svncore = new SvnPersisterCoreImpl(fullPath, svnUsername, svnPassword, provider, true /* shutdown provider */);

        // actively clean up directories every hour: (not relying on cache eviction)
        final long cleanupScheduleMillis = Math.min(TimeUnit.HOURS.toMillis(1), tempDirCleanupAgeMillis);
        LOGGER.info("Scheduling SvnWorkspaceProvider every " + cleanupScheduleMillis + " milliseconds for dir: " + tempDirectory + " with age millis " + tempDirCleanupAgeMillis);
        executor.scheduleWithFixedDelay(provider, cleanupScheduleMillis, cleanupScheduleMillis, TimeUnit.MILLISECONDS);

        if(svnRefreshMillis > 0) {
            final SvnDirectoryRefresher refresher = svncore.createRefresherTask();
            LOGGER.info("Scheduling SvnDirectoryRefresher every " + svnRefreshMillis + " milliseconds for dir: " + refresher.getDirectoryPath());
            executor.scheduleWithFixedDelay(refresher, svnRefreshMillis, svnRefreshMillis, TimeUnit.MILLISECONDS);
        }

        final SvnProctor store = new SvnProctor(cache ? new CachedSvnPersisterCore(svncore) : svncore);
        final VarExporter exporter = VarExporter.forNamespace(SvnProctor.class.getSimpleName()).includeInGlobal();
        final String prefix = relativePath.substring(1).replace('/', '-');
        exporter.export(store, prefix + "-");
        return store;
    }

    /**
     * Identify the root-directory for TempFiles
     * @return
     */
    private File identifyImplicitTempRoot() throws IOException {
        final File tempFile = File.createTempFile("implicit", SvnProctorStoreFactory.class.getSimpleName());

        tempFile.delete();
        return tempFile.getParentFile();
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

    public boolean isCache() {
        return cache;
    }

    public File getTempRoot() {
        return tempRoot;
    }

    public void setTempRoot(File tempRoot) {
        this.tempRoot = tempRoot;
    }
}
