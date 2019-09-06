package com.indeed.proctor.common.dynamic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.common.base.Preconditions;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.emptyList;

@JsonTypeName("meta_tags_filter")
public class MetaTagsFilter implements DynamicFilter {
    private final List<String> metaTags;

    public MetaTagsFilter(@JsonProperty("meta_tags") final List<String> metaTags) {
        Preconditions.checkArgument(!CollectionUtils.isEmpty(metaTags), "meta_tags should be non-empty string list.");
        this.metaTags = metaTags;
    }

    @Override
    public boolean matches(final String testName, final ConsumableTestDefinition testDefinition) {
        return this.metaTags.stream().anyMatch(metaTag -> testDefinition.getMetaTags().contains(metaTag));
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final MetaTagsFilter that = (MetaTagsFilter) o;
        return metaTags.equals(that.metaTags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(metaTags);
    }
}
