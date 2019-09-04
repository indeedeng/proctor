package com.indeed.proctor.common.dynamic;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.indeed.proctor.common.model.ConsumableTestDefinition;

import java.util.List;

@JsonTypeName("meta_tags_filter")
public class MetaTagsFilter implements DynamicFilter {
    private final List<String> metaTags;

    public MetaTagsFilter(@JsonProperty("meta_tags") final List<String> metaTags) {
        this.metaTags = metaTags;
    }

    @Override
    public boolean matches(final String testName, final ConsumableTestDefinition testDefinition) {
        return this.metaTags.stream().anyMatch(metaTag -> testDefinition.getMetaTags().contains(metaTag));
    }
}
