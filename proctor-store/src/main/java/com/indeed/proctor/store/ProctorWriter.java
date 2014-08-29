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
     * This is the normal operation that I expect to happen
     */
    void updateTestDefinition(String username, String password, String previousVersion, String testName, TestDefinition testDefinition, Map<String, String> metadata, String comment) throws StoreException.TestUpdateException;

    void deleteTestDefinition(String username, String password, String previousVersion, String testName, TestDefinition testDefinition, String comment) throws StoreException.TestUpdateException;

    void addTestDefinition(String username, String password, String testName, TestDefinition testDefinition, Map<String, String> metadata, String comment) throws StoreException.TestUpdateException;
}
