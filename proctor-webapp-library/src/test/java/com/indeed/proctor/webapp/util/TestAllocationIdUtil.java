package com.indeed.proctor.webapp.util;

import com.google.common.collect.Lists;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestDefinition;
import org.junit.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author xiaoyun
 */
public class TestAllocationIdUtil {
    @Test
    public void testGenerateAllocationId() {
        assertEquals("#A1", AllocationIdUtil.generateAllocationId(0, 1));
        assertEquals("#Z2", AllocationIdUtil.generateAllocationId(25, 2));
        assertEquals("#BA1", AllocationIdUtil.generateAllocationId(26, 1));
    }

    private TestDefinition createSampleTestDefinition() {
        final Allocation alloc1 = new Allocation("${country='us'}",
                Lists.newArrayList(new Range(0, 0.5d), new Range(1, 0.5d)),
                "#A1");
        final Allocation alloc2 = new Allocation("${country='jp'}",
                Lists.newArrayList(new Range(0, 0.5d), new Range(1, 0.5d)),
                "#B1");

        final TestDefinition testDefinition = new TestDefinition();
        testDefinition.setAllocations(Lists.newArrayList(alloc1, alloc2));
        testDefinition.setSalt("salt");
        return testDefinition;
    }

    @Test
    public void testGetOutdatedAllocations() {
        // When allocations do not change
        final TestDefinition previous = createSampleTestDefinition();
        TestDefinition current = createSampleTestDefinition();
        assertEquals(new HashSet<>(), AllocationIdUtil.getOutdatedAllocations(previous, current));

        // When rule changes, all should update
        current = createSampleTestDefinition();
        current.setRule("${lang=en}");
        assertEquals(new HashSet<>(Lists.newArrayList(current.getAllocations().get(0), current.getAllocations().get(1))), AllocationIdUtil.getOutdatedAllocations(previous, current));

        // When salt change, all should update
        current = createSampleTestDefinition();
        current.setSalt("newSalt");
        assertEquals(new HashSet<>(Lists.newArrayList(current.getAllocations().get(0), current.getAllocations().get(1))), AllocationIdUtil.getOutdatedAllocations(previous, current));

        // When allocation rule change, the rule-changed allocation and allocations after the rule-changed allocation
        current = createSampleTestDefinition();
        current.getAllocations().get(0).setRule("newRule");
        assertEquals(new HashSet<>(Lists.newArrayList(current.getAllocations().get(0), current.getAllocations().get(1))), AllocationIdUtil.getOutdatedAllocations(previous, current));

        // Add allocation, allocations after new allocation
        current = createSampleTestDefinition();
        current.getAllocations().add(1, new Allocation());
        assertEquals(new HashSet<>(Lists.newArrayList(current.getAllocations().get(2))), AllocationIdUtil.getOutdatedAllocations(previous, current));

        // Delete allocation, allocations after deleted allocation
        current = createSampleTestDefinition();
        current.getAllocations().remove(0);
        assertEquals(new HashSet<>(Lists.newArrayList(current.getAllocations().get(0))), AllocationIdUtil.getOutdatedAllocations(previous, current));

        // Unbalanced ratio change, only unbalanced allocation
        current = createSampleTestDefinition();
        current.getAllocations().get(0).setRanges(Lists.newArrayList(new Range(0, 0.2d), new Range(1, 0.8d)));
        assertEquals(new HashSet<>(Lists.newArrayList(current.getAllocations().get(0))), AllocationIdUtil.getOutdatedAllocations(previous, current));

        // Added allocations at the end
        current = createSampleTestDefinition();
        current.getAllocations().add(new Allocation());
        assertEquals(new HashSet<>(), AllocationIdUtil.getOutdatedAllocations(previous, current));

        // Deleted allocations at the end
        current = createSampleTestDefinition();
        current.getAllocations().remove(1);
        assertEquals(new HashSet<>(), AllocationIdUtil.getOutdatedAllocations(previous, current));
    }

    @Test
    public void testConvertCharactersToDecimal() {
        assertEquals(1, AllocationIdUtil.convertBase26ToDecimal("B".toCharArray()));
        assertEquals(26, AllocationIdUtil.convertBase26ToDecimal("BA".toCharArray()));
        assertEquals(28, AllocationIdUtil.convertBase26ToDecimal("BC".toCharArray()));
    }

    @Test
    public void testGetNextVersionOfAllocationId() {
        assertEquals("#A2", AllocationIdUtil.getNextVersionOfAllocationId("#A1"));
    }

    @Test
    public void testAllocationIdComparator() {
        final List<String> allocationIds = Lists.newArrayList("#B1", "#D1", "#A1");
        Collections.sort(allocationIds, AllocationIdUtil.ALLOCATION_ID_COMPARATOR);
        assertEquals(Lists.newArrayList("#A1", "#B1", "#D1"), allocationIds);
    }

    @Test
    public void testGetAllocationName() {
        assertEquals("A", AllocationIdUtil.getAllocationName("#A1"));
        assertEquals("A", AllocationIdUtil.getAllocationName("#A1111"));
        assertEquals("BA", AllocationIdUtil.getAllocationName("#BA1"));
        assertEquals("BBB", AllocationIdUtil.getAllocationName("#BBB123"));
    }
}
