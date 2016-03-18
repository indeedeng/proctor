package com.indeed.proctor.webapp.db;

import com.indeed.proctor.store.ProctorStore;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.FactoryBean;
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

    private boolean cache;
    private long tempDirCleanupAgeMinutes;
    private long scmRefreshMinutes;
    private long scmRefreshSeconds;
    private String scmPath;
    private String scmUsername;
    private String scmPassword;
    private String testDefinitionsDirectory;


    @Override
    public StoreFactory getObject() throws Exception {
        if (scmRefreshMinutes > 0) {
            scmRefreshSeconds = TimeUnit.MINUTES.toSeconds(scmRefreshMinutes);
        }

        if ("svn".equals(revisionControlType)) {
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
                scheduledExecutorService,
                scmRefreshSeconds,
                scmPath,
                scmUsername,
                scmPassword,
                testDefinitionsDirectory);
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
}
