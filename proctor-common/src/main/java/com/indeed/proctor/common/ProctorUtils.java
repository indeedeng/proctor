package com.indeed.proctor.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.indeed.proctor.common.el.MulticontextReadOnlyVariableMapper;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.proctor.common.model.TestMatrixDefinition;
import com.indeed.proctor.common.model.TestMatrixVersion;
import com.indeed.proctor.common.model.TestType;
import org.apache.commons.lang3.StringUtils;
import org.apache.el.ExpressionFactoryImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Strings;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;

/** Helper functions mostly to verify TestMatrix instances. */
public abstract class ProctorUtils {
    private static final ObjectMapper OBJECT_MAPPER_NON_AUTOCLOSE =
            Serializers.lenient().configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
    private static final ObjectWriter OBJECT_WRITER =
            OBJECT_MAPPER_NON_AUTOCLOSE.writerWithDefaultPrettyPrinter();
    private static final ObjectMapper OBJECT_MAPPER = Serializers.lenient();
    private static final Logger LOGGER = LogManager.getLogger(ProctorUtils.class);
    private static final SpecificationGenerator SPECIFICATION_GENERATOR =
            new SpecificationGenerator();

    public static MessageDigest createMessageDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (final NoSuchAlgorithmException e) {
            throw new RuntimeException("Impossible no MD5", e);
        }
    }

    @Nonnull
    public static Map<String, ValueExpression> convertToValueExpressionMap(
            @Nonnull final ExpressionFactory expressionFactory,
            @Nonnull final Map<String, Object> values) {
        final Map<String, ValueExpression> context = new HashMap<>(values.size());
        for (final Entry<String, Object> entry : values.entrySet()) {
            final ValueExpression ve =
                    expressionFactory.createValueExpression(entry.getValue(), Object.class);
            context.put(entry.getKey(), ve);
        }
        return context;
    }

    @SuppressWarnings("UnusedDeclaration") // TODO Remove?
    public static String convertToArtifact(@Nonnull final TestMatrixVersion testMatrix)
            throws IOException {
        try (final StringWriter sw = new StringWriter()) {
            final TestMatrixArtifact artifact = convertToConsumableArtifact(testMatrix);
            serializeArtifact(sw, artifact);
            return sw.toString();
        }
    }

    /** @deprecated Use serialization library like jackson */
    @Deprecated
    public static void serializeArtifact(final Writer writer, final TestMatrixArtifact artifact)
            throws IOException {
        serializeObject(writer, artifact);
    }

    /** @deprecated Use serialization library like jackson */
    @Deprecated
    public static void serializeArtifact(final JsonGenerator jsonGenerator, final Proctor proctor)
            throws IOException {
        jsonGenerator.writeObject(proctor.getArtifact());
    }

    /** @deprecated Use serialization library like jackson */
    @Deprecated
    @SuppressWarnings("UnusedDeclaration") // TODO Remove?
    public static void serializeTestDefinition(final Writer writer, final TestDefinition definition)
            throws IOException {
        serializeObject(writer, definition);
    }

    /** @deprecated Use serialization library like jackson */
    @Deprecated
    @SuppressWarnings("UnusedDeclaration")
    public static JsonNode readJsonFromFile(final File input) throws IOException {
        return OBJECT_MAPPER.readValue(input, JsonNode.class);
    }

    /** @deprecated Use serialization library like jackson */
    @Deprecated
    public static void serializeTestSpecification(
            final Writer writer, final TestSpecification specification) throws IOException {
        serializeObject(writer, specification);
    }

    private static <T> void serializeObject(final Writer writer, final T artifact)
            throws IOException {
        OBJECT_WRITER.writeValue(writer, artifact);
    }

    @Nonnull
    public static TestMatrixArtifact convertToConsumableArtifact(
            @Nonnull final TestMatrixVersion testMatrix) {
        final Audit audit = new Audit();
        final Date published =
                Preconditions.checkNotNull(testMatrix.getPublished(), "Missing publication date");
        audit.setUpdated(published.getTime());
        audit.setVersion(testMatrix.getVersion());
        audit.setUpdatedBy(testMatrix.getAuthor());

        final TestMatrixArtifact artifact = new TestMatrixArtifact();
        artifact.setAudit(audit);

        final TestMatrixDefinition testMatrixDefinition =
                Preconditions.checkNotNull(
                        testMatrix.getTestMatrixDefinition(), "Missing test matrix definition");

        final Map<String, TestDefinition> testDefinitions = testMatrixDefinition.getTests();

        final Map<String, ConsumableTestDefinition> consumableTestDefinitions =
                Maps.newLinkedHashMap();
        for (final Entry<String, TestDefinition> entry : testDefinitions.entrySet()) {
            final TestDefinition td = entry.getValue();
            final ConsumableTestDefinition ctd = ConsumableTestDefinition.fromTestDefinition(td);
            consumableTestDefinitions.put(entry.getKey(), ctd);
        }

        artifact.setTests(consumableTestDefinitions);
        return artifact;
    }

    /** @deprecated Use {@link ConsumableTestDefinition#fromTestDefinition} */
    @Nonnull
    @Deprecated
    public static ConsumableTestDefinition convertToConsumableTestDefinition(
            @Nonnull final TestDefinition td) {
        return ConsumableTestDefinition.fromTestDefinition(td);
    }

    public static ProctorSpecification readSpecification(final File inputFile) {
        final ProctorSpecification spec;
        InputStream stream = null;
        try {
            stream = new BufferedInputStream(new FileInputStream(inputFile));
            spec = readSpecification(stream);
        } catch (final IOException e) {
            throw new RuntimeException("Unable to read test set from " + inputFile, e);
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (final IOException e) {
                    LOGGER.error("Suppressing throwable thrown when closing " + inputFile, e);
                }
            }
        }
        return spec;
    }

    public static ProctorSpecification readSpecification(final InputStream inputFile) {
        final ProctorSpecification spec;
        try {
            spec = OBJECT_MAPPER_NON_AUTOCLOSE.readValue(inputFile, ProctorSpecification.class);
        } catch (final IOException e) {
            throw new RuntimeException("Unable to read test set from " + inputFile, e);
        }
        return spec;
    }

    /**
     * Verifies that the TestMatrix is compatible with all the required tests. Removes non-required
     * tests from the TestMatrix Replaces invalid or missing tests (buckets are not compatible) with
     * default implementation returning the fallback value see defaultFor
     *
     * @param testMatrix the {@link TestMatrixArtifact} to be verified.
     * @param matrixSource a {@link String} of the source of proctor artifact. For example, a path
     *     of proctor artifact file.
     * @param requiredTests a {@link Map} of required test. The {@link TestSpecification} would be
     *     verified
     * @param functionMapper a given el {@link FunctionMapper}
     * @return a {@link ProctorLoadResult} to describe the result of verification. It contains
     *     errors of verification and a list of missing test.
     */
    public static ProctorLoadResult verifyAndConsolidate(
            @Nonnull final TestMatrixArtifact testMatrix,
            final String matrixSource,
            @Nonnull final Map<String, TestSpecification> requiredTests,
            @Nonnull final FunctionMapper functionMapper) {
        return verifyAndConsolidate(
                testMatrix,
                matrixSource,
                requiredTests,
                functionMapper,
                ProvidedContext.nonEvaluableContext(),
                Collections.emptySet());
    }

    public static ProctorLoadResult verifyAndConsolidate(
            @Nonnull final TestMatrixArtifact testMatrix,
            final String matrixSource,
            @Nonnull final Map<String, TestSpecification> requiredTests,
            @Nonnull final FunctionMapper functionMapper,
            final ProvidedContext providedContext) {
        return verifyAndConsolidate(
                testMatrix,
                matrixSource,
                requiredTests,
                functionMapper,
                providedContext,
                Collections.emptySet());
    }

    /** @param testMatrix will be modified by removing unused tests and adding missing tests */
    public static ProctorLoadResult verifyAndConsolidate(
            @Nonnull final TestMatrixArtifact testMatrix,
            final String matrixSource,
            @Nonnull final Map<String, TestSpecification> requiredTests,
            @Nonnull final FunctionMapper functionMapper,
            final ProvidedContext providedContext,
            @Nonnull final Set<String> dynamicTests) {
        final ProctorLoadResult result =
                verify(
                        testMatrix,
                        matrixSource,
                        requiredTests,
                        functionMapper,
                        providedContext,
                        dynamicTests);

        final Map<String, ConsumableTestDefinition> definedTests = testMatrix.getTests();
        // Remove any invalid tests so that any required ones will be replaced with default values
        // during the
        // consolidation below (just like missing tests). Any non-required tests can safely be
        // ignored.
        for (final String invalidTest : result.getTestsWithErrors()) {
            // TODO - mjs - gross that this depends on the mutability of the returned map, but then
            // so does the
            //  consolidate method below.
            definedTests.remove(invalidTest);
        }
        // Remove any invalid dynamic tests. This ones won't be replaced with default values
        // because they are not required tests.
        for (final String invalidDynamicTest : result.getDynamicTestWithErrors()) {
            definedTests.remove(invalidDynamicTest);
        }

        consolidate(testMatrix, requiredTests, dynamicTests);

        return result;
    }

    /**
     * Verifies that the TestMatrix is correct and sane without using a specification. The Proctor
     * API doesn't use a test specification so that it can serve all tests in the matrix without
     * restriction. Does a limited set of sanity checks that are applicable when there is no
     * specification, and thus no required tests or provided context.
     *
     * @param testMatrix the {@link TestMatrixArtifact} to be verified.
     * @param matrixSource a {@link String} of the source of proctor artifact. For example, a path
     *     of proctor artifact file.
     * @return a {@link ProctorLoadResult} to describe the result of verification. It contains
     *     errors of verification and a list of missing test.
     */
    public static ProctorLoadResult verifyWithoutSpecification(
            @Nonnull final TestMatrixArtifact testMatrix, final String matrixSource) {
        final ProctorLoadResult.Builder resultBuilder = ProctorLoadResult.newBuilder();

        for (final Entry<String, ConsumableTestDefinition> entry :
                testMatrix.getTests().entrySet()) {
            final String testName = entry.getKey();
            final ConsumableTestDefinition testDefinition = entry.getValue();

            try {
                verifyInternallyConsistentDefinition(testName, matrixSource, testDefinition);
            } catch (final IncompatibleTestMatrixException e) {
                LOGGER.info(String.format("Unable to load test matrix for %s", testName), e);
                resultBuilder.recordError(testName, e);
            }
        }
        return resultBuilder.build();
    }

    /** verify with default function mapper and empty context and no dynamic tests */
    public static ProctorLoadResult verify(
            @Nonnull final TestMatrixArtifact testMatrix,
            final String matrixSource,
            @Nonnull final Map<String, TestSpecification> requiredTests) {
        return verify(
                testMatrix,
                matrixSource,
                requiredTests,
                RuleEvaluator.FUNCTION_MAPPER,
                ProvidedContext.nonEvaluableContext(), // use default function mapper
                Collections.emptySet());
    }

    /** verify with default function mapper and empty context */
    public static ProctorLoadResult verify(
            @Nonnull final TestMatrixArtifact testMatrix,
            final String matrixSource,
            @Nonnull final Map<String, TestSpecification> requiredTests,
            @Nonnull final Set<String> dynamicTests) {
        return verify(
                testMatrix,
                matrixSource,
                requiredTests,
                RuleEvaluator.FUNCTION_MAPPER,
                ProvidedContext.nonEvaluableContext(), // use default function mapper
                dynamicTests);
    }

    /** verify with default function mapper and no dynamic tests */
    public static ProctorLoadResult verify(
            @Nonnull final TestMatrixArtifact testMatrix,
            final String matrixSource,
            @Nonnull final Map<String, TestSpecification> requiredTests,
            @Nonnull final FunctionMapper functionMapper,
            final ProvidedContext providedContext) {
        return verify(
                testMatrix,
                matrixSource,
                requiredTests,
                functionMapper,
                providedContext,
                Collections.emptySet());
    }

    /**
     * Does not mutate the TestMatrix. Verifies that the test matrix contains all the required tests
     * and that each required test is valid.
     *
     * @param testMatrix the {@link TestMatrixArtifact} to be verified.
     * @param matrixSource a {@link String} of the source of proctor artifact. For example, a path
     *     of proctor artifact file.
     * @param requiredTests a {@link Map} of required test. The {@link TestSpecification} would be
     *     verified
     * @param functionMapper a given el {@link FunctionMapper}
     * @param providedContext a {@link Map} containing variables describing the context in which the
     *     request is executing. These will be supplied to verifying all rules.
     * @param dynamicTests a {@link Set} of dynamic tests determined by filters.
     * @return a {@link ProctorLoadResult} to describe the result of verification. It contains
     *     errors of verification and a list of missing test.
     */
    public static ProctorLoadResult verify(
            @Nonnull final TestMatrixArtifact testMatrix,
            final String matrixSource,
            @Nonnull final Map<String, TestSpecification> requiredTests,
            @Nonnull final FunctionMapper functionMapper,
            final ProvidedContext providedContext,
            @Nonnull final Set<String> dynamicTests) {
        final ProctorLoadResult.Builder resultBuilder = ProctorLoadResult.newBuilder();

        final Set<String> testsToLoad = Sets.union(requiredTests.keySet(), dynamicTests);
        final Map<String, ConsumableTestDefinition> definedTests = testMatrix.getTests();

        final Set<String> missingTests = new HashSet<>();
        final Set<String> incompatibleTests = new HashSet<>();

        for (final String testName : testsToLoad) {
            if (!definedTests.containsKey(testName)) {
                // required by specification but missing in test matrix
                resultBuilder.recordMissing(testName);
                missingTests.add(testName);
            } else if (requiredTests.containsKey(testName)) {
                // required by specification
                try {
                    verifyRequiredTest(
                            testName,
                            definedTests.get(testName),
                            requiredTests.get(testName),
                            matrixSource,
                            functionMapper,
                            providedContext);
                } catch (final IncompatibleTestMatrixException e) {
                    resultBuilder.recordError(testName, e);
                    incompatibleTests.add(testName);
                }
            } else if (dynamicTests.contains(testName)) {
                // resolved by dynamic filter
                try {
                    verifyDynamicTest(
                            testName,
                            definedTests.get(testName),
                            matrixSource,
                            functionMapper,
                            providedContext);
                } catch (final IncompatibleTestMatrixException e) {
                    resultBuilder.recordIncompatibleDynamicTest(testName, e);
                    incompatibleTests.add(testName);
                }
            }
        }

        final Map<String, String> errorReasonsOfTestsByDependency =
                TestDependencies.validateDependenciesAndReturnReasons(
                        testsToLoad.stream()
                                .filter(
                                        testName ->
                                                !missingTests.contains(testName)
                                                        && !incompatibleTests.contains(testName))
                                .collect(
                                        Collectors.toMap(testName -> testName, definedTests::get)));

        errorReasonsOfTestsByDependency.forEach(
                (testName, errorReason) -> {
                    final String message = "Invalid dependency field is detected: " + errorReason;
                    if (requiredTests.containsKey(testName)) {
                        resultBuilder.recordError(
                                testName, new IncompatibleTestMatrixException(message));
                    } else if (dynamicTests.contains(testName)) {
                        resultBuilder.recordIncompatibleDynamicTest(
                                testName, new IncompatibleTestMatrixException(message));
                    }
                });

        resultBuilder.recordVerifiedRules(providedContext.shouldEvaluate());

        return resultBuilder.build();
    }

    /**
     * Verifies that a single required test is valid against {@link TestSpecification} and {@link
     * FunctionMapper} and {@link ProvidedContext}.
     *
     * @param testName the name of the test
     * @param testDefinition {@link ConsumableTestDefinition} of the test
     * @param testSpecification {@link TestSpecification} defined in an application for the test
     * @param matrixSource a {@link String} of the source of proctor artifact. For example, a path
     *     of proctor artifact file.
     * @param functionMapper a given el {@link FunctionMapper}
     * @param providedContext a {@link Map} containing variables describing the context in which the
     *     request is executing. These will be supplied to verifying all rules.
     * @throws IncompatibleTestMatrixException if validation is failed.
     */
    public static void verifyRequiredTest(
            @Nonnull final String testName,
            @Nonnull final ConsumableTestDefinition testDefinition,
            @Nonnull final TestSpecification testSpecification,
            @Nonnull final String matrixSource,
            @Nonnull final FunctionMapper functionMapper,
            final ProvidedContext providedContext)
            throws IncompatibleTestMatrixException {
        final Set<Integer> knownBucketValues = new HashSet<>();
        for (final Integer bucketValue : testSpecification.getBuckets().values()) {
            if (bucketValue == null) {
                throw new IncompatibleTestMatrixException(
                        "Test specification of " + testName + " has null in buckets");
            }
            if (!knownBucketValues.add(bucketValue)) {
                throw new IncompatibleTestMatrixException(
                        "Test specification of "
                                + testName
                                + " has duplicated buckets for value "
                                + bucketValue);
            }
        }

        verifyTest(
                testName,
                testDefinition,
                testSpecification,
                knownBucketValues,
                matrixSource,
                functionMapper,
                providedContext);
    }

    /**
     * Verifies that a single dynamic test is valid against {@link FunctionMapper} and {@link
     * ProvidedContext}.
     *
     * @param testName the name of the test
     * @param testDefinition {@link ConsumableTestDefinition} of the test
     * @param matrixSource a {@link String} of the source of proctor artifact. For example, a path
     *     of proctor artifact file.
     * @param functionMapper a given el {@link FunctionMapper}
     * @param providedContext a {@link Map} containing variables describing the context in which the
     *     request is executing. These will be supplied to verifying all rules.
     * @throws IncompatibleTestMatrixException if validation is failed.
     */
    public static void verifyDynamicTest(
            @Nonnull final String testName,
            @Nonnull final ConsumableTestDefinition testDefinition,
            @Nonnull final String matrixSource,
            @Nonnull final FunctionMapper functionMapper,
            final ProvidedContext providedContext)
            throws IncompatibleTestMatrixException {
        verifyTest(
                testName,
                testDefinition,
                // hack: use empty test spec to not verify buckets and payloads
                new TestSpecification(),
                // this parameter is ignored
                Collections.emptySet(),
                matrixSource,
                functionMapper,
                providedContext);
    }

    private static void verifyTest(
            @Nonnull final String testName,
            @Nonnull final ConsumableTestDefinition testDefinition,
            @Nonnull final TestSpecification testSpecification,
            @Nonnull final Set<Integer> knownBuckets,
            @Nonnull final String matrixSource,
            @Nonnull final FunctionMapper functionMapper,
            final ProvidedContext providedContext)
            throws IncompatibleTestMatrixException {
        final List<Allocation> allocations = testDefinition.getAllocations();

        final TestType declaredType = testDefinition.getTestType();
        if (!TestType.all().contains(declaredType)) {
            throw new IncompatibleTestMatrixException(
                    String.format(
                            "Test '%s' is included in the application specification but refers to unknown id type '%s'.",
                            testName, declaredType));
        }
        verifyInternallyConsistentDefinition(
                testName, matrixSource, testDefinition, functionMapper, providedContext);

        if (!testSpecification.getBuckets().isEmpty()) {
            /*
             * test the matrix for adherence to this application's requirements, if buckets were specified
             */
            final Set<Integer> unknownBuckets = Sets.newHashSet();

            for (final Allocation allocation : allocations) {
                final List<Range> ranges = allocation.getRanges();
                //  ensure that each range refers to a known bucket
                for (final Range range : ranges) {
                    // Externally consistent (application's requirements)
                    if (!knownBuckets.contains(range.getBucketValue())) {
                        // If the bucket has a positive allocation, add it to the list of
                        // unknownBuckets
                        if (range.getLength() > 0) {
                            unknownBuckets.add(range.getBucketValue());
                        }
                    }
                }
            }

            if (!unknownBuckets.isEmpty()) {
                final String bucketString =
                        (unknownBuckets.size() > 1 ? "bucket values: " : "bucket value: ")
                                + Strings.join(unknownBuckets, ',');
                throw new IncompatibleTestMatrixException(
                        "Proctor specification in your application does not contain "
                                + bucketString
                                + ". Please update the proctor specification first");
            }
        }

        final PayloadSpecification payloadSpec = testSpecification.getPayload();
        if (payloadSpec != null) {
            final String specifiedPayloadTypeName =
                    Preconditions.checkNotNull(payloadSpec.getType(), "Missing payload spec type");
            final PayloadType specifiedPayloadType =
                    PayloadType.payloadTypeForName(specifiedPayloadTypeName);
            final Map<String, String> specificationPayloadTypes = payloadSpec.getSchema();
            if (specifiedPayloadType == PayloadType.MAP) {
                if (specificationPayloadTypes == null || specificationPayloadTypes.isEmpty()) {
                    throw new IncompatibleTestMatrixException(
                            String.format(
                                    "The bucket definition of test %s has no payload, but the application is "
                                            + "expecting one. Add a payload to your test definition, or if there should not be one, "
                                            + "remove it from the application's Proctor specification. You can copy the Proctor "
                                            + "specification from the specification tab for the test on Proctor Webapp and add it "
                                            + "to the application's json file that contains the test specification.",
                                    testName));
                }
            }

            if (specifiedPayloadType == null) {
                // This is probably redundant vs. TestGroupsGenerator.
                throw new IncompatibleTestMatrixException(
                        "For test "
                                + testName
                                + " from "
                                + matrixSource
                                + " test specification payload type unknown: "
                                + specifiedPayloadTypeName);
            }
            final String payloadValidatorRule = payloadSpec.getValidator();

            // TODO(pwp): add some test constants?
            final RuleEvaluator ruleEvaluator =
                    makeRuleEvaluator(RuleEvaluator.EXPRESSION_FACTORY, functionMapper);

            for (final TestBucket bucket : testDefinition.getBuckets()) {
                final Payload payload = bucket.getPayload();
                if (payload != null) {
                    if (!Payload.hasType(payload, specifiedPayloadType)) {
                        throw new IncompatibleTestMatrixException(
                                "For test "
                                        + testName
                                        + " from "
                                        + matrixSource
                                        + " expected payload of type "
                                        + specifiedPayloadType.payloadTypeName
                                        + " but matrix has a test bucket payload with wrong type: "
                                        + bucket);
                    }
                    if (specifiedPayloadType == PayloadType.MAP) {
                        checkMapPayloadTypes(
                                payload,
                                specificationPayloadTypes,
                                matrixSource,
                                testName,
                                specifiedPayloadType,
                                payloadValidatorRule,
                                bucket,
                                functionMapper);
                    } else if (payloadValidatorRule != null) {
                        final boolean payloadIsValid =
                                evaluatePayloadValidator(
                                        ruleEvaluator, payloadValidatorRule, payload);
                        if (!payloadIsValid) {
                            throw new IncompatibleTestMatrixException(
                                    "For test "
                                            + testName
                                            + " from "
                                            + matrixSource
                                            + " payload validation rule "
                                            + payloadValidatorRule
                                            + " failed for test bucket: "
                                            + bucket);
                        }
                    }
                }
            }
        }
    }

    private static void checkMapPayloadTypes(
            final Payload payload,
            final Map<String, String> specificationPayloadTypes,
            final String matrixSource,
            final String testName,
            final PayloadType specifiedPayloadType,
            final String payloadValidatorRule,
            final TestBucket bucket,
            final FunctionMapper functionMapper)
            throws IncompatibleTestMatrixException {
        final RuleEvaluator ruleEvaluator =
                makeRuleEvaluator(RuleEvaluator.EXPRESSION_FACTORY, functionMapper);
        if (payload.getMap() == null) {
            throw new IncompatibleTestMatrixException(
                    "For test "
                            + testName
                            + " from "
                            + matrixSource
                            + " expected payload of type "
                            + specifiedPayloadType.payloadTypeName
                            + " but matrix has a test bucket payload with wrong type: "
                            + bucket);
        }
        if (specificationPayloadTypes.size() > payload.getMap().size()) {
            throw new IncompatibleTestMatrixException(
                    "For test "
                            + testName
                            + " from "
                            + matrixSource
                            + " expected payload of equal size to specification "
                            + specifiedPayloadType
                            + "  but matrix has a test bucket payload with wrong type size: "
                            + bucket);
        }
        final Map<String, Object> bucketPayloadMap = payload.getMap();
        for (final Entry<String, String> specificationPayloadEntry :
                specificationPayloadTypes.entrySet()) {
            if (!bucketPayloadMap.containsKey(specificationPayloadEntry.getKey())) {
                throw new IncompatibleTestMatrixException(
                        "For test "
                                + testName
                                + " from "
                                + matrixSource
                                + " expected payload of same order and variable names as specificied in "
                                + specifiedPayloadType
                                + " but matrix has a test bucket payload with wrong type: "
                                + bucket);
            }
            final PayloadType expectedPayloadType;
            try {
                expectedPayloadType =
                        PayloadType.payloadTypeForName(specificationPayloadEntry.getValue());
            } catch (final IllegalArgumentException e) {
                throw new IncompatibleTestMatrixException(
                        "For test "
                                + testName
                                + " from "
                                + matrixSource
                                + " specification payload type unknown in: "
                                + specifiedPayloadType.payloadTypeName);
            }
            if (expectedPayloadType == PayloadType.MAP) {
                throw new IncompatibleTestMatrixException(
                        "For test "
                                + testName
                                + " from "
                                + matrixSource
                                + " specification payload type has unallowed nested map types: "
                                + specifiedPayloadType.payloadTypeName);
            }
            final Object actualPayload = bucketPayloadMap.get(specificationPayloadEntry.getKey());
            if (actualPayload instanceof ArrayList) {
                for (final Object actualPayloadEntry : (ArrayList) actualPayload) {
                    final Class actualClazz = actualPayloadEntry.getClass();
                    if (PayloadType.STRING_ARRAY == expectedPayloadType) {
                        if (!String.class.isAssignableFrom(actualClazz)) {
                            throw new IncompatibleTestMatrixException(
                                    "For test "
                                            + testName
                                            + " from "
                                            + matrixSource
                                            + " expected payload of type "
                                            + specifiedPayloadType.payloadTypeName
                                            + " but matrix has a test bucket payload with wrong nested type: "
                                            + bucket);
                        }
                    } else if (PayloadType.LONG_ARRAY == expectedPayloadType
                            || PayloadType.DOUBLE_ARRAY == expectedPayloadType) {
                        if (!Number.class.isAssignableFrom(actualClazz)) {
                            throw new IncompatibleTestMatrixException(
                                    "For test "
                                            + testName
                                            + " from "
                                            + matrixSource
                                            + " expected payload of type "
                                            + specifiedPayloadType.payloadTypeName
                                            + " but matrix has a test bucket payload with wrong nested type: "
                                            + bucket);
                        }
                    } else {
                        throw new IncompatibleTestMatrixException(
                                "For test "
                                        + testName
                                        + " from "
                                        + matrixSource
                                        + " expected payload of type "
                                        + specifiedPayloadType.payloadTypeName
                                        + " but matrix has a test bucket payload with wrong nested type: "
                                        + bucket);
                    }
                }
            } else if (PayloadType.DOUBLE_ARRAY == expectedPayloadType
                    || PayloadType.STRING_ARRAY == expectedPayloadType
                    || PayloadType.LONG_ARRAY == expectedPayloadType) {
                throw new IncompatibleTestMatrixException(
                        "For test "
                                + testName
                                + " from "
                                + matrixSource
                                + " expected payload of type "
                                + specifiedPayloadType.payloadTypeName
                                + " but matrix has a test bucket payload with wrong nested type: "
                                + bucket);
            } else if (PayloadType.DOUBLE_VALUE == expectedPayloadType
                    || PayloadType.LONG_VALUE == expectedPayloadType) {
                final Class actualClazz = actualPayload.getClass();
                if (!Number.class.isAssignableFrom(actualClazz)) {
                    throw new IncompatibleTestMatrixException(
                            "For test "
                                    + testName
                                    + " from "
                                    + matrixSource
                                    + " expected payload of type "
                                    + specifiedPayloadType.payloadTypeName
                                    + " but matrix has a test bucket payload with wrong nested type: "
                                    + bucket);
                }
            } else {
                try {
                    if (!Class.forName("java.lang." + expectedPayloadType.javaClassName)
                            .isInstance(actualPayload)) {
                        throw new IncompatibleTestMatrixException(
                                "For test "
                                        + testName
                                        + " from "
                                        + matrixSource
                                        + " expected payload of type "
                                        + specifiedPayloadType.payloadTypeName
                                        + " but matrix has a test bucket payload with wrong nested type: "
                                        + bucket);
                    }
                } catch (final ClassNotFoundException e) {
                    throw new IncompatibleTestMatrixException(
                            "For test "
                                    + testName
                                    + " from "
                                    + matrixSource
                                    + " incompatible payload type?");
                }
            }
        }
        if (payloadValidatorRule != null) {
            final boolean payloadIsValid =
                    evaluatePayloadMapValidator(ruleEvaluator, payloadValidatorRule, payload);
            if (!payloadIsValid) {
                throw new IncompatibleTestMatrixException(
                        "For test "
                                + testName
                                + " from "
                                + matrixSource
                                + " payload validation rule "
                                + payloadValidatorRule
                                + " failed for test bucket: "
                                + bucket);
            }
        }
    }

    /**
     * minimizes TestMatrix by removing non-required test definitions, also add definitions from
     * missing tests
     */
    private static void consolidate(
            @Nonnull final TestMatrixArtifact testMatrix,
            @Nonnull final Map<String, TestSpecification> requiredTests,
            @Nonnull final Set<String> dynamicTests) {
        final Map<String, ConsumableTestDefinition> definedTests = testMatrix.getTests();

        // Sets.difference returns a "view" on the original set, which would require concurrent
        // modification while
        // iterating (copying the set will prevent this)
        final Set<String> toRemove =
                ImmutableSet.copyOf(
                        Sets.difference(
                                definedTests.keySet(),
                                Sets.union(requiredTests.keySet(), dynamicTests)));

        for (final String testInMatrixNotRequired : toRemove) {
            //  we don't care about this test
            definedTests.remove(testInMatrixNotRequired);
        }

        // Next, for any required tests that are missing, ensure that
        //  there is a nonnull test definition in the matrix
        final Set<String> missing =
                ImmutableSet.copyOf(Sets.difference(requiredTests.keySet(), definedTests.keySet()));
        for (final String testNotInMatrix : missing) {
            definedTests.put(
                    testNotInMatrix,
                    defaultFor(testNotInMatrix, requiredTests.get(testNotInMatrix)));
        }

        // Now go through definedTests: for each test, if the test spec
        // didn't ask for a payload, then remove any payload that is in
        // the test matrix.  If buckets exist in the specification that
        // do not in the matrix, add buckets with null payloads to allow
        // forcing buckets that aren't in the matrix but are in the spec.
        for (final Entry<String, ConsumableTestDefinition> next : definedTests.entrySet()) {
            final String testName = next.getKey();
            final ConsumableTestDefinition testDefinition = next.getValue();
            if (!requiredTests.containsKey(testName)) {
                // We don't care here about dynamically resolved tests
                continue;
            }
            final TestSpecification testSpec = requiredTests.get(testName);

            final boolean noPayloads = (testSpec.getPayload() == null);
            final Set<Integer> bucketValues = Sets.newHashSet();
            List<TestBucket> buckets = testDefinition.getBuckets();

            for (final TestBucket bucket : buckets) {
                // Note bucket values that exist in matrix.
                bucketValues.add(bucket.getValue());
            }

            if (noPayloads) {
                testDefinition.setBuckets(
                        buckets.stream()
                                .map(
                                        bucket ->
                                                TestBucket.builder()
                                                        .from(bucket)
                                                        .payload(null) // stomp the unexpected
                                                        // payload if exists.
                                                        .build())
                                .collect(Collectors.toList()));
            }

            boolean replaceBuckets = false;
            final Map<String, Integer> specBuckets = testSpec.getBuckets();
            for (final Entry<String, Integer> bucketSpec : specBuckets.entrySet()) {
                if (!bucketValues.contains(bucketSpec.getValue())) {
                    if (!replaceBuckets) {
                        buckets = Lists.newArrayList(buckets);
                        replaceBuckets = true;
                    }
                    buckets.add(
                            new TestBucket(bucketSpec.getKey(), bucketSpec.getValue(), null, null));
                }
            }

            if (replaceBuckets) {
                testDefinition.setBuckets(buckets);
            }
        }
    }

    @Nonnull
    private static ConsumableTestDefinition defaultFor(
            final String testName, @Nonnull final TestSpecification testSpecification) {
        final String missingTestSoleBucketName = "inactive";
        final String missingTestSoleBucketDescription = "fallback because missing in matrix";
        final int fallbackValue = testSpecification.getFallbackValue();
        final Allocation allocation =
                new Allocation(null, Collections.singletonList(new Range(fallbackValue, 1.0)));

        return ConsumableTestDefinition.fromTestDefinition(
                TestDefinition.builder()
                        .setVersion("default")
                        .setTestType(TestType.RANDOM)
                        .setSalt(testName)
                        .setBuckets(
                                ImmutableList.of(
                                        new TestBucket(
                                                missingTestSoleBucketName,
                                                fallbackValue,
                                                missingTestSoleBucketDescription)))
                        // Force a nonnull allocation just in case something somewhere assumes 1.0
                        // total allocation
                        .setAllocations(Collections.singletonList(allocation))
                        .setSilent(false) // non-silent, though typically fallbackValue -1 has same
                        // effect
                        .setDescription(testName)
                        .build());
    }

    public static ProvidedContext convertContextToTestableMap(
            final Map<String, String> providedContext) {
        return convertContextToTestableMap(providedContext, Collections.emptyMap());
    }

    public static ProvidedContext convertContextToTestableMap(
            final Map<String, String> providedContext,
            final Map<String, Object> ruleVerificationContext) {
        final ExpressionFactory expressionFactory = new ExpressionFactoryImpl();
        final Map<String, Object> primitiveVals = new HashMap<>();
        primitiveVals.put("int", 0);
        primitiveVals.put("integer", 0);
        primitiveVals.put("long", (long) 0);
        primitiveVals.put("bool", true);
        primitiveVals.put("boolean", true);
        primitiveVals.put("short", (short) 0);
        primitiveVals.put("string", "");
        primitiveVals.put("double", (double) 0);
        primitiveVals.put("char", "");
        primitiveVals.put("character", "");
        primitiveVals.put("byte", (byte) 0);

        if (providedContext != null) {
            final Map<String, Object> newProvidedContext = new HashMap<>();
            final Set<String> uninstantiatedIdentifiers = Sets.newHashSet();
            for (final Entry<String, String> entry : providedContext.entrySet()) {
                final String identifier = entry.getKey();
                Object toAdd = null;
                if (ruleVerificationContext.containsKey(identifier)) {
                    toAdd = ruleVerificationContext.get(identifier);
                    LOGGER.debug(
                            String.format(
                                    "Use instance for identifier {%s} provided by user %s",
                                    identifier, toAdd));
                } else {
                    LOGGER.debug(
                            String.format(
                                    "Identifier {%s} is not provided, instantiate it via default constructor",
                                    identifier));
                    final String iobjName = entry.getValue();
                    final String objName = iobjName;

                    if (primitiveVals.get(objName.toLowerCase()) != null) {
                        toAdd = primitiveVals.get(objName.toLowerCase());
                    } else {
                        try {
                            final Class clazz = Class.forName(objName);
                            if (clazz.isEnum()) { // If it is a user defined enum
                                toAdd = clazz.getEnumConstants()[0];
                            } else { // If it is a user defined non enum class
                                toAdd = clazz.newInstance();
                            }
                        } catch (final IllegalAccessException e) {
                            uninstantiatedIdentifiers.add(identifier);
                            LOGGER.debug(
                                    "Couldn't access default constructor of "
                                            + iobjName
                                            + " in providedContext. Rule verification will skip this identifier - "
                                            + identifier);
                        } catch (final InstantiationException e) {
                            uninstantiatedIdentifiers.add(identifier);
                            // if a default constructor is not defined, use this flag to not set
                            // context and not evaluate rules
                            LOGGER.debug(
                                    "Couldn't find default constructor for "
                                            + iobjName
                                            + " in providedContext. Rule verification will skip this identifier - "
                                            + identifier);
                        } catch (final ClassNotFoundException e) {
                            uninstantiatedIdentifiers.add(identifier);
                            LOGGER.error("Class not found for " + iobjName + " in providedContext");
                        }
                    }
                }
                newProvidedContext.put(identifier, toAdd);
            }
            /* evaluate the rule even if defaultConstructor method does not exist, */
            return ProvidedContext.forValueExpressionMap(
                    ProctorUtils.convertToValueExpressionMap(expressionFactory, newProvidedContext),
                    uninstantiatedIdentifiers);
        }
        return ProvidedContext.nonEvaluableContext();
    }

    /**
     * verifyInternallyConsistentDefinition with default functionMapper, but do not evaluate rule
     * against any context
     */
    public static void verifyInternallyConsistentDefinition(
            final String testName,
            final String matrixSource,
            @Nonnull final ConsumableTestDefinition testDefinition)
            throws IncompatibleTestMatrixException {
        verifyInternallyConsistentDefinition(
                testName, matrixSource, testDefinition, ProvidedContext.nonEvaluableContext());
    }

    /**
     * verifyInternallyConsistentDefinition with default functionMapper and evaluate against context
     */
    public static void verifyInternallyConsistentDefinition(
            final String testName,
            final String matrixSource,
            @Nonnull final ConsumableTestDefinition testDefinition,
            final ProvidedContext providedContext)
            throws IncompatibleTestMatrixException {
        verifyInternallyConsistentDefinition(
                testName,
                matrixSource,
                testDefinition,
                RuleEvaluator.FUNCTION_MAPPER,
                providedContext);
    }

    /**
     * verify: - test/allocation rules has valid syntax - if providedContext.shouldEvaluate, also
     * verifies that rule contains only identifiers from context - if
     * providedContext.shouldEvaluate, also verifies test/allocation rule evaluates to boolean -
     * buckets have same payload type
     *
     * @throws IncompatibleTestMatrixException on violations
     */
    private static void verifyInternallyConsistentDefinition(
            final String testName,
            final String matrixSource,
            @Nonnull final ConsumableTestDefinition testDefinition,
            final FunctionMapper functionMapper,
            final ProvidedContext providedContext)
            throws IncompatibleTestMatrixException {
        final List<Allocation> allocations = testDefinition.getAllocations();
        final ExpressionFactory expressionFactory = new ExpressionFactoryImpl();
        // verify test rule is valid EL
        final String testRule = testDefinition.getRule();

        final Map<String, ValueExpression> testConstants =
                ProctorUtils.convertToValueExpressionMap(
                        expressionFactory, testDefinition.getConstants());
        final VariableMapper variableMapper =
                new MulticontextReadOnlyVariableMapper(testConstants, providedContext.getContext());
        final RuleEvaluator ruleEvaluator =
                new RuleEvaluator(expressionFactory, functionMapper, testDefinition.getConstants());
        final ELContext elContext = ruleEvaluator.createELContext(variableMapper);

        try {
            RuleVerifyUtils.verifyRule(
                    testRule,
                    providedContext.shouldEvaluate(),
                    expressionFactory,
                    elContext,
                    providedContext.getUninstantiatedIdentifiers());
        } catch (final InvalidRuleException e) {
            throw new IncompatibleTestMatrixException(
                    String.format("Invalid rule in %s: %s", testName, e.getMessage()), e);
        }

        if (allocations.isEmpty()) {
            throw new IncompatibleTestMatrixException(
                    "No allocations specified in test " + testName);
        }
        final List<TestBucket> buckets = testDefinition.getBuckets();

        /*
         * test the definition for consistency with itself
         */
        final Set<Integer> definedBuckets = Sets.newHashSet();
        for (final TestBucket bucket : buckets) {
            definedBuckets.add(bucket.getValue());
        }

        for (int i = 0; i < allocations.size(); i++) {
            final Allocation allocation = allocations.get(i);
            final List<Range> ranges = allocation.getRanges();
            if ((ranges == null) || ranges.isEmpty()) {
                throw new IncompatibleTestMatrixException(
                        "Allocation range has no buckets, needs to add up to 1.");
            }
            //  ensure that each range refers to a known bucket
            double bucketTotal = 0;
            for (final Range range : ranges) {
                bucketTotal += range.getLength();
                // Internally consistent
                if (!definedBuckets.contains(range.getBucketValue())) {
                    throw new IncompatibleTestMatrixException(
                            "Allocation range in "
                                    + testName
                                    + " from "
                                    + matrixSource
                                    + " refers to unknown bucket value "
                                    + range.getBucketValue());
                }
            }
            //  I hate floating points.  TODO: extract a required precision constant/parameter?
            //  compensate for FP imprecision.  TODO: determine what these bounds really should be
            // by testing stuff
            if (bucketTotal < 0.9999 || bucketTotal > 1.0001) {
                throw new IncompatibleTestMatrixException(
                        testName
                                + " range with rule "
                                + allocation.getRule()
                                + " does not add up to 1 : "
                                + ranges.stream()
                                        .map(r -> Double.toString(r.getLength()))
                                        .collect(joining(" + "))
                                + " = "
                                + bucketTotal);
            }
            final String rule = allocation.getRule();
            final boolean lastAllocation = i == (allocations.size() - 1);
            if (!lastAllocation && isEmptyElExpression(rule)) {
                throw new IncompatibleTestMatrixException(
                        "Allocation["
                                + i
                                + "] for test "
                                + testName
                                + " from "
                                + matrixSource
                                + " has empty rule: "
                                + allocation.getRule());
            }

            try {
                RuleVerifyUtils.verifyRule(
                        rule,
                        providedContext.shouldEvaluate(),
                        expressionFactory,
                        elContext,
                        providedContext.getUninstantiatedIdentifiers());
            } catch (final InvalidRuleException e) {
                throw new IncompatibleTestMatrixException(
                        String.format(
                                "Invalid allocation rule in %s: %s", testName, e.getMessage()),
                        e);
            }
        }

        /*
         * When defined, within a single test, all test bucket payloads
         * should be supplied; they should all have just one type each,
         * and they should all be the same type.
         */
        Payload nonEmptyPayload = null;
        final List<TestBucket> bucketsWithoutPayloads = Lists.newArrayList();
        for (final TestBucket bucket : buckets) {
            final Payload p = bucket.getPayload();
            if (p != null) {
                if (p.numFieldsDefined() > 1) {
                    throw new IncompatibleTestMatrixException(
                            "Test "
                                    + testName
                                    + " from "
                                    + matrixSource
                                    + " has a test bucket payload with multiple types: "
                                    + bucket);
                }
                if (nonEmptyPayload == null) {
                    nonEmptyPayload = p;
                } else if (!nonEmptyPayload.sameType(p)) {
                    throw new IncompatibleTestMatrixException(
                            "Test "
                                    + testName
                                    + " from "
                                    + matrixSource
                                    + " has test bucket: "
                                    + bucket
                                    + " incompatible with type of payload: "
                                    + nonEmptyPayload);
                }
            } else {
                bucketsWithoutPayloads.add(bucket);
            }
        }
        if ((nonEmptyPayload != null) && (!bucketsWithoutPayloads.isEmpty())) {
            throw new IncompatibleTestMatrixException(
                    "Test "
                            + testName
                            + " from "
                            + matrixSource
                            + " has some test buckets without payloads: "
                            + bucketsWithoutPayloads);
        }
    }

    /**
     * Returns flag whose value indicates if the string is null, empty or only contains whitespace
     * characters
     *
     * @param s a string
     * @return true if the string is null, empty or only contains whitespace characters
     * @deprecated Use StringUtils.isBlank
     */
    @Deprecated
    static boolean isEmptyWhitespace(@Nullable final String s) {
        return StringUtils.isBlank(s);
    }

    /**
     * Generates a usable test specification for a given test definition Uses the bucket with
     * smallest value as the fallback value
     *
     * @param testDefinition a {@link TestDefinition}
     * @return a {@link TestSpecification} which corresponding to given test definition.
     * @deprecated use SpecificationGenerator
     */
    @Deprecated
    public static TestSpecification generateSpecification(
            @Nonnull final TestDefinition testDefinition) {
        return SPECIFICATION_GENERATOR.generateSpecification(testDefinition);
    }

    /**
     * Removes the expression braces "${ ... }" surrounding the rule.
     *
     * @param rule a given rule String.
     * @return the given rule with the most outside braces stripped
     */
    @CheckForNull
    public static String removeElExpressionBraces(@Nullable final String rule) {
        if (StringUtils.isBlank(rule)) {
            return null;
        }
        int startchar = 0; // inclusive
        int endchar = rule.length() - 1; // inclusive

        // garbage free trim()
        while (startchar < rule.length() && Character.isWhitespace(rule.charAt(startchar))) {
            ++startchar;
        }
        while (endchar > startchar && Character.isWhitespace(rule.charAt(endchar))) {
            --endchar;
        }

        if (rule.regionMatches(startchar, "${", 0, 2) && rule.charAt(endchar) == '}') {
            startchar += 2; // skip "${"
            --endchar; // skip '}'
        }
        // garbage free trim()
        while (startchar < rule.length() && Character.isWhitespace(rule.charAt(startchar))) {
            ++startchar;
        }
        while (endchar > startchar && Character.isWhitespace(rule.charAt(endchar))) {
            --endchar;
        }
        if (endchar < startchar) {
            // null instead of empty string for consistency with 'isEmptyWhitespace' check at the
            // beginning
            return null;
        }
        return rule.substring(startchar, endchar + 1);
    }

    /** An allocaction-free version of {@code StringUtils.isBlank(removeElExpressionBraces(s))} */
    static boolean isEmptyElExpression(@Nullable final String rule) {
        return ElExpressionClassification.EMPTY == clasifyElExpression(rule, false);
    }

    static ElExpressionClassification clasifyElExpression(
            @Nullable final String rule, final boolean checkForBooleanConstants) {
        if (StringUtils.isBlank(rule)) {
            return ElExpressionClassification.EMPTY;
        }
        int startchar = 0; // inclusive
        int endchar = rule.length() - 1; // inclusive

        // garbage free trim()
        while (startchar < rule.length() && Character.isWhitespace(rule.charAt(startchar))) {
            ++startchar;
        }
        while (endchar > startchar && Character.isWhitespace(rule.charAt(endchar))) {
            --endchar;
        }

        if (rule.regionMatches(startchar, "${", 0, 2) && rule.charAt(endchar) == '}') {
            startchar += 2; // skip "${"
            --endchar; // skip '}'
        }
        // garbage free trim()
        while (startchar < rule.length() && Character.isWhitespace(rule.charAt(startchar))) {
            ++startchar;
        }
        while (endchar > startchar && Character.isWhitespace(rule.charAt(endchar))) {
            --endchar;
        }
        if (endchar < startchar) {
            return ElExpressionClassification.EMPTY;
        }

        if (checkForBooleanConstants) {
            final String trueText = Boolean.TRUE.toString();
            if (trueText.regionMatches(true, 0, rule, startchar, trueText.length())) {
                return ElExpressionClassification.CONSTANT_TRUE;
            }

            final String falseText = Boolean.FALSE.toString();
            if (falseText.regionMatches(true, 0, rule, startchar, falseText.length())) {
                return ElExpressionClassification.CONSTANT_FALSE;
            }
        }

        return ElExpressionClassification.OTHER;
    }

    enum ElExpressionClassification {
        CONSTANT_TRUE,
        CONSTANT_FALSE,
        EMPTY,
        OTHER;
    }

    // Make a new RuleEvaluator that captures the test constants.
    // TODO(pwp): add some test constants?
    @Nonnull
    private static RuleEvaluator makeRuleEvaluator(
            final ExpressionFactory expressionFactory, final FunctionMapper functionMapper) {
        // Make the expression evaluation context.
        final Map<String, Object> testConstants = Collections.emptyMap();

        return new RuleEvaluator(expressionFactory, functionMapper, testConstants);
    }

    private static boolean evaluatePayloadMapValidator(
            @Nonnull final RuleEvaluator ruleEvaluator,
            final String rule,
            @Nonnull final Payload payload)
            throws IncompatibleTestMatrixException {
        try {
            return ruleEvaluator.evaluateBooleanRule(rule, payload.getMap());
        } catch (final IllegalArgumentException e) {
            LOGGER.error("Unable to evaluate rule ${" + rule + "} with payload " + payload, e);
        }
        return true;
    }

    private static boolean evaluatePayloadValidator(
            @Nonnull final RuleEvaluator ruleEvaluator,
            final String rule,
            @Nonnull final Payload payload)
            throws IncompatibleTestMatrixException {
        final Map<String, Object> values = Collections.singletonMap("value", payload.fetchAValue());

        try {
            return ruleEvaluator.evaluateBooleanRule(rule, values);
        } catch (final IllegalArgumentException e) {
            LOGGER.error("Unable to evaluate rule ${" + rule + "} with payload " + payload, e);
        }

        return false;
    }

    public static boolean containsUnitlessAllocation(
            final ConsumableTestDefinition testDefinition,
            final Map<String, ValueExpression> localContext) {
        return testDefinition != null
                && testDefinition.getEnableUnitlessAllocations()
                && localContext.containsKey("missingExperimentalUnit")
                && localContext.get("missingExperimentalUnit").getValue(null).equals("true")
                && testDefinition.getAllocations().stream()
                        .anyMatch(
                                allocation ->
                                        allocation.getRule().contains("missingExperimentalUnit")
                                                && allocation.getRanges().stream()
                                                        .anyMatch(range -> range.getLength() == 1));
    }
}
