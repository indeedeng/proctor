package com.indeed.proctor.common.dynamic;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.indeed.proctor.common.model.ConsumableTestDefinition;

import javax.annotation.Nullable;


/**
 * Filter that does matches anything. Provides no validation information, so should only be used for support services
 * which are not able to specify at build-time which tests they will filter.
 */
@JsonTypeName("match_all")
public class MatchAllFilter implements DynamicFilter {

    @Override
    public boolean matches(@Nullable final String testName, final ConsumableTestDefinition testDefinition) {
        return true;
    }

}
