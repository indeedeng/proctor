package com.indeed.proctor.common.dynamic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.indeed.proctor.common.model.ConsumableTestDefinition;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Immutable collection of dynamic filters which is defined in ProctorSpecification and consumed in
 * AbstractProctorLoader
 */
public class DynamicFilters implements JsonSerializable {
    private static final List<Class<? extends DynamicFilter>> FILTER_TYPES =
            Collections.synchronizedList(
                    Lists.newArrayList(
                            TestNamePrefixFilter.class,
                            TestNamePatternFilter.class,
                            MetaTagsFilter.class,
                            MatchAllFilter.class,
                            NamespacesFilter.class));

    private final List<DynamicFilter> filters;

    public DynamicFilters() {
        this.filters = Collections.emptyList();
    }

    @JsonCreator
    public DynamicFilters(final Collection<? extends DynamicFilter> filters) {
        this.filters = ImmutableList.copyOf(filters);
    }

    /** Register custom filter types especially for serializer of specification json file */
    @SafeVarargs
    public static void registerFilterTypes(final Class<? extends DynamicFilter>... types) {
        FILTER_TYPES.addAll(Arrays.asList(types));
    }

    /** Get filter types especially for Jackson's serializer to call registerSubTypes */
    @SuppressWarnings("unchecked")
    public static Class<? extends DynamicFilter>[] getFilterTypes() {
        return (Class<? extends DynamicFilter>[]) FILTER_TYPES.toArray(new Class<?>[0]);
    }

    /** Determine tests which should be dynamically resolved in proctor loader */
    public Set<String> determineTests(
            final Map<String, ConsumableTestDefinition> definedTests,
            final Set<String> requiredTests) {
        return definedTests.entrySet().stream()
                // Skip if testDefinition is null
                .filter(entry -> entry.getValue() != null)
                // Skip if testName exists in requiredTests
                .filter(entry -> !requiredTests.contains(entry.getKey()))
                // Check dynamicFilters
                .filter(entry -> matches(entry.getKey(), entry.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    public final boolean matches(
            @Nullable final String testName, final ConsumableTestDefinition testDefinition) {
        return filters.stream().anyMatch(filter -> filter.matches(testName, testDefinition));
    }

    /** @return unmodifiable view of underlying dynamic filters */
    public Collection<DynamicFilter> asCollection() {
        return Collections.unmodifiableCollection(filters);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final DynamicFilters that = (DynamicFilters) o;
        return Objects.equals(filters, that.filters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(filters);
    }

    @Override
    public void serialize(final JsonGenerator gen, final SerializerProvider serializers)
            throws IOException {
        final JsonSerializer<Object> serializer =
                serializers.findValueSerializer(DynamicFilter[].class);
        serializer.serialize(filters.toArray(), gen, serializers);
    }

    @Override
    public void serializeWithType(
            final JsonGenerator gen,
            final SerializerProvider serializers,
            final TypeSerializer typeSer)
            throws IOException {
        final JsonSerializer<Object> serializer =
                serializers.findValueSerializer(DynamicFilter[].class);
        serializer.serializeWithType(filters.toArray(), gen, serializers, typeSer);
    }
}
