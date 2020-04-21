package com.indeed.proctor.store;

import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Read interface of Proctor Store.
 * It provides read access to test definitions and their histories in the database.
 * Each store has a single global linear history of states, each having a unique revision id.
 *
 * All methods throws {@link StoreException} when it failed to handle request because of errors in the database.
 *
 * @author parker
 */
public interface ProctorReader {
    /**
     * @return the current test matrix in the database.
     */
    TestMatrixVersion getCurrentTestMatrix() throws StoreException;

    /**
     * @param testName name of the test.
     * @return a current test definition of a test in the database.
     * null if the test is not found in the current state.
     */
    @CheckForNull
    TestDefinition getCurrentTestDefinition(String testName) throws StoreException;

    /**
     * Verifies the data store object is ready to operate.
     * Throwing an exception allows the implementer to signal bad health status.
     * @throws StoreException if it's not ready to operate.
     */
    void verifySetup() throws StoreException;

    /**
     * @return the latest revision id.
     */
    @Nonnull
    String getLatestVersion() throws StoreException;

    /**
     * @param revisionId id of the revision
     * @return a test matrix when the revision was made.
     * @throws StoreException if the revision is not found.
     */
    TestMatrixVersion getTestMatrix(String revisionId) throws StoreException;

    /**
     * @param testName name of the test
     * @param revisionId id of the revision
     * @return a test definition of a test when the revision was made.
     * null if the test is not found at the revision
     * @throws StoreException if the revision is not found.
     */
    @CheckForNull
    TestDefinition getTestDefinition(String testName, String revisionId) throws StoreException;

    /**
     * @param start offset of the first revision (0-indexed)
     * @param limit limit of the number of revisions, -1 might be usable for unlimited (depending on implementation)
     * @return a list of revisions for all tests ordered by recency.
     */
    @Nonnull
    List<Revision> getMatrixHistory(int start, int limit) throws StoreException;

    /**
     * @param sinceInclusive earliest date
     * @param untilExclusive latest date
     * @return a list of revisions for a test when test revision was made ordered by recency.
     * @throws StoreException if the revision is not found, or date range is bigger than the implementation allows
     */
    @Nonnull
    List<Revision> getMatrixHistory(Instant sinceInclusive, Instant untilExclusive) throws StoreException;

    /**
     * @param testName name of the test
     * @param start offset of the first revision (0-indexed)
     * @param limit limit of the number of revisions, -1 might be usable for unlimited (depending on implementation)
     * @return a list of revisions for a test ordered by recency.
     */
    @Nonnull
    List<Revision> getHistory(String testName, int start, int limit) throws StoreException;

    /**
     * @param testName name of the test
     * @param start offset of the first revision (0-indexed)
     * @param limit limit of the number of revisions, -1 might be usable for unlimited (depending on implementation)
     * @return a list of revisions for a test when test revision was made ordered by recency.
     * @throws StoreException if the revision is not found.
     */
    @Nonnull
    List<Revision> getHistory(String testName, String revisionId, int start, int limit) throws StoreException;

    /**
     * @param revisionId id of the revision
     * @return details of the single revision.
     * null if the revision is not found in the store.
     */
    @CheckForNull
    RevisionDetails getRevisionDetails(String revisionId) throws StoreException;

    /**
     * @param testName to get test definitions
     * @param start offset of the first revision (0-indexed)
     * @param limit limit of the number of test definitions
     * @return a list of test definitions for a test when test revision was made ordered by recency.
     * @throws StoreException if the revision is not found
     */
    @Nonnull
    List<TestDefinition> getTestDefinitions(String testName, int start, int limit) throws StoreException;

    /**
     * @param testName to get test definitions
     * @param revision from which the search of test definition is initiated
     * @param start offset of the first revision (0-indexed)
     * @param limit limit of the number of test definitions
     * @return a list of test definitions for a test when test revision was made ordered by recency.
     * @throws StoreException if the revision is not found
     */
    @Nonnull
    List<TestDefinition> getTestDefinitions(String testName, String revision, int start, int limit) throws StoreException;

    /**
     * Get all the revisions for all tests in history
     * Same revision may appear in two or more lists if multiple tests are modified in the revision.
     * @return a list of revisions grouped by a test. Each list is ordered by recency.
     * @deprecated does not scale, avoid if possible, prefer getMatrixHistory and getRevisionDetails methods
     */
    @Nonnull
    @Deprecated
    Map<String, List<Revision>> getAllHistories() throws StoreException;

    /**
     * Update the local state with the remote database.
     */
    void refresh() throws StoreException;

}
