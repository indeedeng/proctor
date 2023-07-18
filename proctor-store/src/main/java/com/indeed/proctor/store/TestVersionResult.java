package com.indeed.proctor.store;

import java.util.Date;
import java.util.List;

/** @author parker */
public class TestVersionResult {
    private List<Test> tests;
    private Date published;
    private String author;
    private String version;
    private String description;

    public TestVersionResult(
            List<Test> tests, Date published, String author, String version, String description) {
        this.tests = tests;
        this.published = published;
        this.author = author;
        this.version = version;
        this.description = description;
    }

    public static class Test {
        final String testName;
        final String revision;

        public Test(String testName, String revision) {
            this.testName = testName;
            this.revision = revision;
        }

        public String getTestName() {
            return testName;
        }

        public String getRevision() {
            return revision;
        }
    }

    public List<Test> getTests() {
        return tests;
    }

    public Date getPublished() {
        return published;
    }

    public String getAuthor() {
        return author;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }
}
