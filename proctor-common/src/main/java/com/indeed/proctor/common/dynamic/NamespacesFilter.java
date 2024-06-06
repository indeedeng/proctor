package com.indeed.proctor.common.dynamic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.PayloadExperimentConfig;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@JsonTypeName("namespaces_filter")
public class NamespacesFilter implements DynamicFilter {
    private final Set<String> namespaces;

    public NamespacesFilter(@JsonProperty("namespaces") final Set<String> namespaces) {
        Preconditions.checkArgument(
                !CollectionUtils.isEmpty(namespaces),
                "namespaces should be non-empty string list.");
        this.namespaces = ImmutableSet.copyOf(namespaces);
    }

    @JsonProperty("namespaces")
    public Set<String> getNamespaces() {
        return this.namespaces;
    }

    @Override
    public boolean matches(final String testName, final ConsumableTestDefinition testDefinition) {
        final boolean isMatched =
                Optional.ofNullable(testDefinition.getPayloadExperimentConfig())
                        .map(PayloadExperimentConfig::getNamespaces).orElse(Collections.emptyList())
                        .stream()
                        .anyMatch(this.namespaces::contains);
        testDefinition.setDynamic(isMatched);
        return isMatched;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final NamespacesFilter that = (NamespacesFilter) o;
        return namespaces.equals(that.namespaces);
    }

    @Override
    public int hashCode() {
        return Objects.hash(namespaces);
    }
}
