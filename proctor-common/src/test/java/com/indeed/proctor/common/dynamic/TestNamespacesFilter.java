package com.indeed.proctor.common.dynamic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.indeed.proctor.common.Serializers;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.PayloadExperimentConfig;
import com.indeed.proctor.common.model.TestType;
import org.junit.Test;

import java.util.List;

import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

public class TestNamespacesFilter {
    private static final ObjectMapper OBJECT_MAPPER = Serializers.lenient();

    @Test
    public void testMatchEmptyMetaTags() {
        assertThatThrownBy(() -> new NamespacesFilter(emptySet()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("namespaces should be non-empty string list.");
    }

    @Test
    public void testMatchSingleMetaTag() {
        final NamespacesFilter filter = new NamespacesFilter(ImmutableSet.of("test"));

        assertFilterMatchesEquals(true, filter, ImmutableList.of("test"));
        assertFilterMatchesEquals(true, filter, ImmutableList.of("foo", "test"));

        assertFilterMatchesEquals(false, filter, emptyList());
        assertFilterMatchesEquals(false, filter, ImmutableList.of("foo_test"));
    }

    @Test
    public void testMatchMetaTags() {
        final NamespacesFilter filter = new NamespacesFilter(ImmutableSet.of("test1", "test2"));

        assertFilterMatchesEquals(true, filter, ImmutableList.of("test1"));
        assertFilterMatchesEquals(true, filter, ImmutableList.of("test2"));
        assertFilterMatchesEquals(true, filter, ImmutableList.of("test1", "test2"));
        assertFilterMatchesEquals(true, filter, ImmutableList.of("test2", "test1"));
        assertFilterMatchesEquals(true, filter, ImmutableList.of("foo", "test1", "test2"));

        assertFilterMatchesEquals(false, filter, emptyList());
    }

    private void assertFilterMatchesEquals(
            final boolean expected,
            final NamespacesFilter filter,
            final List<String> testNamespaces) {
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
                        EMPTY_LIST);
        definition.setPayloadExperimentConfig(
                PayloadExperimentConfig.builder().namespaces(testNamespaces).build());

        assertEquals(expected, filter.matches("", definition));
        assertEquals(expected, definition.getDynamic());
    }

    @Test
    public void testSerializeMetaTagsFilter() throws JsonProcessingException {
        final NamespacesFilter filter = new NamespacesFilter(ImmutableSet.of("test1", "test2"));

        final String serializedFilter = OBJECT_MAPPER.writeValueAsString(filter);

        assertEquals(
                "{\"type\":\"namespaces_filter\",\"namespaces\":[\"test1\",\"test2\"]}",
                serializedFilter);
    }
}
