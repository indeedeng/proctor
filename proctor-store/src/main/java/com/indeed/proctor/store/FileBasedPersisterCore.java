package com.indeed.proctor.store;

import com.fasterxml.jackson.core.JsonProcessingException;

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
    <C> C getFileContents(
            Class<C> c, String[] path, C defaultValue, String revision
    ) throws StoreException.ReadException, JsonProcessingException;

    /**
     * @deprecated use ChangeMetadata
     */
    @Deprecated
    default void doInWorkingDirectory(
            final String username,
            final String password,
            final String comment,
            final String previousVersion,
            final FileBasedProctorStore.ProctorUpdater updater
    ) throws StoreException.TestUpdateException {
        doInWorkingDirectory(username, password, username, comment, previousVersion, updater);
    }

    /**
     * @deprecated use ChangeMetadata
     */
    @Deprecated
    default void doInWorkingDirectory(
            final String username,
            final String password,
            final String author,
            final String comment,
            final String previousVersion,
            final FileBasedProctorStore.ProctorUpdater updater
    ) throws StoreException.TestUpdateException {
        doInWorkingDirectory(new ChangeMetadata(username, password, author, comment), previousVersion, updater);
    }

    void doInWorkingDirectory(
            ChangeMetadata changeMetadata,
            String previousVersion,
            FileBasedProctorStore.ProctorUpdater updater
    ) throws StoreException.TestUpdateException;

    TestVersionResult determineVersions(String fetchRevision) throws StoreException.ReadException;

    String getAddTestRevision();
}
