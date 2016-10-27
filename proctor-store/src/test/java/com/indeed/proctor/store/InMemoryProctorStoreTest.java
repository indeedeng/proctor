package com.indeed.proctor.store;

import com.google.common.collect.Lists;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.common.model.TestType;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author yiqing
 */
public class InMemoryProctorStoreTest {

    private InMemoryProctorStore testee;

    @Before
    public void setUp() throws StoreException.TestUpdateException {
        testee = new InMemoryProctorStore();
        testee.addTestDefinition("Mike", "pwd", "tst1",
                createDummyTestDefinition("1", "tst1"),
                Collections.<String, String>emptyMap(), "commit tst1");
        testee.addTestDefinition("William", "pwd", "tst2",
                createDummyTestDefinition("2", "tst2"),
                Collections.<String, String>emptyMap(), "commit tst2");
    }

    @Test
    public void testEmptyProctorStore() throws StoreException {
        testee = new InMemoryProctorStore();
        assertTrue(testee.getAllHistories().isEmpty());
        assertTrue(testee.getCurrentTestMatrix().getTestMatrixDefinition().getTests().isEmpty());
        assertNull(testee.getTestMatrix("revision-1"));
        assertNull(testee.getCurrentTestDefinition("tst1"));
        assertTrue(testee.getAllHistories().isEmpty());
        assertTrue(testee.getMatrixHistory(0, 1).isEmpty());
        assertTrue(testee.getHistory("tst1", 0, 1).isEmpty());
        assertEquals("-1", testee.getLatestVersion());
    }

    @Test
    public void testAddTestDefinition() throws StoreException {
        final Map<String, List<Revision>> allHistories = testee.getAllHistories();
        final List<Revision> tst1 = allHistories.get("tst1");
        assertEquals(1, tst1.size());
        final Revision revision1 = tst1.get(0);
        assertEquals("Mike", revision1.getAuthor());
        assertEquals("revision-1", revision1.getRevision());
        assertEquals("commit tst1", revision1.getMessage());
        final List<Revision> tst2 = allHistories.get("tst2");
        assertEquals(1, tst2.size());
        final Revision revision2 = tst2.get(0);
        assertEquals("William", revision2.getAuthor());
        assertEquals("revision-2", revision2.getRevision());
        assertEquals("commit tst2", revision2.getMessage());
    }

    @Test
    public void testEditTest() throws StoreException {
        final TestDefinition dummyTestDefinition = createDummyTestDefinition("3", "tst1");
        dummyTestDefinition.setDescription("tst1 description has been updated");
        testee.updateTestDefinition("Alex", "pwd", "2", "tst1", dummyTestDefinition, Collections.<String, String>emptyMap(), "update tst1 description");

        /* verify tst1 history */
        final Map<String, List<Revision>> allHistories = testee.getAllHistories();
        final List<Revision> tstRevisions = allHistories.get("tst1");
        assertEquals(2, tstRevisions.size());
        final Revision editRevision = tstRevisions.get(0);
        assertEquals("update tst1 description", editRevision.getMessage());
        assertEquals("Alex", editRevision.getAuthor());
        assertEquals("revision-3", editRevision.getRevision());

        /* verify tst2 history */
        assertEquals(1, allHistories.get("tst2").size());

        final TestMatrixVersion currentTestMatrix = testee.getCurrentTestMatrix();
        assertEquals("3", currentTestMatrix.getVersion());

        final Map<String, TestDefinition> tests = currentTestMatrix.getTestMatrixDefinition().getTests();
        assertEquals("tst1 description has been updated", tests.get("tst1").getDescription());

        final List<Revision> tst1 = testee.getHistory("tst1", 0, 3);
        assertEquals(2, tst1.size());
        assertEquals("revision-3", tst1.get(0).getRevision());

        assertEquals(0, testee.getHistory("tst1", 1, 0).size());
        final List<Revision> tst11 = testee.getHistory("tst1", 1, 1);
        assertEquals("revision-1", tst11.get(0).getRevision());
    }

    @Test
    public void testEditTestIncorrectPreviousRevision() {
        final TestDefinition dummyTestDefinition = createDummyTestDefinition("3", "tst1");
        dummyTestDefinition.setDescription("tst1 description has been updated");
        try {
            testee.updateTestDefinition("Alex", "pwd", "incorrectPreviousVersion", "tst1", dummyTestDefinition, Collections.<String, String>emptyMap(), "update tst1 description");
            fail();
        } catch (final StoreException.TestUpdateException e) {
            assertEquals("Previous version doesn't match", e.getCause().getMessage());
        }

    }

    @Test
    public void testDeleteTest() throws StoreException {
        final TestDefinition dummyTestDefinition = createDummyTestDefinition("3", "tst1");
        testee.deleteTestDefinition("Alex", "pwd", "2", "tst1", dummyTestDefinition, "Delete tst1");
        assertEquals("3", testee.getLatestVersion());
        assertNull(testee.getCurrentTestDefinition("tst1"));
    }

    public static TestDefinition createDummyTestDefinition(final String version, final String testName) {

        final List<TestBucket> buckets = Lists.newArrayList(
                new TestBucket("inactive", -1, ""),
                new TestBucket("control", 0, ""),
                new TestBucket("active", 1, "make it active")
        );

        final List<Allocation> allocations = Lists.newArrayList(
                new Allocation("", Lists.newArrayList(
                        new Range(0, 0.1),
                        new Range(-1, 0.8),
                        new Range(1, 0.1)
                ))
        );

        final Map<String, Object> constants = Collections.emptyMap();
        final Map<String, Object> specialConstants = Collections.emptyMap();
        return new TestDefinition(version,
                "rule-" + version,
                TestType.ANONYMOUS_USER,
                "salt-" + version,
                buckets,
                allocations,
                constants,
                specialConstants,
                "description of " + testName
        );
    }

}
