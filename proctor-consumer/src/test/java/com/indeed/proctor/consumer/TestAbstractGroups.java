package com.indeed.proctor.consumer;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class TestAbstractGroups {
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
                        "bgtst", new TestBucket("control", 0, "control"),
                        "abtst", new TestBucket("active", 1, "active"),
                        "btntst", new TestBucket("inactive", -1, "inactive")
                ),
                ImmutableMap.of(
                        "bgtst", new Allocation(null, Arrays.asList(new Range(0, 1.0)), "#A1"),
                        "abtst", new Allocation(null, Arrays.asList(new Range(1, 1.0)), "#B2"),
                        "btntst", new Allocation(null, Arrays.asList(new Range(-1, 1.0)), "#C3")
                ),
                ImmutableMap.of(
                        "bgtst", new ConsumableTestDefinition(),
                        "abtst", new ConsumableTestDefinition(),
                        "btntst", new ConsumableTestDefinition()
                )
        );
        groups = new TestGroups(proctorResult);
    }

    @Test
    public void testGetLoggingTestNames() {
        assertEquals(
                Sets.newHashSet("bgtst", "abtst"),
                Sets.newHashSet(groups.getLoggingTestNames())
        );
    }

    @Test
    public void testAppendTestGroupsWithoutAllocations() {
        final StringBuilder builder = new StringBuilder();
        groups.appendTestGroupsWithoutAllocations(builder, ',', Lists.newArrayList("bgtst", "abtst"));
        assertEquals(
                Sets.newHashSet("bgtst0", "abtst1"),
                Sets.newHashSet(builder.toString().split(","))
        );
    }

    @Test
    public void testAppendTestGroupsWithAllocations() {
        final StringBuilder builder = new StringBuilder();
        groups.appendTestGroupsWithAllocations(builder, ',', Lists.newArrayList("bgtst", "abtst"));
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
                Sets.newHashSet("bgtst0", "abtst1", "#A1:bgtst0", "#B2:abtst1"),
                Sets.newHashSet(builder.toString().split(","))
        );
    }
}