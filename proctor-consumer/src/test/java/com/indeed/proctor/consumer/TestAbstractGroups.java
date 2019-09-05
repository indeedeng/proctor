package com.indeed.proctor.consumer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestAbstractGroups {

    public static final String CONTROL_TESTNAME = "bgtst";
    public static final String ACTIVE_TESTNAME = "abtst";
    public static final String GROUP_WITH_FALLBACK_TESTNAME = "groupwithfallbacktst";
    public static final String INACTIVE_TESTNAME = "btntst";

    public static final String NO_BUCKETS_WITH_FALLBACK_TESTNAME = "nobucketfallbacktst";
    public static final Bucket FALLBACK_BUCKET = createModelBucket(42);
    public static final Bucket FALLBACK_NOPAYLOAD_BUCKET = createModelBucket(66);

    static class TestGroups extends AbstractGroups {
        protected TestGroups(final ProctorResult proctorResult) {
            super(proctorResult);
        }
    }

    private TestGroups groups;

    @Before
    public void setUp() throws Exception {
        final ProctorResult proctorResult = new ProctorResult(
                "0",
                ImmutableMap.of(
                        CONTROL_TESTNAME, new TestBucket("control", 0, "control", createStringPayload("controlPayload")),
                        ACTIVE_TESTNAME, new TestBucket("active", 1, "active", createStringPayload("activePayload")),
                        GROUP_WITH_FALLBACK_TESTNAME, new TestBucket("active", 2, "active"),
                        INACTIVE_TESTNAME, new TestBucket("inactive", -1, "inactive")
                ),
                ImmutableMap.of(
                        CONTROL_TESTNAME, new Allocation(null, Arrays.asList(new Range(0, 1.0)), "#A1"),
                        ACTIVE_TESTNAME, new Allocation(null, Arrays.asList(new Range(1, 1.0)), "#B2"),
                        GROUP_WITH_FALLBACK_TESTNAME, new Allocation(null, Arrays.asList(new Range(2, 1.0)), "#B2"),
                        INACTIVE_TESTNAME, new Allocation(null, Arrays.asList(new Range(-1, 1.0)), "#C3")
                ),
                ImmutableMap.of(
                        CONTROL_TESTNAME, stubDefinitionWithVersion("vControl"),
                        ACTIVE_TESTNAME, stubDefinitionWithVersion("vActive"),
                        INACTIVE_TESTNAME, stubDefinitionWithVersion("vInactive"),
                        GROUP_WITH_FALLBACK_TESTNAME, stubDefinitionWithVersion(
                                "vGroupWithFallback",
                                new TestBucket(
                                        "fallbackBucket",
                                        FALLBACK_BUCKET.getValue(),
                                        "fallbackDesc",
                                        createStringPayload("fallback"))),
                        // has no buckets in result, but in definition
                        NO_BUCKETS_WITH_FALLBACK_TESTNAME, stubDefinitionWithVersion(
                                "vNoBuckets",
                                new TestBucket(
                                        "fallbackBucket",
                                        FALLBACK_BUCKET.getValue(),
                                        "fallbackDesc",
                                        createStringPayload("fallback")))
                )
        );
        groups = new TestGroups(proctorResult);
    }

    private Payload createStringPayload(final String value) {
        final Payload payload = new Payload();
        payload.setStringValue(value);
        return payload;
    }

    private ConsumableTestDefinition stubDefinitionWithVersion(final String version, TestBucket... buckets) {
        final ConsumableTestDefinition testDefinition = new ConsumableTestDefinition();
        testDefinition.setVersion(version);
        testDefinition.setBuckets(Arrays.asList(buckets));
        return testDefinition;
    }

    @Test
    public void testIsBucketActive() {
        assertFalse(groups.isBucketActive(CONTROL_TESTNAME, -1));
        assertTrue(groups.isBucketActive(CONTROL_TESTNAME, 0));
        assertFalse(groups.isBucketActive(CONTROL_TESTNAME, 1));

        assertFalse(groups.isBucketActive(ACTIVE_TESTNAME, -1));
        assertFalse(groups.isBucketActive(ACTIVE_TESTNAME, 0));
        assertTrue(groups.isBucketActive(ACTIVE_TESTNAME, 1));

        assertFalse(groups.isBucketActive(CONTROL_TESTNAME, -1, 42));
        assertTrue(groups.isBucketActive(CONTROL_TESTNAME, 0, 42));

        assertFalse(groups.isBucketActive("notexist", -1));
        assertTrue(groups.isBucketActive("notexist", 1, 1));
        assertFalse(groups.isBucketActive("notexist", 1, 2));

    }

    @Test
    public void testGetValue() {
        assertThat(groups.getValue(CONTROL_TESTNAME, 42)).isEqualTo(0);
        assertThat(groups.getValue(ACTIVE_TESTNAME, 42)).isEqualTo(1);
        assertThat(groups.getValue("notexist", 42)).isEqualTo(42);
    }

    @Test
    public void testGetTestVersions() {
        assertThat(groups.getTestVersions()).isEqualTo(ImmutableMap.builder()
                .put(CONTROL_TESTNAME, "vControl")
                .put(ACTIVE_TESTNAME, "vActive")
                .put(INACTIVE_TESTNAME, "vInactive")
                .put(GROUP_WITH_FALLBACK_TESTNAME, "vGroupWithFallback")
                .put(NO_BUCKETS_WITH_FALLBACK_TESTNAME, "vNoBuckets")
                .build());

        assertThat(groups.getTestVersions(Collections.emptySet())).isEqualTo(emptyMap());

        assertThat(groups.getTestVersions(ImmutableSet.of(CONTROL_TESTNAME, "notexist"))).isEqualTo(ImmutableMap.builder()
                .put(CONTROL_TESTNAME, "vControl")
                .build());
    }

    @Test
    public void testGetPayload() {
        assertThat(groups.getPayload(INACTIVE_TESTNAME)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groups.getPayload(ACTIVE_TESTNAME)).isEqualTo(createStringPayload("activePayload"));
        assertThat(groups.getPayload(CONTROL_TESTNAME)).isEqualTo(createStringPayload("controlPayload"));
        assertThat(groups.getPayload(ACTIVE_TESTNAME, FALLBACK_BUCKET)).isEqualTo(createStringPayload("activePayload"));
        assertThat(groups.getPayload(CONTROL_TESTNAME, FALLBACK_BUCKET)).isEqualTo(createStringPayload("controlPayload"));
        assertThat(groups.getPayload(GROUP_WITH_FALLBACK_TESTNAME, FALLBACK_BUCKET)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groups.getPayload(NO_BUCKETS_WITH_FALLBACK_TESTNAME, FALLBACK_BUCKET)).isEqualTo(createStringPayload("fallback"));
        assertThat(groups.getPayload(NO_BUCKETS_WITH_FALLBACK_TESTNAME, FALLBACK_NOPAYLOAD_BUCKET)).isEqualTo(Payload.EMPTY_PAYLOAD);
        assertThat(groups.getPayload("notexist")).isEqualTo(Payload.EMPTY_PAYLOAD);
    }

    @Test
    public void testToLongString() {
        assertThat(groups.toLongString()).isEqualTo("abtst-active,bgtst-control,btntst-inactive,groupwithfallbacktst-active");
    }

    @Test
    public void testToLoggingString() {
        assertThat(groups.toLoggingString()).isEqualTo("abtst1,bgtst0,groupwithfallbacktst2,#B2:abtst1,#A1:bgtst0,#B2:groupwithfallbacktst2");
    }

    @Test
    public void testGetLoggingTestNames() {
        assertEquals(
                Sets.newHashSet(CONTROL_TESTNAME, ACTIVE_TESTNAME, GROUP_WITH_FALLBACK_TESTNAME),
                Sets.newHashSet(groups.getLoggingTestNames())
        );
    }

    @Test
    public void testAppendTestGroupsWithoutAllocations() {
        final StringBuilder builder = new StringBuilder();
        groups.appendTestGroupsWithoutAllocations(builder, ',', Lists.newArrayList(CONTROL_TESTNAME, ACTIVE_TESTNAME));
        assertEquals(
                Sets.newHashSet("bgtst0", "abtst1"),
                Sets.newHashSet(builder.toString().split(","))
        );
    }

    @Test
    public void testAppendTestGroupsWithAllocations() {
        final StringBuilder builder = new StringBuilder();
        groups.appendTestGroupsWithAllocations(builder, ',', Lists.newArrayList(CONTROL_TESTNAME, ACTIVE_TESTNAME));
        assertEquals(
                Sets.newHashSet("#A1:bgtst0", "#B2:abtst1"),
                Sets.newHashSet(builder.toString().split(","))
        );
    }

    @Test
    public void testAppendTestGroups() {
        final StringBuilder builder = new StringBuilder();
        groups.appendTestGroups(builder, ',');
        assertEquals(
                Sets.newHashSet("groupwithfallbacktst2", "bgtst0", "abtst1", "#A1:bgtst0", "#B2:abtst1", "#B2:groupwithfallbacktst2"),
                Sets.newHashSet(builder.toString().split(","))
        );
    }

    @Test
    public void testGetJavaScriptConfig() {
        assertThat(groups.getJavaScriptConfig()).isEqualTo(ImmutableMap.builder()
                .put(ACTIVE_TESTNAME, 1)
                .put(CONTROL_TESTNAME, 0)
                .put(GROUP_WITH_FALLBACK_TESTNAME, 2)
            .build());
    }

    @Test
    public void testGetJavaScriptConfigLists() {
        assertThat(groups.getJavaScriptConfig(new StubTest[] {
                new StubTest("notexist", 42),
                new StubTest(CONTROL_TESTNAME, 43),
                new StubTest(ACTIVE_TESTNAME, 44)}))
                .isEqualTo(Arrays.asList(
                        Arrays.asList(42, null),
                        Arrays.asList(0, "controlPayload"),
                        Arrays.asList(1, "activePayload")
                ));
    }

    private static class StubTest implements com.indeed.proctor.consumer.Test {

        private final String name;
        private final int value;

        private StubTest(final String name, final int value) {
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


    private static Bucket createModelBucket(int value) {
        return new Bucket() {
            @Override
            public Enum getTest() {
                return null;
            }

            @Override
            public int getValue() {
                return value;
            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public String getFullName() {
                return null;
            }
        };
    }
}
