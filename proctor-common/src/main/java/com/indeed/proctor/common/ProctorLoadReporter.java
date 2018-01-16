package com.indeed.proctor.common;

public interface ProctorLoadReporter {
    void reportReloaded(final Proctor oldProctor, final Proctor newProctor);
    void reportFailed(final Throwable t);
    void reportNoChange();
}
