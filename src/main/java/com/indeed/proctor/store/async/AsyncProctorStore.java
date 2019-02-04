package com.indeed.proctor.store.async;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.db.StoreFactory;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class AsyncProctorStore implements ProctorStore {
    private static final Logger LOGGER = Logger.getLogger(AsyncProctorStore.class);
    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    private final StoreFactory factory;
    private final String relativePath;
    private Future<ProctorStore> proctorStoreFuture;

    public AsyncProctorStore(final StoreFactory factory, final String relativePath) {
        this.factory = factory;
        this.relativePath = relativePath;
        submitCreateStoreJob();
    }

    private void submitCreateStoreJob() {
        proctorStoreFuture = executorService.submit(new Callable<ProctorStore>() {
            @Override
            public ProctorStore call() throws ConfigurationException {
                return factory.createStore(relativePath);
            }
        });
    }

    private ProctorStore getProctorStore() {
        return getProctorStore(true);
    }

    @VisibleForTesting
    ProctorStore getProctorStore(final boolean retry) {
        try {
            if (proctorStoreFuture.isDone()) {
                final ProctorStore proctorStore = proctorStoreFuture.get();
                Preconditions.checkNotNull(proctorStore, "ProctorStore should not be null");
                return proctorStore;
            } else {
                LOGGER.info("The ProctorStore creation task is not done.");
            }
        } catch (final Exception e) {
            LOGGER.error("Failed to initialize ProctorStore", e);
            if (retry) {
                LOGGER.error("Retry ProctorStore initialization job");
                submitCreateStoreJob();
            }
        }

        throw new RuntimeException("Not initialized.");
    }

    @Override
    public void close() throws IOException {
        try {
            getProctorStore(false).close();
        } catch(final IOException e) {
            throw e;
        } catch(final Exception e) {
            LOGGER.warn("Exception thrown during closing ProctorStore", e);
        }
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
            getProctorStore(false).cleanUserWorkspace(username);
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
}

