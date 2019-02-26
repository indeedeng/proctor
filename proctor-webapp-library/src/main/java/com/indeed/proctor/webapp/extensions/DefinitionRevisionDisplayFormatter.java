package com.indeed.proctor.webapp.extensions;

import com.indeed.proctor.store.Revision;

/**
 */
public interface DefinitionRevisionDisplayFormatter {
    public String formatRevision(final Revision revision);
}
