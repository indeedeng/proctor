package com.indeed.proctor.common.dynamic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Strings;
import com.indeed.proctor.common.model.ConsumableTestDefinition;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * A dynamic filter that matches test name using regular expression
 * @deprecated Use {@link MetaTagsFilter}
 */
@Deprecated
@JsonTypeName("name_pattern")
public class TestNamePatternFilter implements DynamicFilter {
    private final Pattern pattern;

    /**
     * Construct the filter from regular expression string.
     *
     * @param regex regular expression for test name pattern
     * @throws IllegalArgumentException If the regular expression's syntax is invalid.
     */
    public TestNamePatternFilter(@JsonProperty("regex") final String regex) {
        try {
            this.pattern = Pattern.compile(regex);
        } catch (final PatternSyntaxException e) {
            throw new IllegalArgumentException("the regular expression for test name pattern has syntax error.", e);
        }
    }

    public String getRegex() {
        return pattern.pattern();
    }

    /**
     * @param testName if null or empty string, it doesn't match even if the pattern is like ".*".
     * @return true if the testname matches the pattern
     */
    @Override
    public boolean matches(@Nullable final String testName, final ConsumableTestDefinition testDefinition) {
        return !Strings.isNullOrEmpty(testName) && pattern.matcher(testName).matches();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TestNamePatternFilter that = (TestNamePatternFilter) o;
        return Objects.equals(pattern.pattern(), that.pattern.pattern());
    }

    @Override
    public int hashCode() {
        return Objects.hash(pattern.pattern());
    }
}
