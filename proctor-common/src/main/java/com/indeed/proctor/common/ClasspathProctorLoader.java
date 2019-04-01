package com.indeed.proctor.common;

import com.indeed.proctor.common.model.TestMatrixArtifact;

import javax.annotation.Nonnull;
import com.indeed.proctor.shaded.javax.el.FunctionMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Support class for loading a test matrix artifact from a JSON file
 * @author ketan
 */
public class ClasspathProctorLoader extends AbstractJsonProctorLoader {
    @Nonnull
    private final String resourcePath;

    public ClasspathProctorLoader(final ProctorSpecification specification, @Nonnull final String resourcePath, @Nonnull final FunctionMapper functionMapper) {
        super(ClasspathProctorLoader.class, specification, functionMapper);
        this.resourcePath = resourcePath;
    }

    @Nonnull
    @Override
    protected String getSource() {
        return resourcePath;
    }

    @Override
    protected TestMatrixArtifact loadTestMatrix() throws IOException, MissingTestMatrixException {
        final InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (resourceAsStream == null) {
            throw new MissingTestMatrixException("Could not load proctor test matrix from classpath: " + resourcePath);
        }
        final Reader reader = new InputStreamReader(resourceAsStream);
        return loadJsonTestMatrix(reader);
    }
}
