package com.indeed.proctor.consumer;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.consumer.logging.ExposureLogger;
import com.indeed.proctor.consumer.logging.TestUsageObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Wraps a ProctorResult to provide some utility functions. The main purpose is to support
 * proctor-codegen, but also provides String-building function to experiment eligibilities, and
 * functions to help serialize data and use in generated with Javascript code.
 *
 * <p>Subclasses should strive to override only a minimal number of methods, such as
 * overrideDeterminedBucketValue() to change determined buckets, or toLoggingString() to change
 * logging.
 */
public abstract class AbstractGroups {
    private static final Logger LOGGER = LogManager.getLogger(AbstractGroups.class);
    private final ProctorResult proctorResult;

    // Option using injected Observer
    @CheckForNull private TestUsageObserver testUsageObserver;

    @CheckForNull private final ExposureLogger exposureLogger;

    /** A character to separate groups in logging output. */
    protected static final char GROUPS_SEPARATOR = ',';
    /**
     * A character to separate allocation id and test name for a formatted test group with an
     * allocation.
     */
    protected static final char ALLOCATION_GROUP_SEPARATOR = ':';

    protected static final char TESTNAME_BUCKET_CONNECTOR = '-';

    protected AbstractGroups(
            final ProctorResult proctorResult,
            @Nullable final TestUsageObserver testUsageObserver,
            @Nullable final ExposureLogger exposureLogger) {
        this.proctorResult = proctorResult;
        this.testUsageObserver = testUsageObserver;
        this.exposureLogger = exposureLogger;
    }

    /**
     * Setup fields based on eagerly computed bucket allocations in ProctorResult.
     *
     * @param testUsageObserver if non-null, it's methods will be called whenever proctor tests are
     *     used
     */
    protected AbstractGroups(
            final ProctorResult proctorResult,
            @Nullable final TestUsageObserver testUsageObserver) {
        this(proctorResult, testUsageObserver, null);
    }

    /** Setup fields based on eagerly computed bucket allocations in ProctorResult. */
    protected AbstractGroups(final ProctorResult proctorResult) {
        this(proctorResult, null);
    }

    /**
     * Allows to use a different bucket value than the determined one. Default Behavior is to return
     * determinedBucket.getValue(). The most common use-case would be to return a negative
     * (inactive) value based on external conditions.
     *
     * <p>Overriding this will also changes the output of logging methods like toLoggingString().
     *
     * <p>Note: for experiments, this method should not modify the relative ratios of active test
     * buckets to each other, else experiment analysis may produce wrong results. E.g. if raw ratios
     * are - 10%:inactive - 60%:group0 - 30%:group1 then group0 and group1 have ratio 2:1, and so it
     * would be valid to change this to 40/40/20, but invalid to change it to 40/30/30.
     *
     * <p>Note: Because overriding this is local to each deployed application, using this for tests
     * shared with other applications is likely to produce wrong experiment analysis results, as
     * other applications would apply (and log) raw determined buckets.
     *
     * <p>Note: This method is the only one to change when developers want to customize group
     * ownership, such as for implementing hold-out groups. Customizers are encouraged to use
     * meta-tags to drive customization, see getRawProctorResult(). Use
     * getRawProctorResult().getTestDefinitions().get(testName).getBuckets() to select a different
     * valid bucket value. Do NOT use getAsProctorResult(), it will likely cause an infinite loop.
     *
     * <p>Also note that if calling other methods of this class inside this method, it is easily
     * possible to create infinite loops (stackoverflow), so be careful and write unit tests
     *
     * @return the value bucket that has been determined by the current proctorResult
     */
    // returning int instead of TestBucket to prevent e.g. values that are inconsistent with
    // definition
    protected int overrideDeterminedBucketValue(
            final String testName, @Nonnull final TestBucket determinedBucket) {
        return determinedBucket.getValue();
    }

    /**
     * @return true if testname exists and resolved bucket has given value
     * @deprecated Use/Override {@link #isBucketActive(String, int, int)} instead
     */
    @Deprecated
    // used from generated code
    protected boolean isBucketActive(final String testName, final int value) {
        // using getActiveBucket to allow overrides
        return getActiveBucket(testName)
                .filter(testBucket -> value == testBucket.getValue())
                .isPresent();
    }

    /**
     * @return true if testname exists and resolved bucket has given value, else if value equals
     *     defaultValue
     */
    // used from generated code
    protected boolean isBucketActive(
            final String testName, final int value, final int defaultValue) {
        return value == getValue(testName, defaultValue);
    }

    /**
     * @return value of the active bucket if present else default
     *     <p>Prefer to override getActiveBucket to overriding this method. Else make sure to use
     *     getActiveBucket for consistency.
     */
    // used from generated code. This method should rather be final, but is currently overridden, to
    // be made final in the future
    protected int getValue(final String testName, final int defaultValue) {
        // using getActiveBucket to allow overrides
        return getActiveBucket(testName).map(TestBucket::getValue).orElse(defaultValue);
    }

    protected final int getValueWithoutMarkingUsage(final String testName, final int defaultValue) {
        // using getActiveBucket to allow overrides
        return getActiveBucketWithoutMarkingUsage(testName)
                .map(TestBucket::getValue)
                .orElse(defaultValue);
    }

    /**
     * @return the bucket that has been determined by the current proctorResult, or override bucket
     *     if valid, or empty if testname not valid
     */
    // intentionally final, other developers should only override overrideDeterminedBucketValue()
    protected final Optional<TestBucket> getActiveBucket(final String testName) {

        final Optional<TestBucket> bucketOpt = getActiveBucketWithoutMarkingUsage(testName);
        bucketOpt.ifPresent(
                bucket -> {
                    if (testUsageObserver != null) {
                        testUsageObserver.markUsedForToggling(testName);
                    }
                    if (exposureLogger != null) {
                        exposureLogger.logExposureInfo(this.proctorResult, testName);
                    }
                });
        return bucketOpt;
    }

    /**
     * mark tests to additionally be included in getAsUsedTestsProctorResult(), even when no
     * corresponding method has been called. for usecases where exposure happens without calls to
     * generated methods calling getValue()
     */
    public final void markTestsUsed(final Collection<String> testNames) {
        if (testUsageObserver != null) {
            testUsageObserver.markUsedForToggling(testNames);
        }
        if (exposureLogger != null) {
            testNames.forEach(testName -> exposureLogger.logExposureInfo(proctorResult, testName));
        }
    }

    /**
     * mark a test to additionally be included in getAsUsedTestsProctorResult(), even when no
     * corresponding method has been called. for usecases where exposure happens without calls to
     * generated methods calling getValue()
     */
    public final void markTestUsed(final String testName) {
        if (testUsageObserver != null) {
            testUsageObserver.markUsedForToggling(testName);
        }
        if (exposureLogger != null) {
            exposureLogger.logExposureInfo(proctorResult, testName);
        }
    }

    /**
     * @return the bucket that has been determined by the current proctorResult, or override bucket
     *     if valid, or empty if testname not valid
     */
    // intentionally private, clients should only see getActiveBucket()
    private Optional<TestBucket> getActiveBucketWithoutMarkingUsage(final String testName) {
        // intentionally not allowing subclasses to override returning empty for testnames that do
        // not exist in ProctorResult
        // the semantics of this class would become too confusing
        // if clients somehow need that, they should provide a proctorResult instance having
        // additional testNames with buckets
        final TestBucket bucket = proctorResult.getBuckets().get(testName);
        if (bucket == null) {
            return Optional.empty();
        }
        // allow users to select a different testbucket, if testname was valid
        final int overrideBucketValue = overrideDeterminedBucketValue(testName, bucket);
        if ((overrideBucketValue != bucket.getValue())) {
            // get bucket from definition with that override value from Definition
            final Optional<TestBucket> overrideBucketOpt =
                    getTestBucketWithValueOptional(proctorResult, testName, overrideBucketValue);
            if (overrideBucketOpt.isPresent()) {
                return overrideBucketOpt;
            }
            LOGGER.warn(
                    "Overriding bucket value "
                            + overrideBucketValue
                            + " for test '"
                            + testName
                            + "' does not match any bucket in test definition, using determined bucket value "
                            + bucket.getValue());
        }
        return Optional.of(bucket);
    }

    /**
     * Return the Payload attached to the current active bucket for |test|. Always returns a payload
     * so the client doesn't crash on a malformed test definition.
     *
     * @param testName test name
     * @return pay load attached to the current active bucket
     * @deprecated Use {@link #getPayload(String, Bucket)} instead
     */
    @Deprecated
    @Nonnull
    // used from generated code
    protected Payload getPayload(final String testName) {
        return getActiveBucket(testName).map(TestBucket::getPayload).orElse(Payload.EMPTY_PAYLOAD);
    }

    /**
     * If Matrix has a testbucket for this testname, return its payload (or empty). If matrix does
     * not have such a testbucket, looks up different bucket in the testdefinition and return it's
     * payload
     */
    @Nonnull
    // used from generated code
    protected Payload getPayload(final String testName, @Nonnull final Bucket<?> fallbackBucket) {
        return getPayload(testName, fallbackBucket.getValue());
    }

    /**
     * If Matrix has a testbucket for this testname, return its payload (or empty). If matrix does
     * not have such a testbucket, looks up different bucket in the testdefinition and return it's
     * payload
     */
    @Nonnull
    protected final Payload getPayload(final String testName, final int fallbackBucketValue) {
        final Optional<TestBucket> activeBucketOpt = getActiveBucket(testName);
        final Optional<TestBucket> resultBucketOpt;
        if (activeBucketOpt.isPresent()) {
            resultBucketOpt = activeBucketOpt;
        } else {
            resultBucketOpt =
                    Optional.ofNullable(getTestBucketWithValue(testName, fallbackBucketValue));
        }
        return resultBucketOpt.map(TestBucket::getPayload).orElse(Payload.EMPTY_PAYLOAD);
    }

    /**
     * Return the TestBucket, as defined in the current test matrix, for the test called testName
     * with bucket value targetBucket.getValue(). Can return null if it can't find any such bucket.
     * This does a linear search over the list of defined buckets. There shouldn't be too many
     * buckets in any test, so this should be fast enough.
     *
     * @param testName test name
     * @param targetBucket target bucket
     * @return the TestBucket. Return null if not found.
     */
    @CheckForNull
    // used in generated code
    protected final TestBucket getTestBucketForBucket(
            final String testName, final Bucket<?> targetBucket) {
        return getTestBucketWithValue(testName, targetBucket.getValue());
    }

    private static Optional<TestBucket> getTestBucketWithValueOptional(
            final ProctorResult proctorResult, final String testName, final int bucketValue) {
        return Optional.ofNullable(proctorResult.getTestDefinitions())
                .map(testDefinitions -> testDefinitions.get(testName))
                .map(ConsumableTestDefinition::getBuckets)
                .flatMap(
                        buckets ->
                                buckets.stream()
                                        .filter(testBucket -> testBucket.getValue() == bucketValue)
                                        .findFirst());
    }

    @CheckForNull
    final TestBucket getTestBucketWithValue(final String testName, final int bucketValue) {
        return getTestBucketWithValueOptional(proctorResult, testName, bucketValue).orElse(null);
    }

    /** @return a comma-separated String of {testname}-{active-bucket-name} for ALL tests */
    public String toLongString() {
        if (isEmpty()) {
            return "";
        }
        final Map<String, TestBucket> buckets = proctorResult.getBuckets();
        final StringBuilder sb = new StringBuilder(buckets.size() * 10);
        for (final String testName : buckets.keySet()) {
            sb.append(testName)
                    .append(TESTNAME_BUCKET_CONNECTOR)
                    .append(
                            getActiveBucketWithoutMarkingUsage(testName)
                                    .map(TestBucket::getName)
                                    .orElse("unknown"))
                    .append(GROUPS_SEPARATOR);
        }
        return sb.deleteCharAt(sb.length() - 1).toString();
    }

    /**
     * To be called when logging ONLY
     *
     * @return formatted group string
     * @deprecated use toLoggingString()
     */
    @Override
    @Deprecated
    public String toString() {
        return toLoggingString();
    }

    /**
     * String to be logged for experiment analysis. For historic reasons inside Indeed, contains two
     * output formats per testname.
     *
     * <p>Additional custom groups can be added by overriding getCustomGroupsForLogging().
     *
     * @return a comma-separated List of {testname}{active-bucket-VALUE} and
     *     {AllocationId}{testname}{active-bucket-VALUE} for all LIVE tests
     */
    public String toLoggingString() {
        if (isEmpty()) {
            return "";
        }
        final StringBuilder sb = new StringBuilder(proctorResult.getBuckets().size() * 10);
        appendTestGroups(sb);
        // remove trailing comma
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        return sb.toString();
    }

    /**
     * @return an empty string or a comma-separated, comma-finalized list of groups
     * @deprecated use toLoggingString()
     */
    @Deprecated
    public final StringBuilder buildTestGroupString() {
        final StringBuilder sb = new StringBuilder(proctorResult.getBuckets().size() * 10);
        appendTestGroups(sb);
        return sb;
    }

    /**
     * Return a value indicating if the groups are empty and should be represented by an
     * empty-string {@link #toString()}
     *
     * @return true if empty
     */
    protected boolean isEmpty() {
        return proctorResult.getBuckets().isEmpty();
    }

    /** appends an empty string or a comma-separated, comma-finalized list of groups */
    public final void appendTestGroups(final StringBuilder sb) {
        appendTestGroups(sb, GROUPS_SEPARATOR);
    }

    /**
     * For each testname returned by getLoggingTestNames(), appends each test group in the form of
     * [allocation id + ":" + test name + bucket value]. For example, it appends "#A1:bgcolortst1"
     * if test name is bgcolortst and allocation id is #A1 and separator is ",".
     *
     * <p>the separator should be appended after each test group added to the string builder {@link
     * #toString()} {@link #buildTestGroupString()} or {@link #appendTestGroups(StringBuilder)}
     *
     * @param sb a string builder
     * @param separator a char used as separator x appends an empty string or a x-separated,
     *     x-finalized list of groups
     */
    public void appendTestGroups(final StringBuilder sb, final char separator) {
        final List<String> testNames = getLoggingTestNames();
        appendTestGroupsWithAllocations(sb, separator, testNames);
    }

    /**
     * Return test names for tests that are non-silent or doesn't have available definition, and
     * have a non-negative active bucket, in a stable sort. Stable sort is beneficial for log string
     * compression, for debugging, and may help in cases of size-limited output.
     */
    protected final List<String> getLoggingTestNames() {
        final Map<String, ConsumableTestDefinition> testDefinitions =
                proctorResult.getTestDefinitions();
        // following lines should preserve the order in the map to ensure logging values are stable
        final Map<String, TestBucket> buckets = proctorResult.getBuckets();
        return buckets.keySet().stream()
                .filter(
                        testName -> {
                            final ConsumableTestDefinition consumableTestDefinition =
                                    testDefinitions.get(testName);
                            // fallback to non-silent when test definition is not available
                            return (consumableTestDefinition == null)
                                    || !consumableTestDefinition.getSilent();
                        })
                // call to getValueWithouMarkingUsage() to allow overrides of getActiveBucket, but
                // avoid marking
                .filter(testName -> getValueWithoutMarkingUsage(testName, -1) >= 0)
                .collect(Collectors.toList());
    }

    /**
     * Appends test groups in the form with allocation ids as [allocation id + ":" + test name +
     * bucket value] for given test names.
     */
    protected final void appendTestGroupsWithAllocations(
            final StringBuilder sb, final char separator, final List<String> testNames) {
        for (final String testName : testNames) {
            getActiveBucketWithoutMarkingUsage(testName)
                    .ifPresent(
                            testBucket -> {
                                // no allocation might exist for this testbucket
                                final Allocation allocation =
                                        proctorResult.getAllocations().get(testName);
                                // null allocation equals forced group -> log in legacy format
                                // [test name + bucket value]
                                if (allocation == null) {
                                    sb.append(testName)
                                            .append(testBucket.getValue())
                                            .append(separator);

                                } else if (!Strings.isNullOrEmpty(allocation.getId())) {
                                    sb.append(allocation.getId())
                                            .append(ALLOCATION_GROUP_SEPARATOR)
                                            .append(testName)
                                            .append(testBucket.getValue())
                                            .append(separator);
                                }
                            });
        }
    }

    /**
     * Generates a Map[testname, bucketValue] for bucketValues >= 0.
     *
     * <p>The purpose is to conveniently support serializing this map to JSON and used with
     * indeed.proctor.groups.init and indeed.proctor.groups.inGroup(tstName, bucketValue) from
     * common/indeedjs library
     *
     * @return a {@link Map} of config JSON
     */
    public final Map<String, Integer> getJavaScriptConfig() {
        return getJavaScriptConfig(n -> true);
    }

    /**
     * Generates a Map[testname, bucketValue] for given testnames, for bucketValues >= 0.
     *
     * <p>The purpose is to conveniently support serializing this map to JSON and used with
     * indeed.proctor.groups.init and indeed.proctor.groups.inGroup(tstName, bucketValue) from
     * common/indeedjs library
     *
     * @return a {@link Map} of config JSON
     */
    public final Map<String, Integer> getJavaScriptConfig(final Set<String> testNames) {
        return getJavaScriptConfig(testNames::contains);
    }

    private Map<String, Integer> getJavaScriptConfig(final Predicate<String> testNameFilter) {
        // For now this is a simple mapping from {testName to bucketValue}
        return proctorResult.getBuckets().keySet().stream()
                // mirrors appendTestGroups method by skipping *inactive* tests
                .filter(testNameFilter)
                // call to getValue() to allow overrides of getActiveBucket
                // not calling getValueWithoutMarkingUsage because when client calls this method,
                // all tests are potentially used.
                .map(testName -> new AbstractMap.SimpleEntry<>(testName, getValue(testName, -1)))
                .filter(e -> e.getValue() >= 0)
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    /**
     * Generates a list of [bucketValue, payloadValue]'s for each test in the input array The
     * purpose is to provide payloads in the order of tests defined in the client app's proctor
     * spec.
     *
     * <p>To be used with generated javascript files from 'ant generate-proctor-js' by serializing
     * the list to a string and passing it to {packageName}.init();
     *
     * @param tests an alphabetical list of Test enums from your generated proctor java subclass of
     *     {@link com.indeed.proctor.consumer.AbstractGroups}.
     * @param <E> Generic Type of Test
     * @return a list of 2-element lists that hold the bucketValue and payloadValue for each test in
     *     the same order as the input
     * @deprecated Please instead create an instance of {@link
     *     com.indeed.proctor.consumer.ProctorJavascriptPayloadBuilder} and call the method {@link
     *     ProctorJavascriptPayloadBuilder#buildAlphabetizedListJavascriptConfig()}
     */
    @Deprecated
    public <E extends Test> List<List<Object>> getJavaScriptConfig(final E[] tests) {
        // @TODO Move this logic to {@link
        // com.indeed.proctor.consumer.ProctorJavascriptPayloadBuilder} and remove this method
        return Arrays.stream(tests)
                .map(
                        test ->
                                Arrays.asList(
                                        // call to getValue() to allow overrides of getActiveBucket
                                        // not calling getValueWithoutMarkingUsage because when
                                        // client calls this method, all tests are potentially used.
                                        getValue(test.getName(), test.getFallbackValue()),
                                        getPayload(test.getName(), test.getFallbackValue())
                                                .fetchAValue()))
                .collect(Collectors.toList());
    }

    /**
     * creates a new copy of the input proctor result, derived from applying rules and hashing the
     * identifier, and also applying any custom logic from overriding
     * overrideDeterminedBucketValue().
     *
     * <p>Subclass methods like overrideDeterminedBucketValue() should not call this method (risks
     * infinite recursion).
     *
     * <p>Clients using the result to implement feature toggling should also call markTestsAsUsed
     * for such tests, to ensure those tests get logged with exposure logging.
     *
     * <p>For clients not overriding any methods, this should be equal to getRawProctorResult().
     *
     * @return converted data with customizations applied (if any).
     */
    public ProctorResult getAsProctorResult() {
        // Using guava Maps.transformEntries because it creates a lightweight view and does not copy
        // all entries
        final SortedMap<String, TestBucket> customBuckets =
                Maps.transformEntries(
                        (SortedMap<String, TestBucket>) proctorResult.getBuckets(),
                        // calling getActiveBucketWithoutMarkingUsage instead of getActiveBucket
                        // because clients are not
                        // supposed to use this method result to implement feature toggles.
                        (testName, bucket) -> getActiveBucketWithoutMarkingUsage(testName).get());
        return new ProctorResult(
                proctorResult.getMatrixVersion(),
                Collections.unmodifiableSortedMap(customBuckets),
                Collections.unmodifiableSortedMap(
                        (SortedMap<String, Allocation>) proctorResult.getAllocations()),
                Collections.unmodifiableMap(proctorResult.getTestDefinitions()),
                proctorResult.getIdentifiers(),
                Collections.unmodifiableMap(proctorResult.getInputContext()));
    }

    /**
     * returns an immutable copy of the raw proctor result derived from applying rules and hashing
     * the identifier. Calling this method many times will not create new instances. In most cases
     * getAsProctorResult() should be preferred (unless calling from a subclass). This does not take
     * into account customizations from overriding overrideDeterminedBucketValue or other methods.
     *
     * <p>Since apps might pass around AbstractGroups, but some code might want to access
     * ProctorResult directly, return wrapped data for convenience.
     *
     * <p>Clients using the result to implement feature toggling should also call markTestsAsUsed
     *
     * @return wrapped raw data.
     */
    public ProctorResult getRawProctorResult() {
        return ProctorResult.unmodifiableView(proctorResult);
    }

    /**
     * historically exposed the mutable private wrapped object
     *
     * @deprecated Use getAsProctorResult() or getRawProctorResult(), as appropriate.
     */
    @Deprecated
    public ProctorResult getProctorResult() {
        return proctorResult;
    }
}
