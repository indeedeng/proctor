package com.indeed.proctor.service.core.var;

import com.indeed.proctor.service.core.config.JsonVarConfig;

/**
 * A thin wrapper implementing PrefixVariable.
 */
public class Identifier extends PrefixVariable {
    public Identifier(final String varName, final JsonVarConfig varConfig) {
        super(varName, varConfig, "id");
    }

    @Override
    public String getDefaultValue() {
        // Identifiers are optional and should identify each user uniquely.
        // It wouldn't make sense to have a default because then all users would be put into the same bucket.
        return null;
    }
}
