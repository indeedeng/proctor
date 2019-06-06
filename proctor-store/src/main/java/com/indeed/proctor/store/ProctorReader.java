package com.indeed.proctor.store;

import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;

import java.util.List;
import java.util.Map;

/**
 * Read interface of Proctor Store.
 * It provides read access to test definitions and their histories in the database.
 *
 * All methods throws {@link StoreException} when it failed to handle request because of errors in the database.
 *
 * @author parker
 */
public interface ProctorReader {
    /**
     * Returns the current test matrix in the database.
     */
    TestMatrixVersion getCurrentTestMatrix() throws StoreException;

    /**
     * Returns a current test definition of a test in the database.
     * @param testName name of the test
     */
    TestDefinition getCurrentTestDefinition(String testName) throws StoreException;

    /**
     * Verifies the data store object is ready to operate.
     * @throws StoreException if it's not ready to operate.
     */
    void verifySetup() throws StoreException;

    /**
     * Returns the latest revision id.
     */
    String getLatestVersion() throws StoreException;

    /**
     * Returns a test matrix when the revision was made.
     * @param revisionId id of the revision
     */
    TestMatrixVersion getTestMatrix(String revisionId) throws StoreException;

    /**
     * Returns a test definition of a test when the revision was made.
     * @param testName name of the test
     * @param revisionId id of the revision
     */
    TestDefinition getTestDefinition(String testName, String revisionId) throws StoreException;

    /**
     * Returns a list of revisions for all tests ordered by recency.
     * @param start offset of the first revision (0-indexed)
     * @param limit limit of the number of revisions
     */
    List<Revision> getMatrixHistory(int start, int limit) throws StoreException;

    /**
     * Returns a list of revisions for a test ordered by recency.
     * @param testName name of the test
     * @param start offset of the first revision (0-indexed)
     * @param limit limit of the number of revisions
     */
    List<Revision> getHistory(String testName, int start, int limit) throws StoreException;

    /**
     * Returns a list of revisions for a test when test revision was made ordered by recency.
     * @param testName name of the test
     * @param start offset of the first revision (0-indexed)
     * @param limit limit of the number of revisions
     */
    List<Revision> getHistory(String testName, String revisionId, int start, int limit) throws StoreException;

    /**
     * Returns a details of a single revision.
     * @param revisionId id of the revision
     */
    RevisionDetails getRevisionDetails(String revisionId) throws StoreException;

    /**
     * Returns a list of revisions grouped by a test. Each list is ordered by recency.
     * Same revision may appear in two or more lists if multiple tests are modified in the revision.
     * @return Map from test name to a list of revision.
     */
    Map<String, List<Revision>> getAllHistories() throws StoreException;

    /**
     * Update the local state with the remote database.
     */
    void refresh() throws StoreException;

}
