package com.indeed.proctor.webapp.db;

import com.indeed.proctor.store.ProctorStore;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.ScheduledExecutorService;

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
    private String scmPath;
    private String scmUsername;
    private String scmPassword;


    @Override
    public StoreFactory getObject() throws Exception {
        if ("svn".equals(revisionControlType)) {
            return new SvnProctorStoreFactory(scheduledExecutorService, cache, tempDirCleanupAgeMinutes,
                                              scmRefreshMinutes, scmPath, scmUsername, scmPassword);
        } else if ("git".equals(revisionControlType)) {
            return new GitProctorStoreFactory(scheduledExecutorService, scmRefreshMinutes, scmPath, scmUsername, scmPassword);
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

    @Value("${scm.refresh.period.minutes:5}")
    public void setScmRefreshMinutes(long scmRefreshMinutes) {
        this.scmRefreshMinutes = scmRefreshMinutes;
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
}
