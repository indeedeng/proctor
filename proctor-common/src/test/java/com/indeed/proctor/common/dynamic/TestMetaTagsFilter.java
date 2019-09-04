package com.indeed.proctor.common.dynamic;

import com.google.common.collect.ImmutableList;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestType;
import org.junit.Test;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestMetaTagsFilter {
    @Test
    public void testMatchEmptyMetaTags() {
        final MetaTagsFilter filter = new MetaTagsFilter(emptyList());

        assertFalse(filter.matches("", createTestDefinition(emptyList())));
        assertFalse(filter.matches("", createTestDefinition(ImmutableList.of("test"))));
        assertFalse(filter.matches("", createTestDefinition(ImmutableList.of("test", "test1"))));
    }

    @Test
    public void testMatchSingleMetaTag() {
        final MetaTagsFilter filter = new MetaTagsFilter(ImmutableList.of("test"));

        assertTrue(filter.matches("", createTestDefinition(ImmutableList.of("test"))));
        assertTrue(filter.matches("", createTestDefinition(ImmutableList.of("foo", "test"))));

        assertFalse(filter.matches("", createTestDefinition(emptyList())));
        assertFalse(filter.matches("", createTestDefinition(ImmutableList.of("foo_test"))));
    }

    @Test
    public void testMatchMetaTags() {
        final MetaTagsFilter filter = new MetaTagsFilter(ImmutableList.of("test1", "test2"));

        assertTrue(filter.matches("", createTestDefinition(ImmutableList.of("test1"))));
        assertTrue(filter.matches("", createTestDefinition(ImmutableList.of("test2"))));
        assertTrue(filter.matches("", createTestDefinition(ImmutableList.of("test1", "test2"))));
        assertTrue(filter.matches("", createTestDefinition(ImmutableList.of("test2", "test1"))));
        assertTrue(filter.matches("", createTestDefinition(ImmutableList.of("foo", "test1", "test2"))));

        assertFalse(filter.matches("", createTestDefinition(emptyList())));
    }

    private ConsumableTestDefinition createTestDefinition(final List<String> metaTags) {
        return new ConsumableTestDefinition(
                "",
                null,
                TestType.EMAIL_ADDRESS,
                null,
                emptyList(),
                emptyList(),
                false,
                emptyMap(),
                null,
                metaTags
        );
    }
}
