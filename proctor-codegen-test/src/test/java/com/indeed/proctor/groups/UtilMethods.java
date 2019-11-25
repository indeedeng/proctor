package com.indeed.proctor.groups;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import com.indeed.proctor.common.Proctor;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.common.StringProctorLoader;
import com.indeed.proctor.common.model.TestBucket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class UtilMethods {

    private UtilMethods() {
    }

    static Proctor getProctor(final String matrixfilename, final String specificationFilename) {
        try {
            // just read from the resource .json file at the moment.ProctorUtils.java

            final Reader matrixResource = new BufferedReader(new InputStreamReader(TestUnitTestGroupsManager.class.getResourceAsStream(matrixfilename)));
            final StringWriter matrixString = new StringWriter();
            CharStreams.copy(matrixResource, matrixString);

            final ProctorSpecification specification;
            specification = getProctorSpecification(specificationFilename);
            final StringProctorLoader loader = new StringProctorLoader(specification, matrixfilename, matrixString.toString());

            assertTrue("StringProctorLoader should load", loader.load());
            return loader.get();
        } catch (final IOException e) {
            throw new RuntimeException(e);

        }
    }

    static ProctorSpecification getProctorSpecification(final String specificationFilename) throws IOException {
        try (InputStream specicationStream = TestUnitTestGroupsManager.class.getResourceAsStream(specificationFilename)) {
            return ProctorUtils.readSpecification(specicationStream);
        }
    }

    static Map<String, String> calcBuckets(final ProctorResult proctorResult) {

        // Current behavior is mapping from { testName -> TestBucket }

        final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for (Map.Entry<String, TestBucket> entry: proctorResult.getBuckets().entrySet()) {
            final String testName = entry.getKey();
            final TestBucket testBucket = entry.getValue();

            builder.put(testName, testBucket.getName() + testBucket.getValue());
        }
        return builder.build();
    }
}
