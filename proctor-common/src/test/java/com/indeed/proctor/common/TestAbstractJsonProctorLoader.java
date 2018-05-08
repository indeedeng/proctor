package com.indeed.proctor.common;

import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author xiaoyun
 */

public class TestAbstractJsonProctorLoader {
    private ExampleJsonProctorLoader proctorLoader;

    @Before
    public void init() {
        proctorLoader = new ExampleJsonProctorLoader();
    }

    @Test
    public void testLoadJsonTestMatrix() throws IOException {
        final String path = getClass().getResource("example-test-matrix.json").getPath();
        final File testMatrixFile = new File(path);
        final Reader reader = new FileReader(testMatrixFile);
        final TestMatrixArtifact testMatrixArtifact = proctorLoader.loadJsonTestMatrix(reader);
        assertEquals("1524", testMatrixArtifact.getAudit().getVersion());
        assertEquals(1313525000000l, testMatrixArtifact.getAudit().getUpdated());
        assertEquals("shoichi", testMatrixArtifact.getAudit().getUpdatedBy());
        assertEquals(2, testMatrixArtifact.getTests().size());
        assertTrue(testMatrixArtifact.getTests().containsKey("exampletst"));
        assertTrue(testMatrixArtifact.getTests().containsKey("sometst"));
        final ConsumableTestDefinition testDefinition = testMatrixArtifact.getTests().get("exampletst");
        assertEquals("control", testDefinition.getBuckets().get(0).getName());
        assertEquals("test", testDefinition.getBuckets().get(1).getName());
        assertEquals(2, testDefinition.getAllocations().size());
        assertEquals("${lang == ENGLISH}", testDefinition.getAllocations().get(0).getRule());
        assertEquals(0.25d, testDefinition.getAllocations().get(0).getRanges().get(0).getLength(), 1e-6);
        assertEquals(0.75d, testDefinition.getAllocations().get(0).getRanges().get(1).getLength(), 1e-6);
    }

    class ExampleJsonProctorLoader extends AbstractJsonProctorLoader {
        public ExampleJsonProctorLoader() {
            super(ExampleJsonProctorLoader.class, new ProctorSpecification(), RuleEvaluator.defaultFunctionMapperBuilder().build());
        }

        TestMatrixArtifact loadTestMatrix() throws IOException, MissingTestMatrixException {
            return null;
        }

        String getSource() {
            return null;
        }
    }
}
