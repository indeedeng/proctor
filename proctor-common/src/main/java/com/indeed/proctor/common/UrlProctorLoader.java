package com.indeed.proctor.common;

import com.indeed.proctor.common.model.TestMatrixArtifact;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.el.FunctionMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Support class for loading a test matrix artifact from a URL-based JSON file
 * @author jack
 */
public class UrlProctorLoader extends AbstractJsonProctorLoader {
    @Nonnull
    private final URL inputURL;

    public UrlProctorLoader(@Nonnull final ProctorSpecification specification, @Nonnull final String inputUrl) throws MalformedURLException {
        this(specification, new URL(inputUrl));
    }

    public UrlProctorLoader(@Nonnull final ProctorSpecification specification, @Nonnull final String inputUrl, final FunctionMapper functionMapper) throws MalformedURLException {
        this(specification, new URL(inputUrl), functionMapper);
    }

    public UrlProctorLoader(@Nonnull final ProctorSpecification specification, @Nonnull final URL inputUrl) {
        this(specification, inputUrl, RuleEvaluator.FUNCTION_MAPPER);
    }

    public UrlProctorLoader(@Nonnull final ProctorSpecification specification, @Nonnull final URL inputUrl, final FunctionMapper functionMapper) {
        super(UrlProctorLoader.class, specification, functionMapper);
        this.inputURL = inputUrl;
    }

    @Nonnull
    @Override
    protected String getSource() {
        return inputURL.toString();
    }

    @CheckForNull
    @Override
    protected TestMatrixArtifact loadTestMatrix() throws IOException, TestMatrixOutdatedException {
        try (Reader reader = new BufferedReader(new InputStreamReader(inputURL.openStream()))) {
            return loadJsonTestMatrix(reader);
        }
    }
}
