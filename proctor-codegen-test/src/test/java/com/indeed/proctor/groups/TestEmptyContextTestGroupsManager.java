package com.indeed.proctor.groups;

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

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Map.Entry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test to make sure specifications with no context variables generate code properly
 */
@SuppressWarnings({"ConstantConditions", "deprecation"})
public class TestEmptyContextTestGroupsManager {
    @SuppressWarnings("UnusedDeclaration")
    private static final Logger LOGGER = Logger.getLogger(TestEmptyContextTestGroupsManager.class);
    private static final String SPECIFICATION_RESOURCE = "EmptyContextTestGroups.json";
    private static final String SPECIFICATION_MATRIX = "emptycontexttest.proctor-matrix.json";

    private EmptyContextTestGroupsManager manager;


    public TestEmptyContextTestGroupsManager() {
    }

    @Before()
    public void setUp() throws Exception {
        setUp(getProctor());
    }

    private void setUp(final Proctor proctor) {
        manager = new EmptyContextTestGroupsManager(() -> proctor);
    }

    @Nullable
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
        final InputStream specificationStream = getClass().getResourceAsStream(SPECIFICATION_RESOURCE);
        try {
            return ProctorUtils.readSpecification(specificationStream);
        } finally {
            specificationStream.close();
        }
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
            assertEquals("kluj:test1,map_payload:control0,oop_poop:control0,payloaded:inactive-1,payloaded_verified:inactive-1,pimple:inactive-1", calcBuckets(result));
        }
        {
            final ImmutableMap<TestType, String> idMap = ImmutableMap.<TestType, String>builder()
                    .put(TestType.EMAIL_ADDRESS, SPECIFICATION_MATRIX)
                    .put(TestType.AUTHENTICATED_USER, SPECIFICATION_MATRIX)
                    .put(TestType.PAGE, SPECIFICATION_MATRIX)
                    .build();
            final Identifiers identifiers = new Identifiers(idMap, true);

            final ProctorResult result = manager.determineBuckets(identifiers);
            assertEquals(result.getBuckets().get("dubblez").getValue(),2);
        }
    }

    private String calcBuckets(final ProctorResult proctorResult) {
        final StringBuilder sb = new StringBuilder();
        // Current behavior is mapping from { testName -> TestBucket }

        for (final Entry<String, TestBucket> entry : proctorResult.getBuckets().entrySet()) {
            final String testName = entry.getKey();
            final TestBucket testBucket = entry.getValue();

            if (sb.length() > 0) {
                sb.append(",");
            }
            // String format is: {testName}:{testBucket.name}{testBucket.value}
            sb.append(testName).append(":").append(testBucket.getName()).append(testBucket.getValue());
        }
        return sb.toString();
    }

}
