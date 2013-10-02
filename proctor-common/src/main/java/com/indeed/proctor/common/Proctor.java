package com.indeed.proctor.common;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import com.indeed.util.varexport.VarExporter;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.proctor.common.model.TestType;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;

/**
 * The sole entry point for client applications determining the test buckets for a particular client.  See {@link #determineTestGroups(Identifiers, java.util.Map, java.util.Map)}
 * @author ketan
 */
public class Proctor {
    public static final Proctor EMPTY = createEmptyProctor();

    /**
     * Factory method to do the setup and transformation of inputs
     */
    @Nonnull
    public static Proctor construct(@Nonnull final TestMatrixArtifact matrix, ProctorLoadResult loadResult, FunctionMapper functionMapper) {
        final ExpressionFactory expressionFactory = RuleEvaluator.EXPRESSION_FACTORY;

        final Map<String, TestChooser<?>> testChoosers = Maps.newLinkedHashMap();
        final Map<String, Integer> versions = Maps.newLinkedHashMap();

        for (final Entry<String, ConsumableTestDefinition> entry : matrix.getTests().entrySet()) {
            final String testName = entry.getKey();
            final ConsumableTestDefinition testDefinition = entry.getValue();
            final TestType testType = testDefinition.getTestType();
            final TestChooser<?> testChooser;
            if (TestType.RANDOM.equals(testType)) {
                testChooser = new RandomTestChooser(expressionFactory, functionMapper, testName, testDefinition);
            } else {
                testChooser = new StandardTestChooser(expressionFactory, functionMapper, testName, testDefinition);
            }
            testChoosers.put(testName, testChooser);
            versions.put(testName, testDefinition.getVersion());
        }

        return new Proctor(matrix, loadResult, testChoosers);
    }

    @Nonnull
    private static Proctor createEmptyProctor() {
        final Audit audit = new Audit();
        audit.setUpdated(0);
        audit.setUpdatedBy("nobody");
        audit.setVersion(-1);

        final TestMatrixArtifact testMatrix = new TestMatrixArtifact();
        testMatrix.setAudit(audit);

        final ProctorLoadResult loadResult = ProctorLoadResult.emptyResult();

        final Map<String, TestChooser<?>> choosers = Collections.emptyMap();

        return new Proctor(testMatrix, loadResult, choosers);
    }

    static final long INT_RANGE = (long) Integer.MAX_VALUE - (long) Integer.MIN_VALUE;
    private final TestMatrixArtifact matrix;
    private final ProctorLoadResult loadResult;
    @Nonnull
    private final Map<String, TestChooser<?>> testChoosers;

    private final Map<String, ConsumableTestDefinition> testDefinitions = Maps.newLinkedHashMap();

    @VisibleForTesting
    Proctor(
            final TestMatrixArtifact matrix,
            final ProctorLoadResult loadResult,
            @Nonnull Map<String, TestChooser<?>> testChoosers
    ) {
        this.matrix = matrix;
        this.loadResult = loadResult;
        this.testChoosers = testChoosers;
        for (final Entry<String, TestChooser<?>> entry : testChoosers.entrySet()) {
            this.testDefinitions.put(entry.getKey(), entry.getValue().getTestDefinition());
        }

        VarExporter.forNamespace(Proctor.class.getSimpleName()).includeInGlobal().export(this, "");
        VarExporter.forNamespace(DetailedExport.class.getSimpleName()).export(new DetailedExport(), "");  //  intentionally not in global
    }

    class DetailedExport {
        /**
         * TODO: export useful details about the parsed test matrix
         */
    }

    /**
     * Determine which test buckets apply to a particular client.
     * @param testType
     * @param identifier a unique-ish {@link String} identifying the client.  This should be consistent across requests from the same client.
     * @param context a {@link Map} containing variables describing the context in which the request is executing.  These will be supplied to any rules that execute to determine test eligibility.
     * @return a {@link ProctorResult} containing the test buckets that apply to this client as well as the versions of the tests that were executed
     * @deprecated use {@link Proctor#determineTestGroups(Identifiers, java.util.Map, java.util.Map)} instead
     */
    @SuppressWarnings("UnusedDeclaration") // TODO Remove deprecated
    @Nonnull
    public ProctorResult determineTestGroups(final TestType testType, final String identifier, @Nonnull final Map<String, Object> context, @Nonnull final Map<String, Integer> forceGroups) {
        final Identifiers identifiers = new Identifiers(testType, identifier);

        return determineTestGroups(identifiers, context, forceGroups);
    }

    /**
     * Determine which test buckets apply to a particular client.
     * @param identifiers a {@link Map} of unique-ish {@link String}s describing the request in the context of different {@link TestType}s.For example,
     *            {@link TestType#USER} has a CTK associated, {@link TestType#EMAIL} is an email address, {@link TestType#PAGE} might be a url-encoded String
     *            containing the normalized relevant page parameters
     * @param inputContext a {@link Map} containing variables describing the context in which the request is executing. These will be supplied to any rules that
     *            execute to determine test eligibility.
     * @param forceGroups a Map from a String test name to an Integer bucket value. For the specified test allocate the specified bucket (if valid) regardless
     *            of the standard logic
     * @return a {@link ProctorResult} containing the test buckets that apply to this client as well as the versions of the tests that were executed
     */
    @Nonnull
    public ProctorResult determineTestGroups(@Nonnull final Identifiers identifiers, @Nonnull final Map<String, Object> inputContext, @Nonnull final Map<String, Integer> forceGroups) {
        final Map<String, TestBucket> testGroups = Maps.newLinkedHashMap();
        for (final Entry<String, TestChooser<?>> entry : testChoosers.entrySet()) {
            final String testName = entry.getKey();
            final Integer forceGroupBucket = forceGroups.get(testName);
            final TestChooser<?> testChooser = entry.getValue();
            final String identifier;
            if (testChooser instanceof StandardTestChooser) {
                final TestType testType = testChooser.getTestDefinition().getTestType();
                identifier = identifiers.getIdentifier(testType);
                if (identifier == null) {
                    continue;
                }
            } else {
                if (! identifiers.isRandomEnabled()) {
                    continue;
                }
                identifier = null;
            }
            if (forceGroupBucket != null) {
                final TestBucket forcedTestBucket = testChooser.getTestBucket(forceGroupBucket);
                if (forcedTestBucket != null) {
                    testGroups.put(testName, forcedTestBucket);
                    continue;
                }
            }
            final TestBucket testBucket;
            if (identifier == null) {
                testBucket = ((RandomTestChooser) testChooser).choose(null, inputContext);
            } else {
                testBucket = ((StandardTestChooser) testChooser).choose(identifier, inputContext);
            }
            if (testBucket != null) {
                testGroups.put(testName, testBucket);
                testChooser.getTestDefinition();
            }
        }

        // TODO Can we make getAudit nonnull?
        final Audit audit = Preconditions.checkNotNull(matrix.getAudit(), "Missing audit");
        return new ProctorResult(audit.getVersion(), testGroups, testDefinitions);
    }

    @SuppressWarnings("UnusedDeclaration") // TODO Needed?
    public ConsumableTestDefinition getTestDefinition(final String name) {
        return matrix.getTests().get(name);
    }

    public ProctorLoadResult getLoadResult() {
        return loadResult;
    }

    public void appendAllTests(Writer sb) {
        appendTests(sb, Predicates.<TestChooser<?>>alwaysTrue());
    }

    @SuppressWarnings("UnusedDeclaration") // TODO needed?
    public void appendTests(Writer sb, final TestType type) {
        appendTests(sb, new Predicate<TestChooser<?>>() {
            @Override
            public boolean apply(TestChooser<?> input) {
                assert null != input;
                return type == input.getTestDefinition().getTestType();
            }
        });
    }

    public void appendTests(Writer sb, @Nonnull Predicate<TestChooser<?>> shouldIncludeTest) {
        final NumberFormat fmt = NumberFormat.getPercentInstance(Locale.US);
        fmt.setMaximumFractionDigits(2);

        final PrintWriter writer = new PrintWriter(sb);
        for (final Entry<String, TestChooser<?>> entry : testChoosers.entrySet()) {
            final String testName = entry.getKey();
            final TestChooser<?> chooser = entry.getValue();
            if (shouldIncludeTest.apply(chooser)) {
                writer.append(testName).append(" : ");
                chooser.printTestBuckets(writer);
                writer.println();
            }
        }
    }

    public void appendTestMatrix(Writer writer) throws IOException {
        ProctorUtils.serializeArtifact(writer, this.matrix);
    }

}
