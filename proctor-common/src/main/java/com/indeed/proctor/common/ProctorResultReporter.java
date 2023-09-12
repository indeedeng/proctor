package com.indeed.proctor.common;

import com.indeed.proctor.common.model.TestType;

import java.util.Map;

public interface ProctorResultReporter {
    default void reportMetrics(
            final ProctorResult result,
            final Map<TestType, Integer> testTypesWithInvalidIdentifier) {
        reportTotalEvaluatedTests(result);
        reportFallbackTests(result);
        reportInvalidIdentifierTests(result, testTypesWithInvalidIdentifier);
    }

    void reportTotalEvaluatedTests(final ProctorResult result);

    void reportFallbackTests(final ProctorResult result);

    void reportInvalidIdentifierTests(
            final ProctorResult result,
            final Map<TestType, Integer> testTypesWithInvalidIdentifier);
}
