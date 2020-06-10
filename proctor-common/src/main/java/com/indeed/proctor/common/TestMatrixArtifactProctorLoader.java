package com.indeed.proctor.common;

import com.indeed.proctor.common.model.TestMatrixArtifact;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.el.FunctionMapper;
import java.io.IOException;

/**
 * Support using an already-loaded test matrix with an arbitrary specification.
 * @author syd
 */

public class TestMatrixArtifactProctorLoader extends AbstractProctorLoader {
    @Nonnull
    private final String source;
    @Nonnull
    private final TestMatrixArtifact testMatrixArtifact;

    public TestMatrixArtifactProctorLoader(
            @Nonnull ProctorSpecification proctorSpecification,
            @Nonnull final String source,
            @Nonnull final TestMatrixArtifact testMatrixArtifact,
            @Nonnull final FunctionMapper functionMapper) {
        super(TestMatrixArtifactProctorLoader.class, proctorSpecification, functionMapper);
        this.source = source;
        this.testMatrixArtifact = new TestMatrixArtifact();
        this.testMatrixArtifact.setAudit(testMatrixArtifact.getAudit());
        this.testMatrixArtifact.setTests(testMatrixArtifact.getTests());
    }

    public TestMatrixArtifactProctorLoader(
            @Nonnull ProctorSpecification proctorSpecification,
            @Nonnull final String source,
            @Nonnull final TestMatrixArtifact testMatrixArtifact) {
        this(proctorSpecification, source, testMatrixArtifact, RuleEvaluator.FUNCTION_MAPPER);
    }

    @CheckForNull
    @Override
    protected TestMatrixArtifact loadTestMatrix() throws IOException, MissingTestMatrixException {
        return testMatrixArtifact;
    }

    @Nonnull
    @Override
    protected String getSource() {
        return source;
    }
}
