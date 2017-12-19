package com.indeed.proctor.webapp.util;

import com.google.common.collect.Lists;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestDefinition;
import org.junit.Test;

import java.util.HashSet;

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
        assertEquals(new HashSet<>(Lists.newArrayList(0, 1)), AllocationIdUtil.getOutdatedAllocations(previous, current));

        // When salt change, all should update
        current = createSampleTestDefinition();
        current.setSalt("newSalt");
        assertEquals(new HashSet<>(Lists.newArrayList(0, 1)), AllocationIdUtil.getOutdatedAllocations(previous, current));

        // When allocation rule change, the rule-changed allocation and allocations after the rule-changed allocation
        current = createSampleTestDefinition();
        current.getAllocations().get(0).setRule("newRule");
        assertEquals(new HashSet<>(Lists.newArrayList(0, 1)), AllocationIdUtil.getOutdatedAllocations(previous, current));

        // Add allocation, allocations after new allocation
        current = createSampleTestDefinition();
        current.getAllocations().add(1, new Allocation());
        assertEquals(new HashSet<>(Lists.newArrayList(2)), AllocationIdUtil.getOutdatedAllocations(previous, current));

        // Delete allocation, allocations after deleted allocation
        current = createSampleTestDefinition();
        current.getAllocations().remove(0);
        assertEquals(new HashSet<>(Lists.newArrayList(0)), AllocationIdUtil.getOutdatedAllocations(previous, current));

        // Unbalanced ratio change, only unbalanced allocation
        current = createSampleTestDefinition();
        current.getAllocations().get(0).setRanges(Lists.newArrayList(new Range(0, 0.2d), new Range(1, 0.8d)));
        assertEquals(new HashSet<>(Lists.newArrayList(0)), AllocationIdUtil.getOutdatedAllocations(previous, current));
    }

    @Test
    public void testConvertCharactersToDecimal() {
        assertEquals(1, AllocationIdUtil.convertCharactersToDecimal("B".toCharArray()));
        assertEquals(26, AllocationIdUtil.convertCharactersToDecimal("BA".toCharArray()));
        assertEquals(28, AllocationIdUtil.convertCharactersToDecimal("BC".toCharArray()));
    }

    @Test
    public void testGetNextVersionOfAllocationId() {
        assertEquals("#A2", AllocationIdUtil.getNextVersionOfAllocationId("#A1"));
    }

    @Test
    public void testPadAllocationIdWithAs() {
        assertEquals("AB", AllocationIdUtil.padAllocationIdWithAs("B", 2));
        assertEquals("B", AllocationIdUtil.padAllocationIdWithAs("B", 1));
    }
}
