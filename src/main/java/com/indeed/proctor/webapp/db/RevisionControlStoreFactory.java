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

    @Value("${revision.control:svn}")
    private String revisionControlType;
    private ScheduledExecutorService scheduledExecutorService;

    private boolean cache;
    private long tempDirCleanupAgeMinutes;
    private long svnRefreshMinutes;
    private String svnPath;
    private String svnUsername;
    private String svnPassword;


    @Override
    public StoreFactory getObject() throws Exception {
        if ("svn".equals(revisionControlType)) {
            return new SvnProctorStoreFactory(scheduledExecutorService, cache, tempDirCleanupAgeMinutes,
                                              svnRefreshMinutes, svnPath, svnUsername, svnPassword);
        } else if ("git".equals(revisionControlType)) {
            LOGGER.error("Git is not yet supported as a revision control type.");
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

    @Value("${svn.refresh.period.minutes:5}")
    public void setSvnRefreshMinutes(long svnRefreshMinutes) {
        this.svnRefreshMinutes = svnRefreshMinutes;
    }

    @Value("${svn.path}")
    public void setSvnPath(final String svnPath) {
        this.svnPath = svnPath;
    }

    @Value("${svn.login}")
    public void setSvnUsername(final String svnUsername) {
        this.svnUsername = svnUsername;
    }

    @Value("${svn.password}")
    public void setSvnPassword(final String svnPassword) {
        this.svnPassword = svnPassword;
    }
}
