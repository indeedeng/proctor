package com.indeed.proctor.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.indeed.proctor.common.dynamic.DynamicFilters;
import com.indeed.proctor.common.dynamic.MetaTagsFilter;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author xiaoyun
 */

public class TestAbstractJsonProctorLoader {
    private static final Set<String> TESTS_IN_EXAMPLE_TEST_MATRIX = ImmutableSet.of(
            "exampletst",
            "sometst",
            "null_tst",
            "meta_tags_tst"
    );
    private ExampleJsonProctorLoader proctorLoader;

    @Test
    public void testLoadJsonTestMatrix() throws IOException {
        proctorLoader = new ExampleJsonProctorLoader(
                TESTS_IN_EXAMPLE_TEST_MATRIX,
                Collections.emptySet()
        );

        final String path = getClass().getResource("example-test-matrix.json").getPath();
        final File testMatrixFile = new File(path);
        final Reader reader = new FileReader(testMatrixFile);
        final TestMatrixArtifact testMatrixArtifact = proctorLoader.loadJsonTestMatrix(reader);
        assertEquals("1524", testMatrixArtifact.getAudit().getVersion());
        assertEquals(1313525000000l, testMatrixArtifact.getAudit().getUpdated());
        assertEquals("shoichi", testMatrixArtifact.getAudit().getUpdatedBy());
        assertEquals(4, testMatrixArtifact.getTests().size());
        assertTrue(testMatrixArtifact.getTests().containsKey("exampletst"));
        assertTrue(testMatrixArtifact.getTests().containsKey("sometst"));
        assertTrue(testMatrixArtifact.getTests().containsKey("null_tst"));
        assertTrue(testMatrixArtifact.getTests().containsKey("meta_tags_tst"));

        final ConsumableTestDefinition testDefinition = testMatrixArtifact.getTests().get("exampletst");
        assertEquals("control", testDefinition.getBuckets().get(0).getName());
        assertEquals("test", testDefinition.getBuckets().get(1).getName());
        assertEquals(2, testDefinition.getAllocations().size());
        assertEquals("${lang == ENGLISH}", testDefinition.getAllocations().get(0).getRule());
        assertEquals(0.25d, testDefinition.getAllocations().get(0).getRanges().get(0).getLength(), 1e-6);
        assertEquals(0.75d, testDefinition.getAllocations().get(0).getRanges().get(1).getLength(), 1e-6);

        assertNull(testMatrixArtifact.getTests().get("null_tst"));
    }

    @Test
    public void testLoadJsonTestMatrixWithOneRequiredTest() throws IOException {
        proctorLoader = new ExampleJsonProctorLoader(
                ImmutableSet.of("exampletst"),
                Collections.emptySet()
        );

        final String path = getClass().getResource("example-test-matrix.json").getPath();
        final File testMatrixFile = new File(path);
        final Reader reader = new FileReader(testMatrixFile);
        final TestMatrixArtifact testMatrixArtifact = proctorLoader.loadJsonTestMatrix(reader);

        // only verify test names because other checks are done in testLoadJsonTestMatrix()
        Assertions.assertThat(testMatrixArtifact.getTests().keySet())
                .containsExactly("exampletst");
    }

    @Test
    public void testLoadJsonTestMatrixWithMetaTags() throws IOException {
        proctorLoader = new ExampleJsonProctorLoader(
                Collections.emptySet(),
                ImmutableSet.of("sometag", "example_tag")
        );

        final String path = getClass().getResource("example-test-matrix.json").getPath();
        final File testMatrixFile = new File(path);
        final Reader reader = new FileReader(testMatrixFile);
        final TestMatrixArtifact testMatrixArtifact = proctorLoader.loadJsonTestMatrix(reader);

        // only verify test names because other checks are done in testLoadJsonTestMatrix()
        Assertions.assertThat(testMatrixArtifact.getTests().keySet())
                .containsExactlyInAnyOrder("sometst", "meta_tags_tst");
    }

    class ExampleJsonProctorLoader extends AbstractJsonProctorLoader {
        public ExampleJsonProctorLoader(final Set<String> requiredTests, final Set<String> metaTags) {
            super(
                    ExampleJsonProctorLoader.class,
                    new ProctorSpecification(
                            Collections.emptyMap(),
                            requiredTests.stream()
                                    .collect(Collectors.toMap(Function.identity(), (e) -> new TestSpecification())),
                            metaTags.isEmpty() ? new DynamicFilters() : new DynamicFilters(
                                    ImmutableList.of(new MetaTagsFilter(metaTags))
                            )
                    ),
                    RuleEvaluator.defaultFunctionMapperBuilder().build());
        }

        TestMatrixArtifact loadTestMatrix() throws IOException, MissingTestMatrixException {
            return null;
        }

        String getSource() {
            return null;
        }
    }
}
