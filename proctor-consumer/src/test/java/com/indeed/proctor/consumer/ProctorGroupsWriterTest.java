package com.indeed.proctor.consumer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Ordering;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.consumer.logging.TestGroupFormatter;
import org.assertj.core.util.Strings;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class ProctorGroupsWriterTest {

    private static final String INACTIVE_TEST_NAME = "a_inactive_tst";
    private static final String MISSING_DEFINITION_TEST_NAME = "b_missing_definition";
    private static final String EMPTY_ALLOCID_DEFINITION_TEST_NAME = "c_empty_alloc_id";
    private static final String GROUP1_TEST_NAME = "d_foo_tst";
    private static final String SILENT_TEST_NAME = "e_silent_tst";

    // for this test, buckets can be reused between test definitions, sorting by testname
    public static final TestBucket INACTIVE_BUCKET = new TestBucket("fooname", -1, "foodesc");
    public static final TestBucket CONTROL_BUCKET = new TestBucket("control", 0, "foodesc");
    public static final TestBucket GROUP1_BUCKET = new TestBucket("group1", 1, "foodesc");

    private static final Allocation ALLOCATION_A =
            new Allocation(null, Collections.emptyList(), "#A");
    private static final Allocation ALLOCATION_EMPTY_ID =
            new Allocation(null, Collections.emptyList(), "");

    // Using Ordering.natural() because stable Guava Immutable builder behavior only implemented
    // starting guava 22.0
    private static final Map<String, TestBucket> BUCKETS =
            new ImmutableSortedMap.Builder<String, TestBucket>(Ordering.natural())
                    .put(INACTIVE_TEST_NAME, INACTIVE_BUCKET)
                    .put(MISSING_DEFINITION_TEST_NAME, CONTROL_BUCKET)
                    .put(EMPTY_ALLOCID_DEFINITION_TEST_NAME, CONTROL_BUCKET)
                    .put(GROUP1_TEST_NAME, GROUP1_BUCKET)
                    .put(SILENT_TEST_NAME, GROUP1_BUCKET)
                    .build();
    private static final Map<String, Allocation> ALLOCATIONS =
            new ImmutableSortedMap.Builder<String, Allocation>(Ordering.natural())
                    .put(INACTIVE_TEST_NAME, ALLOCATION_A)
                    .put(MISSING_DEFINITION_TEST_NAME, ALLOCATION_A)
                    .put(EMPTY_ALLOCID_DEFINITION_TEST_NAME, ALLOCATION_EMPTY_ID)
                    .put(GROUP1_TEST_NAME, ALLOCATION_A)
                    .put(SILENT_TEST_NAME, ALLOCATION_A)
                    .build();

    private static final Map<String, ConsumableTestDefinition> DEFINITIONS =
            ImmutableMap.<String, ConsumableTestDefinition>builder()
                    .put(INACTIVE_TEST_NAME, stubDefinition(INACTIVE_BUCKET))
                    .put(EMPTY_ALLOCID_DEFINITION_TEST_NAME, stubDefinition(GROUP1_BUCKET))
                    .put(GROUP1_TEST_NAME, stubDefinition(GROUP1_BUCKET))
                    .put(SILENT_TEST_NAME, stubDefinition(GROUP1_BUCKET, d -> d.setSilent(true)))
                    .build();

    private static final ProctorResult PROCTOR_RESULT =
            new ProctorResult(null, BUCKETS, ALLOCATIONS, DEFINITIONS);

    @Test
    public void testWithEmptyResult() {
        final ProctorGroupsWriter simpleWriter =
                new ProctorGroupsWriter.Builder(TestGroupFormatter.WITH_ALLOC_ID).build();
        assertThat(
                        simpleWriter.writeGroupsAsString(
                                new ProctorResult(
                                        "v1",
                                        Collections.emptyMap(),
                                        Collections.emptyMap(),
                                        Collections.emptyMap())))
                .isEmpty();
    }

    @Test
    public void testDoubleFormattingWriter() {
        // legacy Indeed behavior
        final String expected =
                "b_missing_definition0,c_empty_alloc_id0,d_foo_tst1,#A:b_missing_definition0,#A:d_foo_tst1";

        final ProctorGroupsWriter defaultWriter =
                new ProctorGroupsWriter.Builder(
                                TestGroupFormatter.WITHOUT_ALLOC_ID,
                                TestGroupFormatter.WITH_ALLOC_ID)
                        .build();
        assertThat(defaultWriter.writeGroupsAsString(PROCTOR_RESULT))
                .isEqualTo(expected)
                .isEqualTo(
                        Strings.join(
                                        MISSING_DEFINITION_TEST_NAME + 0,
                                        EMPTY_ALLOCID_DEFINITION_TEST_NAME + 0,
                                        GROUP1_TEST_NAME + 1,
                                        "#A:" + MISSING_DEFINITION_TEST_NAME + 0,
                                        "#A:" + GROUP1_TEST_NAME + 1)
                                .with(","));
    }

    @Test
    public void testCustomWriter() {
        final ProctorGroupsWriter writerWithAllocIds =
                new ProctorGroupsWriter.Builder(TestGroupFormatter.WITH_ALLOC_ID)
                        .setIncludeSilentTests(true)
                        .setIncludeTestWithoutDefinition(false)
                        .setIncludeInactiveGroups(true)
                        .build();
        assertThat(writerWithAllocIds.writeGroupsAsString(PROCTOR_RESULT))
                .isEqualTo("#A:a_inactive_tst-1,#A:d_foo_tst1,#A:e_silent_tst1");

        final ProctorGroupsWriter writerWithoutAllocIds =
                new ProctorGroupsWriter.Builder(TestGroupFormatter.WITHOUT_ALLOC_ID)
                        .setIncludeSilentTests(true)
                        .setIncludeTestWithoutDefinition(false)
                        .build();
        assertThat(writerWithoutAllocIds.writeGroupsAsString(PROCTOR_RESULT))
                .isEqualTo("c_empty_alloc_id0,d_foo_tst1,e_silent_tst1");
    }

    @Test
    public void testWriterWithCustomFilter() {
        // example for using additional filter like exposure logging
        assertThat(
                        new ProctorGroupsWriter.Builder(TestGroupFormatter.WITH_ALLOC_ID)
                                .setAdditionalCustomFilter(
                                        (testName, proctorResult) ->
                                                testName.equals(GROUP1_TEST_NAME))
                                .build()
                                .writeGroupsAsString(PROCTOR_RESULT))
                .isEqualTo("#A:d_foo_tst1");
    }

    @Test
    public void testWriterCanReturnTheSameLoggingStringAsAbstractGroups() {
        assertThat(
                        new ProctorGroupsWriter.Builder(
                                        TestGroupFormatter.WITHOUT_ALLOC_ID,
                                        TestGroupFormatter.WITH_ALLOC_ID)
                                .build()
                                .writeGroupsAsString(PROCTOR_RESULT))
                .isEqualTo(new AbstractGroups(PROCTOR_RESULT) {}.toLoggingString());
    }

    @SafeVarargs
    private static ConsumableTestDefinition stubDefinition(
            final TestBucket buckets, final Consumer<ConsumableTestDefinition>... modifiers) {
        final ConsumableTestDefinition testDefinition = new ConsumableTestDefinition();
        testDefinition.setBuckets(Collections.singletonList(buckets));
        for (final Consumer<ConsumableTestDefinition> consumer : modifiers) {
            consumer.accept(testDefinition);
        }
        return testDefinition;
    }
}
