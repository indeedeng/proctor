package com.indeed.proctor.webapp.db;

import com.google.common.base.Preconditions;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.webapp.extensions.GlobalCacheStore;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 */
public class RevisionControlStoreFactory implements FactoryBean<StoreFactory> {
    private final Logger LOGGER = Logger.getLogger(RevisionControlStoreFactory.class);

    @Value("${revision.control}")
    private String revisionControlType;
    private ScheduledExecutorService scheduledExecutorService;

    private boolean cache;  // only svn
    private long tempDirCleanupAgeMinutes; // only svn
    private long scmRefreshMinutes; // only svn
    private long scmRefreshSeconds; // only svn
    private String scmPath;
    private String scmUsername;
    private String scmPassword;
    private String testDefinitionsDirectory;
    private String tempRootDirectory;  // only git

    private int gitDirectoryLockTimeoutSeconds;
    private int gitPullPushTimeoutSeconds;
    private int gitCloneTimeoutSeconds;
    private boolean gitCleanInitialization;

    private GlobalCacheStore globalCacheStore;

    @Override
    public StoreFactory getObject() throws Exception {
        if ("svn".equals(revisionControlType)) {
            Preconditions.checkArgument(
                    globalCacheStore == null,
                    "Global cache is not supported in SVN"
            );
            if (scmRefreshMinutes > 0) {
                scmRefreshSeconds = TimeUnit.MINUTES.toSeconds(scmRefreshMinutes);
            }
            return new SvnProctorStoreFactory(
                    scheduledExecutorService,
                    cache,
                    tempDirCleanupAgeMinutes,
                    scmRefreshSeconds,
                    scmPath,
                    scmUsername,
                    scmPassword,
                    testDefinitionsDirectory);
        } else if ("git".equals(revisionControlType)) {
            return new GitProctorStoreFactory(
                    scmPath,
                    scmUsername,
                    scmPassword,
                    testDefinitionsDirectory,
                    tempRootDirectory,
                    gitDirectoryLockTimeoutSeconds,
                    gitPullPushTimeoutSeconds,
                    gitCloneTimeoutSeconds,
                    gitCleanInitialization,
                    globalCacheStore);
        }
        return null;
    }

    @Override
    public Class<?> getObjectType() {
        return ProctorStore.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Value("${revision.control}")
    public void setRevisionControlType(String revisionControlType) {
        this.revisionControlType = revisionControlType;
    }

    public String getRevisionControlType() {
        return revisionControlType;
    }

    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }

    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }

    @Value("${svn.cache:true}")
    public void setCache(boolean cache) {
        this.cache = cache;
    }

    @Value("${svn.tempdir.max.age.minutes:1440}")
    public void setTempDirCleanupAgeMinutes(long tempDirCleanupAgeMinutes) {
        this.tempDirCleanupAgeMinutes = tempDirCleanupAgeMinutes;
    }

    @Deprecated
    @Value("${scm.refresh.period.minutes:-1}")
    /**
     * @deprecated use scm.refresh.period.seconds instead
     */
    public void setScmRefreshMinutes(long scmRefreshMinutes) {
        this.scmRefreshMinutes = scmRefreshMinutes;
    }

    @Value("${scm.refresh.period.seconds:300}")
    public void setScmRefreshSeconds(long scmRefreshSeconds) {
        this.scmRefreshSeconds = scmRefreshSeconds;
    }

    @Value("${scm.path}")
    public void setScmPath(final String scmPath) {
        this.scmPath = scmPath;
    }

    @Value("${scm.login}")
    public void setScmUsername(final String scmUsername) {
        this.scmUsername = scmUsername;
    }

    @Value("${scm.password}")
    public void setScmPassword(final String scmPassword) {
        this.scmPassword = scmPassword;
    }

    @Value("${test.definitions.directory:test-definitions}")
    public void setTestDefinitionsDirectory(final String testDefinitionsDirectory) {
        this.testDefinitionsDirectory = testDefinitionsDirectory;
    }

    @Value("${temp.root.directory:}")
    public void setTempRootDirectory(final String tempRootDirectory) {
        this.tempRootDirectory = tempRootDirectory;
    }

    @Value("${git.timeout.seconds.lock.directory:90}")
    public void setGitDirectoryLockTimeoutSeconds(final int seconds) {
        gitDirectoryLockTimeoutSeconds = seconds;
    }

    @Value("${git.timeout.seconds.operation.pull-push:45}")
    public void setGitPullPushTimeoutSeconds(final int seconds) {
        gitPullPushTimeoutSeconds = seconds;
    }

    @Value("${git.timeout.seconds.operation.clone:180}")
    public void setGitCloneTimeoutSeconds(final int seconds) {
        gitCloneTimeoutSeconds = seconds;
    }

    @Value("${git.initialize.clean:false}")
    public void setGitCleanInitialization(final boolean cleanInitialization) {
        gitCleanInitialization = cleanInitialization;
    }

    @Autowired(required = false)
    public void setGlobalCacheStore(final GlobalCacheStore globalCacheStore) {
        this.globalCacheStore = globalCacheStore;
    }
}
