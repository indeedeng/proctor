package com.indeed.proctor.common;

/**
 * A placeholder for when Proctor users explicitly don't want to use a specification.
 *
 * Without a specification, Proctor does a limited verification for internal consistency, and determineGroups()
 * can return all tests in the test matrix. Using the testNameFilter in this case would be a good idea.
 *
 * The Proctor REST API uses this so that it isn't limited by a test specification. Instead, it can serve all tests.
 */
public class AbsentProctorSpecification extends ProctorSpecification {
    public AbsentProctorSpecification() {
        super();
        setTests(null);
    }
}
