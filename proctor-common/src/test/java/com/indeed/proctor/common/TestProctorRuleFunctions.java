package com.indeed.proctor.common;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestProctorRuleFunctions {
    @Test
    public void testContains() {
        assertTrue(ProctorRuleFunctions.contains(Arrays.asList(1, 2, 3), 1));
        assertTrue(ProctorRuleFunctions.contains(Arrays.asList(1, 2, 3), 1L));
        assertTrue(ProctorRuleFunctions.contains(Arrays.asList(1, 2, 3), (byte)1));
        assertTrue(ProctorRuleFunctions.contains(Arrays.asList(1, 2, 3), (short)1));
        assertTrue(ProctorRuleFunctions.contains(Arrays.asList(1L, 2L, 3L), 1L));
        assertTrue(ProctorRuleFunctions.contains(Arrays.asList(1L, 2L, 3L), 1));

        assertFalse(ProctorRuleFunctions.contains(Arrays.asList(1, 2, 3), -1));
        assertFalse(ProctorRuleFunctions.contains(Arrays.asList(1, 2, 3), -1L));
        assertFalse(ProctorRuleFunctions.contains(Arrays.asList(1, 2, 3), "1"));
        assertFalse(ProctorRuleFunctions.contains(Arrays.asList(1, 2, 3), null));
        assertFalse(ProctorRuleFunctions.contains(Collections.emptyList(), 1));
    }
}