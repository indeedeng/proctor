package com.indeed.proctor.store.async;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.store.ChangeMetadata;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.RevisionDetails;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.webapp.util.RetryWithExponentialBackoff;
import org.apache.log4j.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * AsyncInitializedProctorStore is an implementation of ProctorStore.
 * This is delegating all overridden methods to proctorStore.
 * This initializes the delegated proctorStore in a background job when the constructor is called.
 * Before finishing the initialization, this throws NotInitializedException if proctorStore is referred.
 *
 * If it fails to initialize proctorStore, it will retry to initialize up to MAX_ATTEMPT_COUNT with 2^(attemptCount - 1) seconds interval.
 */
public class AsyncInitializedProctorStore implements ProctorStore {
    private static final Logger LOGGER = Logger.getLogger(AsyncInitializedProctorStore.class);
    private static final int MAX_ATTEMPT_COUNT = 10;
    private static final long MAX_ATTEMPT_INTERVAL_INCREASE = 8;
    private final Future<Optional<ProctorStore>> proctorStoreFuture;
    private ProctorStore proctorStore;

    @VisibleForTesting
    AsyncInitializedProctorStore(
            final Supplier<ProctorStore> storeSupplier,
            final ExecutorService executor,
            final RetryWithExponentialBackoff retryWithExponentialBackoff
    ) {
        Preconditions.checkNotNull(executor);
        proctorStoreFuture = executor.submit(() -> retryWithExponentialBackoff.retry(
                storeSupplier,
                MAX_ATTEMPT_COUNT,
                MAX_ATTEMPT_INTERVAL_INCREASE,
                (e, attemptCount) -> LOGGER.error(String.format("Failed to initialize ProctorStore %s times", attemptCount), e)
        ));
    }

    public AsyncInitializedProctorStore(final Supplier<ProctorStore> storeSupplier, final ExecutorService executor) {
        this(storeSupplier, executor, new RetryWithExponentialBackoff());
    }

    @VisibleForTesting
    ProctorStore getProctorStore() {
        if (proctorStore == null) {
            if (!proctorStoreFuture.isDone()) {
                throw new NotInitializedException("Still initializing");
            }

            final Optional<ProctorStore> proctorStoreOptional;

            try {
                proctorStoreOptional = proctorStoreFuture.get();
            } catch (final ExecutionException e) {
                throw new InitializationFailedException("Failed to initialize ProctorStore", e.getCause());
            } catch (final Exception e) {
                throw new InitializationFailedException("Failed to initialize ProctorStore", e);
            }

            proctorStore = proctorStoreOptional.orElseThrow(() ->
                    new InitializationFailedException("Initializing proctorStore process has finished but proctorStore is not initialized."));
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
        getProctorStore().verifySetup();
    }

    @Nonnull
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

    @Nonnull
    @Override
    public List<Revision> getMatrixHistory(final int start, final int limit) throws StoreException {
        return getProctorStore().getMatrixHistory(start, limit);
    }

    @Nonnull
    @Override
    public List<Revision> getHistory(final String test, final int start, final int limit) throws StoreException {
        return getProctorStore().getHistory(test, start, limit);
    }

    @Nonnull
    @Override
    public List<Revision> getHistory(final String test, final String revision, final int start, final int limit) throws StoreException {
        return getProctorStore().getHistory(test, revision, start, limit);
    }

    @CheckForNull
    @Override
    public RevisionDetails getRevisionDetails(final String revisionId) throws StoreException {
        return getProctorStore().getRevisionDetails(revisionId);
    }


    @Nonnull
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
    public void updateTestDefinition(
            final ChangeMetadata changeMetadata,
            final String previousVersion,
            final String testName,
            final TestDefinition testDefinition,
            final Map<String, String> metadata
    ) throws StoreException.TestUpdateException {
        getProctorStore().updateTestDefinition(changeMetadata, previousVersion, testName, testDefinition, metadata);
    }

    @Override
    public void deleteTestDefinition(
            final ChangeMetadata changeMetadata,
            final String previousVersion,
            final String testName,
            final TestDefinition testDefinition
    ) throws StoreException.TestUpdateException {
        getProctorStore().deleteTestDefinition(changeMetadata, previousVersion, testName, testDefinition);
    }

    @Override
    public void addTestDefinition(
            final ChangeMetadata changeMetadata,
            final String testName,
            final TestDefinition testDefinition,
            final Map<String, String> metadata
    ) throws StoreException.TestUpdateException {
        getProctorStore().addTestDefinition(changeMetadata, testName, testDefinition, metadata);
    }

    @Override
    public String getName() {
        return getProctorStore().getName();
    }

    public static class NotInitializedException extends RuntimeException {
        NotInitializedException(final String message) {
            super(message);
        }
    }

    public static class InitializationFailedException extends RuntimeException {
        InitializationFailedException(final String message) {
            super(message);
        }

        InitializationFailedException(final String message, final Throwable throwable) {
            super(message, throwable);
        }
    }
}

