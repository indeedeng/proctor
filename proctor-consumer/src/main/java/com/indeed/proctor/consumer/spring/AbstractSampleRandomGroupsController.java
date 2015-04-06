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
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

/**
 * Take a random sampling of group determinations for a test or set of tests.
 *
 * Usage requires subclassing this controller and implementing {@link #getRandomGroups} which should return the Proctor groups
 * object for a given context and Identifiers. Generally this just means passing fields from the ProctorContext to
 * a groups manager and calling determineBuckets.
 *
 * The ProctorContext class should be a bean that has all of the fields you need to run groups determination - usually
 * all of the fields in the specification's providedContext. Any field with a bean-style setter can be set using URL
 * parameters.
 *
 * The page will be mapped at /sampleRandomGroups under wherever the controller is mapped.
 *
 * @author jsgroth
 */
public abstract class AbstractSampleRandomGroupsController<ProctorContext> {
    private static final int DEFAULT_SAMPLES = 1000;

    private final AbstractProctorLoader proctorLoader;
    private final Class<ProctorContext> contextClass;

    private final Random random;

    protected AbstractSampleRandomGroupsController(final AbstractProctorLoader proctorLoader, final Class<ProctorContext> contextClass) {
        this.proctorLoader = proctorLoader;
        this.contextClass = contextClass;

        this.random = new Random();
    }

    @RequestMapping(value = "/sampleRandomGroups", method = RequestMethod.GET)
    public void handleSampleRandomGroups(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
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

        final ProctorContext proctorContext = getProctorContext(request);
        final Set<String> targetTestNames = Sets.newTreeSet(Splitter.on(',').omitEmptyStrings().split(targetTestGroupsString));
        if (targetTestNames.isEmpty()) {
            printUsage(request, response);
            return;
        }

        final TestType testType = getTestType(targetTestNames);
        final Identifiers identifiers = new Identifiers(testType, Long.toString(random.nextLong()));

        final String samplesString = request.getParameter("samples");
        final int samples;
        if (!Strings.isNullOrEmpty(samplesString)) {
            samples = Integer.parseInt(samplesString);
        } else {
            samples = DEFAULT_SAMPLES;
        }

        final Map<String, Integer> testGroupToOccurrences = runSampling(proctorContext, targetTestNames, identifiers, samples);

        final PrintWriter writer = response.getWriter();
        printResults(writer, testGroupToOccurrences, testType, samples);
        writer.println(Strings.repeat("-", 100));
        printProctorContext(writer, proctorContext);
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
            writer.println("Found '" + testGroup + "' for " + occurrences + " out of " + determinationsRun + " random group samples for test type " + testType);
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
            final Identifiers identifiers,
            final int determinationsToRun
    ) {
        final Set<String> targetTestGroups = getTargetTestGroups(targetTestNames);
        final Map<String, Integer> testGroupToOccurrences = Maps.newTreeMap();
        for (final String testGroup : targetTestGroups) {
            testGroupToOccurrences.put(testGroup, 0);
        }

        for (int i = 0; i < determinationsToRun; ++i) {
            final AbstractGroups groups = getRandomGroups(proctorContext, identifiers);
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

    // Do some magic to turn request parameters into a context object
    private ProctorContext getProctorContext(final HttpServletRequest request) throws IllegalAccessException, InstantiationException {
        final ProctorContext proctorContext = contextClass.newInstance();
        final BeanWrapper beanWrapper = new BeanWrapperImpl(proctorContext);
        for (final PropertyDescriptor descriptor : beanWrapper.getPropertyDescriptors()) {
            final String propertyName = descriptor.getName();
            if (!"class".equals(propertyName)) { // ignore class property which every object has
                final String parameterValue = request.getParameter(propertyName);
                if (parameterValue != null) {
                    beanWrapper.setPropertyValue(propertyName, parameterValue);
                }
            }
        }
        return proctorContext;
    }

    /**
     * Determine groups for the given context and identifiers.
     */
    protected abstract AbstractGroups getRandomGroups(ProctorContext proctorContext, Identifiers identifiers);

    private void printProctorContext(PrintWriter writer, ProctorContext proctorContext) throws IOException {
        final BeanWrapper beanWrapper = new BeanWrapperImpl(proctorContext);
        for (final PropertyDescriptor descriptor : beanWrapper.getPropertyDescriptors()) {
            final String propertyName = descriptor.getName();
            if (!"class".equals(propertyName)) { // ignore class property which every object has
                final Object propertyValue = beanWrapper.getPropertyValue(propertyName);
                writer.println(propertyName + ": '" + propertyValue + "'");
            }
        }
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
