package com.indeed.proctor.store.utils.test;

import com.google.common.collect.Lists;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.store.StoreException;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertNull;

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
                Collections.emptyMap(), "commit tst1");
        testee.addTestDefinition("William", "pwd", "tst2",
                createDummyTestDefinition("2", "tst2"),
                Collections.emptyMap(), "commit tst2");
    }

    @Test
    public void testEmptyProctorStore() throws StoreException {
        testee = new InMemoryProctorStore();
        assertThat(testee.getAllHistories()).isEmpty();
        assertThat(testee.getCurrentTestMatrix().getTestMatrixDefinition().getTests()).isEmpty();
        assertThatThrownBy(() -> testee.getTestMatrix("1"))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining("Unknown revision 1");
        assertThatThrownBy(() -> testee.getTestDefinition("tst1", "1"))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining("Unknown revision 1");
        assertNull(testee.getCurrentTestDefinition("tst1"));
        assertThat(testee.getAllHistories()).isEmpty();
        assertThat(testee.getMatrixHistory(0, 1))
                .containsExactly(
                        new Revision(
                                "0",
                                "proctor",
                                new Date(0),
                                "initialize in-memory store"
                        )
                );
        assertThat(testee.getHistory("tst1", 0, 1)).isEmpty();
        assertThatThrownBy(() -> testee.getHistory("tst1", "1", 0, 1))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining("Unknown revision 1");
        assertThat(testee.getLatestVersion()).isEqualTo("0");
        assertThat(testee.getRevisionDetails("0").getRevision().getAuthor())
                .isEqualTo("proctor");
        assertThat(testee.getRevisionDetails("0").getModifiedTests())
                .isEmpty();
    }

    @Test
    public void testAddTestDefinition() throws StoreException {
        final Map<String, List<Revision>> allHistories = testee.getAllHistories();
        final List<Revision> tst1 = allHistories.get("tst1");
        assertThat(tst1).hasSize(1);
        final Revision revision1 = tst1.get(0);
        assertThat(revision1.getAuthor()).isEqualTo("Mike");
        assertThat(revision1.getRevision()).isEqualTo("1");
        assertThat(revision1.getMessage()).isEqualTo("commit tst1");
        final List<Revision> tst2 = allHistories.get("tst2");
        assertThat(tst2).hasSize(1);
        final Revision revision2 = tst2.get(0);
        assertThat(revision2.getAuthor()).isEqualTo("William");
        assertThat(revision2.getRevision()).isEqualTo("2");
        assertThat(revision2.getMessage()).isEqualTo("commit tst2");
    }

    @Test
    public void testEditTest() throws StoreException {
        final TestDefinition dummyTestDefinition = createDummyTestDefinition("3", "tst1");
        dummyTestDefinition.setDescription("tst1 description has been updated");
        testee.updateTestDefinition("Alex", "pwd", "1", "tst1", dummyTestDefinition, Collections.emptyMap(), "update tst1 description");

        /* verify tst1 history */
        final Map<String, List<Revision>> allHistories = testee.getAllHistories();
        final List<Revision> tstRevisions = allHistories.get("tst1");
        assertThat(tstRevisions).hasSize(2);
        final Revision editRevision = tstRevisions.get(0);
        assertThat(editRevision.getMessage()).isEqualTo("update tst1 description");
        assertThat(editRevision.getAuthor()).isEqualTo("Alex");
        assertThat(editRevision.getRevision()).isEqualTo("3");

        /* verify tst2 history */
        assertThat(allHistories.get("tst2")).hasSize(1);

        final TestMatrixVersion currentTestMatrix = testee.getCurrentTestMatrix();
        assertThat(currentTestMatrix.getVersion()).isEqualTo("3");

        final Map<String, TestDefinition> tests = currentTestMatrix.getTestMatrixDefinition().getTests();
        assertThat(tests.get("tst1").getDescription()).isEqualTo("tst1 description has been updated");

        final List<Revision> tst1 = testee.getHistory("tst1", 0, 3);
        assertThat(tst1).hasSize(2);
        assertThat(tst1.get(0).getRevision()).isEqualTo("3");

        assertThat(testee.getHistory("tst1", 1, 0)).isEmpty();
        final List<Revision> tst11 = testee.getHistory("tst1", 1, 1);
        assertThat(tst11.get(0).getRevision()).isEqualTo("1");
    }

    @Test
    public void testEditTestIncorrectPreviousRevision() {
        final TestDefinition dummyTestDefinition = createDummyTestDefinition("3", "tst1");
        dummyTestDefinition.setDescription("tst1 description has been updated");
        assertThatThrownBy(() ->
            testee.updateTestDefinition("Alex", "pwd", "incorrectPreviousVersion",
                    "tst1", dummyTestDefinition, Collections.emptyMap(), "update tst1 description")
        ).isInstanceOf(StoreException.TestUpdateException.class);

    }

    @Test
    public void testDeleteTest() throws StoreException {
        final TestDefinition dummyTestDefinition = createDummyTestDefinition("3", "tst1");
        testee.deleteTestDefinition("Alex", "pwd", "1", "tst1", dummyTestDefinition, "Delete tst1");
        assertThat(testee.getLatestVersion()).isEqualTo("3");
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
