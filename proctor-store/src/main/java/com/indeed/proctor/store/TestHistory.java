package com.indeed.proctor.store;

import java.util.List;

public class TestHistory {
    private final long[] versions;
    private final List<String> messages;

    public TestHistory(final long[] versions, final List<String> messages) {
        this.versions = versions;
        this.messages = messages;
    }

    public long[] getVersions() {
        return versions;
    }

    public List<String> getMessages() {
        return messages;
    }
}
