package com.indeed.proctor.common.dynamic;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.indeed.proctor.common.model.ConsumableTestDefinition;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable collection of dynamic filters which is defined in ProctorSpecification and consumed in AbstractProctorLoader
 */
public class DynamicFilters implements JsonSerializable {
    private final List<DynamicFilter> filters;

    public DynamicFilters() {
        this.filters = Collections.emptyList();
    }

    @JsonCreator
    public DynamicFilters(final Collection<? extends DynamicFilter> filters) {
        this.filters = ImmutableList.copyOf(filters);
    }

    public Set<String> determineTests(
            final Map<String, ConsumableTestDefinition> definedTests,
            final Set<String> requiredTests
    ) {
        final ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for (final Map.Entry<String, ConsumableTestDefinition> entry : definedTests.entrySet()) {
            final String testName = entry.getKey();
            final ConsumableTestDefinition testDefinition = entry.getValue();
            if ((testDefinition != null) && !requiredTests.contains(testName)) {
                for (final DynamicFilter filter : filters) {
                    if (filter.matches(testName, testDefinition)) {
                        builder.add(testName);
                    }
                }
            }
        }
        return builder.build();
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
    public void serialize(final JsonGenerator gen, final SerializerProvider serializers) throws IOException {
        final JsonSerializer<Object> serializer = serializers.findValueSerializer(DynamicFilter[].class);
        serializer.serialize(filters.toArray(), gen, serializers);
    }

    @Override
    public void serializeWithType(final JsonGenerator gen, final SerializerProvider serializers, final TypeSerializer typeSer) throws IOException {
        final JsonSerializer<Object> serializer = serializers.findValueSerializer(DynamicFilter[].class);
        serializer.serializeWithType(filters.toArray(), gen, serializers, typeSer);
    }
}
