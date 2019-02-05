package com.indeed.proctor.store.async;

import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.webapp.db.StoreFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.assertj.core.api.Fail.fail;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AsyncProctorStoreTest {

    private static final String RELATIVE_PATH = "relative/path";

    @Mock
    private StoreFactory factory;
    @Mock
    private ProctorStore proctorStore;
    @Mock
    private ExecutorService executor;
    @Mock
    private Future<Optional<ProctorStore>> proctorStoreFuture;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(factory.createStore(RELATIVE_PATH)).thenReturn(proctorStore);

        when(proctorStoreFuture.isDone()).thenReturn(true);
        when(proctorStoreFuture.get()).thenReturn(Optional.of(proctorStore));
    }

    @Test
    public void testAsyncProctorStoreConstructor() throws Exception {
        when(executor.submit(Mockito.any(Callable.class))).thenAnswer((i) -> {
            final Callable callable = (Callable) i.getArguments()[0];
            callable.call();
            return proctorStoreFuture;
        });

        new AsyncProctorStore(factory, RELATIVE_PATH, executor);

        verify(factory, times(1)).createStore(RELATIVE_PATH);
    }

    @Test
    public void testGetProctorStoreSuccess() throws Exception {
        when(executor.submit(Mockito.any(Callable.class))).thenReturn(proctorStoreFuture);
        when(proctorStoreFuture.isDone()).thenReturn(true);
        when(proctorStoreFuture.get()).thenReturn(Optional.of(proctorStore));

        final AsyncProctorStore asyncProctorStore = new AsyncProctorStore(factory, RELATIVE_PATH, executor);

        final ProctorStore result = asyncProctorStore.getProctorStore();

        assertEquals(proctorStore, result);
    }

    @Test
    public void testGetInitializationNotDoneProctorStore() {
        when(executor.submit(Mockito.any(Callable.class))).thenReturn(proctorStoreFuture);
        when(proctorStoreFuture.isDone()).thenReturn(false);

        final AsyncProctorStore asyncProctorStore = new AsyncProctorStore(factory, RELATIVE_PATH, executor);

        try {
            asyncProctorStore.getProctorStore();
            fail("getProctorStore should throw");
        } catch (final AsyncProctorStore.NotInitializedException e) {
            assertEquals("Still initializing", e.getMessage());
        }
    }

    @Test
    public void testGetNotInitializedProctorStore() throws Exception {
        when(executor.submit(Mockito.any(Callable.class))).thenReturn(proctorStoreFuture);
        when(proctorStoreFuture.isDone()).thenReturn(true);
        when(proctorStoreFuture.get()).thenReturn(Optional.empty());

        final AsyncProctorStore asyncProctorStore = new AsyncProctorStore(factory, RELATIVE_PATH, executor);

        try {
            asyncProctorStore.getProctorStore();
            fail("getProctorStore should throw");
        } catch (final AsyncProctorStore.NotInitializedException e) {
            assertEquals("Initializing process has unsuccessfully finished", e.getMessage());
        }
    }
}
