package com.indeed.proctor.common;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
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
import org.apache.el.ExpressionFactoryImpl;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ValueExpression;
import javax.el.ELContext;
import javax.el.ELException;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


public abstract class ProctorUtils {
    private static final ObjectMapper OBJECT_MAPPER = Serializers.lenient();
    private static final Logger LOGGER = Logger.getLogger(ProctorUtils.class);

    public static MessageDigest createMessageDigest() {
        try {
            return MessageDigest.getInstance("MD5");
        } catch (@Nonnull final NoSuchAlgorithmException e) {
            throw new RuntimeException("Impossible no MD5", e);
        }
    }

    @Nonnull
    public static Map<String, ValueExpression> convertToValueExpressionMap(
            @Nonnull final ExpressionFactory expressionFactory,
            @Nonnull final Map<String, Object> values
    ) {
        final Map<String, ValueExpression> context = new HashMap<String, ValueExpression>(values.size());
        for (final Entry<String, Object> entry: values.entrySet()) {
            final ValueExpression ve = expressionFactory.createValueExpression(entry.getValue(), Object.class);
            context.put(entry.getKey(), ve);
        }
        return context;
    }

    @SuppressWarnings("UnusedDeclaration") // TODO Remove?
    public static String convertToArtifact(@Nonnull final TestMatrixVersion testMatrix) throws IOException {
        final StringWriter sw = new StringWriter();
        final TestMatrixArtifact artifact = convertToConsumableArtifact(testMatrix);
        serializeArtifact(sw, artifact);
        return sw.toString();
    }

    public static void serializeArtifact(Writer writer, final TestMatrixArtifact artifact) throws IOException {
        serializeObject(writer, artifact);
    }

    @SuppressWarnings("UnusedDeclaration") // TODO Remove?
    public static void serializeTestDefinition(Writer writer, final TestDefinition definition) throws IOException {
        serializeObject(writer, definition);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static JsonNode readJsonFromFile(final File input) throws IOException {
        final ObjectMapper mapper = Serializers.lenient();
        final JsonNode rootNode = mapper.readValue(input, JsonNode.class);
        return rootNode;
    }

    private static <T> void serializeObject(Writer writer, final T artifact) throws IOException {
        OBJECT_MAPPER.defaultPrettyPrintingWriter().writeValue(writer, artifact);
    }

    @Nonnull
    public static TestMatrixArtifact convertToConsumableArtifact(@Nonnull final TestMatrixVersion testMatrix) {
        final Audit audit = new Audit();
        final Date published = Preconditions.checkNotNull(testMatrix.getPublished(), "Missing publication date");
        audit.setUpdated(published.getTime());
        audit.setVersion(testMatrix.getVersion());
        audit.setUpdatedBy(testMatrix.getAuthor());

        final TestMatrixArtifact artifact = new TestMatrixArtifact();
        artifact.setAudit(audit);

        final TestMatrixDefinition testMatrixDefinition = Preconditions.checkNotNull(testMatrix.getTestMatrixDefinition(), "Missing test matrix definition");

        final Map<String, TestDefinition> testDefinitions = testMatrixDefinition.getTests();

        final Map<String, ConsumableTestDefinition> consumableTestDefinitions = Maps.newLinkedHashMap();
        for (final Entry<String, TestDefinition> entry : testDefinitions.entrySet()) {
            final TestDefinition td = entry.getValue();
            final ConsumableTestDefinition ctd = convertToConsumableTestDefinition(td);
            consumableTestDefinitions.put(entry.getKey(), ctd);
        }

        artifact.setTests(consumableTestDefinitions);
        return artifact;
    }

    @Nonnull
    public static ConsumableTestDefinition convertToConsumableTestDefinition(@Nonnull final TestDefinition td) {
        final Map<String, Object> specialConstants = td.getSpecialConstants();

        final List<String> ruleComponents = Lists.newArrayList();
        //noinspection unchecked
        final List<String> countries = (List<String>) specialConstants.get("__COUNTRIES");
        if (countries != null) {
            ruleComponents.add("proctor:contains(__COUNTRIES, country)");
        }
        final String rawRule = removeElExpressionBraces(td.getRule());
        if (!isEmptyWhitespace(rawRule)) {
            ruleComponents.add(rawRule);
        }

        final String rule;
        if (ruleComponents.isEmpty()) {
            rule = null;
        } else {
            final StringBuilder ruleBuilder = new StringBuilder("${");
            for (int i = 0; i < ruleComponents.size(); i++) {
                if (i != 0) {
                    ruleBuilder.append(" && ");
                }
                ruleBuilder.append(ruleComponents.get(i));
            }
            ruleBuilder.append("}");

            rule = ruleBuilder.toString();
        }

        final List<Allocation> allocations = td.getAllocations();
        for (final Allocation alloc : allocations) {
            final String rawAllocRule = removeElExpressionBraces(alloc.getRule());
            if(isEmptyWhitespace(rawAllocRule)) {
                alloc.setRule(null);
            } else {
                // ensure that all rules in the generated test-matrix are wrapped in "${" ... "}"
                if (! (rawAllocRule.startsWith("${") && rawAllocRule.endsWith("}"))) {
                    final String newAllocRule = "${" + rawAllocRule + "}";
                    alloc.setRule(newAllocRule);
                }
            }
        }

        final Map<String, Object> constants = Maps.newLinkedHashMap();
        constants.putAll(td.getConstants());
        constants.putAll(specialConstants);

        return new ConsumableTestDefinition(td.getVersion(), rule, td.getTestType(), td.getSalt(), td.getBuckets(), allocations, constants, td.getDescription());
    }

    public static ProctorSpecification readSpecification(final File inputFile) {
        final ProctorSpecification spec;
        InputStream stream = null;
        try {
            stream = new BufferedInputStream(new FileInputStream(inputFile));
            spec = readSpecification(stream);
        } catch (IOException e) {
            throw new RuntimeException("Unable to read test set from " + inputFile, e);
        } finally {
            if(stream != null ) {
                try {
                    stream.close();
                } catch (final IOException e) {
                    LOGGER.error("Suppressing throwable thrown when closing "+inputFile, e);
                }
            }
        }
        return spec;
    }


    public static ProctorSpecification readSpecification(final InputStream inputFile) {
        final ProctorSpecification spec;
        try {
            spec = OBJECT_MAPPER.readValue(inputFile, ProctorSpecification.class);
        } catch (@Nonnull final JsonParseException e) {
            throw new RuntimeException("Unable to read test set from " + inputFile + ": ", e);
        } catch (@Nonnull final JsonMappingException e) {
            throw new RuntimeException("Unable to read test set from " + inputFile, e);
        } catch (@Nonnull final IOException e) {
            throw new RuntimeException("Unable to read test set from " + inputFile, e);
        }
        return spec;
    }

    /**
     * Verifies that the TestMatrix is compatible with all the required tests.
     * Removes non-required tests from the TestMatrix
     * Replaces invalid or missing tests (buckets are not compatible) with
     * default implementation returning the fallback value see defaultFor
     * @param testMatrix
     * @param matrixSource
     * @param requiredTests
     * @return
     */
    public static ProctorLoadResult verifyAndConsolidate(@Nonnull final TestMatrixArtifact testMatrix, final String matrixSource, @Nonnull final Map<String, TestSpecification> requiredTests, @Nonnull final FunctionMapper functionMapper) {
        return verifyAndConsolidate(testMatrix, matrixSource, requiredTests, functionMapper, new ProvidedContext(ProvidedContext.EMPTY_CONTEXT,false));
    }
    public static ProctorLoadResult verifyAndConsolidate(@Nonnull final TestMatrixArtifact testMatrix, final String matrixSource, @Nonnull final Map<String, TestSpecification> requiredTests, @Nonnull final FunctionMapper functionMapper, final ProvidedContext providedContext) {
        final ProctorLoadResult result = verify(testMatrix, matrixSource, requiredTests, functionMapper, providedContext);

        final Map<String, ConsumableTestDefinition> definedTests = testMatrix.getTests();
        // Remove any invalid tests so that any required ones will be replaced with default values during the
        // consolidation below (just like missing tests). Any non-required tests can safely be ignored.
        for (final String invalidTest: result.getTestsWithErrors()) {
            // TODO - mjs - gross that this depends on the mutability of the returned map, but then so does the
            //  consolidate method below.
            definedTests.remove(invalidTest);
        }

        consolidate(testMatrix, requiredTests);

        return result;
    }


    public static ProctorLoadResult verify(@Nonnull final TestMatrixArtifact testMatrix, final String matrixSource, @Nonnull final Map<String, TestSpecification> requiredTests) {
        return verify(testMatrix, matrixSource, requiredTests, RuleEvaluator.FUNCTION_MAPPER, new ProvidedContext(ProvidedContext.EMPTY_CONTEXT,false)); //use default function mapper
    }


        /**
         * Does not mutate the TestMatrix.
         *
         * Verifies that the test matrix contains all the required tests and that
         * each required test is valid.
         * @param testMatrix
         * @param matrixSource
         * @param requiredTests
         * @return
         */
    public static ProctorLoadResult verify(@Nonnull final TestMatrixArtifact testMatrix, final String matrixSource, @Nonnull final Map<String, TestSpecification> requiredTests, @Nonnull final FunctionMapper functionMapper, final ProvidedContext providedContext) {
        final ProctorLoadResult.Builder resultBuilder = ProctorLoadResult.newBuilder();

        final Map<String, Map<Integer, String>> allTestsKnownBuckets = Maps.newHashMapWithExpectedSize(requiredTests.size());
        for (final Entry<String, TestSpecification> entry : requiredTests.entrySet()) {
            final Map<Integer, String> bucketValueToName = Maps.newHashMap();
            for (final Entry<String, Integer> bucket : entry.getValue().getBuckets().entrySet()) {
                bucketValueToName.put(bucket.getValue(), bucket.getKey());
            }
            allTestsKnownBuckets.put(entry.getKey(), bucketValueToName);
        }

        final Map<String, ConsumableTestDefinition> definedTests = testMatrix.getTests();
        final SetView<String> missingTests = Sets.difference(requiredTests.keySet(), definedTests.keySet());
        resultBuilder.recordAllMissing(missingTests);
        for (final Entry<String, ConsumableTestDefinition> entry : definedTests.entrySet()) {
            final String testName = entry.getKey();
            final Map<Integer, String> knownBuckets = allTestsKnownBuckets.remove(testName);
            if (knownBuckets == null) { //  we don't care about this test
                // iterator.remove(); DO NOT CONSOLIDATE
                continue;
            }

            final ConsumableTestDefinition testDefinition = entry.getValue();

            try {
                verifyTest(testName, testDefinition, requiredTests.get(testName), knownBuckets, matrixSource, functionMapper, providedContext);

            } catch (IncompatibleTestMatrixException e) {
                LOGGER.error(String.format("Unable to load test matrix for %s", testName), e);
                resultBuilder.recordError(testName);
            }
        }

        // TODO mjs - is this check additive?
        resultBuilder.recordAllMissing(allTestsKnownBuckets.keySet());

        resultBuilder.recordVerifiedRules(providedContext.isEvaluable());

        final ProctorLoadResult loadResult = resultBuilder.build();

        return loadResult;
    }

    private static void verifyTest(
            @Nonnull final String testName,
            @Nonnull final ConsumableTestDefinition testDefinition,
            @Nonnull final TestSpecification testSpecification,
            @Nonnull final Map<Integer, String> knownBuckets,
            @Nonnull final String matrixSource,
            @Nonnull final FunctionMapper functionMapper
    ) throws IncompatibleTestMatrixException {
        verifyTest(testName,testDefinition,testSpecification,knownBuckets,matrixSource,functionMapper,new ProvidedContext(ProvidedContext.EMPTY_CONTEXT,false));
    }

    private static void verifyTest(
            @Nonnull final String testName,
            @Nonnull final ConsumableTestDefinition testDefinition,
            @Nonnull final TestSpecification testSpecification,
            @Nonnull final Map<Integer, String> knownBuckets,
            @Nonnull final String matrixSource,
            @Nonnull final FunctionMapper functionMapper,
            final ProvidedContext providedContext
    ) throws IncompatibleTestMatrixException {
        final List<Allocation> allocations = testDefinition.getAllocations();

        final TestType declaredType = testDefinition.getTestType();
        if (!TestType.all().contains(declaredType)) {
            throw new IncompatibleTestMatrixException(String.format(
                    "Test '%s' is included in the application specification but refers to unknown id type '%s'.",
                    testName, declaredType));
        }
        verifyInternallyConsistentDefinition(testName, matrixSource, testDefinition, functionMapper, providedContext);
            /*
             * test the matrix for adherence to this application's requirements
             */
        final Set<Integer> unknownBuckets = Sets.newHashSet();

        for (final Allocation allocation : allocations) {
            final List<Range> ranges = allocation.getRanges();
            //  ensure that each range refers to a known bucket
            for (final Range range : ranges) {
                // Externally consistent (application's requirements)
                if (! knownBuckets.containsKey(range.getBucketValue())) {
                    // If the bucket has a positive allocation, add it to the list of unknownBuckets
                    if(range.getLength() > 0) {
                        unknownBuckets.add(range.getBucketValue());
                    }
                }
            }
        }
        if(unknownBuckets.size() > 0) {
            throw new IncompatibleTestMatrixException("Allocation range in " + testName + " from " + matrixSource + " refers to unknown bucket value(s) " + unknownBuckets + " with length > 0");
        }

        // TODO(pwp): add some test constants?
        final RuleEvaluator ruleEvaluator = makeRuleEvaluator(RuleEvaluator.EXPRESSION_FACTORY, functionMapper);

        PayloadSpecification payloadSpec = testSpecification.getPayload();
        if (payloadSpec != null) {
            final String specifiedPayloadTypeName = Preconditions.checkNotNull(payloadSpec.getType(), "Missing payload spec type");
            final PayloadType specifiedPayloadType = PayloadType.payloadTypeForName(specifiedPayloadTypeName);
            if (specifiedPayloadType == null) {
                // This is probably redundant vs. TestGroupsGenerator.
                throw new IncompatibleTestMatrixException("For test " + testName + " from " + matrixSource + " test specification payload type unknown: " + specifiedPayloadTypeName);
            }
            final String payloadValidatorRule = payloadSpec.getValidator();
            final List<TestBucket> buckets = testDefinition.getBuckets();
            for (final TestBucket bucket : buckets) {
                Payload payload = bucket.getPayload();
                if (payload != null) {
                    if (!specifiedPayloadType.payloadHasThisType(payload)) {
                        throw new IncompatibleTestMatrixException("For test " + testName + " from " + matrixSource + " expected payload of type " + specifiedPayloadType.payloadTypeName + " but matrix has a test bucket payload with wrong type: " + bucket);

                    }
                    if (payloadValidatorRule != null) {
                        final boolean payloadIsValid = evaluatePayloadValidator(ruleEvaluator, payloadValidatorRule, payload);
                        if (!payloadIsValid) {
                            throw new IncompatibleTestMatrixException("For test " + testName + " from " + matrixSource + " payload validation rule " + payloadValidatorRule + " failed for test bucket: " + bucket);
                        }
                    }
                }
            }
        }
    }

    private static void consolidate(@Nonnull final TestMatrixArtifact testMatrix, @Nonnull final Map<String, TestSpecification> requiredTests) {
        final Map<String, ConsumableTestDefinition> definedTests = testMatrix.getTests();

        // Sets.difference returns a "view" on the original set, which would require concurrent modification while iterating (copying the set will prevent this)
        final Set<String> toRemove = ImmutableSet.copyOf(Sets.difference(definedTests.keySet(), requiredTests.keySet()));
        for (String testInMatrixNotRequired : toRemove) {
            //  we don't care about this test
            definedTests.remove(testInMatrixNotRequired);
        }

        // Next, for any required tests that are missing, ensure that
        //  there is a nonnull test definition in the matrix
        final Set<String> missing =
                ImmutableSet.copyOf(Sets.difference(requiredTests.keySet(), definedTests.keySet()));
        for (String testNotInMatrix: missing) {
            definedTests.put(testNotInMatrix, defaultFor(testNotInMatrix, requiredTests.get(testNotInMatrix)));
        }

        // Now go through definedTests: for each test, if the test spec
        // didn't ask for a payload, then remove any payload that is in
        // the test matrix.
        for (Entry<String, ConsumableTestDefinition> next : definedTests.entrySet()) {
            final String testName = next.getKey();
            final ConsumableTestDefinition testDefinition = next.getValue();
            final TestSpecification testSpec = requiredTests.get(testName);

            if (testSpec.getPayload() == null) {
                // No payload was requested...
                final List<TestBucket> buckets = testDefinition.getBuckets();
                for (final TestBucket bucket : buckets) {
                    if (bucket.getPayload() != null) {
                        // ... so stomp the unexpected payloads.
                        bucket.setPayload(null);
                    }
                }
            }
        }
    }

    @Nonnull
    private static ConsumableTestDefinition defaultFor(final String testName, @Nonnull final TestSpecification testSpecification) {
        final String missingTestSoleBucketName = "inactive";
        final String missingTestSoleBucketDescription = "inactive";
        final Allocation allocation = new Allocation();
        allocation.setRanges(ImmutableList.of(new Range(testSpecification.getFallbackValue(), 1.0)));

        return new ConsumableTestDefinition(
                "default",
                null,
                TestType.RANDOM,
                testName,
                ImmutableList.of(new TestBucket(
                        missingTestSoleBucketName,
                        testSpecification.getFallbackValue(),
                        missingTestSoleBucketDescription)),
                // Force a nonnull allocation just in case something somewhere assumes 1.0 total allocation
                Collections.singletonList(allocation),
                Collections.<String, Object>emptyMap(),
                testName);
    }

    public static ProvidedContext convertContextToTestableMap(Map<String,String> providedContext) {
        final ExpressionFactory expressionFactory = new ExpressionFactoryImpl();
        boolean methodNotFoundException = false;
        Map<String, Object> primitiveVals = new HashMap<String, Object>();
        primitiveVals.put("int", 0);
        primitiveVals.put("integer", 0);
        primitiveVals.put("long", (long)0);
        primitiveVals.put("bool", true);
        primitiveVals.put("boolean", true);
        primitiveVals.put("short", (short)0);
        primitiveVals.put("string", "");
        primitiveVals.put("double", (double)0);
        primitiveVals.put("char", "");
        primitiveVals.put("character", "");
        primitiveVals.put("byte", (byte)0);

        if (providedContext != null) {
            Map<String, Object> newProvidedContext = new HashMap<String, Object>();
            for(Entry<String,String> entry : providedContext.entrySet()) {
                final String iobjName = entry.getValue();
                String objName = iobjName;
                Object toAdd = null;
                if (primitiveVals.get(objName.toLowerCase()) != null) {
                    toAdd = primitiveVals.get(objName.toLowerCase());
                } else {
                    try {
                        final Class clazz = Class.forName(objName);
                        if (clazz.isEnum()) { //If it is a user defined enum
                            toAdd = clazz.getEnumConstants()[0];
                        } else { //If it is a user defined non enum class
                            toAdd = clazz.newInstance();
                        }
                    } catch (final IllegalAccessException e) {
                        methodNotFoundException = true;
                        LOGGER.debug("Couldn't access default constructor of " + iobjName + " in providedContext");
                    } catch (final InstantiationException e) {
                        methodNotFoundException = true;
                        //if a default constructor is not defined, use this flag to not set context and not evaluate rules
                        LOGGER.debug("Couldn't find default constructor for " + iobjName + " in providedContext");
                    } catch (final ClassNotFoundException e) {
                        LOGGER.error("Class not found for " + iobjName + " in providedContext");
                        return new ProvidedContext(ProvidedContext.EMPTY_CONTEXT,false);
                    }

                }
                newProvidedContext.put(entry.getKey(),toAdd);
            }
            if (!methodNotFoundException) { //defaultConstructor method does not exist, can't evaluate rules without instantiation of user defined class
                return new ProvidedContext(ProctorUtils.convertToValueExpressionMap(expressionFactory, newProvidedContext),true);
            }
        }
        return new ProvidedContext(ProvidedContext.EMPTY_CONTEXT,false);
    }

    public static void verifyInternallyConsistentDefinition(final String testName, final String matrixSource, @Nonnull final ConsumableTestDefinition testDefinition) throws IncompatibleTestMatrixException {
        verifyInternallyConsistentDefinition(testName, matrixSource, testDefinition, RuleEvaluator.FUNCTION_MAPPER, new ProvidedContext(ProvidedContext.EMPTY_CONTEXT,false));
    }

    public static void verifyInternallyConsistentDefinition(final String testName, final String matrixSource, @Nonnull final ConsumableTestDefinition testDefinition, final FunctionMapper functionMapper, final ProvidedContext providedContext) throws IncompatibleTestMatrixException {
        final List<Allocation> allocations = testDefinition.getAllocations();
        ExpressionFactory expressionFactory = new ExpressionFactoryImpl();
        //verify test rule is valid EL
        final String testRule = testDefinition.getRule();

        final Map<String, ValueExpression> testConstants = ProctorUtils.convertToValueExpressionMap(expressionFactory, testDefinition.getConstants());
        final VariableMapper variableMapper = new MulticontextReadOnlyVariableMapper(testConstants, providedContext.getContext());
        final RuleEvaluator ruleEvaluator = new RuleEvaluator(expressionFactory, functionMapper, testDefinition.getConstants());
        final ELContext elContext = ruleEvaluator.createELContext(variableMapper);
        if(!isEmptyWhitespace(testRule)){
            try {
                final ValueExpression valueExpression = expressionFactory.createValueExpression(elContext, testRule, Boolean.class);
                if (providedContext.isEvaluable()) { /*
                                                       * must have a context to test against, even if it's "Collections.emptyMap()", how to
                                                       * tell if this method is used for ProctorBuilder or during load of the testMatrix.
                                                       * also used to check to make sure any classes included in the EL have a
                                                       * default constructor for evaluation (unless they are an enum)
                                                       */
                    valueExpression.getValue(elContext);
                }
            } catch (final ELException e) {
                throw new IncompatibleTestMatrixException("Unable to evaluate rule ${" + testRule + "} in " + testName);
            }
        }

        if (allocations.isEmpty()) {
            throw new IncompatibleTestMatrixException("No allocations specified in test " + testName);
        }
        final List<TestBucket> buckets = testDefinition.getBuckets();

        /*
         * test the matrix for consistency with itself
         */
        final Set<Integer> definedBuckets = Sets.newHashSet();
        for (final TestBucket bucket : buckets) {
            definedBuckets.add(bucket.getValue());
        }

        for(int i = 0; i < allocations.size(); i++ ) {
            final Allocation allocation = allocations.get(i);
            final List<Range> ranges = allocation.getRanges();
            //  ensure that each range refers to a known bucket
            double bucketTotal = 0;
            for (final Range range : ranges) {
                bucketTotal += range.getLength();
                // Internally consistent (within matrix itself)
                if (!definedBuckets.contains(range.getBucketValue())) {
                    throw new IncompatibleTestMatrixException("Allocation range in " + testName + " from " + matrixSource + " refers to unknown bucket value " + range.getBucketValue());
                }
            }
            //  I hate floating points.  TODO: extract a required precision constant/parameter?
            if (bucketTotal < 0.9999 || bucketTotal > 1.0001) { //  compensate for FP imprecision.  TODO: determine what these bounds really should be by testing stuff
                final StringBuilder sb = new StringBuilder(testName + " range with rule " + allocation.getRule() + " does not add up to 1 : ").append(ranges.get(0).getLength());
                for (int r = 1; r < ranges.size(); r++) {
                    sb.append(" + ").append(ranges.get(r).getLength());
                }
                sb.append(" = ").append(bucketTotal);
                throw new IncompatibleTestMatrixException(sb.toString());
            }
            final String rule = allocation.getRule();
            final boolean lastAllocation = i == allocations.size() - 1;
            final String bareRule = removeElExpressionBraces(rule);
            if(lastAllocation) {
                if (!isEmptyWhitespace(bareRule)) {
                    throw new IncompatibleTestMatrixException("Allocation[" + i + "] for test " + testName + " from " + matrixSource + " has non-empty rule: " + allocation.getRule());
                }
            } else {
                if (isEmptyWhitespace(bareRule)) {
                    throw new IncompatibleTestMatrixException("Allocation[" + i + "] for test " + testName + " from " + matrixSource + " has empty rule: " + allocation.getRule());
                }
            }
            //verify allocation rules are valid EL
            if(!isEmptyWhitespace(bareRule)){
                try {
                    final ValueExpression valueExpression = expressionFactory.createValueExpression(elContext, rule, Boolean.class);
                    if (providedContext.isEvaluable()) { /*
                                                           * must have a context to test against, even if it's "Collections.emptyMap()", how to
                                                           * tell if this method is used for ProctorBuilder or during load of the testMatrix.
                                                           * also used to check to make sure any classes included in the EL have a
                                                           * default constructor for evaluation (unless they are an enum)
                                                           */
                        valueExpression.getValue(elContext);
                    }
                } catch (final ELException e) {
                    throw new IncompatibleTestMatrixException("Unable to evaluate rule ${" + rule + "} in allocations of " + testName);
                }
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
                if (p.numFieldsDefined() != 1) {
                    throw new IncompatibleTestMatrixException("Test " + testName + " from " + matrixSource + " has a test bucket payload with multiple types: " + bucket);
                }
                if (nonEmptyPayload == null) {
                    nonEmptyPayload = p;
                } else if (!nonEmptyPayload.sameType(p)) {
                    throw new IncompatibleTestMatrixException("Test " + testName + " from " + matrixSource + " has test bucket: " + bucket + " incompatible with type of payload: " + nonEmptyPayload);
                }
            } else {
                bucketsWithoutPayloads.add(bucket);
            }
        }
        if ((nonEmptyPayload != null) && (bucketsWithoutPayloads.size() != 0)) {
            throw new IncompatibleTestMatrixException("Test " + testName + " from " + matrixSource + " has some test buckets without payloads: " + bucketsWithoutPayloads);
        }
    }

    /**
     * Returns flag whose value indicates if the string is null, empty or
     * only contains whitespace characters
     * @param s
     * @return
     */
    static boolean isEmptyWhitespace(@Nullable final String s) {
        if (s == null) {
            return true;
        }
        return CharMatcher.WHITESPACE.matchesAllOf(s);
    }

    /**
     * Removes the expression braces "${ ... }" surrounding the rule.
     * @param rule
     * @return
     */
    @Nullable
    static String removeElExpressionBraces(@Nullable final String rule) {
        if(isEmptyWhitespace(rule)) {
            return null;
        }
        int startchar = 0; // inclusive
        int endchar = rule.length() - 1; // inclusive

        // garbage free trim()
        while(startchar < rule.length() && CharMatcher.WHITESPACE.matches(rule.charAt(startchar))) {
            ++startchar;
        }
        while(endchar > startchar && CharMatcher.WHITESPACE.matches(rule.charAt(endchar))) {
            --endchar;
        }

        if(rule.regionMatches(startchar, "${", 0, 2) && rule.charAt(endchar) == '}') {
            startchar += 2; // skip "${"
            --endchar; // skip '}'
        }
        // garbage free trim()
        while(startchar < rule.length() && CharMatcher.WHITESPACE.matches(rule.charAt(startchar))) {
            ++startchar;
        }
        while(endchar > startchar && CharMatcher.WHITESPACE.matches(rule.charAt(endchar))) {
            --endchar;
        }
        if(endchar < startchar ) {
            // null instead of empty string for consistency with 'isEmptyWhitespace' check at the beginning
            return null;
        }
        return rule.substring(startchar, endchar + 1);
    }

    // Make a new RuleEvaluator that captures the test constants.
    // TODO(pwp): add some test constants?
    @Nonnull
    private static RuleEvaluator makeRuleEvaluator(final ExpressionFactory expressionFactory, final FunctionMapper functionMapper) {
        // Make the expression evaluation context.
        final Map<String, Object> testConstants = Collections.emptyMap();

        return new RuleEvaluator(expressionFactory, functionMapper, testConstants);
    }

    private static boolean evaluatePayloadValidator(@Nonnull final RuleEvaluator ruleEvaluator, final String rule, @Nonnull final Payload payload) throws IncompatibleTestMatrixException {
        Map<String, Object> values = Collections.singletonMap("value", payload.fetchAValue());

        try {
            return ruleEvaluator.evaluateBooleanRule(rule, values);
        } catch (@Nonnull final IllegalArgumentException e) {
            LOGGER.error("Unable to evaluate rule ${" + rule + "} with payload " + payload, e);
        }

        return false;
    }

}
