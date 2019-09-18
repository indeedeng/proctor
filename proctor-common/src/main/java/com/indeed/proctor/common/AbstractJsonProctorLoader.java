package com.indeed.proctor.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import org.apache.log4j.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.el.FunctionMapper;
import java.io.IOException;
import java.io.Reader;

/**
 * parses a Json source as TestMatrixArtifact
 */
public abstract class AbstractJsonProctorLoader extends AbstractProctorLoader {
    private static final Logger LOGGER = Logger.getLogger(AbstractJsonProctorLoader.class);

    @Nonnull
    private final ObjectMapper objectMapper = Serializers.lenient();

    public AbstractJsonProctorLoader(@Nonnull final Class<?> cls, @Nonnull final ProctorSpecification specification, @Nonnull final FunctionMapper functionMapper) {
        super(cls, specification, functionMapper);
    }

    @CheckForNull
    protected TestMatrixArtifact loadJsonTestMatrix(@Nonnull final Reader reader) throws IOException {
        try {
            return objectMapper.readValue(reader, TestMatrixArtifact.class);
        } catch (final IOException e) {
            LOGGER.error("Unable to load test matrix from " + getSource(), e);
            throw e;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    LOGGER.error("Suppressing throwable thrown when closing " + reader, e);
                }
            }
        }
    }
}
