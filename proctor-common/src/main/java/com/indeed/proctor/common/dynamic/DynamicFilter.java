package com.indeed.proctor.common.dynamic;

import com.indeed.proctor.common.model.ConsumableTestDefinition;

/**
 * Filter to determine what tests you resolve dynamically besides static tests defined in a specification
 */
interface DynamicFilter {
    boolean matches(final String testName, final ConsumableTestDefinition testDefinition);
}
