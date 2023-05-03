package com.indeed.proctor.common;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
    private static final Logger LOGGER = LogManager.getLogger(AbstractJsonProctorLoader.class);
    private static final String TEST_MATRIX_ARTIFACT_JSON_KEY_AUDIT = "audit";
    private static final String TEST_MATRIX_ARTIFACT_JSON_KEY_TESTS = "tests";
    private static final ObjectMapper OBJECT_MAPPER = Serializers.lenient();

    public AbstractJsonProctorLoader(
            @Nonnull final Class<?> cls,
            @Nonnull final ProctorSpecification specification,
            @Nonnull final FunctionMapper functionMapper
    ) {
        super(cls, specification, functionMapper);
    }

    public AbstractJsonProctorLoader(
            @Nonnull final Class<?> cls,
            @Nonnull final ProctorSpecification specification,
            @Nonnull final FunctionMapper functionMapper,
            @Nonnull final IdentifierValidator identifierValidator
    ) {
        super(cls, specification, functionMapper, identifierValidator);
    }

    /**
     * Load a part of test matrix json file as TestMatrixArtifact. Parsed test matrix json has the following structure:
     * {
     *     "audit": {
     *         ... // audit fields
     *     },
     *     "tests": {
     *         "test1": {
     *             ... // test definition fields
     *         },
     *         "test2": {
     *             ...
     *         },
     *         ...
     *     }
     * }.
     * The value for "tests" includes all of proctor tests, so it is very huge. In order to avoid big memory footprints,
     * this method only loads referenced tests, which are determined by requiredTests and dynamicFilters, by iterating over
     * entries under the value for "tests".
     *
     * @param reader
     * @return TestMatrixArtifact with referenced test definitions only
     * @throws IOException
     */
    @CheckForNull
<<<<<<< HEAD
    protected TestMatrixArtifact loadJsonTestMatrix(@Nonnull final Reader reader) throws IOException, TestMatrixOutdatedException {
||||||| parent of a496e85b (PROC-960: Remove autostyle code)
    protected TestMatrixArtifact loadJsonTestMatrix(@Nonnull final Reader reader)
            throws IOException {
=======
    protected TestMatrixArtifact loadJsonTestMatrix(@Nonnull final Reader reader) throws IOException {
>>>>>>> a496e85b (PROC-960: Remove autostyle code)
        try {
            final TestMatrixArtifact testMatrixArtifact = new TestMatrixArtifact();

            final JsonFactory jsonFactory = new JsonFactory();
            final JsonParser jsonParser = jsonFactory.createParser(reader);
            // At this point, currentToken() returns null.

            // Go to the next token, which must be "{".
            // This condition will be verified in consumeJson.
            jsonParser.nextToken();

            JsonParserUtils.consumeJson(
                    jsonParser,
                    (key, parser) -> {
                        switch (key) {
                            case TEST_MATRIX_ARTIFACT_JSON_KEY_AUDIT:
                                // The value for "audit" field must be an object.
                                Preconditions.checkState(parser.currentToken() == JsonToken.START_OBJECT);

                                testMatrixArtifact.setAudit(OBJECT_MAPPER.readValue(parser, Audit.class));
                                break;

                            case TEST_MATRIX_ARTIFACT_JSON_KEY_TESTS:
                                // The value for "tests" field must be an object.
                                Preconditions.checkState(parser.currentToken() == JsonToken.START_OBJECT);

                                testMatrixArtifact.setTests(extractReferencedTests(parser));
                                break;

                            default:
                                LOGGER.warn("Unknown test matrix artifact json key: '" + key + "'");
                                // If key is not either "audit" or "tests", just skip the value.
                                parser.skipChildren();
                                break;
                        }
                    }
            );

            Preconditions.checkNotNull(testMatrixArtifact.getAudit(), "Field \"audit\" was not found in json");
            Preconditions.checkNotNull(testMatrixArtifact.getTests(), "Field \"tests\" was not found in json");

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

    protected Map<String, ConsumableTestDefinition> extractReferencedTests(@Nonnull final JsonParser jsonParser) throws IOException {
        // use HashMap instead of ImmutableMap.Builder because null might be put
        final Map<String, ConsumableTestDefinition> tests = new HashMap<>();

        JsonParserUtils.consumeJson(
                jsonParser,
                (testName, parser) -> {
                    final ConsumableTestDefinition testDefinition = OBJECT_MAPPER.readValue(jsonParser, ConsumableTestDefinition.class);

                    if (isTestReferenced(testName, testDefinition)) {
                        tests.put(testName, testDefinition);
                    }
                }
        );

        return tests;
    }

    protected boolean isTestReferenced(final String testName, final ConsumableTestDefinition testDefinition) {
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
