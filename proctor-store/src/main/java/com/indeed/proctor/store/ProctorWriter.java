package com.indeed.proctor.store;

import com.indeed.proctor.common.model.TestDefinition;

import java.util.Map;

/**
 * @author parker
 */
public interface ProctorWriter {

    void verifySetup() throws StoreException;

    boolean cleanUserWorkspace(String username);

    /**
     * {@see other ProctorWriter.updateTestDefinition}, using username as author
     */
    default void updateTestDefinition(
            final String username,
            final String password,
            final String previousVersion,
            final String testName,
            final TestDefinition testDefinition,
            final Map<String, String> metadata,
            final String comment
    ) throws StoreException.TestUpdateException {
        updateTestDefinition(username, password, username, previousVersion, testName, testDefinition, metadata, comment);
    }

    /**
     * Updates a test with testName that already exists in this store
     *
     * Fails with Exception when testName does not exist, or
     * neither testDefinition nor metadata has changes to current version
     *
     * @throws StoreException.TestUpdateException
     */
    void updateTestDefinition(
            String username,
            String password,
            String author,
            String previousVersion,
            String testName,
            TestDefinition testDefinition,
            Map<String, String> metadata,
            String comment
    ) throws StoreException.TestUpdateException;

    /**
     * {@see other ProctorWriter.deleteTestDefinition}, using username as author
     */
    default void deleteTestDefinition(
            final String username,
            final String password,
            final String previousVersion,
            final String testName,
            final TestDefinition testDefinition,
            final String comment
    ) throws StoreException.TestUpdateException {
        deleteTestDefinition(username, password, username, previousVersion, testName, testDefinition, comment);
    }

    /**
     * @throws StoreException.TestUpdateException when
     */
    void deleteTestDefinition(
            String username,
            String password,
            String author,
            String previousVersion,
            String testName,
            TestDefinition testDefinition,
            String comment
    ) throws StoreException.TestUpdateException;

    /**
     * {@see other ProctorWriter.addTestDefinition}, using username as author
     */
    default void addTestDefinition(
            final String username,
            final String password,
            final String testName,
            final TestDefinition testDefinition,
            final Map<String, String> metadata,
            final String comment
    ) throws StoreException.TestUpdateException {
        addTestDefinition(username, password, username, testName, testDefinition, metadata, comment);
    }

    /**
     * Add new test definition to this store.
     *
     * Fails with Exception when testName already exists
     *
     * @throws StoreException.TestUpdateException on invalid inputs
     */
    void addTestDefinition(
            String username,
            String password,
            String author,
            String testName,
            TestDefinition testDefinition,
            Map<String, String> metadata,
            String comment
    ) throws StoreException.TestUpdateException;
}
