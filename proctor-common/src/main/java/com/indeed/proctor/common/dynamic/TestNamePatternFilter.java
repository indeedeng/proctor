package com.indeed.proctor.common.dynamic;

import com.indeed.proctor.common.model.ConsumableTestDefinition;

import java.util.regex.Pattern;

public final class TestNamePatternFilter implements DynamicFilter {
    private final Pattern pattern;

    public TestNamePatternFilter(final String regexp) {
        this.pattern = Pattern.compile(regexp);
    }

    @Override
    public boolean match(final String testName, final ConsumableTestDefinition testDefinition) {
        return pattern.matcher(testName).matches();
    }
}
