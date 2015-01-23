package com.indeed.proctor.groups;

import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import com.indeed.proctor.common.Identifiers;
import com.indeed.proctor.common.Proctor;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.common.StringProctorLoader;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestType;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestSplitSpecificationTestGroupsManager {
    private static final Logger LOGGER = Logger.getLogger(TestSplitSpecificationTestGroupsManager.class);
    private static final String SPECIFICATION_RESOURCE = "SplitSpecificationTestGroups.json";
    private static final String SPECIFICATION_MATRIX = "splitspecificationtest.proctor-matrix.json";

    private SplitSpecificationTestGroupsManager manager;


    public TestSplitSpecificationTestGroupsManager() {
    }

    @Before()
    public void setUp() throws Exception {
        setUp(getProctor());
    }

    private void setUp(final Proctor proctor) {
        manager = new SplitSpecificationTestGroupsManager(new Supplier<Proctor>() {
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
        final SplitSpecificationTestGroupsContext testContext = SplitSpecificationTestGroupsContext.newBuilder()
                .setLoggedIn(true)
                .setCountry("FR")
                .setAccountId(10)
                .build();
        {
            final Identifiers identifiers = new Identifiers(ImmutableMap.<TestType, String>builder()
                    .put(TestType.ANONYMOUS_USER, SPECIFICATION_MATRIX)
                    .put(TestType.AUTHENTICATED_USER, SPECIFICATION_MATRIX)
                    .put(TestType.PAGE, SPECIFICATION_MATRIX)
                    .build());

            final ProctorResult result = testContext.getProctorResult(manager, identifiers);
            assertEquals("one:test32,three:inactive-1,two:test22", calcBuckets(result));
        }
        {
            final ImmutableMap<TestType, String> idMap = ImmutableMap.<TestType, String>builder()
                    .put(TestType.EMAIL_ADDRESS, SPECIFICATION_MATRIX)
                    .put(TestType.AUTHENTICATED_USER, SPECIFICATION_MATRIX)
                    .put(TestType.PAGE, SPECIFICATION_MATRIX)
                    .build();
            final Identifiers identifiers = new Identifiers(idMap, true);

            final ProctorResult result = testContext.getProctorResult(manager, identifiers);
            assertEquals(result.getBuckets().get("one").getValue(),2);
        }
    }

    @Test
    public void testSomeBuckets() {
        final SplitSpecificationTestGroupsContext testContext = SplitSpecificationTestGroupsContext.newBuilder()
                .setLoggedIn(true)
                .setCountry("FR")
                .setAccountId(10)
                .build();
        {
            final Identifiers identifiers = new Identifiers(TestType.ANONYMOUS_USER, "16s2o7s01001d9vj");
            final ProctorResult result = testContext.getProctorResult(manager, identifiers);
            assertEquals("three:inactive-1,two:test33", calcBuckets(result));
            // Check and make sure SpecificationCreationGroups respects these groups and works as expected.
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
            assertEquals("two3",grps.toString());
        }
    }

    @Test
    public void testPayloads() {
        final SplitSpecificationTestGroupsContext testContext = SplitSpecificationTestGroupsContext.newBuilder()
                .setLoggedIn(true)
                .setCountry("US")
                .setAccountId(10)
                .build();
        final Identifiers identifiers = new Identifiers(ImmutableMap.<TestType, String>builder()
                .put(TestType.ANONYMOUS_USER, SPECIFICATION_MATRIX)
                .build());
        final ProctorResult result = testContext.getProctorResult(manager, identifiers);
        assertEquals("three:inactive-1,two:test22", calcBuckets(result));
        // Check and make sure SpecificationCreationGroups respects these groups and works as expected.
        final SplitSpecificationTestGroups grps = new SplitSpecificationTestGroups(result);
        System.out.println("grps == "+grps);
        assertNotNull(grps.getThree());
        assertEquals(-1, grps.getThreeValue(-99));
        // The "Inactive" condition should be true.
        assertTrue(grps.isThreeInactive());
        assertFalse(grps.isThreeControl());
        assertFalse(grps.isThreeTest());
        // Get the current test payload
        assertEquals(0, grps.getThreePayload(), 0.001);
        // Test per-bucket payload fetch
        assertEquals(0, grps.getThreePayloadForBucket(SplitSpecificationTestGroups.Three.INACTIVE), 0.001);
        assertEquals(5, grps.getThreePayloadForBucket(SplitSpecificationTestGroups.Three.CONTROL), 0.001);
        assertEquals(50, grps.getThreePayloadForBucket(SplitSpecificationTestGroups.Three.TEST), 0.001);

        assertEquals("two2", grps.toString());
    }

    @Test
    public void testTestDescriptions(){
        final SplitSpecificationTestGroupsContext testContext = SplitSpecificationTestGroupsContext.newBuilder()
                .setLoggedIn(true)
                .setCountry("FR")
                .setAccountId(10)
                .build();
        final Identifiers identifiers = new Identifiers(TestType.USER, "16s2o7s01001d9vj");
        final ProctorResult result = testContext.getProctorResult(manager, identifiers);
        assertEquals("three:inactive-1,two:test33", calcBuckets(result));
        // Check and make sure SpecificationCreationGroups respects these groups and works as expected.
        final SplitSpecificationTestGroups grps = new SplitSpecificationTestGroups(result);
        //make sure getDescription method exists and returns the correct description
        assertEquals(grps.getThreeDescription(),"2nd test");
        assertEquals(grps.getOneDescription(),"3rd \n\t\"test");
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
