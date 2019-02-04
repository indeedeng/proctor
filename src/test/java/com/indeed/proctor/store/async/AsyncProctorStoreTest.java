package com.indeed.proctor.store.async;

import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.webapp.db.StoreFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
public class AsyncProctorStoreTest {

    private static final String RELATIVE_PATH = "relative/path";

    @Mock
    private ProctorStore proctorStore;
    @Mock
    private StoreFactory factory;
    @Mock
    private ThreadPoolExecutor executor;
    @Mock
    private Future<ProctorStore> proctorStoreFuture;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(factory.createStore(RELATIVE_PATH)).thenReturn(proctorStore);

        // Mock Executor
        PowerMockito.mockStatic(Executors.class);
        when(Executors.newCachedThreadPool()).thenReturn(executor);
        when(executor.submit(Mockito.any(Callable.class))).thenAnswer(i -> {
            final Callable callable = (Callable) i.getArguments()[0];
            callable.call();
            return proctorStoreFuture;
        });

        // Mock ProctorStoreFuture
        when(proctorStoreFuture.isDone()).thenReturn(true);
        when(proctorStoreFuture.get()).thenReturn(proctorStore);
    }

    @Test
    @PrepareForTest( { AsyncProctorStore.class } )
    public void testAsyncProctorStoreConstructor() throws Exception {
        new AsyncProctorStore(factory, RELATIVE_PATH);

        verify(factory, times(1)).createStore(RELATIVE_PATH);
        verify(executor, times(1)).submit(Mockito.any(Callable.class));
    }

    @Test
    @PrepareForTest( { AsyncProctorStore.class } )
    public void testGetProctorStoreSuccess() throws Exception {
        final AsyncProctorStore asyncProctorStore = new AsyncProctorStore(factory, RELATIVE_PATH);

        final ProctorStore result = asyncProctorStore.getProctorStore(false);

        verify(proctorStoreFuture, times(1)).isDone();
        verify(proctorStoreFuture, times(1)).get();
        assertEquals(proctorStore, result);
    }

    @Test
    @PrepareForTest( { AsyncProctorStore.class } )
    public void testGetProctorStoreByNotDoneJob() {
        when(proctorStoreFuture.isDone()).thenReturn(false);

        final AsyncProctorStore asyncProctorStore = new AsyncProctorStore(factory, RELATIVE_PATH);

        try {
            asyncProctorStore.getProctorStore(false);
            fail("getProctorStore should throw");
        } catch (final RuntimeException e) {
            assertEquals("Not initialized.", e.getMessage());
        }
        verify(executor, times(1)).submit(Mockito.any(Callable.class));
    }

    @Test
    @PrepareForTest( { AsyncProctorStore.class } )
    public void testGetProctorStoreByFailedJob() throws Exception {
        when(proctorStoreFuture.get())
                .thenThrow(new ExecutionException("Failed to run job", new RuntimeException()));

        final AsyncProctorStore asyncProctorStore = new AsyncProctorStore(factory, RELATIVE_PATH);

        try {
            asyncProctorStore.getProctorStore(true);
            fail("getProctorStore should throw");
        } catch (final RuntimeException e) {
            assertEquals("Not initialized.", e.getMessage());
        }
        verify(executor, times(2)).submit(Mockito.any(Callable.class));
    }
}
