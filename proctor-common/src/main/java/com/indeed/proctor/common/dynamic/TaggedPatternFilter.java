package com.indeed.proctor.common.dynamic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import org.apache.log4j.Logger;

import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * @author due
 *
 * At a high level, this filter behaves the same way as TestTestNamePatternFilter, with the added benefit that the set of
 * patterns can be specified under a tag, and the mapping of tags to patterns can be dynamically resolved at runtime via
 * {@link com.indeed.proctor.common.dynamic.PatternSupplier}.
 *
 * This dynamic filter expects a valid PatternSupplier to be set before any call to {@link #matches(String, ConsumableTestDefinition)}
 * are made.
 */
@JsonTypeName("tag")
public class TaggedPatternFilter implements DynamicFilter {
    private static final Logger LOGGER = Logger.getLogger(TaggedPatternFilter.class);
    private final List<String> tags;
    private PatternSupplier patternSupplier;
    /**
     * Construct the filter from tag list
     *
     * @param tags list of pattern tags to be included
     * @throws IllegalArgumentException if tags is null or empty list
     */
    public TaggedPatternFilter(@JsonProperty("tags") final List<String> tags) {
        Preconditions.checkNotNull(tags, "Tags must not be null");
        Preconditions.checkArgument(!tags.isEmpty(), "Tags must not be empty");
        this.tags = tags;
    }

    @Override
    public boolean matches(final String testName, final ConsumableTestDefinition testDefinition) {
        if (patternSupplier == null) {
            return false;
        }

        final List<Pattern> patterns = patternSupplier.getPatterns(tags);
        for (final Pattern compiledPattern : patterns) {
            if (compiledPattern.matcher(testName).matches()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final TaggedPatternFilter that = (TaggedPatternFilter) o;
        return Objects.equals(tags, that.tags) && Objects.equals(patternSupplier, that.patternSupplier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tags, patternSupplier);
    }

    /**
     * A PatternSupplier should be set by calling this method before any invokation of {@link #matches(String, ConsumableTestDefinition)}
     * @param patternSupplier
     */
    public void setPatternSupplier(final PatternSupplier patternSupplier) {
        this.patternSupplier = patternSupplier;
    }

    public List<String> getTags() {
        return ImmutableList.copyOf(tags);
    }
}
