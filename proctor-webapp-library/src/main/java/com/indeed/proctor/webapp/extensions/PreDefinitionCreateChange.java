package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.common.model.TestDefinition;

import java.util.Map;

/**
 */
public interface PreDefinitionCreateChange {
    public DefinitionChangeLog preCreate(final TestDefinition testDefinition, final Map<String, String[]> extensionFields);
}
