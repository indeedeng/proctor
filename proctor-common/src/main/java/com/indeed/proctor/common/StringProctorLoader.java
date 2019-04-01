package com.indeed.proctor.common;

import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.shaded.javax.el7.FunctionMapper;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

/**
 * Support class for loading a test matrix artifact from a JSON file
 * @author ketan
 */
public class StringProctorLoader extends AbstractJsonProctorLoader {
    @Nonnull
    private final String source;
    @Nonnull
    private final String testMatrixJson;

    public StringProctorLoader(
            @Nonnull final ProctorSpecification specification,
            @Nonnull final String source,
            @Nonnull final String testMatrixJson,
            @Nonnull final FunctionMapper functionMapper
    ) {
        super(StringProctorLoader.class, specification, functionMapper);

        this.source = source;
        this.testMatrixJson = testMatrixJson;
    }

    public StringProctorLoader(
            @Nonnull final ProctorSpecification specification,
            @Nonnull final String source,
            @Nonnull final String testMatrixJson
    ) {
        super(StringProctorLoader.class, specification, RuleEvaluator.FUNCTION_MAPPER);

        this.source = source;
        this.testMatrixJson = testMatrixJson;
    }

    @Nonnull
    @Override
    protected String getSource() {
        return source;
    }

    @Override
    protected TestMatrixArtifact loadTestMatrix() throws IOException {
        final Reader reader = new StringReader(testMatrixJson);
        return loadJsonTestMatrix(reader);
    }
}
