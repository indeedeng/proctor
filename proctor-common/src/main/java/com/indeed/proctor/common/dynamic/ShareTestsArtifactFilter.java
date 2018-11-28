package com.indeed.proctor.common.dynamic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import org.apache.log4j.Logger;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

@JsonTypeName("tag")
public class ShareTestsArtifactFilter implements DynamicFilter {
    private static final Logger LOGGER = Logger.getLogger(ShareTestsArtifactFilter.class);
    private final List<String> tags;
    private Supplier<List<Pattern>> patternSupplier;
    /**
     * Construct the filter from tag list
     *
     * @param tags list of regex that matches the test names
     * @throws IllegalArgumentException if tags is empty string
     */
    public ShareTestsArtifactFilter(@JsonProperty("tags") final List<String> tags) {
        Preconditions.checkNotNull(tags, "Tags must not be null");
        Preconditions.checkArgument(!tags.isEmpty(), "Tags must not be empty");
        this.tags = tags;
    }

    @Override
    public boolean matches(final String testName, final ConsumableTestDefinition testDefinition) {
        if (patternSupplier == null) {
            return false;
        }

        for (final Pattern compiledPattern : patternSupplier.get()) {
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
        final ShareTestsArtifactFilter that = (ShareTestsArtifactFilter) o;
        return Objects.equals(tags, that.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tags);
    }

    public void setPatternSupplier(final Supplier<List<Pattern>> patternSupplier) {
        this.patternSupplier = patternSupplier;
    }

    public List<String> getTags() {
        return tags;
    }
}
