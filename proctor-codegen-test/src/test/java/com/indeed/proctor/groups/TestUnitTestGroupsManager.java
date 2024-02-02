package com.indeed.proctor.groups;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.indeed.proctor.SampleOuterClass.Account;
import com.indeed.proctor.codegen.test.groups.UnitTestGroups;
import com.indeed.proctor.codegen.test.groups.UnitTestGroups.Payloaded;
import com.indeed.proctor.codegen.test.groups.UnitTestGroupsContext;
import com.indeed.proctor.codegen.test.groups.UnitTestGroupsManager;
import com.indeed.proctor.codegen.test.groups.UnitTestGroupsPayload;
import com.indeed.proctor.common.ForceGroupsOptions;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.common.ProvidedContext;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.consumer.logging.TestMarkingObserver;
import com.indeed.util.varexport.VarExporter;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static com.indeed.proctor.groups.UtilMethods.calcBuckets;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** @author parker */
public class TestUnitTestGroupsManager {
    private static final Logger LOGGER = LogManager.getLogger(TestUnitTestGroupsManager.class);
    private static final String SPECIFICATION_RESOURCE = "UnitTestGroups.json";
    private static final String SPECIFICATION_MATRIX = "unittest.proctor-matrix.json";
    public static final String SAMPLE_ID = "16s2o7s01001d9vj";

    private UnitTestGroupsManager manager;

    @BeforeClass
    public static void quietLogs() {
        LogManager.getLogger(VarExporter.class).atLevel(Level.FATAL);
    }

    @Before()
    public void setUp() throws Exception {
        manager =
                new UnitTestGroupsManager(
                        () -> UtilMethods.getProctor(SPECIFICATION_MATRIX, SPECIFICATION_RESOURCE));
    }

    @Test
    public void testMultipleTypes() {
        // some unstructured combination of tests
        final UnitTestGroupsContext testContext =
                UnitTestGroupsContext.newBuilder()
                        .setLoggedIn(true)
                        .setCountry("FR")
                        .setAccount(new Account(10))
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
                                    .put("kluj", "kloo2")
                                    .put("map_payload", "inactive-1")
                                    .put("no_buckets_specified", "test1")
                                    .put("oop_poop", "test1")
                                    .put("payloaded", "inactive-1")
                                    .put("payloaded_verified", "inactive-1")
                                    .put("pimple", "control0")
                                    .build());
        }
        {
            final Identifiers identifiers =
                    new Identifiers(
                            ImmutableMap.<TestType, String>builder()
                                    .put(TestType.EMAIL_ADDRESS, SPECIFICATION_MATRIX)
                                    .put(TestType.AUTHENTICATED_USER, SPECIFICATION_MATRIX)
                                    .put(TestType.PAGE, SPECIFICATION_MATRIX)
                                    .build(),
                            true);

            final ProctorResult result = testContext.getProctorResult(manager, identifiers);
            assertEquals(0, result.getBuckets().get("pimple").getValue());
            assertNotNull(result.getBuckets().get("bubble"));
            assertEquals(2, result.getBuckets().get("dubblez").getValue());
        }
    }

    // this is an integration test with Proctor/RandomTestChooser
    @Test
    public void testRandom() {
        // assumes each of 4 bucket gets ~25% of requests in matrix
        final UnitTestGroupsContext testContext =
                UnitTestGroupsContext.newBuilder()
                        .setLoggedIn(true)
                        .setCountry("FR")
                        .setAccount(new Account(10))
                        .build();
        final Identifiers identifiers = new Identifiers(Collections.emptyMap(), true);

        final int[] valuesFound = new int[4];
        for (int i = 0; i < 2000; i++) {
            final ProctorResult result = testContext.getProctorResult(manager, identifiers);
            valuesFound[result.getBuckets().get("bubble").getValue()]++;
        }
        for (final int value : valuesFound) {
            assertThat(value).isBetween(425, 575);
        }
    }

    @Test
    public void testUserBuckets() {
        {
            final UnitTestGroupsContext testContext =
                    UnitTestGroupsContext.newBuilder()
                            .setLoggedIn(true)
                            .setCountry("FR")
                            .setAccount(new Account(10))
                            .build();
            final Identifiers identifiers = new Identifiers(TestType.ANONYMOUS_USER, SAMPLE_ID);
            final ProctorResult result = testContext.getProctorResult(manager, identifiers);
            assertThat(calcBuckets(result))
                    .isEqualTo(
                            ImmutableMap.builder()
                                    .put("kluj", "test1")
                                    .put("map_payload", "inactive-1")
                                    .put("no_buckets_specified", "test1")
                                    .put("oop_poop", "control0")
                                    .put("payloaded", "inactive-1")
                                    .put("payloaded_verified", "inactive-1")
                                    .build());
            // Check and make sure UnitTestGroups respects these groups and works as expected.
            final UnitTestGroups grps = new UnitTestGroups(result);

            assertNotNull(grps.getPimple());
            assertEquals(-99, grps.getPimpleValue(-99));
            assertEquals(UnitTestGroups.Kluj.TEST, grps.getKluj());
            assertEquals(1, grps.getKlujValue(-99));
            assertEquals(UnitTestGroups.Oop_poop.CONTROL, grps.getOop_poop());
            assertEquals(0, grps.getOop_poopValue(-99));

            // Check the boolean conditions for one of the tests
            assertTrue(grps.isPimpleInactive());
            assertFalse(grps.isPimpleControl());
            assertFalse(grps.isPimpleTest());

            assertFalse(grps.isKlujControl());
            assertTrue(grps.isKlujTest());
            assertFalse(grps.isKlujKloo());
            assertFalse(grps.isKlujLoooj());
            assertEquals("", grps.toString());
        }
    }

    @Test
    public void testUsageObserver() {
        final UnitTestGroupsContext testContext =
                UnitTestGroupsContext.newBuilder()
                        .setLoggedIn(true)
                        .setCountry("FR")
                        .setAccount(new Account(10))
                        .build();
        final Identifiers identifiers = new Identifiers(TestType.ANONYMOUS_USER, SAMPLE_ID);
        final ProctorResult result = testContext.getProctorResult(manager, identifiers);
        final TestMarkingObserver testUsageObserver = new TestMarkingObserver(result);
        final UnitTestGroups grps = new UnitTestGroups(result, testUsageObserver);
        grps.getKlujValue(); // test used given SAMPLE_ID hash
        assertEquals(1, testUsageObserver.asProctorResult().getBuckets().size());
        assertNotNull(testUsageObserver.asProctorResult().getBuckets().get("kluj"));
    }

    @Test
    public void testPageBuckets() {
        {
            final UnitTestGroupsContext testContext =
                    UnitTestGroupsContext.newBuilder()
                            .setLoggedIn(true)
                            .setCountry("FR")
                            .setAccount(new Account(10))
                            .build();
            // LoggedIn + MX maps to [0, 0.5, 0.5] ranges
            final Identifiers identifiers = new Identifiers(TestType.PAGE, SAMPLE_ID);
            final ProctorResult result = testContext.getProctorResult(manager, identifiers);
            assertThat(calcBuckets(result))
                    .isEqualTo(ImmutableMap.builder().put("pimple", "test1").build());
            // Check and make sure UnitTestGroups respects these groups and works as expected.
            final UnitTestGroups grps = new UnitTestGroups(result);
            assertEquals(UnitTestGroups.Pimple.TEST, grps.getPimple());
            assertEquals(1, grps.getPimpleValue(-99));
            assertNotNull(grps.getKluj());
            assertEquals(-99, grps.getKlujValue(-99));
            assertNotNull(grps.getOop_poop());
            assertEquals(-99, grps.getOop_poopValue(-99));

            // Check the boolean conditions for one of the tests
            assertFalse(grps.isPimpleInactive());
            assertFalse(grps.isPimpleControl());
            assertTrue(grps.isPimpleTest());
            assertEquals("", grps.toString());
        }
        {
            final UnitTestGroupsContext testContext =
                    UnitTestGroupsContext.newBuilder()
                            .setLoggedIn(true)
                            .setCountry("US")
                            .setAccount(new Account(10))
                            .build();
            // LoggedIn + US maps to [1, 0, 0] range
            final Identifiers identifiers = new Identifiers(TestType.PAGE, SAMPLE_ID);
            final ProctorResult result = testContext.getProctorResult(manager, identifiers);
            assertThat(calcBuckets(result))
                    .isEqualTo(ImmutableMap.builder().put("pimple", "inactive-1").build());
            // Check and make sure UnitTestGroups respects these groups and works as expected.
            final UnitTestGroups grps = new UnitTestGroups(result);
            assertEquals(UnitTestGroups.Pimple.INACTIVE, grps.getPimple());
            assertEquals(-1, grps.getPimpleValue(-99));
            assertNotNull(grps.getKluj());
            assertEquals(-99, grps.getKlujValue(-99));
            assertNotNull(grps.getOop_poop());
            assertEquals(-99, grps.getOop_poopValue(-99));

            // Check the boolean conditions for one of the tests
            assertTrue(grps.isPimpleInactive());
            assertFalse(grps.isPimpleControl());
            assertFalse(grps.isPimpleTest());
            assertEquals("", grps.toString());
        }
        {
            final UnitTestGroupsContext testContext =
                    UnitTestGroupsContext.newBuilder()
                            .setLoggedIn(false)
                            .setCountry("FR")
                            .setAccount(new Account(10))
                            .build();
            // LoggedIn=false + MX maps to [1, 0, 0] range
            final Identifiers identifiers = new Identifiers(TestType.PAGE, SAMPLE_ID);
            final ProctorResult result = testContext.getProctorResult(manager, identifiers);
            assertThat(calcBuckets(result)).isEmpty();
            // Check and make sure UnitTestGroups respects these groups and works as expected.
            final UnitTestGroups grps = new UnitTestGroups(result);
            assertNotNull(grps.getPimple());
            assertEquals(-99, grps.getPimpleValue(-99));
            assertNotNull(grps.getKluj());
            assertEquals(-99, grps.getKlujValue(-99));
            assertNotNull(grps.getOop_poop());
            assertEquals(-99, grps.getOop_poopValue(-99));

            // Check the boolean conditions for one of the tests
            assertTrue(grps.isPimpleInactive());
            assertFalse(grps.isPimpleControl());
            assertFalse(grps.isPimpleTest());
            assertEquals("", grps.toString());
        }
    }

    @Test
    public void testCompanyBuckets() {
        final UnitTestGroupsContext testContext =
                UnitTestGroupsContext.newBuilder()
                        .setLoggedIn(true)
                        .setCountry("US")
                        .setAccount(new Account(10))
                        .build();
        final Identifiers identifiers = new Identifiers(TestType.COMPANY, SAMPLE_ID);
        final ProctorResult result = testContext.getProctorResult(manager, identifiers);
        assertThat(calcBuckets(result)).isEmpty();
        // Check and make sure UnitTestGroups respects these groups and works as expected.
        final UnitTestGroups grps = new UnitTestGroups(result);
        assertNotNull(grps.getPimple());
        assertEquals(-99, grps.getPimpleValue(-99));
        assertNotNull(grps.getKluj());
        assertEquals(-99, grps.getKlujValue(-99));
        assertNotNull(grps.getOop_poop());
        assertEquals(-99, grps.getOop_poopValue(-99));

        // Check the boolean conditions for one of the tests
        assertTrue(grps.isPimpleInactive());
        assertFalse(grps.isPimpleControl());
        assertFalse(grps.isPimpleTest());
        assertEquals("", grps.toString());
    }

    @Test
    public void testPayloads() {
        final UnitTestGroupsContext testContext =
                UnitTestGroupsContext.newBuilder()
                        .setLoggedIn(true)
                        .setCountry("US")
                        .setAccount(new Account(10))
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
                                .put("kluj", "kloo2")
                                .put("map_payload", "inactive-1")
                                .put("no_buckets_specified", "test1")
                                .put("oop_poop", "test1")
                                .put("payloaded", "inactive-1")
                                .put("payloaded_verified", "inactive-1")
                                .build());
        // Check and make sure UnitTestGroups respects these groups and works as expected.
        final UnitTestGroups grps = new UnitTestGroups(result);
        assertNotNull(grps.getPayloaded_verified());
        assertEquals(-1, grps.getPayloaded_verifiedValue(-99));
        // The "Inactive" condition should be true.
        assertTrue(grps.isPayloaded_verifiedInactive());
        assertFalse(grps.isPayloaded_verifiedControl());
        assertFalse(grps.isPayloaded_verifiedTest());
        // Get the current test payload
        assertEquals(0, grps.getPayloaded_verifiedPayload(), 0.001);
        // Test per-bucket payload fetch
        assertEquals(
                0,
                grps.getPayloaded_verifiedPayloadForBucket(
                        UnitTestGroups.Payloaded_verified.INACTIVE),
                0.001);
        assertEquals(
                5,
                grps.getPayloaded_verifiedPayloadForBucket(
                        UnitTestGroups.Payloaded_verified.CONTROL),
                0.001);
        assertEquals(
                50,
                grps.getPayloaded_verifiedPayloadForBucket(UnitTestGroups.Payloaded_verified.TEST),
                0.001);

        assertEquals("", grps.toString());

        assertNotNull(grps.getPayloaded_excluded());
        assertEquals(
                "Expected inactive even though there are no explicit assignments made to that group",
                Payloaded.INACTIVE,
                grps.getPayloaded());
        assertArrayEquals(
                "Expected inactive payload to be used, not 'empty' default",
                new String[] {"preexisting"},
                grps.getPayloaded_excludedPayload());
    }

    @Test
    public void testTestDescriptions() {
        final UnitTestGroupsContext testContext =
                UnitTestGroupsContext.newBuilder()
                        .setLoggedIn(true)
                        .setCountry("FR")
                        .setAccount(new Account(10))
                        .build();
        final Identifiers identifiers = new Identifiers(TestType.USER, SAMPLE_ID);
        final ProctorResult result = testContext.getProctorResult(manager, identifiers);
        assertThat(calcBuckets(result))
                .isEqualTo(
                        ImmutableMap.builder()
                                .put("kluj", "test1")
                                .put("map_payload", "inactive-1")
                                .put("no_buckets_specified", "test1")
                                .put("oop_poop", "control0")
                                .put("payloaded", "inactive-1")
                                .put("payloaded_verified", "inactive-1")
                                .build());
        // Check and make sure UnitTestGroups respects these groups and works as expected.
        final UnitTestGroups grps = new UnitTestGroups(result);
        // make sure getDescription method exists and returns the correct description
        assertEquals("2nd test", grps.getKlujDescription());
    }

    @Test
    public void testTestDescriptions_checkEscaping() {
        final UnitTestGroupsContext testContext =
                UnitTestGroupsContext.newBuilder()
                        .setLoggedIn(true)
                        .setCountry("FR")
                        .setAccount(new Account(10))
                        .build();
        final Identifiers identifiers = new Identifiers(TestType.USER, SAMPLE_ID);
        final ProctorResult result = testContext.getProctorResult(manager, identifiers);
        assertThat(calcBuckets(result))
                .isEqualTo(
                        ImmutableMap.builder()
                                .put("kluj", "test1")
                                .put("map_payload", "inactive-1")
                                .put("no_buckets_specified", "test1")
                                .put("oop_poop", "control0")
                                .put("payloaded", "inactive-1")
                                .put("payloaded_verified", "inactive-1")
                                .build());
        // Check and make sure UnitTestGroups respects these groups and works as expected.
        final UnitTestGroups grps = new UnitTestGroups(result);
        // make sure getDescription method exists and returns the correct description with escaping
        assertEquals("3rd \n\t\"test", grps.getBubbleDescription());
    }

    @Test
    public void testMapPayloadReturns() {
        final UnitTestGroupsContext testContext =
                UnitTestGroupsContext.newBuilder()
                        .setLoggedIn(true)
                        .setCountry("FR")
                        .setAccount(new Account(10))
                        .build();
        final Identifiers identifiers = new Identifiers(TestType.USER, SAMPLE_ID);
        final ProctorResult result = testContext.getProctorResult(manager, identifiers);

        final UnitTestGroups grps = new UnitTestGroups(result);
        assertEquals("lol", grps.getMap_payloadPayload().getAstring());
        assertEquals(grps.getMap_payloadPayload().getAdouble(), (Double) 2.1);
        assertArrayEquals(grps.getMap_payloadPayload().getAnarray(), new Long[] {1L, 2L, 3L});
        assertArrayEquals(
                grps.getMap_payloadPayload().getAstringarr(), new String[] {"one", "two", "three"});
        assertArrayEquals(grps.getMap_payloadPayload().getAdarray(), new Double[] {1.1, 2.1, 3.1});

        final UnitTestGroupsPayload.Map_payload unitTestGroupsPayloadTest =
                grps.getMap_payloadPayloadForBucket(UnitTestGroups.Map_payload.TEST);
        assertEquals("l", unitTestGroupsPayloadTest.getAstring());
        assertEquals(unitTestGroupsPayloadTest.getAdouble(), (Double) 1.1);
        assertArrayEquals(unitTestGroupsPayloadTest.getAnarray(), new Long[] {1L, 2L, 3L});
        assertArrayEquals(
                unitTestGroupsPayloadTest.getAstringarr(), new String[] {"one", "two", "three"});
        assertArrayEquals(unitTestGroupsPayloadTest.getAdarray(), new Double[] {1.1, 2.1, 3.1});

        final UnitTestGroupsPayload.Map_payload unitTestGroupsPayloadControl =
                grps.getMap_payloadPayloadForBucket(UnitTestGroups.Map_payload.CONTROL);
        assertEquals("str2", unitTestGroupsPayloadControl.getAstring());
        assertEquals(unitTestGroupsPayloadControl.getAdouble(), (Double) 3.1);
        assertArrayEquals(unitTestGroupsPayloadControl.getAnarray(), new Long[] {1L, 2L, 3L});
        assertArrayEquals(
                unitTestGroupsPayloadControl.getAstringarr(), new String[] {"one", "two", "three"});
        assertArrayEquals(unitTestGroupsPayloadControl.getAdarray(), new Double[] {1.1, 2.1, 3.1});
    }

    @Test
    public void testMapJsonReturns() {
        final UnitTestGroupsContext testContext =
                UnitTestGroupsContext.newBuilder()
                        .setLoggedIn(true)
                        .setCountry("FR")
                        .setAccount(new Account(10))
                        .build();
        final Identifiers identifiers = new Identifiers(TestType.USER, SAMPLE_ID);
        final ProctorResult result = testContext.getProctorResult(manager, identifiers);

        final UnitTestGroups grps = new UnitTestGroups(result);
        assertEquals(
                "{\"foo\":{\"bar\":\"baz\",\"abc\":123}}",
                grps.getJson_payload_testPayload().toString());
        assertEquals(
                "{\"foo\":{\"bar\":\"baz\",\"abc\":456}}",
                grps.getJson_payload_testPayloadForBucket(UnitTestGroups.Json_payload_test.CONTROL)
                        .toString());
        assertEquals(
                "{\"foo\":{\"bar\":\"baz\",\"abc\":789}}",
                grps.getJson_payload_testPayloadForBucket(UnitTestGroups.Json_payload_test.TEST)
                        .toString());

        assertEquals(
                "{\"foo\":{\"bar\":\"baz\",\"abc\":123}}",
                grps.getJson_payload_testPayload(JsonNode.class).toString());
        assertNull(grps.getJson_payload_testPayload(Double.class));
        assertNull(grps.getJson_payload_testPayload(Long.class));
        assertNull(grps.getJson_payload_testPayload(String.class));
        assertNull(grps.getJson_payload_testPayload(Double[].class));
        assertNull(grps.getJson_payload_testPayload(Long[].class));
        assertNull(grps.getJson_payload_testPayload(String[].class));
        assertEquals(
                ImmutableMap.of("foo", ImmutableMap.of("bar", "baz", "abc", 123)),
                grps.getJson_payload_testPayload(Map.class));
    }

    @Test
    public void testGetMapPayloadForControlGroups() {
        final UnitTestGroupsContext testContext =
                UnitTestGroupsContext.newBuilder()
                        .setLoggedIn(true)
                        .setCountry("FR")
                        .setAccount(new Account(10))
                        .build();

        final String identifier =
                findIdentifier(
                        TestType.USER,
                        testContext,
                        UnitTestGroups.Test.MAP_PAYLOAD,
                        UnitTestGroups.Map_payload.CONTROL.getValue(),
                        1000);
        final Identifiers identifiers = new Identifiers(TestType.USER, identifier);
        final ProctorResult result = testContext.getProctorResult(manager, identifiers);

        final UnitTestGroups grps = new UnitTestGroups(result);

        assertEquals(
                "`control` group should be chosen by the identifier",
                UnitTestGroups.Map_payload.CONTROL.getValue(),
                grps.getMap_payloadValue());

        // GetPayload method should return a payload of `control` group
        final UnitTestGroupsPayload.Map_payload payload = grps.getMap_payloadPayload();
        assertEquals("str2", payload.getAstring());
        assertEquals(payload.getAdouble(), (Double) 3.1);
        assertArrayEquals(payload.getAnarray(), new Long[] {1L, 2L, 3L});
        assertArrayEquals(payload.getAstringarr(), new String[] {"one", "two", "three"});
        assertArrayEquals(payload.getAdarray(), new Double[] {1.1, 2.1, 3.1});
    }

    @Test
    public void testNestedClasses() throws Exception {
        final Map<String, String> declaredContext =
                UtilMethods.getProctorSpecification(SPECIFICATION_RESOURCE).getProvidedContext();
        final Map<String, String> innerClassTypes =
                Maps.filterValues(
                        declaredContext,
                        new Predicate<String>() {
                            @Override
                            public boolean apply(final String subfrom) {
                                return subfrom.contains("$");
                            }
                        });
        assertFalse(
                "Sample groups need to contain at least one inner class type",
                innerClassTypes.isEmpty());

        final ProvidedContext providedContext =
                ProctorUtils.convertContextToTestableMap(declaredContext);
        assertFalse(
                "Expected the provided context to be populated since no class-not-found-error should have been thrown",
                providedContext.getContext().isEmpty());
    }

    @Test
    public void testMapPayloadWithIntegerFormat() throws IOException {
        final UnitTestGroupsContext testContext =
                UnitTestGroupsContext.newBuilder().setAccount(new Account(123)).build();
        final String identifier =
                findIdentifier(
                        TestType.USER, testContext, UnitTestGroups.Test.MAP_PAYLOAD_INT, 1, 1000);
        final Identifiers identifiers = new Identifiers(TestType.USER, identifier);
        final ProctorResult result = testContext.getProctorResult(manager, identifiers);

        final UnitTestGroups grps = new UnitTestGroups(result);
        assertEquals(1, grps.getMap_payload_intValue());
        assertEquals(2000000000000L, grps.getMap_payload_intPayload().getAlong().longValue());
        assertEquals(2, grps.getMap_payload_intPayload().getAdouble().doubleValue(), 1e-8);
    }

    @Test
    public void testPayloadOnlyMapType() {
        final UnitTestGroupsContext testContext =
                UnitTestGroupsContext.newBuilder().setAccount(new Account(123)).build();
        final String identifier =
                findIdentifier(
                        TestType.USER,
                        testContext,
                        UnitTestGroups.Test.PAYLOADONLY_MAPTYPE,
                        1,
                        1000);
        final Identifiers identifiers = new Identifiers(TestType.USER, identifier);
        final ProctorResult result = testContext.getProctorResult(manager, identifiers);

        final UnitTestGroups grps = new UnitTestGroups(result);
        assertEquals(1, grps.getPayloadonly_maptypeValue());
        assertEquals(1.0, grps.getPayloadonly_maptypePayload().getAdouble(), 1e-6);
        assertEquals("test", grps.getPayloadonly_maptypePayload().getAstring());
    }

    @Test
    public void testPayloadOnlyDoubleType() {
        final UnitTestGroupsContext testContext =
                UnitTestGroupsContext.newBuilder().setAccount(new Account(123)).build();
        final String identifier =
                findIdentifier(
                        TestType.USER,
                        testContext,
                        UnitTestGroups.Test.PAYLOADONLY_DOUBLETYPE,
                        0,
                        1000);
        final Identifiers identifiers = new Identifiers(TestType.USER, identifier);
        final ProctorResult result = testContext.getProctorResult(manager, identifiers);

        final UnitTestGroups grps = new UnitTestGroups(result);
        assertEquals(0, grps.getPayloadonly_doubletypeValue());
        assertEquals(0.0, grps.getPayloadonly_doubletypePayload(), 1e-6);
    }

    @Test
    public void testPayloadOfEmptyTestResult() {
        assertNull(UnitTestGroups.EMPTY.getPayloaded_verifiedPayload());
        assertNull(UnitTestGroups.EMPTY.getMap_payloadPayload());
        assertNull(UnitTestGroups.EMPTY.getPayloadonly_doubletypePayload());
        assertNull(UnitTestGroups.EMPTY.getPayloadonly_maptypePayload());
    }

    @Test
    public void testPayloadReturningFallbackPayload() {
        final UnitTestGroupsContext testContext =
                UnitTestGroupsContext.newBuilder()
                        .setLoggedIn(true)
                        .setCountry("US")
                        .setAccount(new Account(0))
                        .build();
        final Identifiers identifiers =
                new Identifiers(
                        ImmutableMap.<TestType, String>builder()
                                .put(TestType.ANONYMOUS_USER, "dummy_token")
                                .build());
        final ProctorResult result = testContext.getProctorResult(manager, identifiers);
        assertThat(result.getTestDefinitions()).containsKey("payloadonly_doubletype");
        assertThat(result.getBuckets()).doesNotContainKey("payloadonly_doubletype");

        final UnitTestGroups grps = new UnitTestGroups(result);
        assertThat(grps.getPayloadonly_doubletypeValue()).isEqualTo(-1);
        assertThat(grps.getPayloadonly_doubletypePayload()).isNotNull().isEqualTo(-1.0);
    }

    private String findIdentifier(
            final TestType testType,
            final UnitTestGroupsContext context,
            final UnitTestGroups.Test test,
            final int targetValue,
            final int maxIteration) {
        for (int i = 0; i < maxIteration; i++) {
            final String identifier = String.valueOf(i);
            final Identifiers identifiers = Identifiers.of(testType, identifier);
            final ProctorResult result = context.getProctorResult(manager, identifiers);
            final TestBucket bucket = result.getBuckets().get(test.getName());
            if (bucket == null) {
                throw new RuntimeException("Failed to load a test " + test.getName());
            }
            if (bucket.getValue() == targetValue) {
                return identifier;
            }
        }
        throw new RuntimeException(
                "identifier not found for target bucket within " + maxIteration + " iterations");
    }

    @Test
    public void testProctorResultWithSingleTestNameFilter() {
        final UnitTestGroupsContext testContext =
                UnitTestGroupsContext.newBuilder()
                        .setLoggedIn(true)
                        .setCountry("FR")
                        .setAccount(new Account(10))
                        .build();
        final Identifiers identifiers = new Identifiers(TestType.USER, SAMPLE_ID);
        final ProctorResult result =
                testContext.getProctorResult(
                        manager,
                        new MockHttpServletRequest(),
                        new MockHttpServletResponse(),
                        identifiers,
                        false,
                        ImmutableList.of("kluj"));

        assertThat(calcBuckets(result))
                .isEqualTo(ImmutableMap.builder().put("kluj", "test1").build());
    }

    @Test
    public void testProctorResultWithTestNameFilter() {
        final UnitTestGroupsContext testContext =
                UnitTestGroupsContext.newBuilder()
                        .setLoggedIn(true)
                        .setCountry("FR")
                        .setAccount(new Account(10))
                        .build();
        final Identifiers identifiers = new Identifiers(TestType.USER, SAMPLE_ID);
        final Collection<String> testNameFilter =
                ImmutableList.of("kluj", "map_payload", "no_buckets_specified", "payloaded");
        final ProctorResult result =
                testContext.getProctorResult(
                        manager,
                        new MockHttpServletRequest(),
                        new MockHttpServletResponse(),
                        identifiers,
                        false,
                        testNameFilter);

        // Only calc bucket allocation for filtered test names
        assertThat(result.getBuckets())
                .containsOnlyKeys("kluj", "map_payload", "no_buckets_specified", "payloaded");

        assertThat(result.getAllocations())
                .containsOnlyKeys("kluj", "map_payload", "no_buckets_specified", "payloaded");

        final int expectedNumberOfTests = 15;

        assertThat(result.getTestDefinitions().size()).isEqualTo(expectedNumberOfTests);
    }

    @Test
    public void testProctorResultWithTestNameFilterWithoutHttp() {
        final UnitTestGroupsContext testContext =
                UnitTestGroupsContext.newBuilder()
                        .setLoggedIn(true)
                        .setCountry("FR")
                        .setAccount(new Account(10))
                        .build();
        final Identifiers identifiers = new Identifiers(TestType.USER, SAMPLE_ID);
        final Collection<String> testNameFilter =
                ImmutableList.of("kluj", "map_payload", "no_buckets_specified", "payloaded");
        final ProctorResult result =
                testContext.getProctorResult(
                        manager,
                        identifiers,
                        new ForceGroupsOptions.Builder().build(),
                        testNameFilter);

        // Only calc bucket allocation for filtered test names
        assertThat(result.getBuckets())
                .containsOnlyKeys("kluj", "map_payload", "no_buckets_specified", "payloaded");

        assertThat(result.getAllocations())
                .containsOnlyKeys("kluj", "map_payload", "no_buckets_specified", "payloaded");

        final int expectedNumberOfTests = 15;

        assertThat(result.getTestDefinitions().size()).isEqualTo(expectedNumberOfTests);
    }
}
