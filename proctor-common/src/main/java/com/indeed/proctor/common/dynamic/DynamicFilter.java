package com.indeed.proctor.common.dynamic;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.indeed.proctor.common.model.ConsumableTestDefinition;

import javax.annotation.Nullable;

/**
 * Filter to determine what tests you resolve dynamically besides required tests defined in a specification
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public interface DynamicFilter {
    /**
     * @param testName if null or empty string, only match testDefinition
     * @return true if the testname or the definition match the filter
     */
    boolean matches(@Nullable final String testName, final ConsumableTestDefinition testDefinition);
}
