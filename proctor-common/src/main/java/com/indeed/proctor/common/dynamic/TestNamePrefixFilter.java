package com.indeed.proctor.common.dynamic;

import com.google.common.base.Preconditions;
import com.indeed.proctor.common.model.ConsumableTestDefinition;

public class TestNamePrefixFilter implements DynamicFilter {
    private final String prefix;

    public TestNamePrefixFilter(final String prefix) {
        Preconditions.checkArgument(!prefix.isEmpty(), "Prefix should be non-empty string");
        this.prefix = prefix;
    }

    @Override
    public boolean matches(final String testName, final ConsumableTestDefinition testDefinition) {
        return testName.startsWith(prefix);
    }
}
