package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.webapp.db.Environment;

import java.util.Map;

/**
 */
public interface PostDefinitionPromoteChange {
    public DefinitionChangeLog postPromote(final Map<String, String[]> extensionFields, final Environment src, final Environment destination);
}
