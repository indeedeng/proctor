package com.indeed.proctor.groups;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.Proctor;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.TestType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import static com.indeed.proctor.groups.UtilMethods.calcBuckets;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import com.indeed.proctor.codegen.test.groups.EmptyContextTestGroupsManager;

import java.util.function.Supplier;

/**
 * Test to make sure specifications with no context variables generate code properly
 */
@SuppressWarnings({"ConstantConditions", "deprecation"})
public class TestEmptyContextTestGroupsManager {
    @SuppressWarnings("UnusedDeclaration")
    private static final Logger LOGGER = LogManager.getLogger(TestEmptyContextTestGroupsManager.class);
    private static final String SPECIFICATION_RESOURCE = "EmptyContextTestGroups.json";
    private static final String SPECIFICATION_MATRIX = "emptycontexttest.proctor-matrix.json";

    private EmptyContextTestGroupsManager manager;

    @Before()
    public void setUp() throws Exception {
        final Proctor proctor = UtilMethods.getProctor(SPECIFICATION_MATRIX, SPECIFICATION_RESOURCE);
        manager = new EmptyContextTestGroupsManager(() -> proctor);
    }

    @Test
    public void sanityCheck() {
        {
            final Identifiers identifiers = new Identifiers(ImmutableMap.<TestType, String>builder()
                    .put(TestType.ANONYMOUS_USER, SPECIFICATION_MATRIX)
                    .put(TestType.AUTHENTICATED_USER, SPECIFICATION_MATRIX)
                    .put(TestType.PAGE, SPECIFICATION_MATRIX)
                    .build());

            final ProctorResult result = manager.determineBuckets(identifiers);
            assertThat(calcBuckets(result)).isEqualTo(ImmutableMap.builder()
                    .put("kluj", "test1")
                    .put("map_payload", "control0")
                    .put("oop_poop", "control0")
                    .put("payloaded", "inactive-1")
                    .put("payloaded_verified", "inactive-1")
                    .put("pimple", "inactive-1")
                    .build());
        }
        {
            final ImmutableMap<TestType, String> idMap = ImmutableMap.<TestType, String>builder()
                    .put(TestType.EMAIL_ADDRESS, SPECIFICATION_MATRIX)
                    .put(TestType.AUTHENTICATED_USER, SPECIFICATION_MATRIX)
                    .put(TestType.PAGE, SPECIFICATION_MATRIX)
                    .build();
            final Identifiers identifiers = new Identifiers(idMap, true);

            final ProctorResult result = manager.determineBuckets(identifiers);
            assertEquals(2, result.getBuckets().get("dubblez").getValue());
        }
    }

}
