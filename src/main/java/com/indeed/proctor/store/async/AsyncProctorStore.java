package com.indeed.proctor.store.async;

import com.google.common.annotations.VisibleForTesting;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.db.StoreFactory;
import com.indeed.proctor.webapp.util.RetryWithExponentialBackoff;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * AsyncProctorStore is an implementation of ProctorStore.
 * This is delegating all overridden methods to proctorStore.
 * This initializes the delegated proctorStore in a background job when the constructor is called.
 * Before finishing the initialization, this throws NotInitializedException if proctorStore is referred.
 *
 * If it fails to initialize proctorStore, it will retry to initialize up to MAX_RETRY_COUNT with 2^(retryCount - 1) seconds interval.
 */
public class AsyncProctorStore implements ProctorStore {
    private static final Logger LOGGER = Logger.getLogger(AsyncProctorStore.class);
    private static final int MAX_RETRY_COUNT = 10;
    private static final long MAX_RETRY_INTERVAL_INCREASE = 8; // Max interval: (1L << MAX_RETRY_INTERVAL_INCREASE) * 1000
    private final Future<Optional<ProctorStore>> proctorStoreFuture;
    private ProctorStore proctorStore;

    public AsyncProctorStore(final StoreFactory factory, final String relativePath, final ExecutorService executor) {
        proctorStoreFuture = executor.submit(() -> RetryWithExponentialBackoff.retry(
                () -> {
                    try {
                        return factory.createStore(relativePath);
                    } catch (ConfigurationException e) {
                        throw new InitializationFailedException(e);
                    }
                },
                MAX_RETRY_COUNT,
                MAX_RETRY_INTERVAL_INCREASE,
                (e, retryCount) -> LOGGER.error(String.format("Failed to initialize ProctorStore %s times", ++retryCount), e)
        ));
    }

    @VisibleForTesting
    ProctorStore getProctorStore() {
        if (proctorStore == null) {
            if (!proctorStoreFuture.isDone()) {
                throw new NotInitializedException("Still initializing");
            }
            try {
                final Optional<ProctorStore> proctorStoreOptional = proctorStoreFuture.get();
                proctorStore = proctorStoreOptional.orElseThrow(() -> new NotInitializedException("Not initialized."));
            } catch (final ExecutionException e) {
                throw new RuntimeException(e.getCause());
            } catch (final Exception e) {
                LOGGER.warn("Initializing process has finished but proctorStore is not initialized.", e);
                throw new NotInitializedException("Initializing process has unsuccessfully finished", e);
            }
        }
        return proctorStore;
    }

    @Override
    public void close() throws IOException {
        final ProctorStore store;
        try {
            store = getProctorStore();
        } catch(final Exception e) {
            LOGGER.warn("Exception thrown during closing ProctorStore", e);
            return;
        }
        store.close();
    }

    @Override
    public TestMatrixVersion getCurrentTestMatrix() throws StoreException {
        return getProctorStore().getCurrentTestMatrix();
    }

    @Override
    public TestDefinition getCurrentTestDefinition(final String test) throws StoreException {
        return getProctorStore().getCurrentTestDefinition(test);
    }

    @Override
    public void verifySetup() throws StoreException {
        if (!proctorStoreFuture.isDone()) {

        } else {
            getProctorStore().verifySetup();
        }
    }

    @Override
    public String getLatestVersion() throws StoreException {
        return getProctorStore().getLatestVersion();
    }

    @Override
    public TestMatrixVersion getTestMatrix(final String fetchRevision) throws StoreException {
        return getProctorStore().getTestMatrix(fetchRevision);
    }

    @Override
    public TestDefinition getTestDefinition(final String test, final String fetchRevision) throws StoreException {
        return getProctorStore().getTestDefinition(test, fetchRevision);
    }

    @Override
    public List<Revision> getMatrixHistory(final int start, final int limit) throws StoreException {
        return getProctorStore().getMatrixHistory(start, limit);
    }

    @Override
    public List<Revision> getHistory(final String test, final int start, final int limit) throws StoreException {
        return getProctorStore().getHistory(test, start, limit);
    }

    @Override
    public List<Revision> getHistory(final String test, final String revision, final int start, final int limit) throws StoreException {
        return getProctorStore().getHistory(test, revision, start, limit);
    }

    @Override
    public Map<String, List<Revision>> getAllHistories() throws StoreException {
        return getProctorStore().getAllHistories();
    }

    @Override
    public void refresh() throws StoreException {
        getProctorStore().refresh();
    }

    @Override
    public boolean cleanUserWorkspace(final String username) {
        try {
            getProctorStore().cleanUserWorkspace(username);
        } catch (final Exception e) {
            LOGGER.warn("Exception thrown during cleaning user workspace", e);
        }
        return false;
    }

    @Override
    public void updateTestDefinition(final String username,
                                     final String password,
                                     final String previousVersion,
                                     final String testName,
                                     final TestDefinition testDefinition,
                                     final Map<String, String> metadata,
                                     final String comment) throws StoreException.TestUpdateException {
        getProctorStore().updateTestDefinition(username, password, previousVersion, testName, testDefinition, metadata, comment);
    }

    @Override
    public void updateTestDefinition(final String username,
                                     final String password,
                                     final String author,
                                     final String previousVersion,
                                     final String testName,
                                     final TestDefinition testDefinition,
                                     final Map<String, String> metadata,
                                     final String comment) throws StoreException.TestUpdateException {
        getProctorStore().updateTestDefinition(username, password, author, previousVersion, testName, testDefinition, metadata, comment);
    }

    @Override
    public void deleteTestDefinition(final String username,
                                     final String password,
                                     final String previousVersion,
                                     final String testName,
                                     final TestDefinition testDefinition,
                                     final String comment) throws StoreException.TestUpdateException {
        getProctorStore().deleteTestDefinition(username, password, previousVersion, testName, testDefinition, comment);
    }

    @Override
    public void deleteTestDefinition(final String username,
                                     final String password,
                                     final String author,
                                     final String previousVersion,
                                     final String testName,
                                     final TestDefinition testDefinition,
                                     final String comment) throws StoreException.TestUpdateException {
        getProctorStore().deleteTestDefinition(username, password, author, previousVersion, testName, testDefinition, comment);
    }

    @Override
    public void addTestDefinition(final String username,
                                  final String password,
                                  final String testName,
                                  final TestDefinition testDefinition,
                                  final Map<String, String> metadata,
                                  final String comment) throws StoreException.TestUpdateException {
        getProctorStore().addTestDefinition(username, password, testName, testDefinition, metadata, comment);
    }

    @Override
    public void addTestDefinition(final String username,
                                  final String password,
                                  final String author,
                                  final String testName,
                                  final TestDefinition testDefinition,
                                  final Map<String, String> metadata,
                                  final String comment) throws StoreException.TestUpdateException {
        getProctorStore().addTestDefinition(username, password, author, testName, testDefinition, metadata, comment);
    }

    @Override
    public String getName() {
        return getProctorStore().getName();
    }

    public static class NotInitializedException extends RuntimeException {
        NotInitializedException(final String message) {
            super(message);
        }

        NotInitializedException(final String message, final Throwable throwable) {
            super(message, throwable);
        }
    }

    public static class InitializationFailedException extends RuntimeException {
        InitializationFailedException(final Throwable throwable) {
            super(throwable);
        }
    }
}

