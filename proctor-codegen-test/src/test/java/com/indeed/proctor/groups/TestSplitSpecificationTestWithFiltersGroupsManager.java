package com.indeed.proctor.groups;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.codegen.test.groups.SplitSpecificationTestWithFiltersGroupsContext;
import com.indeed.proctor.codegen.test.groups.SplitSpecificationTestWithFiltersGroupsManager;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.TestType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestSplitSpecificationTestWithFiltersGroupsManager {
    private static final Logger LOGGER =
            LogManager.getLogger(TestSplitSpecificationTestWithFiltersGroupsManager.class);
    private static final String SPECIFICATION_RESOURCE =
            "temp/SplitSpecificationTestWithFiltersGroups.json";
    private static final String SPECIFICATION_MATRIX = "splitspecificationtest.proctor-matrix.json";

    private SplitSpecificationTestWithFiltersGroupsManager manager;

    @Before()
    public void setUp() throws Exception {
        manager =
                new SplitSpecificationTestWithFiltersGroupsManager(
                        () -> UtilMethods.getProctor(SPECIFICATION_MATRIX, SPECIFICATION_RESOURCE));
    }

    @Test
    public void testLoadDynamicTests() {
        final SplitSpecificationTestWithFiltersGroupsContext testContext =
                SplitSpecificationTestWithFiltersGroupsContext.newBuilder()
                        .setLoggedIn(true)
                        .setCountry("FR")
                        .setAccountId(10)
                        .build();
        final Identifiers identifiers =
                new Identifiers(
                        ImmutableMap.<TestType, String>builder()
                                .put(TestType.ANONYMOUS_USER, SPECIFICATION_MATRIX)
                                .put(TestType.AUTHENTICATED_USER, SPECIFICATION_MATRIX)
                                .put(TestType.PAGE, SPECIFICATION_MATRIX)
                                .build());
        final ProctorResult result = testContext.getProctorResult(manager, identifiers);
        assertTrue(
                "a test defined in specification should be loaded",
                result.getAllocations().containsKey("one"));
        assertTrue(
                "a test matched defined filter should be loaded",
                result.getAllocations().containsKey("two"));
        assertFalse(
                "a test not matched in both of specification and filters shouldn't be loaded",
                result.getAllocations().containsKey("three"));
    }
}
