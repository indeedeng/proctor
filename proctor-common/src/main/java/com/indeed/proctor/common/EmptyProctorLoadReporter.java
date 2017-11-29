package com.indeed.proctor.common;

public class EmptyProctorLoadReporter implements ProctorLoadReporter {


    @Override
    public void reportReloaded(final Proctor oldProctor, final Proctor newProctor) {

    }

    @Override
    public void reportFailed(final Throwable t) {

    }

    @Override
    public void reportNoChange() {

    }
}
