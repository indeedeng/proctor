package com.indeed.proctor.store;

import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;

import java.util.List;
import java.util.Map;

/**
 * @author parker
 */
public interface ProctorReader {
    TestMatrixVersion getCurrentTestMatrix() throws StoreException;

    TestDefinition getCurrentTestDefinition(String test) throws StoreException;

    void verifySetup() throws StoreException;

    /***** Versioned ProctorReader *****/

    String getLatestVersion() throws StoreException;

    TestMatrixVersion getTestMatrix(String fetchRevision) throws StoreException;

    TestDefinition getTestDefinition(String test, String fetchRevision) throws StoreException;

    List<Revision> getMatrixHistory(int start, int limit) throws StoreException;

    List<Revision> getHistory(String test, int start, int limit) throws StoreException;

    List<Revision> getHistory(String test, String revision, int start, int limit) throws StoreException;

    RevisionDetail getRevisionDetail(String revision) throws StoreException;

    Map<String, List<Revision>> getAllHistories() throws StoreException;

    void refresh() throws StoreException;

}
