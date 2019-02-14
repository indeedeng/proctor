package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.webapp.db.Environment;

import java.util.Map;

/**
 */
public abstract class AbstractDefinitionPromoteChange implements PreDefinitionPromoteChange, PostDefinitionPromoteChange{
    public DefinitionChangeLog prePromote(final TestDefinition testDefinition, final Map<String, String[]> extensionFields, final Environment src, final Environment destination) {
        return null;
    }


    public DefinitionChangeLog postPromote(final Map<String, String[]> extensionFields, final Environment src, final Environment destination) {
        return null;
    }
}