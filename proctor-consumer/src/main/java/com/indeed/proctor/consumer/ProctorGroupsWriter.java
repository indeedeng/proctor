package com.indeed.proctor.consumer;

import com.indeed.proctor.common.PayloadProperty;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.consumer.logging.TestGroupFormatter;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

import static com.indeed.proctor.consumer.AbstractGroups.loggableAllocation;

/**
 * Helper class to build a Strings to log, helping with analysis of experiments.
 *
 * <p>Instance is thread-safe and can be reused for concurrent requests.
 */
@ThreadSafe
public class ProctorGroupsWriter {

    public static final char DEFAULT_GROUPS_SEPARATOR = ',';

    private final char groupsSeparator;
    // for historic reasons, allow more than one formatter
    private final TestGroupFormatter[] formatters;
    private final BiPredicate<String, ProctorResult> testFilter;
    private Set<String> winningPayloadExperiment;
    /**
     * @param groupsSeparator how elements in logged string are separated
     * @param formatters ideally only one, all groups will be logged for all formatters
     * @param testFilter only log tests passing this predicate
     */
    private ProctorGroupsWriter(
            final char groupsSeparator,
            final TestGroupFormatter[] formatters,
            final BiPredicate<String, ProctorResult> testFilter) {
        this.groupsSeparator = groupsSeparator;
        this.formatters = formatters.clone();
        this.testFilter = testFilter;
    }

    /**
     * provide a String from the proctorResult for the purpose of logging. Algorithm: - add all
     * classifiers separated by groupSeparator - for each formatter: -- iterate over all filtered
     * testgroups -- append the testgroups as formatted by this formatter separated by
     * groupSeparator
     *
     * @param classifiers: Optional, Used to for filtering in log string analysis
     * @return a String like
     *     "classifier1,classifier2,..,group1format1,group2format1,...,group1format2,group2format2..."
     */
    public final String writeGroupsAsString(
            final ProctorResult proctorResult, final String... classifiers) {
        final List<String> filteredTestNames = new ArrayList<>(proctorResult.getBuckets().size());
        for (final String testName : proctorResult.getBuckets().keySet()) {
            if (testFilter.test(testName, proctorResult)) {
                filteredTestNames.add(testName);
            }
        }

        // estimate capacity to reduce memory impact
        int initialCapacity = 0;
        for (final String testName : filteredTestNames) {
            // rough estimate of a complex formatter, e.g. "#A11:" + "footst" + "-1,"
            initialCapacity += testName.length() + 10;
        }
        initialCapacity *= formatters.length;
        for (final String c : classifiers) {
            initialCapacity += c.length() + 1;
        }

        final StringBuilder stringBuilder = new StringBuilder(initialCapacity);

        for (final String classifier : classifiers) {
            stringBuilder.append(classifier).append(groupsSeparator);
        }

        for (final TestGroupFormatter formatter : formatters) {
            for (final String testName : filteredTestNames) {
                // no allocation might exist for this testbucket which represents a force group
                final String allocId =
                        proctorResult.getAllocations().get(testName) == null
                                ? "force"
                                : Optional.ofNullable(proctorResult.getAllocations().get(testName))
                                        .map(Allocation::getId)
                                        .orElse("");
                // allocation should never be null, guarding against NPE anyway
                // id can be blank for historical data
                final int lengthBefore = stringBuilder.length();
                formatter.appendProctorTestGroup(
                        stringBuilder, testName, allocId, proctorResult.getBuckets().get(testName));
                // append separator unless formatter did not append anything
                if (lengthBefore < stringBuilder.length()) {
                    stringBuilder.append(groupsSeparator);
                }
            }
        }
        // remove final separator if necessary
        if (stringBuilder.length() > 0) {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }
        return stringBuilder.toString();
    }

    public static class Builder {
        private final TestGroupFormatter[] formatters;
        private char groupSeparator = DEFAULT_GROUPS_SEPARATOR;
        private boolean includeSilentTests;
        private boolean includeTestWithoutDefinition = true;
        private boolean includeInactiveGroups;
        private BiPredicate<String, ProctorResult> additionalFilter;

        public Builder(final TestGroupFormatter formatter) {
            this.formatters = new TestGroupFormatter[] {formatter};
        }

        /**
         * multi-formatter constructor provided for historic reasons
         *
         * @param formatters should only need a single formatter in normal cases
         */
        public Builder(final TestGroupFormatter... formatters) {
            this.formatters = formatters;
        }

        /** @param formatter typically could use TestGroupFormatter.WITH_ALLOC_ID */
        public static Builder withFormatter(final TestGroupFormatter formatter) {
            // For legacy reasons, log both with and without allocation Id
            return new Builder(TestGroupFormatter.WITH_ALLOC_ID);
        }

        /** default ProctorLogStringConcatenator.DEFAULT_GROUPS_SEPARATOR */
        public Builder setGroupSeparator(final char separator) {
            this.groupSeparator = separator;
            return this;
        }

        /** default false (that's what silent means) */
        public Builder setIncludeSilentTests(final boolean includeSilentTests) {
            this.includeSilentTests = includeSilentTests;
            return this;
        }

        /** default true, logs in error cases */
        public Builder setIncludeTestWithoutDefinition(final boolean includeTestWithoutDefinition) {
            this.includeTestWithoutDefinition = includeTestWithoutDefinition;
            return this;
        }

        /** default false, because inactive groups should not be evaluateds in experiments */
        public Builder setIncludeInactiveGroups(final boolean includeInactiveGroups) {
            this.includeInactiveGroups = includeInactiveGroups;
            return this;
        }

        /**
         * default null, can implement any logic, can be combined with other filters
         *
         * @param testFilter will be called during each invocation of toLoggingString(), must be
         *     threadsafe
         */
        public Builder setAdditionalCustomFilter(
                final BiPredicate<String, ProctorResult> testFilter) {
            this.additionalFilter = testFilter;
            return this;
        }

        public ProctorGroupsWriter build() {
            return new ProctorGroupsWriter(
                    groupSeparator,
                    formatters,
                    // by default, same logic as in AbstractGroups.toLoggingString()
                    (testName, proctorResult) -> {
                        final Map<String, ConsumableTestDefinition> testDefinitions =
                                proctorResult.getTestDefinitions();
                        // testDefinition should never be null, guarding against NPE anyway
                        @Nullable
                        final ConsumableTestDefinition consumableTestDefinition =
                                testDefinitions.get(testName);
                        if (consumableTestDefinition == null && !includeTestWithoutDefinition) {
                            return false;
                        }
                        // fallback to non-silent when test definition is not available
                        if (consumableTestDefinition != null
                                && consumableTestDefinition.getSilent()
                                && !includeSilentTests) {
                            return false;
                        }
                        // only live buckets.
                        final TestBucket testBucket = proctorResult.getBuckets().get(testName);
                        if (testBucket == null) {
                            // testBucket should never be null, guarding against NPE anyway (here
                            // and later in formatter)
                            return false;
                        }
                        if (testBucket.getValue() < 0 && !includeInactiveGroups) {
                            return false;
                        }
                        if (additionalFilter != null) {
                            return additionalFilter.test(testName, proctorResult);
                        }

                        // Do not log payload experiments which were overwritten
                        if (consumableTestDefinition != null
                                && consumableTestDefinition.getPayloadExperimentConfig() != null
                                && !proctorResult.getProperties().values().stream()
                                        .map(PayloadProperty::getTestName)
                                        .collect(Collectors.toSet())
                                        .contains(testName)) {
                            return false;
                        }

                        // Suppress 100% allocation logging
                        return loggableAllocation(
                                testName, consumableTestDefinition, proctorResult);
                    });
        }
    }
}
