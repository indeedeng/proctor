package com.indeed.proctor.consumer.spring;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.indeed.proctor.common.AbstractProctorLoader;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.Proctor;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.consumer.AbstractGroups;
import org.springframework.web.HttpRequestHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

/**
 * Take a random sampling of group determinations for a test or set of tests.
 *
 * Usage requires implementing {@link ContextSupplier} which should return the Proctor groups
 * object for a given context and Identifiers. Generally this just means passing fields from the ProctorContext to
 * a groups manager and calling determineBuckets.
 *
 * The ProctorContext class should be a bean that has all of the fields you need to run groups determination - usually
 * all of the fields in the specification's providedContext.
 *
 * The page will be mapped at /sampleRandomGroups under wherever the controller is mapped.
 *
 * @author jsgroth
 */
public class SampleRandomGroupsHttpHandler<ProctorContext> implements HttpRequestHandler {
    private static final int DEFAULT_SAMPLES = 1000;

    private final AbstractProctorLoader proctorLoader;
    private final ContextSupplier<ProctorContext> supplier;

    private final Random random;

    public SampleRandomGroupsHttpHandler(final AbstractProctorLoader proctorLoader,
                                         final ContextSupplier<ProctorContext> supplier) {
        this.proctorLoader = proctorLoader;
        this.supplier = supplier;

        this.random = new Random();
    }

    @Override
    public void handleRequest(final HttpServletRequest request,
                              final HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        response.setCharacterEncoding("UTF-8");

        try {
            handleSampleRandomGroupsInner(request, response);
        } catch (Throwable t) {
            t.printStackTrace(response.getWriter());
            response.flushBuffer();
        }
    }


    private void handleSampleRandomGroupsInner(final HttpServletRequest request, final HttpServletResponse response) throws IOException, InstantiationException, IllegalAccessException {
        final String targetTestGroupsString = request.getParameter("test");
        if (targetTestGroupsString == null) {
            printUsage(request, response);
            return;
        }

        final ProctorContext proctorContext = supplier.resolveContext(request);
        final Set<String> targetTestNames = Sets.newTreeSet(Splitter.on(',').omitEmptyStrings().split(targetTestGroupsString));
        if (targetTestNames.isEmpty()) {
            printUsage(request, response);
            return;
        }

        final TestType testType = getTestType(targetTestNames);

        final String samplesString = request.getParameter("samples");
        final int samples;
        if (!Strings.isNullOrEmpty(samplesString)) {
            samples = Integer.parseInt(samplesString);
        } else {
            samples = DEFAULT_SAMPLES;
        }

        final Map<String, Integer> testGroupToOccurrences = runSampling(proctorContext, targetTestNames, testType, samples);

        final PrintWriter writer = response.getWriter();
        printResults(writer, testGroupToOccurrences, testType, samples);
        writer.println(Strings.repeat("-", 100));
        writer.println(supplier.printProctorContext(proctorContext));
        response.flushBuffer();
    }

    private static void printResults(
            final PrintWriter writer,
            final Map<String, Integer> testGroupToOccurrences,
            final TestType testType,
            final int determinationsRun
    ) throws IOException {
        for (final String testGroup : testGroupToOccurrences.keySet()) {
            final int occurrences = testGroupToOccurrences.get(testGroup);
            final float percentage = ((float) occurrences * 100) / determinationsRun;
            writer.printf("Found '%s' for %d out of %d (%.2f%%) random group samples for test type %s%n", testGroup, occurrences, determinationsRun, percentage, testType);
        }
    }

    // Given the list of target test names, determine the test type that should be used.
    // Throws IllegalArgumentException if there are tests in the list with different test types.
    private TestType getTestType(final Set<String> targetTestNames) {
        final Proctor proctor = getProctorNotNull();

        TestType testType = null;
        for (final String testName : targetTestNames) {
            final ConsumableTestDefinition testDefinition = proctor.getTestDefinition(testName);
            if (testDefinition == null) {
                throw new IllegalArgumentException("Unrecognized test name: '" + testName + "'");
            }
            if (testType == null) {
                testType = testDefinition.getTestType();
            } else if (testDefinition.getTestType() != testType) {
                throw new IllegalArgumentException("Target test group list contains tests of multiple test types: " +
                        testType + " and " + testDefinition.getTestType());
            }
        }
        assert testType != null;
        return testType;
    }

    // Run random group determination.
    // This will run the specified number of group determinations and will record, for each group in each target
    // test, how many times the group was present in the list of groups.
    private Map<String, Integer> runSampling(
            final ProctorContext proctorContext,
            final Set<String> targetTestNames,
            final TestType testType,
            final int determinationsToRun
    ) {
        final Set<String> targetTestGroups = getTargetTestGroups(targetTestNames);
        final Map<String, Integer> testGroupToOccurrences = Maps.newTreeMap();
        for (final String testGroup : targetTestGroups) {
            testGroupToOccurrences.put(testGroup, 0);
        }

        for (int i = 0; i < determinationsToRun; ++i) {
            final Identifiers identifiers = Identifiers.of(testType, Long.toString(random.nextLong()));
            final AbstractGroups groups = supplier.getRandomGroups(proctorContext, identifiers);
            for (final Entry<String, TestBucket> e : groups.getProctorResult().getBuckets().entrySet()) {
                final String testName = e.getKey();
                if (targetTestNames.contains(testName)) {
                    final int group = e.getValue().getValue();
                    final String testGroup = testName + group;
                    testGroupToOccurrences.put(testGroup, testGroupToOccurrences.get(testGroup) + 1);
                }
            }
        }

        return testGroupToOccurrences;
    }

    // Get all test group strings for the given set of test names
    private Set<String> getTargetTestGroups(final Set<String> targetTestNames) {
        final Proctor proctor = getProctorNotNull();

        final Set<String> testGroups = Sets.newTreeSet();
        for (final String testName : targetTestNames) {
            final ConsumableTestDefinition testDefinition = proctor.getTestDefinition(testName);
            for (final TestBucket bucket : testDefinition.getBuckets()) {
                final String testGroup = testName + bucket.getValue();
                testGroups.add(testGroup);
            }
        }

        return testGroups;
    }

    public static interface ContextSupplier<ProctorContext> {

        ProctorContext resolveContext(HttpServletRequest request);

        String printProctorContext(ProctorContext proctorContext);

        /*
         * Determine groups for the given context and identifiers.
         */
        AbstractGroups getRandomGroups(ProctorContext proctorContext, Identifiers identifiers);
    }

    private static void printUsage(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        final PrintWriter writer = response.getWriter();

        writer.println("To use this page, add a 'test' parameter with the name of the test you want to sample distributions for.");
        writer.println("For example: " + request.getContextPath() + request.getServletPath() + "?test=example_test");

        response.flushBuffer();
    }

    // return currently-loaded Proctor instance, throwing IllegalStateException if not loaded
    private Proctor getProctorNotNull() {
        final Proctor proctor = proctorLoader.get();
        if (proctor == null) {
            throw new IllegalStateException("Proctor specification and/or text matrix has not been loaded");
        }
        return proctor;
    }
}
