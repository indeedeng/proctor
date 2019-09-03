package com.indeed.proctor.common.dynamic;

import com.indeed.proctor.common.model.ConsumableTestDefinition;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class TestTestNamePatternFilter {
    @Test
    public void testPrefixPattern() {
        final TestNamePatternFilter filter = new TestNamePatternFilter("abc_.*");
        assertTrue(filter.matches("abc_something", new ConsumableTestDefinition()));
        assertTrue(filter.matches("abc_", new ConsumableTestDefinition()));
        assertFalse(filter.matches("_abc_", new ConsumableTestDefinition()));
        assertFalse(filter.matches("abc", new ConsumableTestDefinition()));
    }

    @Test
    public void testSuffixPattern() {
        final TestNamePatternFilter filter = new TestNamePatternFilter(".*_abc");
        assertTrue(filter.matches("something_abc", new ConsumableTestDefinition()));
        assertTrue(filter.matches("_abc", new ConsumableTestDefinition()));
        assertFalse(filter.matches("abc_", new ConsumableTestDefinition()));
        assertFalse(filter.matches("abc", new ConsumableTestDefinition()));
    }

    @Test
    public void testMiddlePattern() {
        final TestNamePatternFilter filter = new TestNamePatternFilter("abc_[a-z]+_def_[a-z]+_hij");
        assertTrue(filter.matches("abc_abc_def_w_hij", new ConsumableTestDefinition()));
        assertFalse(filter.matches("abc__def__hij", new ConsumableTestDefinition()));
        assertFalse(filter.matches(" abc_a_def_w_hij ", new ConsumableTestDefinition()));
    }

    @Test
    public void testInvalidPattern() {
        try {
            new TestNamePatternFilter("[invalid syntax regex");
            fail("Expected as IllegalArgumentException to be thrown");
        } catch(final IllegalArgumentException e) {
            assertNotNull(e.getCause());
        }
    }
}
