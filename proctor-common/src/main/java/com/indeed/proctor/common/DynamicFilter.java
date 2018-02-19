package com.indeed.proctor.common;

import com.indeed.proctor.common.model.ConsumableTestDefinition;

interface DynamicFilter {
    boolean match(final String testName, final ConsumableTestDefinition testDefinition);
}
