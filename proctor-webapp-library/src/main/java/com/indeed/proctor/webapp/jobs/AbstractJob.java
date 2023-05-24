package com.indeed.proctor.webapp.jobs;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.webapp.db.Environment;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public abstract class AbstractJob {
    private final Map<Environment, ProctorStore> stores;

    public AbstractJob(
            final ProctorStore trunkStore,
            final ProctorStore qaStore,
            final ProctorStore productionStore) {
        this.stores =
                ImmutableMap.of(
                        Environment.WORKING, trunkStore,
                        Environment.QA, qaStore,
                        Environment.PRODUCTION, productionStore);
    }

    static void validateUsernamePassword(final String username, final String password) {
        if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
            throw new IllegalArgumentException("No username or password provided");
        }
    }

    static void validateComment(final String comment) {
        if (StringUtils.isBlank(comment)) {
            throw new IllegalArgumentException("Comment is required.");
        }
    }

    protected ProctorStore determineStoreFromEnvironment(final Environment branch) {
        final ProctorStore store = stores.get(branch);
        if (store == null) {
            throw new RuntimeException("Unknown store for branch " + branch);
        }
        return store;
    }
}
