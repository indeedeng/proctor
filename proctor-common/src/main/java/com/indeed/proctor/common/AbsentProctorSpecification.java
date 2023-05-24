package com.indeed.proctor.common;

import com.indeed.proctor.common.dynamic.DynamicFilter;
import com.indeed.proctor.common.dynamic.DynamicFilters;
import com.indeed.proctor.common.dynamic.MatchAllFilter;

import java.util.Collections;

/**
 * A placeholder for when Proctor users explicitly don't want to use a specification.
 *
 * <p>Without a specification, Proctor does a limited verification for internal consistency, and
 * determineGroups() can return all tests in the test matrix. Using the testNameFilter in this case
 * would be a good idea.
 *
 * <p>The Proctor REST API uses this so that it isn't limited by a test specification. Instead, it
 * can serve all tests.
 */
public class AbsentProctorSpecification extends ProctorSpecification {
    // filter that matches all proctor tests.
    // used to serve all tests with determineGroups()
    private static final DynamicFilter MATCH_ALL_FILTER = new MatchAllFilter();

    public AbsentProctorSpecification() {
        super();
        setDynamicFilters(new DynamicFilters(Collections.singleton(MATCH_ALL_FILTER)));
    }
}
