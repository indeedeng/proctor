package com.indeed.proctor.common;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import org.apache.log4j.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.el.FunctionMapper;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/**
 * parses a Json source as TestMatrixArtifact
 */
public abstract class AbstractJsonProctorLoader extends AbstractProctorLoader {
    private static final Logger LOGGER = Logger.getLogger(AbstractJsonProctorLoader.class);
    private static final String TEST_MATRIX_ARTIFACT_JSON_KEY_AUDIT = "audit";
    private static final String TEST_MATRIX_ARTIFACT_JSON_KEY_TESTS = "tests";

    @Nonnull
    private final ObjectMapper objectMapper = Serializers.lenient();

    public AbstractJsonProctorLoader(@Nonnull final Class<?> cls, @Nonnull final ProctorSpecification specification, @Nonnull final FunctionMapper functionMapper) {
        super(cls, specification, functionMapper);
    }

    @CheckForNull
    protected TestMatrixArtifact loadJsonTestMatrix(@Nonnull final Reader reader) throws IOException {
        try {
            final TestMatrixArtifact testMatrixArtifact = new TestMatrixArtifact();

            final JsonFactory jsonFactory = new JsonFactory();
            final JsonParser jsonParser = jsonFactory.createParser(reader);

            Preconditions.checkState(jsonParser.nextToken() == JsonToken.START_OBJECT);

            while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
                Preconditions.checkState(jsonParser.currentToken() == JsonToken.FIELD_NAME);

                final String key = jsonParser.currentName();

                jsonParser.nextToken();
                Preconditions.checkState(jsonParser.currentToken() == JsonToken.START_OBJECT);
                switch (key) {
                    case TEST_MATRIX_ARTIFACT_JSON_KEY_AUDIT:
                        testMatrixArtifact.setAudit(objectMapper.readValue(jsonParser, Audit.class));
                        break;

                    case TEST_MATRIX_ARTIFACT_JSON_KEY_TESTS:
                        testMatrixArtifact.setTests(extractReferencedTests(jsonParser));
                        break;

                    default:
                        LOGGER.warn("Unknown test matrix artifact json key: '" + key + "'");
                        jsonParser.skipChildren();
                        break;
                }
            }

            Preconditions.checkNotNull(testMatrixArtifact.getAudit());
            Preconditions.checkNotNull(testMatrixArtifact.getTests());

            return testMatrixArtifact;
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

    private Map<String, ConsumableTestDefinition> extractReferencedTests(@Nonnull final JsonParser jsonParser) throws IOException {
        // use HashMap instead of ImmutableMap.Builder because null might be put
        final Map<String, ConsumableTestDefinition> tests = new HashMap<>();

        while (jsonParser.nextToken() != JsonToken.END_OBJECT) {
            Preconditions.checkState(jsonParser.currentToken() == JsonToken.FIELD_NAME);

            final String testName = jsonParser.currentName();
            jsonParser.nextToken();

            final ConsumableTestDefinition testDefinition;
            if (jsonParser.currentToken() == JsonToken.VALUE_NULL) {
                testDefinition = null;
            } else {
                Preconditions.checkState(jsonParser.currentToken() == JsonToken.START_OBJECT);

                testDefinition = objectMapper.readValue(jsonParser, ConsumableTestDefinition.class);
            }

            if (isTestReferenced(testName, testDefinition)) {
                tests.put(testName, testDefinition);
            }
        }

        return tests;
    }

    private boolean isTestReferenced(final String testName, final ConsumableTestDefinition testDefinition) {
        // check required tests
        if (Preconditions.checkNotNull(requiredTests).containsKey(testName)) {
            return true;
        }

        // skip null test definition
        if (testDefinition == null) {
            return false;
        }

        // check dynamic filters
        return dynamicFilters.matches(testName, testDefinition);
    }
}
