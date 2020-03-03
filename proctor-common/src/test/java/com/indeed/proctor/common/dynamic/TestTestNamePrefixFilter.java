package com.indeed.proctor.common.dynamic;

import com.indeed.proctor.common.model.ConsumableTestDefinition;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestTestNamePrefixFilter {
    @Test
    public void testPrefixPattern() {
        final TestNamePrefixFilter filter = new TestNamePrefixFilter("abc_");
        assertTrue(filter.matches("abc_", new ConsumableTestDefinition()));
        assertTrue(filter.matches("abc_something", new ConsumableTestDefinition()));
        assertFalse(filter.matches(" abc_something", new ConsumableTestDefinition()));
        assertFalse(filter.matches("abc", new ConsumableTestDefinition()));
    }

    @Test
    public void testEmptyPrefix() {
        try {
            new TestNamePrefixFilter("");
            fail("Expected an IllegalArgumentException to be thrown");
        } catch (final IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("empty"));
        }
    }

    @Test
    public void testNullOrEmptyTestName() {
        final TestNamePrefixFilter filter = new TestNamePrefixFilter("n");
        assertFalse(filter.matches(null, new ConsumableTestDefinition()));
        assertFalse(filter.matches("", new ConsumableTestDefinition()));
    }
}
