package com.indeed.proctor.webapp.model;

import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.store.Revision;

public class RevisionDefinition {

    private Revision revision;
    private TestDefinition definition;

    public TestDefinition getDefinition() {
        return definition;
    }

    public Revision getRevision() {
        return revision;
    }

    public RevisionDefinition(Revision revision, TestDefinition definition) {
        this.revision = revision;
        this.definition = definition;
    }
}
