package com.indeed.proctor.webapp.jobs;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.webapp.db.Environment;

import java.util.List;
import java.util.Map;

public abstract class AbstractJob {
    private final Map<Environment, ProctorStore> stores;

    public AbstractJob(final ProctorStore trunkStore, final ProctorStore qaStore, final ProctorStore productionStore) {
        this.stores = ImmutableMap.of(
                Environment.WORKING, trunkStore,
                Environment.QA, qaStore,
                Environment.PRODUCTION, productionStore
        );
    }

    protected static void validateUsernamePassword(final String username, final String password) throws IllegalArgumentException {
        if (CharMatcher.WHITESPACE.matchesAllOf(Strings.nullToEmpty(username)) || CharMatcher.WHITESPACE.matchesAllOf(Strings.nullToEmpty(password))) {
            throw new IllegalArgumentException("No username or password provided");
        }
    }

    protected static void validateComment(final String comment) throws IllegalArgumentException {
        if (CharMatcher.WHITESPACE.matchesAllOf(Strings.nullToEmpty(comment))) {
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
