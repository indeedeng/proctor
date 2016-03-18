package com.indeed.proctor.webapp.db;

import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.SvnProctor;
import org.springframework.beans.factory.FactoryBean;

public class SvnProctorFactory implements FactoryBean<ProctorStore> {
    private String svnPath;
    private String username;
    private String password;
    private String testDefinitionsDirectory;

    public void setSvnPath(final String svnPath) {
        this.svnPath = svnPath;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public void setTestDefinitionsDirectory(final String testDefinitionsDirectory) {
        this.testDefinitionsDirectory = testDefinitionsDirectory;
    }

    @Override
    public ProctorStore getObject() throws Exception {
        return new SvnProctor(svnPath, username, password, testDefinitionsDirectory);
    }

    @Override
    public Class<?> getObjectType() {
        return ProctorStore.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}