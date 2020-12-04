package com.indeed.proctor.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Audit;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import com.indeed.proctor.common.model.TestType;
import com.indeed.util.varexport.VarExporter;

import javax.annotation.Nonnull;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The sole entry point for client applications determining the test buckets for a particular client.
 * Basically a Factory to create ProctorResult for a given identifier and context, based on a TestMatrix and a specification.
 * Supposedly immutable result of loading a test matrix, so each reload creates a new instance of this class.
 *
 * See {@link #determineTestGroups(Identifiers, Map, Map)}
 * @author ketan
 */
public class Proctor {
    public static final Proctor EMPTY = createEmptyProctor();
    private static final ObjectWriter OBJECT_WRITER = Serializers
            .lenient()
            .configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false).writerWithDefaultPrettyPrinter();

    /**
     * Factory method to do the setup and transformation of inputs
     *
     * @param matrix a {@link TestMatrixArtifact} loaded by ProctorLoader
     * @param loadResult a {@link ProctorLoadResult} which contains result of validation of test definition
     * @param functionMapper a given el {@link FunctionMapper}
     * @return constructed Proctor object
     */
    @Nonnull
    public static Proctor construct(
            @Nonnull final TestMatrixArtifact matrix,
            final ProctorLoadResult loadResult,
            final FunctionMapper functionMapper
    ) {
        final ExpressionFactory expressionFactory = RuleEvaluator.EXPRESSION_FACTORY;

        final Map<String, TestChooser<?>> testChoosers = Maps.newLinkedHashMap();
        final Map<String, String> versions = Maps.newLinkedHashMap();

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
    @VisibleForTesting
    static Proctor createEmptyProctor() {
        final Audit audit = new Audit();
        audit.setUpdated(0);
        audit.setUpdatedBy("nobody");
        audit.setVersion(Audit.EMPTY_VERSION);

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
            @Nonnull final Map<String, TestChooser<?>> testChoosers
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

    private static class DetailedExport {
        /*
         * TODO: export useful details about the parsed test matrix
         */
    }

    /**
     * Determine which test buckets apply to a particular client.
     *
     * @param testType    the {@link TestType} of the test
     * @param identifier  a unique-ish {@link String} identifying the client.
     *                    This should be consistent across requests from the same client.
     * @param context     a {@link Map} containing variables describing the context in which the request is executing.
     *                    These will be supplied to any rules that execute to determine test eligibility.
     * @param forceGroups a {@link Map} from a String test name to an Integer bucket value. For the specified test allocate the specified bucket (if valid) regardless
     *                    of the standard logic
     * @return a {@link ProctorResult} containing the test buckets that apply to this client as well as the versions of the tests that were executed
     * @deprecated use {@link Proctor#determineTestGroups(Identifiers, Map, Map)} instead
     */
    @SuppressWarnings("UnusedDeclaration") // TODO Remove deprecated
    @Nonnull
    public ProctorResult determineTestGroups(
            final TestType testType,
            final String identifier,
            @Nonnull final Map<String, Object> context,
            @Nonnull final Map<String, Integer> forceGroups) {
        final Identifiers identifiers = new Identifiers(testType, identifier);

        return determineTestGroups(identifiers, context, forceGroups);
    }

    /**
     * Determine which test buckets apply to a particular client.
     *
     * @param identifiers  a {@link Map} of unique-ish {@link String}s describing the request in the context of different {@link TestType}s.For example,
     *                     {@link TestType#USER} has a CTK associated, {@link TestType#EMAIL} is an email address, {@link TestType#PAGE} might be a url-encoded String
     *                     containing the normalized relevant page parameters
     * @param inputContext a {@link Map} containing variables describing the context in which the request is executing. These will be supplied to any rules that
     *                     execute to determine test eligibility.
     * @param forceGroups  a {@link Map} from a String test name to an Integer bucket value. For the specified test allocate the specified bucket (if valid) regardless
     *                     of the standard logic
     * @return a {@link ProctorResult} containing the test buckets that apply to this client as well as the versions of the tests that were executed
     */
    @Nonnull
    public ProctorResult determineTestGroups(
            @Nonnull final Identifiers identifiers,
            @Nonnull final Map<String, Object> inputContext,
            @Nonnull final Map<String, Integer> forceGroups) {
        return determineTestGroups(identifiers, inputContext, forceGroups, Collections.emptyList());
    }

    /**
     * See determineTestGroups() above. Adds a test name filter for returning a subset of tests.
     * This is useful for the Proctor REST API. It lacks a specification and needs a way to evaluate
     * only the tests mentioned in the HTTP parameters by each particular query. Otherwise, there will be
     * logged errors due to missing context variables.
     *
     * @param identifiers    a {@link Map} of unique-ish {@link String}s describing the request in the context of different {@link TestType}s.For example,
     *                       {@link TestType#USER} has a CTK associated, {@link TestType#EMAIL} is an email address, {@link TestType#PAGE} might be a url-encoded String
     *                       containing the normalized relevant page parameters
     * @param inputContext   a {@link Map} containing variables describing the context in which the request is executing. These will be supplied to any rules that
     *                       execute to determine test eligibility.
     * @param forceGroups    a {@link Map} from a String test name to an Integer bucket value. For the specified test allocate the specified bucket (if valid) regardless
     *                       of the standard logic
     * @param testNameFilter Only evaluates and returns the tests named in this collection. If empty, no filter is applied.
     * @return a {@link ProctorResult} containing the test buckets that apply to this client as well as the versions of the tests that were executed
     */
    @Nonnull
    public ProctorResult determineTestGroups(
            @Nonnull final Identifiers identifiers,
            @Nonnull final Map<String, Object> inputContext,
            @Nonnull final Map<String, Integer> forceGroups,
            @Nonnull final Collection<String> testNameFilter
    ) {
        // ProctorResult requires SortedMap internally, avoid copy overhead
        // use mutable map for legacy reasons, inside this codebase should not be modified after this method
        final SortedMap<String, TestBucket> testGroups = new TreeMap<>();
        final SortedMap<String, Allocation> testAllocations = new TreeMap<>();

        Map<String, TestChooser<?>> filteredChoosers = testChoosers;
        if (!testNameFilter.isEmpty()) {
            filteredChoosers = Maps.filterKeys(filteredChoosers, Predicates.in(testNameFilter));
        }

        for (final Entry<String, TestChooser<?>> entry : filteredChoosers.entrySet()) {
            final String testName = entry.getKey();
            final Integer forceGroupBucket = forceGroups.get(testName);
            final TestChooser<?> testChooser = entry.getValue();
            final String identifier;
            if (testChooser instanceof StandardTestChooser) {
                final TestType testType = testChooser.getTestDefinition().getTestType();
                identifier = identifiers.getIdentifier(testType);
                if (identifier == null) {
                    // No identifier for the testType of this chooser, nothing to do
                    continue;
                }
            } else {
                if (! identifiers.isRandomEnabled()) {
                    // test wants random chooser, but client disabled random, nothing to do
                    continue;
                }
                identifier = null;
            }
            if (forceGroupBucket != null) {
                final TestBucket forcedTestBucket = testChooser.getTestBucket(forceGroupBucket);
                if (forcedTestBucket != null) {
                    testGroups.put(testName, forcedTestBucket);
                    // use forced group
                    continue;
                }
            }
            final TestChooser.Result chooseResult;
            if (identifier == null) {
                chooseResult = ((RandomTestChooser) testChooser).choose(null, inputContext);
            } else {
                chooseResult = ((StandardTestChooser) testChooser).choose(identifier, inputContext);
            }
            if (chooseResult.getTestBucket() != null) {
                testGroups.put(testName, chooseResult.getTestBucket());
            }
            if (chooseResult.getAllocation() != null) {
                testAllocations.put(testName, chooseResult.getAllocation());
            }
        }

        // TODO Can we make getAudit nonnull?
        final Audit audit = Preconditions.checkNotNull(matrix.getAudit(), "Missing audit");
        return new ProctorResult(audit.getVersion(), testGroups, testAllocations, testDefinitions);
    }

    TestMatrixArtifact getArtifact() {
        return matrix;
    }

    public Set<String> getTestNames() {
        return Collections.unmodifiableSet(matrix.getTests().keySet());
    }

    public ConsumableTestDefinition getTestDefinition(final String name) {
        return matrix.getTests().get(name);
    }

    public ProctorLoadResult getLoadResult() {
        return loadResult;
    }

    public void appendAllTests(final Writer sb) {
        appendTests(sb, Predicates.alwaysTrue());
    }

    public void appendTests(final Writer sb, final TestType type) {
        appendTests(sb, new Predicate<TestChooser<?>>() {
            @Override
            public boolean apply(final TestChooser<?> input) {
                assert null != input;
                return type == input.getTestDefinition().getTestType();
            }
        });
    }

    public void appendTestsNameFiltered(final Writer sb, final Collection<String> testNameFilter)
    {
        final Function<TestChooser<?>, String> getTestName = new Function<TestChooser<?>, String>() {
            @Override
            public String apply(final TestChooser<?> input) {
                return input.getTestName();
            }
        };
        appendTests(sb, Predicates.compose(Predicates.in(testNameFilter), getTestName));
    }

    public void appendTests(final Writer sb, @Nonnull final Predicate<TestChooser<?>> shouldIncludeTest) {
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

    /**
     * appends json representation of testmatrix. Does not close the writer.
     */
    public void appendTestMatrix(final Writer writer) throws IOException {
        OBJECT_WRITER.writeValue(writer, this.matrix);
    }

    /**
     * appends json representation of testmatrix with only given testnames. Does not close the writer.
     */
    public void appendTestMatrixFiltered(final Writer writer, final Collection<String> testNameFilter) throws IOException {
        // Create new matrix object copied from the old one,
        // but keep only the tests with names in testNameFilter.
        final TestMatrixArtifact filtered = new TestMatrixArtifact();
        filtered.setAudit(this.matrix.getAudit());
        filtered.setTests(Maps.filterKeys(this.matrix.getTests(), Predicates.in(testNameFilter)));
        OBJECT_WRITER.writeValue(writer, filtered);
    }

}
