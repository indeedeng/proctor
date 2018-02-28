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
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestSplitSpecificationWithFiltersTestGroupsManager {
    private static final Logger LOGGER = Logger.getLogger(TestSplitSpecificationWithFiltersTestGroupsManager.class);
    private static final String SPECIFICATION_RESOURCE = "SplitSpecificationTestGroups.json";
    private static final String SPECIFICATION_MATRIX = "splitspecificationtest.proctor-matrix.json";

    private SplitSpecificationTestGroupsManager manager;


    public TestSplitSpecificationWithFiltersTestGroupsManager() {
    }

    @Before()
    public void setUp() throws Exception {
        setUp(getProctor());
    }

    private void setUp(final Proctor proctor) {
        manager = new SplitSpecificationWithFiltersTestGroupsManager(new Supplier<Proctor>() {
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
    public void testLoadDynamicTests() {
        final SplitSpecificationTestGroupsContext testContext = SplitSpecificationTestGroupsContext.newBuilder()
                .setLoggedIn(true)
                .setCountry("FR")
                .setAccountId(10)
                .build();
        final Identifiers identifiers = new Identifiers(ImmutableMap.<TestType, String>builder()
                .put(TestType.ANONYMOUS_USER, SPECIFICATION_MATRIX)
                .put(TestType.AUTHENTICATED_USER, SPECIFICATION_MATRIX)
                .put(TestType.PAGE, SPECIFICATION_MATRIX)
                .build());
        final ProctorResult result = testContext.getProctorResult(manager, identifiers);
        assertTrue(
                "a test defined in specification should be loaded",
                result.getAllocations().containsKey("one")
        );
        assertTrue(
                "a test matched defined filter should be loaded",
                result.getAllocations().containsKey("two")
        );
        assertTrue(
                "a test not matched in both of specification and filters shouldn't be loaded",
                result.getAllocations().containsKey("three")
        );
    }

}
