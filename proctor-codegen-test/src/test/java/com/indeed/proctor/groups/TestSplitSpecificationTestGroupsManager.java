package com.indeed.proctor.groups;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.codegen.test.groups.SplitSpecificationTestGroups;
import com.indeed.proctor.codegen.test.groups.SplitSpecificationTestGroupsContext;
import com.indeed.proctor.codegen.test.groups.SplitSpecificationTestGroupsManager;
import com.indeed.proctor.common.ForceGroupsDefaultMode;
import com.indeed.proctor.common.ForceGroupsOptions;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.TestType;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static com.indeed.proctor.groups.UtilMethods.calcBuckets;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Tests a GroupsManager generated for a specification split over 4 files, */
public class TestSplitSpecificationTestGroupsManager {
    private static final String SPECIFICATION_RESOURCE = "temp/SplitSpecificationTestGroups.json";
    private static final String SPECIFICATION_MATRIX = "splitspecificationtest.proctor-matrix.json";

    private SplitSpecificationTestGroupsManager manager;

    @Before()
    public void setUp() throws Exception {
        manager =
                new SplitSpecificationTestGroupsManager(
                        () -> UtilMethods.getProctor(SPECIFICATION_MATRIX, SPECIFICATION_RESOURCE));
    }

    @Test
    public void testMultipleTypes() {
        final SplitSpecificationTestGroupsContext testContext =
                SplitSpecificationTestGroupsContext.newBuilder()
                        .setLoggedIn(true)
                        .setCountry("FR")
                        .setAccountId(10)
                        .build();
        {
            final Identifiers identifiers =
                    new Identifiers(
                            ImmutableMap.<TestType, String>builder()
                                    .put(TestType.ANONYMOUS_USER, SPECIFICATION_MATRIX)
                                    .put(TestType.AUTHENTICATED_USER, SPECIFICATION_MATRIX)
                                    .put(TestType.PAGE, SPECIFICATION_MATRIX)
                                    .build());

            final ProctorResult result = testContext.getProctorResult(manager, identifiers);
            assertThat(calcBuckets(result))
                    .isEqualTo(
                            ImmutableMap.builder()
                                    .put("one", "test32")
                                    .put("three", "inactive-1")
                                    .put("two", "test22")
                                    .build());
        }
        {
            final ImmutableMap<TestType, String> idMap =
                    ImmutableMap.<TestType, String>builder()
                            .put(TestType.EMAIL_ADDRESS, SPECIFICATION_MATRIX)
                            .put(TestType.AUTHENTICATED_USER, SPECIFICATION_MATRIX)
                            .put(TestType.PAGE, SPECIFICATION_MATRIX)
                            .build();
            final Identifiers identifiers = new Identifiers(idMap, true);

            final ProctorResult result = testContext.getProctorResult(manager, identifiers);
            assertEquals(2, result.getBuckets().get("one").getValue());
        }
    }

    @Test
    public void testSomeBuckets() {
        final SplitSpecificationTestGroupsContext testContext =
                SplitSpecificationTestGroupsContext.newBuilder()
                        .setLoggedIn(true)
                        .setCountry("FR")
                        .setAccountId(10)
                        .build();
        {
            final Identifiers identifiers =
                    new Identifiers(TestType.ANONYMOUS_USER, "16s2o7s01001d9vj");
            final ProctorResult result = testContext.getProctorResult(manager, identifiers);
            assertThat(calcBuckets(result))
                    .isEqualTo(
                            ImmutableMap.builder()
                                    .put("three", "inactive-1")
                                    .put("two", "test33")
                                    .build());
            // Check and make sure SpecificationCreationGroups respects these groups and works as
            // expected.
            final SplitSpecificationTestGroups grps = new SplitSpecificationTestGroups(result);

            assertNotNull(grps.getOne());
            assertEquals(-99, grps.getOneValue(-99));
            assertEquals(SplitSpecificationTestGroups.Two.TEST3, grps.getTwo());
            assertEquals(3, grps.getTwoValue(-99));

            // Check the boolean conditions for one of the tests
            assertFalse(grps.isThreeTest());
            assertFalse(grps.isThreeControl());
            assertTrue(grps.isThreeInactive());
            assertFalse(grps.isTwoControl());
            assertTrue(grps.isTwoTest3());
            assertFalse(grps.isTwoTest1());
            assertFalse(grps.isTwoTest2());
            assertEquals("two3", grps.toString());
        }
    }

    @Test
    public void testPayloads() {
        final SplitSpecificationTestGroupsContext testContext =
                SplitSpecificationTestGroupsContext.newBuilder()
                        .setLoggedIn(true)
                        .setCountry("US")
                        .setAccountId(10)
                        .build();
        final Identifiers identifiers =
                new Identifiers(
                        ImmutableMap.<TestType, String>builder()
                                .put(TestType.ANONYMOUS_USER, SPECIFICATION_MATRIX)
                                .build());
        final ProctorResult result = testContext.getProctorResult(manager, identifiers);
        assertThat(calcBuckets(result))
                .isEqualTo(
                        ImmutableMap.builder()
                                .put("three", "inactive-1")
                                .put("two", "test22")
                                .build());
        // Check and make sure SpecificationCreationGroups respects these groups and works as
        // expected.
        final SplitSpecificationTestGroups grps = new SplitSpecificationTestGroups(result);
        assertNotNull(grps.getThree());
        assertEquals(-1, grps.getThreeValue(-99));
        // The "Inactive" condition should be true.
        assertTrue(grps.isThreeInactive());
        assertFalse(grps.isThreeControl());
        assertFalse(grps.isThreeTest());
        // Get the current test payload
        assertEquals(0, grps.getThreePayload(), 0.001);
        // Test per-bucket payload fetch
        assertEquals(
                0,
                grps.getThreePayloadForBucket(SplitSpecificationTestGroups.Three.INACTIVE),
                0.001);
        assertEquals(
                5,
                grps.getThreePayloadForBucket(SplitSpecificationTestGroups.Three.CONTROL),
                0.001);
        assertEquals(
                50, grps.getThreePayloadForBucket(SplitSpecificationTestGroups.Three.TEST), 0.001);

        assertEquals("two2", grps.toString());
    }

    @Test
    public void testTestDescriptions() {
        final SplitSpecificationTestGroupsContext testContext =
                SplitSpecificationTestGroupsContext.newBuilder()
                        .setLoggedIn(true)
                        .setCountry("FR")
                        .setAccountId(10)
                        .build();
        final Identifiers identifiers = new Identifiers(TestType.USER, "16s2o7s01001d9vj");
        final ProctorResult result = testContext.getProctorResult(manager, identifiers);
        assertThat(calcBuckets(result))
                .isEqualTo(
                        ImmutableMap.builder()
                                .put("three", "inactive-1")
                                .put("two", "test33")
                                .build());
        // Check and make sure SpecificationCreationGroups respects these groups and works as
        // expected.
        final SplitSpecificationTestGroups grps = new SplitSpecificationTestGroups(result);
        // make sure getDescription method exists and returns the correct description
        assertEquals("2nd test", grps.getThreeDescription());
        assertEquals("3rd \n\t\"test", grps.getOneDescription());
    }

    @Test
    public void testGetProctorResult_forcedGroups_shouldForceCorrectGroups() {
        final SplitSpecificationTestGroupsContext context =
                SplitSpecificationTestGroupsContext.newBuilder().build();
        final Identifiers identifiers =
                new Identifiers(TestType.USER, "16s2o7s01001d9vj"); // resolves two3 and three-1
        final ProctorResult result =
                context.getProctorResult(
                        manager,
                        identifiers,
                        ImmutableMap.of(
                                "two", 1,
                                "three", 0));
        assertThat(result.getBuckets())
                .containsOnlyKeys("two", "three")
                .hasEntrySatisfying("two", x -> assertThat(x.getValue()).isEqualTo(1))
                .hasEntrySatisfying("three", x -> assertThat(x.getValue()).isEqualTo(0));
    }

    @Test
    public void testGetProctorResult_forcedGroupsOptions_shouldForceCorrectGroups() {
        final SplitSpecificationTestGroupsContext context =
                SplitSpecificationTestGroupsContext.newBuilder().build();
        final Identifiers identifiers =
                new Identifiers(TestType.USER, "16s2o7s01001d9vj"); // resolves two3 and three-1
        final ProctorResult result =
                context.getProctorResult(
                        manager,
                        identifiers,
                        ForceGroupsOptions.builder()
                                .putForceGroup("two", 1)
                                .putForceGroup("three", 0)
                                .build());
        assertThat(result.getBuckets())
                .containsOnlyKeys("two", "three")
                .hasEntrySatisfying("two", x -> assertThat(x.getValue()).isEqualTo(1))
                .hasEntrySatisfying("three", x -> assertThat(x.getValue()).isEqualTo(0));
    }

    @Test
    public void testGetProctorResult_forcedGroupsOptions_shouldForceFallback() {
        final SplitSpecificationTestGroupsContext context =
                SplitSpecificationTestGroupsContext.newBuilder().build();
        final Identifiers identifiers =
                new Identifiers(TestType.USER, "foo"); // resolves two1 and three1
        final ProctorResult result =
                context.getProctorResult(
                        manager,
                        identifiers,
                        ForceGroupsOptions.builder()
                                .setDefaultMode(ForceGroupsDefaultMode.FALLBACK)
                                .putForceGroup("three", 0)
                                .build());
        assertThat(result.getBuckets())
                .containsOnlyKeys("three")
                .hasEntrySatisfying("three", x -> assertThat(x.getValue()).isEqualTo(0));
    }

    @Test
    public void testGetProctorResult_forcedGroupsOptions_shouldForceMinLive() {
        final SplitSpecificationTestGroupsContext context =
                SplitSpecificationTestGroupsContext.newBuilder().build();
        final Identifiers identifiers =
                new Identifiers(TestType.USER, "foo"); // resolves two1 and three1
        final ProctorResult result =
                context.getProctorResult(
                        manager,
                        identifiers,
                        ForceGroupsOptions.builder()
                                .setDefaultMode(ForceGroupsDefaultMode.MIN_LIVE)
                                .putForceGroup("three", 0)
                                .build());
        assertThat(result.getBuckets())
                .containsOnlyKeys("two", "three")
                .hasEntrySatisfying("two", x -> assertThat(x.getValue()).isEqualTo(0))
                .hasEntrySatisfying("three", x -> assertThat(x.getValue()).isEqualTo(0));
    }

    @Test
    public void testGetProctorResult_prforceGroupsUrlParam_shouldForceFallback() {
        final SplitSpecificationTestGroupsContext context =
                SplitSpecificationTestGroupsContext.newBuilder().build();
        final Identifiers identifiers =
                new Identifiers(TestType.USER, "foo"); // resolves two1 and three1
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("prforceGroups", "default_to_fallback,three0");
        final ProctorResult result =
                context.getProctorResult(
                        manager, request, new MockHttpServletResponse(), identifiers, true);
        assertThat(result.getBuckets())
                .containsOnlyKeys("three")
                .hasEntrySatisfying("three", x -> assertThat(x.getValue()).isEqualTo(0));
    }

    @Test
    public void testGetProctorResult_prforceGroupsUrlParam_shouldForceMinLive() {
        final SplitSpecificationTestGroupsContext context =
                SplitSpecificationTestGroupsContext.newBuilder().build();
        final Identifiers identifiers =
                new Identifiers(TestType.USER, "foo"); // resolves two1 and three1
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("prforceGroups", "default_to_min_live,three0");
        final ProctorResult result =
                context.getProctorResult(
                        manager, request, new MockHttpServletResponse(), identifiers, true);
        assertThat(result.getBuckets())
                .containsOnlyKeys("two", "three")
                .hasEntrySatisfying("two", x -> assertThat(x.getValue()).isEqualTo(0)) // min live
                .hasEntrySatisfying("three", x -> assertThat(x.getValue()).isEqualTo(0));
    }

    @Test
    public void testGetProctorResult_prforceGroupsUrlParam_shouldNotForceWhenNotAllowed() {
        final SplitSpecificationTestGroupsContext context =
                SplitSpecificationTestGroupsContext.newBuilder().build();
        final Identifiers identifiers =
                new Identifiers(TestType.USER, "foo"); // resolves two1 and three1
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("prforceGroups", "default_to_fallback,three0");
        final ProctorResult result =
                context.getProctorResult(
                        manager, request, new MockHttpServletResponse(), identifiers, false);
        assertThat(result.getBuckets())
                .containsOnlyKeys("two", "three")
                .hasEntrySatisfying("two", x -> assertThat(x.getValue()).isEqualTo(1))
                .hasEntrySatisfying("three", x -> assertThat(x.getValue()).isEqualTo(1));
    }
}
