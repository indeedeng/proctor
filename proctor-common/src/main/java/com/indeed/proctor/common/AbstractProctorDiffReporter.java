package com.indeed.proctor.common;

import com.indeed.proctor.common.model.TestMatrixArtifact;

public abstract class AbstractProctorDiffReporter implements ProctorLoadReporter {

    /* provide your implementation to customize reporting */
    public abstract void reportProctorDiff(final TestMatrixArtifact oldProctor, final TestMatrixArtifact newProctor);

    @Override
    public void reportReloaded(final Proctor oldProctor, final Proctor newProctor) {
        if ((oldProctor != null) && (newProctor != null)) {
            reportProctorDiff(oldProctor.getArtifact(), newProctor.getArtifact());
        }
    }

    @Override
    public void reportFailed(final Throwable t) {
        /* do nothing */
    }

    @Override
    public void reportNoChange() {
        /* do nothing */
    }
}
