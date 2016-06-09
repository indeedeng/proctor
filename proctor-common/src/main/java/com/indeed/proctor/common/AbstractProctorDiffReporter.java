package com.indeed.proctor.common;

import com.indeed.proctor.common.model.TestMatrixArtifact;

public class AbstractProctorDiffReporter {
    public void reportProctorDiff(final TestMatrixArtifact oldProctor, final TestMatrixArtifact newProctor) {
        // do nothing by default, provide your implementation to customize reporting
    }
}
