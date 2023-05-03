package com.indeed.proctor.store.cache;

import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.store.ChangeMetadata;
import com.indeed.proctor.store.ProctorStore;
import com.indeed.proctor.store.StoreException;
import com.indeed.proctor.store.StoreException.TestUpdateException;
import com.indeed.proctor.store.utils.test.InMemoryProctorStore;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import static com.indeed.proctor.store.utils.test.InMemoryProctorStoreTest.createDummyTestDefinition;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CachingProctorStoreTest {

    private static final Thread.UncaughtExceptionHandler ASSERTION_FAILURE_HANDLER = (thread, throwable) -> {
        if (throwable instanceof AssertionError) {
            fail();
        }
    };

    private CachingProctorStore testee;
    private ProctorStore delegate;

    @Before
    public void setUpTestee() throws TestUpdateException {
        delegate = new InMemoryProctorStore();
        delegate.addTestDefinition(
                ChangeMetadata.builder()
                        .setUsernameAndAuthor("Mike")
                        .setPassword("pwd")
                        .setComment("commit tst1")
                        .build(),
                "tst1",
                createDummyTestDefinition("1", "tst1"),
                emptyMap());
        delegate.addTestDefinition(
                ChangeMetadata.builder()
                        .setUsernameAndAuthor("William")
                        .setPassword("pwd")
                        .setComment("commit tst2")
                        .build(),
                "tst2",
                createDummyTestDefinition("2", "tst2"),
                emptyMap());
        testee = new CachingProctorStore(delegate);
        /* we stop the background refresh task to perform in order to do better unit test */
        testee.getRefreshTaskFuture().cancel(false);
    }

    @Test
    public void testAddTestDefinition() throws StoreException, InterruptedException {
        final String initialRevision = delegate.getLatestVersion();
        final TestDefinition tst3 = createDummyTestDefinition("3", "tst3");

        final Thread thread = new Thread(() -> {
            try {
                testee.addTestDefinition(
                        ChangeMetadata.builder()
                                .setUsernameAndAuthor("Mike")
                                .setPassword("pwd")
                                .setComment("Create tst2")
                                .build(),
                        "tst3", tst3, emptyMap());
            } catch (final TestUpdateException e) {
                fail();
            }
        });
        assertEquals(initialRevision, testee.getLatestVersion());
        assertNull(testee.getCurrentTestDefinition("tst3"));
        assertTrue(testee.getRefreshTaskFuture().isCancelled());
        assertEquals(2, testee.getAllHistories().size());
        startThreadsAndSleep(thread);
        assertFalse(testee.getRefreshTaskFuture().isCancelled());
        testee.getRefreshTaskFuture().cancel(false);
        assertEquals(3, testee.getAllHistories().size());
        assertNotEquals(initialRevision, delegate.getLatestVersion());
        assertEquals(delegate.getLatestVersion(), testee.getLatestVersion());
        assertEquals("description of tst3", testee.getCurrentTestDefinition("tst3").getDescription());
        assertEquals("description of tst3", testee.getTestDefinition("tst3", testee.getLatestVersion()).getDescription());
    }

    @Test
    public void testUpdateTestDefinition() throws StoreException, InterruptedException {
        final String initialRevision = delegate.getLatestVersion();
        final TestDefinition newTst1 = createDummyTestDefinition("3", "tst1");
        newTst1.setDescription("updated description tst1");
        final Thread thread = new Thread(() -> {
            try {
                final String lastRevision =
                        testee.getHistory(
                                "tst1", 0, 1
                        ).get(0).getRevision();
                testee.updateTestDefinition(
                        ChangeMetadata.builder()
                                .setUsernameAndAuthor("Mike")
                                .setPassword("pwd")
                                .setComment("Update description of tst1")
                                .build(),
                        lastRevision, "tst1", newTst1, emptyMap());
            } catch (final StoreException e) {
                fail();
            }
        });
        assertEquals("description of tst1", testee.getCurrentTestDefinition("tst1").getDescription());
        assertEquals(1, testee.getHistory("tst1", 0, 2).size());
        startThreadsAndSleep(thread);
        assertEquals("updated description tst1", testee.getCurrentTestDefinition("tst1").getDescription());
        assertEquals(2, testee.getHistory("tst1", 0, 2).size());
        assertEquals("updated description tst1", testee.getTestDefinition("tst1", delegate.getLatestVersion()).getDescription());
        assertEquals("description of tst1", testee.getTestDefinition("tst1", initialRevision).getDescription());
    }

    @Test
    public void testUpdateTestDefinitionWithIncorrectPrevVersion() throws StoreException, InterruptedException {
        final String incorrectPreviousVersion = "1";
        final TestDefinition newTst1 = createDummyTestDefinition("3", "tst1");
        newTst1.setDescription("updated description tst1");
        final Thread thread = new Thread(() -> {
            try {
                testee.updateTestDefinition(ChangeMetadata.builder()
                        .setUsernameAndAuthor("Mike")
                        .setPassword("pwd")
                        .setComment("Update description of tst1")
                        .build(),
                        incorrectPreviousVersion, "tst1", newTst1, emptyMap());
                fail();
            } catch (final TestUpdateException e) {
                /* expected */
            }
        });
        thread.start();
        assertEquals("description of tst1", testee.getCurrentTestDefinition("tst1").getDescription());
        assertTrue(testee.getRefreshTaskFuture().isCancelled());
    }

    @Test
    public void testTwoUpdates() throws StoreException, InterruptedException, ExecutionException {
        final TestDefinition newTst1 = createDummyTestDefinition("3", "tst1");
        final String description1 = "updated description tst1 from newTst1";
        newTst1.setDescription(description1);
        final TestDefinition newTst2 = createDummyTestDefinition("3", "tst1");
        final String description2 = "updated description tst1 from newTst2";
        newTst2.setDescription(description2);

        final String lastRevision =
                testee.getHistory(
                        "tst1", 0, 1
                ).get(0).getRevision();
        final FutureTask<Boolean> future1 = getFutureTaskUpdateTestDefinition(lastRevision, "tst1", newTst1, "update description tst1");
        final FutureTask<Boolean> future2 = getFutureTaskUpdateTestDefinition(lastRevision, "tst1", newTst2, "update description tst1");
        startThreadsAndSleep(new Thread(future1), new Thread(future2));
        final boolean thread1UpdateSuccess = future1.get();
        final boolean thread2UpdateSuccess = future2.get();
        assertTrue(thread1UpdateSuccess ^ thread2UpdateSuccess);
        assertEquals(thread1UpdateSuccess ? description1 : description2, testee.getCurrentTestDefinition("tst1").getDescription());
        assertFalse(testee.getRefreshTaskFuture().isCancelled());
    }

    private FutureTask<Boolean> getFutureTaskUpdateTestDefinition(final String previousVersion, final String testName, final TestDefinition test, final String comment) {
        return new FutureTask<>(() -> {
            try {
                testee.updateTestDefinition(ChangeMetadata.builder()
                        .setUsernameAndAuthor("Mike")
                        .setPassword("pwd")
                        .setComment(comment)
                        .build(),
                        previousVersion, testName, test, emptyMap());
                return true;
            } catch (final TestUpdateException e) {
                return false;
            }
        });
    }

    @Test
    public void testOneUpdateAndOneAdd() throws StoreException, InterruptedException, ExecutionException {
        final TestDefinition newTst1 = createDummyTestDefinition("3", "tst1");
        newTst1.setDescription("updated description tst1");
        final TestDefinition tst4 = createDummyTestDefinition("4", "tst4");

        final FutureTask<Boolean> updateFuture = getFutureTaskUpdateTestDefinition("2", "tst1", newTst1, "Update description of tst1");
        final Thread addThread = new Thread(() -> {
            try {
                testee.addTestDefinition(
                        ChangeMetadata.builder()
                                .setUsernameAndAuthor("Tom")
                                .setPassword("pwd")
                                .setComment("Create tst4")
                                .build(),
                        "tst4", tst4, emptyMap());
            } catch (final TestUpdateException e) {
                fail();
            }
        });
        assertEquals(1, testee.getHistory("tst1", 0, 4).size());
        startThreadsAndSleep(new Thread(updateFuture), addThread);
        final boolean updateSuccess = updateFuture.get();
        assertEquals("description of tst4", testee.getCurrentTestDefinition("tst4").getDescription());
        if (updateSuccess) {
            assertEquals("updated description tst1", testee.getCurrentTestDefinition("tst1").getDescription());
            assertEquals(2, testee.getHistory("tst1", 0, 4).size());
            assertEquals("Update description of tst1", testee.getHistory("tst1", 0, 4).get(0).getMessage());
        }
    }

    @Test
    public void testDelete() throws StoreException, InterruptedException {
        final String initialRevision = delegate.getLatestVersion();
        final TestDefinition tst1 = createDummyTestDefinition("3", "tst1"); /* this would not be actually used */

        final Thread deleteThread = new Thread(() -> {
            try {
                final String lastRevision =
                        testee.getHistory(
                                "tst1", 0, 1
                        ).get(0).getRevision();
                testee.deleteTestDefinition(
                        ChangeMetadata.builder()
                                .setUsernameAndAuthor("Mike")
                                .setPassword("pwd")
                                .setComment("Delete tst1")
                                .build(),
                        lastRevision,
                        "tst1",
                        tst1
                );
            } catch (final StoreException e) {
                fail();
            }
        });
        startThreadsAndSleep(deleteThread);
        assertNotEquals(initialRevision, delegate.getLatestVersion());
        assertEquals(delegate.getLatestVersion(), testee.getLatestVersion());
        assertNull(testee.getCurrentTestDefinition("tst1"));
        assertNotNull(testee.getTestDefinition("tst1", initialRevision));
    }

    /*
     * Note this is not a useful way to test multithreading, and in this class it
     * is also used single-threaded for no good reason.
     *
     * Not cleaning it up because this file is due to be deleted soon
     */
    private static void startThreadsAndSleep(final Thread... threads) throws InterruptedException {
        for (final Thread thread : threads) {
            thread.setUncaughtExceptionHandler(ASSERTION_FAILURE_HANDLER);
            thread.start();
        }

        for (final Thread thread : threads) {
            thread.join();
        }
    }
}
