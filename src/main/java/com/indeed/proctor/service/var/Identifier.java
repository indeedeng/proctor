package com.indeed.proctor.service.var;

import com.indeed.proctor.service.JsonVarConfig;

/**
 * A thin wrapper implementing PrefixVariable.
 */
public class Identifier extends PrefixVariable {
    public Identifier(final String varName, final JsonVarConfig varConfig) {
        super(varName, varConfig, "id");
    }
}
