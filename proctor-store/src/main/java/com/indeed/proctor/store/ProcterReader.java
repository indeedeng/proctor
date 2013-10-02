package com.indeed.proctor.store;

import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;

import java.util.List;

/**
 * @author parker
 */
public interface ProcterReader {
    TestMatrixVersion getCurrentTestMatrix() throws StoreException;

    TestDefinition getCurrentTestDefinition(String test) throws StoreException;

    void verifySetup() throws StoreException;

    /***** Versioned ProcterReader *****/

    long getLatestVersion() throws StoreException;

    TestMatrixVersion getTestMatrix(long fetchRevision) throws StoreException;

    TestDefinition getTestDefinition(String test, long fetchRevision) throws StoreException;

    List<Revision> getMatrixHistory(int start, int limit) throws StoreException;

    List<Revision> getHistory(String test, int start, int limit) throws StoreException;

    List<Revision> getHistory(String test, long revision, int start, int limit) throws StoreException;
}
