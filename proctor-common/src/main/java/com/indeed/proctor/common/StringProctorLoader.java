package com.indeed.proctor.common;

import com.indeed.proctor.common.model.TestMatrixArtifact;

import javax.annotation.Nonnull;
import javax.el.FunctionMapper;
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
        this(specification, source, testMatrixJson, RuleEvaluator.FUNCTION_MAPPER);
    }

    @Nonnull
    @Override
    protected String getSource() {
        return source;
    }

    @Override
    protected TestMatrixArtifact loadTestMatrix() throws IOException {
        try (Reader reader = new StringReader(testMatrixJson)) {
            return loadJsonTestMatrix(reader);
        }
    }
}
