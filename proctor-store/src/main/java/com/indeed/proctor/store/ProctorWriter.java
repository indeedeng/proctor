package com.indeed.proctor.store;

import com.indeed.proctor.common.model.TestDefinition;

import java.util.Map;

/** @author parker */
public interface ProctorWriter {

    void verifySetup() throws StoreException;

    boolean cleanUserWorkspace(String username);

    /**
     * {@see other ProctorWriter.updateTestDefinition}, using username as author
     *
     * @deprecated use ChangeMetadata
     */
    @Deprecated
    default void updateTestDefinition(
            final String username,
            final String password,
            final String previousVersion,
            final String testName,
            final TestDefinition testDefinition,
            final Map<String, String> metadata,
            final String comment)
            throws StoreException.TestUpdateException {
        updateTestDefinition(
                username,
                password,
                username,
                previousVersion,
                testName,
                testDefinition,
                metadata,
                comment);
    }

    /**
     * {@see other ProctorWriter.updateTestDefinition}, using Instant.now
     *
     * @deprecated use ChangeMetadata
     */
    @Deprecated
    default void updateTestDefinition(
            final String username,
            final String password,
            final String author,
            final String previousVersion,
            final String testName,
            final TestDefinition testDefinition,
            final Map<String, String> metadata,
            final String comment)
            throws StoreException.TestUpdateException {
        updateTestDefinition(
                new ChangeMetadata(username, password, author, comment),
                previousVersion,
                testName,
                testDefinition,
                metadata);
    }

    /**
     * Updates a test with testName that already exists in this store
     *
     * <p>Fails with Exception when testName does not exist, or neither testDefinition nor metadata
     * has changes to current version
     *
     * @throws StoreException.TestUpdateException
     */
    void updateTestDefinition(
            ChangeMetadata changeMetadata,
            String previousVersion,
            String testName,
            TestDefinition testDefinition,
            Map<String, String> metadata)
            throws StoreException.TestUpdateException;

    /**
     * {@see other ProctorWriter.deleteTestDefinition}, using username as author
     *
     * @deprecated use ChangeMetadata
     */
    @Deprecated
    default void deleteTestDefinition(
            final String username,
            final String password,
            final String previousVersion,
            final String testName,
            final TestDefinition testDefinition,
            final String comment)
            throws StoreException.TestUpdateException {
        deleteTestDefinition(
                username, password, username, previousVersion, testName, testDefinition, comment);
    }

    /**
     * {@see other ProctorWriter.deleteTestDefinition}, using Instant.now
     *
     * @deprecated use ChangeMetadata
     */
    @Deprecated
    default void deleteTestDefinition(
            final String username,
            final String password,
            final String author,
            final String previousVersion,
            final String testName,
            final TestDefinition testDefinition,
            final String comment)
            throws StoreException.TestUpdateException {
        deleteTestDefinition(
                new ChangeMetadata(username, password, author, comment),
                previousVersion,
                testName,
                testDefinition);
    }

    /** @throws StoreException.TestUpdateException when */
    void deleteTestDefinition(
            ChangeMetadata changeMetadata,
            String previousVersion,
            String testName,
            TestDefinition testDefinition)
            throws StoreException.TestUpdateException;

    /**
     * {@see other ProctorWriter.addTestDefinition}, using username as author
     *
     * @deprecated use ChangeMetadata
     */
    @Deprecated
    default void addTestDefinition(
            final String username,
            final String password,
            final String testName,
            final TestDefinition testDefinition,
            final Map<String, String> metadata,
            final String comment)
            throws StoreException.TestUpdateException {
        addTestDefinition(
                username, password, username, testName, testDefinition, metadata, comment);
    }

    /**
     * {@see other ProctorWriter.addTestDefinition}, using Instant.now
     *
     * @deprecated use ChangeMetadata
     */
    @Deprecated
    default void addTestDefinition(
            final String username,
            final String password,
            final String author,
            final String testName,
            final TestDefinition testDefinition,
            final Map<String, String> metadata,
            final String comment)
            throws StoreException.TestUpdateException {
        addTestDefinition(
                new ChangeMetadata(username, password, author, comment),
                testName,
                testDefinition,
                metadata);
    }

    /**
     * Add new test definition to this store.
     *
     * <p>Fails with Exception when testName already exists
     *
     * @throws StoreException.TestUpdateException on invalid inputs
     */
    void addTestDefinition(
            ChangeMetadata changeMetadata,
            final String testName,
            TestDefinition testDefinition,
            Map<String, String> metadata)
            throws StoreException.TestUpdateException;
}
