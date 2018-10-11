package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.webapp.db.Environment;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

/**
 * Interface for global cache which holds test definition {@code TestDefinition} and history {@code List<Revision>} per
 * test and environment. Its implementation is expected to provide a synchronized cache across distributed instances.
 */
public interface GlobalCacheStore {
    /**
     * Get history for a test in a specified environment
     *
     * @param env      environment
     * @param testName test name
     * @return optional of list of revision. It is expected to return {@code Optional.empty()} when there is no
     * corresponding data in global cache or failed to get history from it.
     */
    Optional<List<Revision>> getCachedHistory(final Environment env, final String testName);

    /**
     * Get test definition for a test in a specified environment
     *
     * @param env      environment
     * @param testName test name
     * @return optional of test definition. It is expected to return {@code Optional.empty()} when there is no
     * corresponding data in global cache or failed to get test definition from it.
     */
    Optional<TestDefinition> getCachedTestDefinition(final Environment env, final String testName);

    /**
     * Update test definition and history for a test in a specified environment
     *
     * @param env            environment
     * @param testName       test name
     * @param testDefinition test definition to update
     * @param history        list of revision to update
     * @throws com.indeed.proctor.store.cache.GlobalCacheUpdateException Failed to update global cache
     */
    void updateCache(final Environment env,
                     final String testName,
                     @Nullable final TestDefinition testDefinition,
                     final List<Revision> history);
}
