package com.indeed.proctor.store.cache;

import com.google.common.collect.Lists;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.store.InMemoryProctorStore;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.store.StoreException.TestUpdateException;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.indeed.proctor.store.InMemoryProctorStoreTest.createDummyTestDefinition;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CachingProctorStoreTest {

    private static final List<Integer> DUMMY_HISTORY = Lists.newArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    private static final List<Revision> REVISION_HISTORY = Lists.newArrayList(
            makeRevision("1"),
            makeRevision("2"),
            makeRevision("3"),
            makeRevision("4"),
            makeRevision("5")
    );

    private CachingProctorStore testee;
    private ProctorStore delegate;

    @Before
    public void setUpTestee() throws TestUpdateException {
        delegate = new InMemoryProctorStore();
        delegate.addTestDefinition("Mike", "pwd", "tst1",
                createDummyTestDefinition("1", "tst1"),
                Collections.<String, String>emptyMap(), "commit tst1");
        delegate.addTestDefinition("William", "pwd", "tst2",
                createDummyTestDefinition("2", "tst2"),
                Collections.<String, String>emptyMap(), "commit tst2");
        testee = new CachingProctorStore(delegate);
        /* we stop the background refresh task to perform in order to do better unit test */
        testee.getRefreshTaskFuture().cancel(false);
    }

    @Test
    public void testSelectHistorySet() {
        final List<Integer> result = CachingProctorStore.selectHistorySet(DUMMY_HISTORY, 0, 1);
        assertEquals(Lists.newArrayList(1), result);
    }

    @Test
    public void testSelectHistorySetWithNegativeStart() {
        final List<Integer> result = CachingProctorStore.selectHistorySet(DUMMY_HISTORY, -1, 1);
        assertEquals(Lists.newArrayList(1), result);
    }

    @Test
    public void testSelectHistorySetWithNonPositiveLimit() {
        final List<Integer> result = CachingProctorStore.selectHistorySet(DUMMY_HISTORY, 2, -1);
        assertEquals(Collections.<Integer>emptyList(), result);
    }

    @Test
    public void testSelectHistorySetNotOverflow() {
        List<Integer> result = CachingProctorStore.selectHistorySet(DUMMY_HISTORY, 0, Integer.MAX_VALUE);
        assertEquals(DUMMY_HISTORY, result);

        result = CachingProctorStore.selectHistorySet(DUMMY_HISTORY, 0, Integer.MIN_VALUE);
        assertEquals(Collections.<Integer>emptyList(), result);

        result = CachingProctorStore.selectHistorySet(DUMMY_HISTORY, Integer.MIN_VALUE, Integer.MAX_VALUE);
        assertEquals(DUMMY_HISTORY, result);
    }

    @Test
    public void testSelectRevisionHistorySetFrom() {
        final List<Revision> result = CachingProctorStore.selectRevisionHistorySetFrom(REVISION_HISTORY, "2", 0, 3);
        assertEquals(REVISION_HISTORY.subList(1, 4), result);
    }

    @Test
    public void testSelectRevisionHistorySetFromReturnEmtpy() {
        final List<Revision> result = CachingProctorStore.selectRevisionHistorySetFrom(REVISION_HISTORY, "5", 1, 3);
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void testSelectRevisionHistorySetFromNonexistence() {
        final List<Revision> result = CachingProctorStore.selectRevisionHistorySetFrom(REVISION_HISTORY, "hello", 0, 3);
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void testAddTestDefinition() throws StoreException, InterruptedException {
        final TestDefinition tst3 = createDummyTestDefinition("3", "tst3");

        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    testee.addTestDefinition("Mike", "pwd", "tst3", tst3, Collections.<String, String>emptyMap(), "Create tst2");
                } catch (final TestUpdateException e) {
                    fail();
                }
            }
        });
        assertEquals("2", testee.getLatestVersion());
        assertNull(testee.getCurrentTestDefinition("tst3"));
        assertTrue(testee.getRefreshTaskFuture().isCancelled());
        assertEquals(2, testee.getAllHistories().size());
        startThreadsAndSleep(thread);
        assertFalse(testee.getRefreshTaskFuture().isCancelled());
        testee.getRefreshTaskFuture().cancel(false);
        assertEquals(3, testee.getAllHistories().size());
        assertEquals("3", testee.getLatestVersion());
        assertEquals("description of tst3", testee.getCurrentTestDefinition("tst3").getDescription());
        assertEquals("description of tst3", testee.getTestDefinition("tst3", "revision-3").getDescription());
    }

    @Test
    public void testUpdateTestDefinition() throws StoreException, InterruptedException {
        final TestDefinition newTst1 = createDummyTestDefinition("3", "tst1");
        newTst1.setDescription("updated description tst1");
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    testee.updateTestDefinition("Mike", "pwd", "2", "tst1", newTst1, Collections.<String, String>emptyMap(), "Update description of tst1");
                } catch (final TestUpdateException e) {
                    fail();
                }
            }
        });
        assertEquals("description of tst1", testee.getCurrentTestDefinition("tst1").getDescription());
        assertEquals(1, testee.getHistory("tst1", 0, 2).size());
        startThreadsAndSleep(thread);
        assertEquals("updated description tst1", testee.getCurrentTestDefinition("tst1").getDescription());
        assertEquals(2, testee.getHistory("tst1", 0, 2).size());
        assertEquals("updated description tst1", testee.getTestDefinition("tst1", "revision-3").getDescription());
        assertEquals("description of tst1", testee.getTestDefinition("tst1", "revision-1").getDescription());
    }

    @Test
    public void testUpdateTestDefinitionWithIncorrectPrevVersion() throws StoreException, InterruptedException {
        final String incorrectPreviousVersion = "1";
        final TestDefinition newTst1 = createDummyTestDefinition("3", "tst1");
        newTst1.setDescription("updated description tst1");
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    testee.updateTestDefinition("Mike", "pwd", incorrectPreviousVersion, "tst1", newTst1, Collections.<String, String>emptyMap(), "Update description of tst1");
                    fail();
                } catch (final TestUpdateException e) {
                    /* expected */
                }
            }
        });
        thread.start();
        assertEquals("description of tst1", testee.getCurrentTestDefinition("tst1").getDescription());
        assertTrue(testee.getRefreshTaskFuture().isCancelled());
    }

    @Test
    public void testTwoUpdates() throws StoreException, InterruptedException {
        final TestDefinition newTst1 = createDummyTestDefinition("3", "tst1");
        newTst1.setDescription("updated description tst1");
        final TestDefinition newTst2 = createDummyTestDefinition("3", "tst1");
        newTst2.setDescription("This test shouldn't be updated");

        final Thread successThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    testee.updateTestDefinition("Mike", "pwd", "2", "tst1", newTst1, Collections.<String, String>emptyMap(), "Update description of tst1");
                } catch (final TestUpdateException e) {
                    fail();
                    /* expected */
                }
            }
        });
        final Thread failureThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1);
                    testee.updateTestDefinition("Mike", "pwd", "2", "tst1", newTst2, Collections.<String, String>emptyMap(), "Update description of tst1");
                    fail();
                } catch (final TestUpdateException e) {
                    /* expected */
                } catch (final InterruptedException ignored) {
                }
            }
        });
        startThreadsAndSleep(successThread, failureThread);
        assertEquals("updated description tst1", testee.getCurrentTestDefinition("tst1").getDescription());
        assertFalse(testee.getRefreshTaskFuture().isCancelled());
    }

    @Test
    public void testOneUpdateAndOneAdd() throws StoreException, InterruptedException {
        final TestDefinition newTst1 = createDummyTestDefinition("3", "tst1");
        newTst1.setDescription("updated description tst1");
        final TestDefinition tst4 = createDummyTestDefinition("4", "tst4");

        final Thread updateThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    testee.updateTestDefinition("Mike", "pwd", "2", "tst1", newTst1, Collections.<String, String>emptyMap(), "Update description of tst1");
                } catch (final TestUpdateException e) {
                    fail();
                }
            }
        });
        final Thread addThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    testee.addTestDefinition("Tom", "pwd", "tst4", tst4, Collections.<String, String>emptyMap(), "Create tst4");
                } catch (final TestUpdateException e) {
                    fail();
                }
            }
        });
        assertEquals(1, testee.getHistory("tst1", 0, 4).size());
        startThreadsAndSleep(updateThread, addThread);
        assertEquals("updated description tst1", testee.getCurrentTestDefinition("tst1").getDescription());
        assertEquals("description of tst4", testee.getCurrentTestDefinition("tst4").getDescription());
        assertEquals(2, testee.getHistory("tst1", 0, 4).size());
        assertEquals("Update description of tst1", testee.getHistory("tst1", 0, 4).get(0).getMessage());
    }

    @Test
    public void testDelete() throws StoreException, InterruptedException {
        final TestDefinition tst1 = createDummyTestDefinition("3", "tst1"); /* this would not be actually used */

        final Thread deleteThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    testee.deleteTestDefinition("Mike", "pwd", "2", "tst1", tst1, "Delete tst1");
                } catch (final TestUpdateException e) {
                    fail();
                }
            }
        });
        startThreadsAndSleep(deleteThread);
        assertEquals("3", testee.getLatestVersion());
        assertNull(testee.getCurrentTestDefinition("tst1"));
        assertNotNull(testee.getTestDefinition("tst1", "revision-1"));
    }

    private static void startThreadsAndSleep(final Thread... threads) throws InterruptedException {
        for (final Thread thread : threads) {
            thread.start();
        }

        for (final Thread thread : threads)  {
            thread.join();
        }
    }

    private static Revision makeRevision(final String revision) {
        return new Revision(revision, "author", new Date(), "revision message");
    }
}