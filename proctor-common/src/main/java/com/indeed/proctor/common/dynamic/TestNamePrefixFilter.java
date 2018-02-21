package com.indeed.proctor.common.dynamic;

import com.indeed.proctor.common.model.ConsumableTestDefinition;

public final class TestNamePrefixFilter implements DynamicFilter {
    private final String prefix;

    public TestNamePrefixFilter(final String prefix) {
        this.prefix = prefix;
    }

    @Override
    public boolean matches(final String testName, final ConsumableTestDefinition testDefinition) {
        return testName.startsWith(prefix);
    }
}
