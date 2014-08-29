package com.indeed.proctor.store;

import org.codehaus.jackson.JsonProcessingException;

import java.io.Closeable;

/**
 * @author parker
 */
public interface FileBasedPersisterCore extends Closeable {
    /**
     * Parses a JSON class from a specified path relative to the root of the base directory.
     *
     * @param c
     * @param path
     * @param defaultValue
     * @return
     * @throws com.indeed.proctor.store.StoreException.ReadException
     */
    <C> C getFileContents(Class<C> c, String[] path, C defaultValue, String revision) throws StoreException.ReadException, JsonProcessingException;

    void doInWorkingDirectory(String username, String password, String comment, String previousVersion, FileBasedProctorStore.ProctorUpdater updater) throws StoreException.TestUpdateException;

    TestVersionResult determineVersions(String fetchRevision) throws StoreException.ReadException;

    String getAddTestRevision();
}
