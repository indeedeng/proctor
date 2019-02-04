package com.indeed.proctor.store.async;

import com.google.common.annotations.VisibleForTesting;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.db.StoreFactory;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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
    private AtomicReference<ProctorStore> proctorStore;

    public AsyncProctorStore(final StoreFactory factory, final String relativePath) {
        proctorStore = new AtomicReference<>(null);
        startCreateStoreJob(factory, relativePath, proctorStore);
    }

    /**
     * startCreateStoreJob starts initializing ProctorStore.
     * When `factory.createStore(relativePath)` fails, this will retry it up to MAX_RETRY_COUNT times with interval.
     * The interval time increases by twice of the previous interval time for each time.
     * The initial interval time is 1 second.
     * The maximum interval time is 2^MAX_RETRY_INTERVAL_INCREASE.
     * @param factory
     * @param relativePath
     * @param atomicStore This method sets the created ProctorStore to atomicStore.
     */
    private static void startCreateStoreJob(
            final StoreFactory factory,
            final String relativePath,
            final AtomicReference<ProctorStore> atomicStore
    ) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int retryCount = 0;
                while(true) {
                    try {
                        atomicStore.set(factory.createStore(relativePath));
                        break;
                    } catch (final Exception e) {
                        LOGGER.error(String.format("Failed to initialize ProctorStore %s times", ++retryCount), e);
                    }

                    if (retryCount > MAX_RETRY_COUNT) {
                        LOGGER.error("Reached max-retries to initialize ProctorStore");
                        break;
                    }

                    try {
                        final long sleepTimeMillis = (1L << Math.min(retryCount - 1, MAX_RETRY_INTERVAL_INCREASE)) * 1000;
                        LOGGER.info(String.format("Sleep %s seconds before retrying to initialize ProctorStore", sleepTimeMillis / 1000));
                        Thread.sleep(sleepTimeMillis);
                    } catch (final InterruptedException e) {
                        LOGGER.error("Failed to sleep", e);
                    }
                }
            }
        }).start();
    }

    @VisibleForTesting
    ProctorStore getProctorStore() {
        final ProctorStore store = proctorStore.get();
        if (store == null) {
            throw new NotInitializedException("Not initialized.");
        }
        return store;
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
        getProctorStore().verifySetup();
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

    @VisibleForTesting
    static class NotInitializedException extends RuntimeException {
        NotInitializedException(final String message) {
            super(message);
        }
    }
}

