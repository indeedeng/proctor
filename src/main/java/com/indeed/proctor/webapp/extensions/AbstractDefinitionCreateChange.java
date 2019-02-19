package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.common.model.TestDefinition;

import java.util.Map;

public abstract class AbstractDefinitionCreateChange implements PreDefinitionCreateChange, PostDefinitionCreateChange{
    @Override
    public void postCreate(
            final TestDefinition testDefinition,
            final Map<String, String[]> extensionFields,
            final DefinitionChangeLogger logger
    ) {
    }

    @Override
    public void preCreate(
            final TestDefinition testDefinition,
            final Map<String, String[]> extensionFields,
            final DefinitionChangeLogger logger
    ) {
    }
}