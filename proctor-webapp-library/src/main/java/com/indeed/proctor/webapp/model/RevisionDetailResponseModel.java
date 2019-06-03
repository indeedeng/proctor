package com.indeed.proctor.webapp.model;

import java.util.List;

public class RevisionDetailResponseModel {
    private final List<String> modifiedTests;

    public RevisionDetailResponseModel(final List<String> modifiedTests) {
        this.modifiedTests = modifiedTests;
    }

    public List<String> getModifiedTests() {
        return modifiedTests;
    }
}
