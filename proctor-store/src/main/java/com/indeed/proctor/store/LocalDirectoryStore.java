package com.indeed.proctor.store;

import java.io.File;
import java.util.List;

public class LocalDirectoryStore extends FileBasedProctorStore {

    final File baseDir;

    public LocalDirectoryStore(File baseDir) {
        super(new LocalDirectoryCore(baseDir));
        if(!baseDir.isDirectory()) {
            throw new IllegalArgumentException("Base dir " + baseDir + " is not a directory");
        }
        this.baseDir = baseDir;
    }

    @Override
    public long getLatestVersion() throws StoreException {
        return -1;
    }

    @Override
    public boolean cleanUserWorkspace(String username) {
        // no op
        return true;
    }

    @Override
    public List<Revision> getHistory(final String test, final int ignoredStart, final int limit) throws StoreException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public List<Revision> getHistory(String test, long revision, int start, int limit) throws StoreException {
        throw new UnsupportedOperationException("Not implemented yet");
    }
    @Override
    public List<Revision> getMatrixHistory(final int start, final int limit) throws StoreException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void verifySetup() throws StoreException {
        if(!this.baseDir.isDirectory()) {
            throw new RuntimeException("Base dir (" + this.baseDir.getPath() + ") is not a directory.");
        }
        if(!this.baseDir.canRead()) {
            throw new RuntimeException("Cannot read from " + this.baseDir.getPath());
        }
        if(!this.baseDir.canWrite()) {
            throw new RuntimeException("Cannot write to " + this.baseDir.getPath());
        }
    }

    @Override
    public String toString() {
        return baseDir.getAbsolutePath();
    }
}
