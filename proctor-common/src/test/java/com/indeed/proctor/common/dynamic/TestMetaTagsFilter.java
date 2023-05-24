package com.indeed.proctor.common.dynamic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.indeed.proctor.common.Serializers;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestType;
import org.junit.Test;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

public class TestMetaTagsFilter {
    private static final ObjectMapper OBJECT_MAPPER = Serializers.lenient();

    @Test
    public void testMatchEmptyMetaTags() {
        assertThatThrownBy(() -> new MetaTagsFilter(emptySet()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("meta_tags should be non-empty string list.");
    }

    @Test
    public void testMatchSingleMetaTag() {
        final MetaTagsFilter filter = new MetaTagsFilter(ImmutableSet.of("test"));

        assertFilterMatchesEquals(true, filter, ImmutableList.of("test"));
        assertFilterMatchesEquals(true, filter, ImmutableList.of("foo", "test"));

        assertFilterMatchesEquals(false, filter, emptyList());
        assertFilterMatchesEquals(false, filter, ImmutableList.of("foo_test"));
    }

    @Test
    public void testMatchMetaTags() {
        final MetaTagsFilter filter = new MetaTagsFilter(ImmutableSet.of("test1", "test2"));

        assertFilterMatchesEquals(true, filter, ImmutableList.of("test1"));
        assertFilterMatchesEquals(true, filter, ImmutableList.of("test2"));
        assertFilterMatchesEquals(true, filter, ImmutableList.of("test1", "test2"));
        assertFilterMatchesEquals(true, filter, ImmutableList.of("test2", "test1"));
        assertFilterMatchesEquals(true, filter, ImmutableList.of("foo", "test1", "test2"));

        assertFilterMatchesEquals(false, filter, emptyList());
    }

    private void assertFilterMatchesEquals(
            final boolean expected, final MetaTagsFilter filter, final List<String> testMetaTags) {
        final ConsumableTestDefinition definition =
                new ConsumableTestDefinition(
                        "",
                        null,
                        TestType.EMAIL_ADDRESS,
                        null,
                        emptyList(),
                        emptyList(),
                        false,
                        emptyMap(),
                        null,
                        testMetaTags);

        assertEquals(expected, filter.matches("", definition));
    }

    @Test
    public void testSerializeMetaTagsFilter() throws JsonProcessingException {
        final MetaTagsFilter filter = new MetaTagsFilter(ImmutableSet.of("test1", "test2"));

        final String serializedFilter = OBJECT_MAPPER.writeValueAsString(filter);

        assertEquals(
                "{\"type\":\"meta_tags_filter\",\"meta_tags\":[\"test1\",\"test2\"]}",
                serializedFilter);
    }
}
