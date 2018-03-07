package com.indeed.proctor.common.dynamic;

import com.indeed.proctor.common.model.ConsumableTestDefinition;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class TestNamePatternFilter implements DynamicFilter {
    private final Pattern pattern;

    /**
     * Construct the filter from regular expression string.
     *
     * @param regex regular expression for test name pattern
     * @throws IllegalArgumentException If the regular expression's syntax is invalid.
     */
    public TestNamePatternFilter(final String regex) {
        try {
            this.pattern = Pattern.compile(regex);
        } catch (final PatternSyntaxException e) {
            throw new IllegalArgumentException("the regular expression for test name pattern has syntax error.", e);
        }
    }

    @Override
    public boolean matches(final String testName, final ConsumableTestDefinition testDefinition) {
        return pattern.matcher(testName).matches();
    }
}
