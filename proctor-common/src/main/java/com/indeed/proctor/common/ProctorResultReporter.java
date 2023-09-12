package com.indeed.proctor.common;

import com.indeed.proctor.common.model.TestType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;

public interface ProctorResultReporter {
    default void reportMetrics(final ProctorResult result, final Map<TestType, Integer> testTypesWithInvalidIdentifier) {
        reportTotalEvaluatedTests(result);
        reportFallbackTests(result);
        reportInvalidIdentifierTests(result, testTypesWithInvalidIdentifier);
    }

    void reportTotalEvaluatedTests(final ProctorResult result);

    void reportFallbackTests(final ProctorResult result);

    void reportInvalidIdentifierTests(final ProctorResult result, final Map<TestType, Integer> testTypesWithInvalidIdentifier);

    class ProctorResultReporterToDisk implements ProctorResultReporter {
        private static final Logger LOGGER = LogManager.getLogger(ProctorResultReporterToDisk.class);
        @Override
        public void reportTotalEvaluatedTests(final ProctorResult result) {
            LOGGER.debug("Total number of tests evaluated: {}", result.getTestDefinitions().size());
        }

        @Override
        public void reportFallbackTests(final ProctorResult result) {
            final int fallbackTotal = result.getTestDefinitions().size() - result.getBuckets().size();
            if (fallbackTotal > 0) {
                LOGGER.debug("Total number of tests which used fallback value: {}", fallbackTotal);
            }
        }

        @Override
        public void reportInvalidIdentifierTests(final ProctorResult result, final Map<TestType, Integer> testTypesWithInvalidIdentifier) {
            for (testTypesWithInvalidIdentifier)
        }
    }
}
