package com.indeed.proctor.consumer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.TestBucket;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Optional;

import static com.indeed.proctor.consumer.ProctorGroupStubber.CONTROL_BUCKET_WITH_PAYLOAD;
import static com.indeed.proctor.consumer.ProctorGroupStubber.StubTest.CONTROL_SELECTED_TEST;
import static com.indeed.proctor.consumer.ProctorGroupStubber.StubTest.GROUP1_SELECTED_TEST;
import static com.indeed.proctor.consumer.ProctorGroupStubber.StubTest.GROUP_WITH_FALLBACK_TEST;
import static com.indeed.proctor.consumer.ProctorGroupStubber.StubTest.HOLDOUT_MASTER_TEST;
import static com.indeed.proctor.consumer.ProctorGroupStubber.StubTest.INACTIVE_SELECTED_TEST;
import static com.indeed.proctor.consumer.ProctorGroupStubber.StubTest.MISSING_DEFINITION_TEST;
import static com.indeed.proctor.consumer.ProctorGroupStubber.buildSampleProctorResult;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestAbstractGroupsWithForced {

    /**
     * This is one simple example modifying a testbucket for whatever purpose.
     * Some purposes could be to implement sub-experiments, or have special environments with forced groups.
     */
    static class ProctorGroupsWithForced extends AbstractGroups {
        private final ProctorGroupStubber.StubTest testToForceToControl;

        ProctorGroupsWithForced(final ProctorResult proctorResult, final ProctorGroupStubber.StubTest testToForceToControl) {
            super(proctorResult);
            this.testToForceToControl = testToForceToControl;
        }

        @Override
        protected int overrideDeterminedBucketValue(final String testName, @Nonnull final TestBucket determinedBucket) {
            // override determined bucket from group1 to control
            if (testToForceToControl.getName().equals(testName)) {
                // return bucket with control value
                return Optional.ofNullable(getProctorResult().getTestDefinitions().get(testName))
                        .map(ConsumableTestDefinition::getBuckets)
                        // use control bucket instead of active
                        .flatMap(buckets -> buckets.stream().filter(b -> b.getValue() == 0).findFirst())
                        .map(TestBucket::getValue)
                        .orElse(determinedBucket.getValue());
            }
            return determinedBucket.getValue();
        }
    }
    private ProctorResult proctorResult;
    private AbstractGroups sampleGroupsWithForced;

    @Before
    public void setUp() {

        proctorResult = buildSampleProctorResult();

        // Using ProctorGroupsWithForced to make GROUP1_SELECTED_TEST select control instead of group1
        sampleGroupsWithForced = new ProctorGroupsWithForced(proctorResult, GROUP1_SELECTED_TEST);

    }

    @Test
    public void testIsBucketActive() {
        // forced control group
        assertFalse(sampleGroupsWithForced.isBucketActive(GROUP1_SELECTED_TEST.getName(), -1));
        assertTrue(sampleGroupsWithForced.isBucketActive(GROUP1_SELECTED_TEST.getName(), 0)); // forced
        assertFalse(sampleGroupsWithForced.isBucketActive(GROUP1_SELECTED_TEST.getName(), 1));
    }

    @Test
    public void testGetValue() {
        assertThat(sampleGroupsWithForced.getValue(CONTROL_SELECTED_TEST.getName(), 42)).isEqualTo(0);
        assertThat(sampleGroupsWithForced.getValue(GROUP1_SELECTED_TEST.getName(), 42)).isEqualTo(0); // forced
        assertThat(sampleGroupsWithForced.getValue("notexist", 42)).isEqualTo(42);
    }

    @Test
    public void testGetPayload() {
        assertThat(sampleGroupsWithForced.getPayload(INACTIVE_SELECTED_TEST.getName())).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(sampleGroupsWithForced.getPayload(GROUP1_SELECTED_TEST.getName())).isEqualTo(CONTROL_BUCKET_WITH_PAYLOAD.getPayload()); // forced
        assertThat(sampleGroupsWithForced.getPayload(CONTROL_SELECTED_TEST.getName())).isEqualTo(CONTROL_BUCKET_WITH_PAYLOAD.getPayload());
    }

    @Test
    public void testIsEmpty() {
        assertThat(sampleGroupsWithForced.isEmpty()).isFalse();
    }

    @Test
    public void testToLongString() {
        assertThat(sampleGroupsWithForced.toLongString()).isEqualTo("abtst-control,bgtst-control,btntst-inactive,groupwithfallbacktst-group1,holdout_tst-group1,no_definition_tst-group1");
    }

    @Test
    public void testToLoggingString() {
        assertThat((new AbstractGroups(new ProctorResult("0", emptyMap(), emptyMap(), emptyMap())) {}).toLoggingString()).isEmpty();
        assertThat(sampleGroupsWithForced.toLoggingString()).isEqualTo("abtst0,bgtst0,groupwithfallbacktst2,holdout_tst2,no_definition_tst2,#A1:abtst0,#A1:bgtst0,#A1:groupwithfallbacktst2,#A1:holdout_tst2,#A1:no_definition_tst2");
    }

    @Test
    public void testGetLoggingTestNames() {
        assertThat(Sets.newHashSet(sampleGroupsWithForced.getLoggingTestNames()))
                .containsExactlyInAnyOrder(CONTROL_SELECTED_TEST.getName(), GROUP1_SELECTED_TEST.getName(), GROUP_WITH_FALLBACK_TEST.getName(), HOLDOUT_MASTER_TEST.getName(), MISSING_DEFINITION_TEST.getName());
    }

    @Test
    public void testAppendTestGroupsWithoutAllocations() {
        StringBuilder builder = new StringBuilder();
        sampleGroupsWithForced.appendTestGroupsWithoutAllocations(builder, ',', Lists.newArrayList(CONTROL_SELECTED_TEST.getName(), GROUP1_SELECTED_TEST.getName()));
        assertThat(builder.toString().split(",")).containsExactly("bgtst0", "abtst0");
    }

    @Test
    public void testAppendTestGroupsWithAllocations() {
        StringBuilder builder = new StringBuilder();
        sampleGroupsWithForced.appendTestGroupsWithAllocations(builder, ',', Lists.newArrayList(CONTROL_SELECTED_TEST.getName(), GROUP1_SELECTED_TEST.getName()));
        assertThat(builder.toString().split(",")).containsExactly("#A1:bgtst0", "#A1:abtst0");
        builder = new StringBuilder();
    }

    @Test
    public void testAppendTestGroups() {
        StringBuilder builder = new StringBuilder();
        sampleGroupsWithForced.appendTestGroups(builder, ',');
        assertThat(builder.toString().split(","))
                .containsExactlyInAnyOrder(
                        "groupwithfallbacktst2", "bgtst0", "abtst0", "holdout_tst2",
                        "#A1:bgtst0", "#A1:abtst0", "#A1:groupwithfallbacktst2", "#A1:holdout_tst2",
                        "#A1:no_definition_tst2", "no_definition_tst2");
    }

    @Test
    public void testGetJavaScriptConfig() {
        assertThat(sampleGroupsWithForced.getJavaScriptConfig())
                .hasSize(5)
                .containsEntry(GROUP1_SELECTED_TEST.getName(), 0) // forced
                .containsEntry(CONTROL_SELECTED_TEST.getName(), 0)
                .containsEntry(GROUP_WITH_FALLBACK_TEST.getName(), 2)
                .containsEntry(HOLDOUT_MASTER_TEST.getName(), 2)
                .containsEntry(MISSING_DEFINITION_TEST.getName(), 2);
    }

    @Test
    public void testGetJavaScriptConfigLists() {
        assertThat(sampleGroupsWithForced.getJavaScriptConfig(new FakeTest[] {
                new FakeTest("notexist", 42),
                new FakeTest(CONTROL_SELECTED_TEST.getName(), 43),
                new FakeTest(GROUP1_SELECTED_TEST.getName(), 44)}))
                .containsExactly(
                        Arrays.asList(42, null),
                        Arrays.asList(0, CONTROL_BUCKET_WITH_PAYLOAD.getPayload().getStringValue()),
                        Arrays.asList(0, CONTROL_BUCKET_WITH_PAYLOAD.getPayload().getStringValue()) // forced
                );
    }


    private static class FakeTest implements com.indeed.proctor.consumer.Test {

        private final String name;
        private final int value;

        private FakeTest(final String name, final int value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getFallbackValue() {
            return value;
        }
    }

}
