package com.indeed.proctor.webapp.model;

public enum BackgroundJobType {
    TEST_CREATION("test-creation"),
    TEST_EDIT("test-edit"),
    TEST_DELETION("test-deletion"),
    TEST_PROMOTION("test-promotion"),
    WORKING_DIRECTORY_CLEANING("working-directory-cleaning"),
    UNKNOWN("unknown");

    private final String name;

    BackgroundJobType(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
