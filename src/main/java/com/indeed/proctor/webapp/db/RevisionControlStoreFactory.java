package com.indeed.proctor.webapp.db;

import com.indeed.proctor.store.ProctorStore;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.FactoryBean;

import java.io.File;
import java.util.concurrent.ScheduledExecutorService;

/**
 */
public class RevisionControlStoreFactory implements FactoryBean<StoreFactory> {
    private final Logger LOGGER = Logger.getLogger(RevisionControlStoreFactory.class);

    private String revisionControlType;
    private String revisionControlConfigFile;
    private ScheduledExecutorService scheduledExecutorService;


    @Override
    public StoreFactory getObject() throws Exception {
        if ("svn".equals(revisionControlType)) {
            return new SvnProctorStoreFactory(revisionControlConfigFile, scheduledExecutorService);
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

    public void setRevisionControlConfigFile(String revisionControlConfigFile) {
        this.revisionControlConfigFile = revisionControlConfigFile;
    }

    public String getRevisionControlConfigFile() {
        return revisionControlConfigFile;
    }

    public void setScheduledExecutorService(ScheduledExecutorService scheduledExecutorService) {
        this.scheduledExecutorService = scheduledExecutorService;
    }

    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduledExecutorService;
    }
}
