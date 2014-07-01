package com.indeed.proctor.webapp.db;

import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.GitProctor;
import org.springframework.beans.factory.FactoryBean;

public class GitProctorFactory implements FactoryBean<ProctorStore> {
    private String gitUrl;
    private String username;
    private String password;

    public void setGitUrl(final String gitUrl) {
        this.gitUrl = gitUrl;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    @Override
    public ProctorStore getObject() throws Exception {
        return new GitProctor(gitUrl, username, password);
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