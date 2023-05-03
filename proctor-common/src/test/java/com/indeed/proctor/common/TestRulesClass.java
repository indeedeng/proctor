package com.indeed.proctor.common;

public class TestRulesClass {
    private final boolean IPadder;
    public TestRulesClass() {
        this.IPadder = false;
    }
    public TestRulesClass (final boolean IPad) {
        this.IPadder = IPad;
    }
    public boolean isIPad() {
        return IPadder;
    }
}
