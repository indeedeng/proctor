package com.indeed.proctor.integration.sample;

/** capture logs written in different ways for testing */
public class SampleGroupsLogger {

    private String logFullStringFromAbstractGroups;
    private String exposureString;

    public String getLogFullStringFromAbstractGroups() {
        return logFullStringFromAbstractGroups;
    }

    public SampleGroupsLogger setLogFullStringFromAbstractGroups(
            final String logFullStringFromAbstractGroups) {
        this.logFullStringFromAbstractGroups = logFullStringFromAbstractGroups;
        return this;
    }

    public String getExposureString() {
        return exposureString;
    }

    public SampleGroupsLogger setExposureString(final String exposureString) {
        this.exposureString = exposureString;
        return this;
    }
}
