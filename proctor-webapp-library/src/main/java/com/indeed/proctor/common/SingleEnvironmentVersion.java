package com.indeed.proctor.common;

import com.indeed.proctor.store.Revision;

public class SingleEnvironmentVersion {
    private final Revision revision;
    private final String effectiveRevision;

    public SingleEnvironmentVersion(final Revision revision, final String effectiveRevision) {
        this.revision = revision;
        this.effectiveRevision = effectiveRevision;
    }

    public Revision getRevision() {
        return revision;
    }

    public String getVersion() {
        return effectiveRevision;
    }
}
