package com.indeed.proctor.common.dynamic;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.indeed.proctor.common.model.ConsumableTestDefinition;

/**
 * Filter to determine what tests you resolve dynamically besides required tests defined in a specification
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = TestNamePrefixFilter.class, name = "name_prefix"),
        @JsonSubTypes.Type(value = TestNamePatternFilter.class, name = "name_pattern")
})
public interface DynamicFilter {
    boolean matches(final String testName, final ConsumableTestDefinition testDefinition);
}
