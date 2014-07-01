package com.indeed.proctor.store;

import java.io.Closeable;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonProcessingException;
import org.eclipse.jgit.lib.Repository;

public class GitPersisterCoreImpl implements GitPersisterCore, Closeable {
    private static final Logger LOGGER = Logger.getLogger(GitPersisterCoreImpl.class);

    private final String gitUrl;
    private final Repository repo;

    public GitPersisterCoreImpl(final String gitUrl, final String username, final String password, final File tempDir) {
        this(gitUrl, username, password, new SvnWorkspaceProviderImpl(tempDir, TimeUnit.DAYS.toMillis(1)), true);
    }

    @Override
    public Repository getRepo() {
        checkShutdownState();
        return repo;
    }

    @Override
    public String getGitUrl() {
        return gitUrl;
    }

    @Override
    public boolean cleanUserWorkspace(String username) {
        return false;
    }

    @Override
    public <C> C getFileContents(Class<C> c, String[] path, C defaultValue, long revision)
            throws StoreException.ReadException, JsonProcessingException {
        return null;
    }

    @Override
    public void doInWorkingDirectory(String username, String password, String comment, long previousVersion,
            FileBasedProctorStore.ProctorUpdater updater) throws StoreException.TestUpdateException {

    }

    @Override
    public FileBasedProctorStore.TestVersionResult determineVersions(long fetchRevision)
            throws StoreException.ReadException {
        return null;
    }

    @Override
    public void close() throws IOException {

    }
}