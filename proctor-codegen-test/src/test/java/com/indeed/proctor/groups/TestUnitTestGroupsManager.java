package com.indeed.proctor.groups;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;

import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;
import com.indeed.proctor.SampleOuterClass.Account;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.Proctor;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.common.ProvidedContext;
import com.indeed.proctor.common.StringProctorLoader;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestType;
import com.indeed.proctor.groups.UnitTestGroups.Payloaded;
import com.indeed.util.varexport.VarExporter;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author parker
 */
public class TestUnitTestGroupsManager {
    private static final Logger LOGGER = Logger.getLogger(TestUnitTestGroupsManager.class);
    private static final String SPECIFICATION_RESOURCE = "UnitTestGroups.json";
    private static final String SPECIFICATION_MATRIX = "unittest.proctor-matrix.json";

    private UnitTestGroupsManager manager;

    public TestUnitTestGroupsManager() {
    }

    @BeforeClass
    public static void quietLogs() {
        Logger.getLogger(VarExporter.class).setLevel(Level.FATAL);
    }

    @Before()
    public void setUp() throws Exception {
        setUp(getProctor());
    }

    private void setUp(final Proctor proctor) {
        manager = new UnitTestGroupsManager(new Supplier<Proctor>() {
            @Override
            public Proctor get() {
                return proctor;
            }
        });
    }

    private Proctor getProctor() throws IOException {
        // just read from the resource .json file at the moment.ProctorUtils.java

        final Reader matrixResource = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(SPECIFICATION_MATRIX)));
        final StringWriter matrixString = new StringWriter();
        CharStreams.copy(matrixResource, matrixString);


        final ProctorSpecification specification = getProctorSpecification();
        final StringProctorLoader loader = new StringProctorLoader(specification, SPECIFICATION_MATRIX, matrixString.toString());

        assertTrue("StringProctorLoader should load", loader.load());
        return loader.get();
    }

    private ProctorSpecification getProctorSpecification() throws IOException {
        final InputStream specicationStream = getClass().getResourceAsStream(SPECIFICATION_RESOURCE);
        try {
            return ProctorUtils.readSpecification(specicationStream);
        } finally {
            specicationStream.close();
        }
    }

    @Test
    public void testMultipleTypes() {
        final UnitTestGroupsContext testContext = UnitTestGroupsContext.newBuilder()
                .setLoggedIn(true)
                .setCountry("FR")
                .setAccount(new Account(10))
                .build();
        {
            final Identifiers identifiers = new Identifiers(ImmutableMap.<TestType, String>builder()
                                                            .put(TestType.ANONYMOUS_USER, SPECIFICATION_MATRIX)
                                                            .put(TestType.AUTHENTICATED_USER, SPECIFICATION_MATRIX)
                                                            .put(TestType.PAGE, SPECIFICATION_MATRIX)
                                                            .build());

            final ProctorResult result = testContext.getProctorResult(manager, identifiers);
            assertEquals("kluj:kloo2,map_payload:inactive-1,oop_poop:test1,payloaded:inactive-1,payloaded_verified:inactive-1,pimple:control0", calcBuckets(result));
        }
        {
            final ImmutableMap<TestType, String> idMap = ImmutableMap.<TestType, String>builder()
                                                                .put(TestType.EMAIL_ADDRESS, SPECIFICATION_MATRIX)
                                                                .put(TestType.AUTHENTICATED_USER, SPECIFICATION_MATRIX)
                                                                .put(TestType.PAGE, SPECIFICATION_MATRIX)
                                                                .build();
            final Identifiers identifiers = new Identifiers(idMap, true);

            final ProctorResult result = testContext.getProctorResult(manager, identifiers);
            assertEquals(result.getBuckets().get("pimple").getValue(), 0);
            assertNotNull(result.getBuckets().get("bubble").getValue());
            assertEquals(result.getBuckets().get("dubblez").getValue(), 2);
        }
    }

    @Test
    public void testRandom() {
        final UnitTestGroupsContext testContext = UnitTestGroupsContext.newBuilder()
                .setLoggedIn(true)
                .setCountry("FR")
                .setAccount(new Account(10))
                .build();
        final Identifiers identifiers = new Identifiers(Collections.<TestType, String>emptyMap(), true);

        final int[] valuesFound = new int[4];
        for (int i = 0; i < 2000; i++) {
            final ProctorResult result = testContext.getProctorResult(manager, identifiers);
            valuesFound[result.getBuckets().get("bubble").getValue()]++;
        }
        for (int i = 0; i < valuesFound.length; i++) {
            assertTrue(valuesFound[i] >= 425);
            assertTrue(valuesFound[i] <= 575);
        }
    }

    @Test
    public void testUserBuckets() {
        {
            final UnitTestGroupsContext testContext = UnitTestGroupsContext.newBuilder()
                    .setLoggedIn(true)
                    .setCountry("FR")
                    .setAccount(new Account(10))
                    .build();
            final Identifiers identifiers = new Identifiers(TestType.ANONYMOUS_USER, "16s2o7s01001d9vj");
            final ProctorResult result = testContext.getProctorResult(manager, identifiers);
            assertEquals("kluj:test1,map_payload:inactive-1,oop_poop:control0,payloaded:inactive-1,payloaded_verified:inactive-1", calcBuckets(result));
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
            assertEquals("kluj1,oop_poop0", grps.toString());
        }
    }

    @Test
    public void testPageBuckets() {
        final String uidString = "16s2o7s01001d9vj";
        {
            final UnitTestGroupsContext testContext = UnitTestGroupsContext.newBuilder()
                    .setLoggedIn(true)
                    .setCountry("FR")
                    .setAccount(new Account(10))
                    .build();
            // LoggedIn + MX maps to [0, 0.5, 0.5] ranges
            final Identifiers identifiers = new Identifiers(TestType.PAGE, uidString);
            final ProctorResult result = testContext.getProctorResult(manager, identifiers);
            assertEquals("pimple:test1", calcBuckets(result));
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
            assertEquals("pimple1", grps.toString());
        }
        {
            final UnitTestGroupsContext testContext = UnitTestGroupsContext.newBuilder()
                    .setLoggedIn(true)
                    .setCountry("US")
                    .setAccount(new Account(10))
                    .build();
            // LoggedIn + US maps to [1, 0, 0] range
            final Identifiers identifiers = new Identifiers(TestType.PAGE, uidString);
            final ProctorResult result = testContext.getProctorResult(manager, identifiers);
            assertEquals("pimple:inactive-1", calcBuckets(result));
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
            final UnitTestGroupsContext testContext = UnitTestGroupsContext.newBuilder()
                    .setLoggedIn(false)
                    .setCountry("FR")
                    .setAccount(new Account(10))
                    .build();
            // LoggedIn=false + MX maps to [1, 0, 0] range
            final Identifiers identifiers = new Identifiers(TestType.PAGE, uidString);
            final ProctorResult result = testContext.getProctorResult(manager, identifiers);
            assertEquals("", calcBuckets(result));
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
        final UnitTestGroupsContext testContext = UnitTestGroupsContext.newBuilder()
                .setLoggedIn(true)
                .setCountry("US")
                .setAccount(new Account(10))
                .build();
        final Identifiers identifiers = new Identifiers(TestType.COMPANY, "16s2o7s01001d9vj");
        final ProctorResult result = testContext.getProctorResult(manager, identifiers);
        assertEquals("", calcBuckets(result));
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
        final UnitTestGroupsContext testContext = UnitTestGroupsContext.newBuilder()
                .setLoggedIn(true)
                .setCountry("US")
                .setAccount(new Account(10))
                .build();
        final Identifiers identifiers = new Identifiers(ImmutableMap.<TestType, String>builder()
                .put(TestType.ANONYMOUS_USER, SPECIFICATION_MATRIX)
                .build());
        final ProctorResult result = testContext.getProctorResult(manager, identifiers);
        assertEquals("kluj:kloo2,map_payload:inactive-1,oop_poop:test1,payloaded:inactive-1,payloaded_verified:inactive-1", calcBuckets(result));
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
        assertEquals(0, grps.getPayloaded_verifiedPayloadForBucket(UnitTestGroups.Payloaded_verified.INACTIVE), 0.001);
        assertEquals(5, grps.getPayloaded_verifiedPayloadForBucket(UnitTestGroups.Payloaded_verified.CONTROL), 0.001);
        assertEquals(50, grps.getPayloaded_verifiedPayloadForBucket(UnitTestGroups.Payloaded_verified.TEST), 0.001);

        assertEquals("kluj2,oop_poop1", grps.toString());

        assertNotNull(grps.getPayloaded_excluded());
        assertEquals(
                "Expected inactive even though there are no explicit assignments made to that group",
                Payloaded.INACTIVE, grps.getPayloaded());
        assertArrayEquals(
                "Expected inactive payload to be used, not 'empty' default",
                new String[]{"preexisting"}, grps.getPayloaded_excludedPayload());
    }

    @Test
    public void testTestDescriptions(){
        final UnitTestGroupsContext testContext = UnitTestGroupsContext.newBuilder()
                .setLoggedIn(true)
                .setCountry("FR")
                .setAccount(new Account(10))
                .build();
        final Identifiers identifiers = new Identifiers(TestType.USER, "16s2o7s01001d9vj");
        final ProctorResult result = testContext.getProctorResult(manager, identifiers);
        assertEquals("kluj:test1,map_payload:inactive-1,oop_poop:control0,payloaded:inactive-1,payloaded_verified:inactive-1", calcBuckets(result));
        // Check and make sure UnitTestGroups respects these groups and works as expected.
        final UnitTestGroups grps = new UnitTestGroups(result);
        //make sure getDescription method exists and returns the correct description
        assertEquals(grps.getKlujDescription(),"2nd test");
    }

    @Test
    public void testTestDescriptions_checkEscaping(){
        final UnitTestGroupsContext testContext = UnitTestGroupsContext.newBuilder()
                .setLoggedIn(true)
                .setCountry("FR")
                .setAccount(new Account(10))
                .build();
        final Identifiers identifiers = new Identifiers(TestType.USER, "16s2o7s01001d9vj");
        final ProctorResult result = testContext.getProctorResult(manager, identifiers);
        assertEquals("kluj:test1,map_payload:inactive-1,oop_poop:control0,payloaded:inactive-1,payloaded_verified:inactive-1", calcBuckets(result));
        // Check and make sure UnitTestGroups respects these groups and works as expected.
        final UnitTestGroups grps = new UnitTestGroups(result);
        //make sure getDescription method exists and returns the correct description with escaping
        assertEquals(grps.getBubbleDescription(),"3rd \n\t\"test");
    }

    @Test
    public void testMapPayloadReturns(){
        final UnitTestGroupsContext testContext = UnitTestGroupsContext.newBuilder()
                .setLoggedIn(true)
                .setCountry("FR")
                .setAccount(new Account(10))
                .build();
        final Identifiers identifiers = new Identifiers(TestType.USER, "16s2o7s01001d9vj");
        final ProctorResult result = testContext.getProctorResult(manager, identifiers);

        final UnitTestGroups grps = new UnitTestGroups(result);
        assertEquals(grps.getMap_payloadPayload().getAstring(),"lol");
        assertEquals(grps.getMap_payloadPayload().getAdouble(), (Double) 2.1);
        assertArrayEquals(grps.getMap_payloadPayload().getAnarray(), new Long[]{1L, 2L, 3L});
        assertArrayEquals(grps.getMap_payloadPayload().getAstringarr(), new String[]{"one","two","three"});
        assertArrayEquals(grps.getMap_payloadPayload().getAdarray(), new Double[]{1.1,2.1,3.1});

        final UnitTestGroupsPayload.Map_payload unitTestGroupsPayloadTest = grps.getMap_payloadPayloadForBucket(UnitTestGroups.Map_payload.TEST);
        assertEquals(unitTestGroupsPayloadTest.getAstring(),"l");
        assertEquals(unitTestGroupsPayloadTest.getAdouble(), (Double) 1.1);
        assertArrayEquals(unitTestGroupsPayloadTest.getAnarray(), new Long[]{1L, 2L, 3L});
        assertArrayEquals(unitTestGroupsPayloadTest.getAstringarr(), new String[]{"one","two","three"});
        assertArrayEquals(unitTestGroupsPayloadTest.getAdarray(), new Double[]{1.1,2.1,3.1});

        final UnitTestGroupsPayload.Map_payload unitTestGroupsPayloadControl = grps.getMap_payloadPayloadForBucket(UnitTestGroups.Map_payload.CONTROL);
        assertEquals(unitTestGroupsPayloadControl.getAstring(),"str2");
        assertEquals(unitTestGroupsPayloadControl.getAdouble(), (Double) 3.1);
        assertArrayEquals(unitTestGroupsPayloadControl.getAnarray(), new Long[]{1L, 2L, 3L});
        assertArrayEquals(unitTestGroupsPayloadControl.getAstringarr(), new String[]{"one","two","three"});
        assertArrayEquals(unitTestGroupsPayloadControl.getAdarray(), new Double[]{1.1,2.1,3.1});

    }

    @Test
    public void testNestedClasses() throws Exception {
        final Map<String, String> declaredContext = getProctorSpecification().getProvidedContext();
        final Map<String, String> innerClassTypes = Maps.filterValues(declaredContext, new Predicate<String>() {
            @Override
            public boolean apply(final String subfrom) {
                return subfrom.contains("$");
            }
        });
        assertTrue(
                "Sample groups need to contain at least one inner class type",
                !innerClassTypes.isEmpty());

        final ProvidedContext providedContext = ProctorUtils.convertContextToTestableMap(declaredContext);
        assertTrue(
                "Expected the provided context to be populated since no class-not-found-error should have been thrown",
                !providedContext.getContext().isEmpty());
    }



    private String calcBuckets(ProctorResult proctorResult) {
        final StringBuilder sb = new StringBuilder();
        // Current behavior is mapping from { testName -> TestBucket }


        for(final Iterator<Map.Entry<String, TestBucket>> iterator = proctorResult.getBuckets().entrySet().iterator(); iterator.hasNext(); ) {
            final Map.Entry<String, TestBucket> entry = iterator.next();
            final String testName = entry.getKey();
            final TestBucket testBucket = entry.getValue();

            if(sb.length() > 0) {
                sb.append(",");
            }
            // String format is: {testName}:{testBucket.name}{testBucket.value}
            sb.append(testName).append(":").append(testBucket.getName()).append(testBucket.getValue());
        }
        return sb.toString();
    }
}
