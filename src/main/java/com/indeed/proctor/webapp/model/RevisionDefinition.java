package com.indeed.proctor.webapp.model;

import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.store.Revision;

public class RevisionDefinition {

    private Revision revision;

    public TestDefinition getDefinition() {
        return definition;
    }

    public Revision getRevision() {
        return revision;
    }

    private TestDefinition definition;


    public RevisionDefinition(Revision revision, TestDefinition definition) {
        this.revision = revision;
        this.definition = definition;
    }
}