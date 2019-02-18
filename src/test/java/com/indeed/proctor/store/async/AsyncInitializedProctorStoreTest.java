package com.indeed.proctor.store.async;

import com.google.common.util.concurrent.MoreExecutors;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.webapp.util.RetryWithExponentialBackoff;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static org.assertj.core.api.Fail.fail;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

public class AsyncInitializedProctorStoreTest {

    private static final ExecutorService EXECUTOR = MoreExecutors.sameThreadExecutor();
    private static final RetryWithExponentialBackoff RETRY_WITH_EXPONENTIAL_BACKOFF = new RetryWithExponentialBackoff() {
        @Override
        public void sleep(final long sleepTimeMillis) {
        }
    };

    @Mock
    private ProctorStore proctorStore;
    @Mock
    private ExecutorService executorMock;

    private final Supplier<ProctorStore> supplier = () -> proctorStore;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetProctorStoreSuccess() {
        final AsyncInitializedProctorStore asyncInitializedProctorStore = new AsyncInitializedProctorStore(supplier, EXECUTOR, RETRY_WITH_EXPONENTIAL_BACKOFF);
        final ProctorStore result = asyncInitializedProctorStore.getProctorStore();

        assertEquals(proctorStore, result);
    }

    @Test
    public void testGetInitializationNotDoneProctorStore() {
        when(executorMock.submit(Mockito.any(Callable.class))).thenReturn(new CompletableFuture());

        final AsyncInitializedProctorStore asyncInitializedProctorStore = new AsyncInitializedProctorStore(supplier, executorMock, RETRY_WITH_EXPONENTIAL_BACKOFF);

        try {
            asyncInitializedProctorStore.getProctorStore();
            fail("getProctorStore should throw");
        } catch (final AsyncInitializedProctorStore.NotInitializedException e) {
            assertEquals("Still initializing", e.getMessage());
        }
    }

    @Test
    public void testGetNotInitializedProctorStore() {
        when(executorMock.submit(Mockito.any(Callable.class))).thenReturn(CompletableFuture.completedFuture(Optional.empty()));

        final AsyncInitializedProctorStore asyncInitializedProctorStore = new AsyncInitializedProctorStore(supplier, executorMock, RETRY_WITH_EXPONENTIAL_BACKOFF);

        try {
            asyncInitializedProctorStore.getProctorStore();
            fail("getProctorStore should throw");
        } catch (final AsyncInitializedProctorStore.InitializationFailedException e) {
            assertEquals("Initializing proctorStore process has finished but proctorStore is not initialized.", e.getMessage());
        }
    }
}
