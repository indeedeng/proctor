package com.indeed.proctor.store;

import javax.annotation.Nonnull;
import java.io.File;
import java.util.List;
import java.util.Map;

public class LocalDirectoryStore extends FileBasedProctorStore {

    private final File baseDir;

    public LocalDirectoryStore(final File baseDir) {
        this(baseDir, FileBasedProctorStore.DEFAULT_TEST_DEFINITIONS_DIRECTORY);
    }

    public LocalDirectoryStore(final File baseDir, final String testDefinitionsDirectory) {
        super(new LocalDirectoryCore(baseDir, testDefinitionsDirectory));
        if (!baseDir.isDirectory()) {
            throw new IllegalArgumentException("Base dir " + baseDir + " is not a directory");
        }
        this.baseDir = baseDir;
    }

    @Override
    public String getLatestVersion() throws StoreException {
        return "";
    }

    @Override
    public boolean cleanUserWorkspace(String username) {
        // no op
        return true;
    }

    @Nonnull
    @Override
    public List<Revision> getHistory(final String test, final int ignoredStart, final int limit) throws StoreException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Nonnull
    @Override
    public List<Revision> getHistory(String test, String revision, int start, int limit) throws StoreException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public RevisionDetails getRevisionDetails(final String revisionId) throws StoreException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Nonnull
    @Override
    public List<Revision> getMatrixHistory(final int start, final int limit) throws StoreException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Nonnull
    @Override
    public Map<String, List<Revision>> getAllHistories() throws StoreException {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public void refresh() throws StoreException {
        /* do nothing since it doesn't need to */
    }

    @Override
    public void verifySetup() throws StoreException {
        if (!this.baseDir.isDirectory()) {
            throw new RuntimeException("Base dir (" + this.baseDir.getPath() + ") is not a directory.");
        }
        if (!this.baseDir.canRead()) {
            throw new RuntimeException("Cannot read from " + this.baseDir.getPath());
        }
        if (!this.baseDir.canWrite()) {
            throw new RuntimeException("Cannot write to " + this.baseDir.getPath());
        }
    }

    @Override
    public String toString() {
        return baseDir.getAbsolutePath();
    }

    @Override
    public String getName() {
        return "";
    }
}
