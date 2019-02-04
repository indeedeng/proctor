package com.indeed.proctor.store.async;

import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.webapp.db.StoreFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest( { AsyncProctorStore.class } )
public class AsyncProctorStoreTest {

    private static final String RELATIVE_PATH = "relative/path";

    @Mock
    private ProctorStore proctorStore;
    @Mock
    private StoreFactory factory;
    @Mock
    private Thread thread;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(factory.createStore(RELATIVE_PATH)).thenReturn(proctorStore);

        // Mock Thread.sleep
        PowerMockito.mockStatic(Thread.class);
        PowerMockito.doNothing().when(Thread.class);
        Thread.sleep(anyLong());
    }

    /**
     * Mock `Thread(Runnable runnable)`
     * When `new Thread(runnable)` is called, it will also mock `thread.start()` to run `runnable`
     * @throws Exception
     */
    private void mockNewThreadWithRunnable() throws Exception {
        PowerMockito.whenNew(Thread.class)
                .withParameterTypes(Runnable.class)
                .withArguments(Mockito.any(Runnable.class))
                .thenAnswer(new Answer() {
                    @Override
                    public Thread answer(final InvocationOnMock invocationOnMock) {
                        final Runnable runnable = (Runnable) invocationOnMock.getArguments()[0];
                        doAnswer(new Answer() {
                            @Override
                            public Object answer(final InvocationOnMock invocationOnMock) {
                                runnable.run();
                                return null;
                            }
                        }).when(thread).start();
                        return thread;
                    }
                });
    }

    @Test
    public void testAsyncProctorStoreConstructor() throws Exception {
        mockNewThreadWithRunnable();
        new AsyncProctorStore(factory, RELATIVE_PATH);

        verify(thread, times(1)).start();
        verify(factory, times(1)).createStore(RELATIVE_PATH);
    }

    @Test
    public void testAsyncProctorStoreConstructorRetries() throws Exception {
        mockNewThreadWithRunnable();
        when(factory.createStore(RELATIVE_PATH))
                .thenThrow(new RuntimeException("Failed to create"))
                .thenReturn(proctorStore);

        new AsyncProctorStore(factory, RELATIVE_PATH);

        verify(thread, times(1)).start();
        verify(factory, times(2)).createStore(RELATIVE_PATH);
    }

    @Test
    public void testGetProctorStoreSuccess() throws Exception {
        mockNewThreadWithRunnable();
        final AsyncProctorStore asyncProctorStore = new AsyncProctorStore(factory, RELATIVE_PATH);

        final ProctorStore result = asyncProctorStore.getProctorStore();

        assertEquals(proctorStore, result);
    }

    @Test
    public void testGetNotInitializedProctorStore() throws Exception {
        PowerMockito.whenNew(Thread.class)
                .withParameterTypes(Runnable.class)
                .withArguments(Mockito.any(Runnable.class))
                .thenReturn(thread);
        doNothing().when(thread).start();

        final AsyncProctorStore asyncProctorStore = new AsyncProctorStore(factory, RELATIVE_PATH);

        try {
            asyncProctorStore.getProctorStore();
            fail("getProctorStore should throw");
        } catch (final AsyncProctorStore.NotInitializedException e) {
            assertEquals("Not initialized.", e.getMessage());
        }
    }
}
