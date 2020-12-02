package com.indeed.proctor.integration.sample;

/**
 * capture logs written in different ways for testing
 */
public class SampleGroupsLogger {

    private String logFullStringFromAbstractGroups;
    private String logFullStringFromWriter;
    private String exposureString;

    public String getLogFullStringFromAbstractGroups() {
        return logFullStringFromAbstractGroups;
    }

    public SampleGroupsLogger setLogFullStringFromAbstractGroups(String logFullStringFromAbstractGroups) {
        this.logFullStringFromAbstractGroups = logFullStringFromAbstractGroups;
        return this;
    }

    public String getExposureString() {
        return exposureString;
    }

    public SampleGroupsLogger setExposureString(String exposureString) {
        this.exposureString = exposureString;
        return this;
    }

    public String getLogFullStringFromWriter() {
        return logFullStringFromWriter;
    }

    public SampleGroupsLogger setLogFullStringFromWriter(String logFullStringFromWriter) {
        this.logFullStringFromWriter = logFullStringFromWriter;
        return this;
    }
}
