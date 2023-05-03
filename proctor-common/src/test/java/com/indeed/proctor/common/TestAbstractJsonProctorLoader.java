package com.indeed.proctor.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.indeed.proctor.common.dynamic.DynamicFilters;
import com.indeed.proctor.common.dynamic.MetaTagsFilter;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.Payload;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;


public class TestAbstractJsonProctorLoader {
    private static final Set<String> TESTS_IN_EXAMPLE_TEST_MATRIX = ImmutableSet.of(
            "exampletst",
            "sometst",
            "null_tst",
            "meta_tags_tst"
    );
    private ExampleJsonProctorLoader proctorLoader;

    @Test
<<<<<<< HEAD
    public void testLoadJsonTestMatrix() throws IOException, TestMatrixOutdatedException {
        proctorLoader = new ExampleJsonProctorLoader(
                TESTS_IN_EXAMPLE_TEST_MATRIX,
                Collections.emptySet()
        );
||||||| parent of 1ef67212 (PROC-960: Create gradlew and build files, working compile and test)
    public void testLoadJsonTestMatrix() throws IOException {
        proctorLoader = new ExampleJsonProctorLoader(
                TESTS_IN_EXAMPLE_TEST_MATRIX,
                Collections.emptySet()
        );
=======
    public void testLoadJsonTestMatrix() throws IOException {
<<<<<<< HEAD
        proctorLoader =
                new ExampleJsonProctorLoader(TESTS_IN_EXAMPLE_TEST_MATRIX, Collections.emptySet());
>>>>>>> 1ef67212 (PROC-960: Create gradlew and build files, working compile and test)
||||||| parent of a496e85b (PROC-960: Remove autostyle code)
        proctorLoader =
                new ExampleJsonProctorLoader(TESTS_IN_EXAMPLE_TEST_MATRIX, Collections.emptySet());
=======
        proctorLoader = new ExampleJsonProctorLoader(
                TESTS_IN_EXAMPLE_TEST_MATRIX,
                Collections.emptySet()
        );
>>>>>>> a496e85b (PROC-960: Remove autostyle code)

        final String path = getClass().getResource("example-test-matrix.json").getPath();
        final File testMatrixFile = new File(path);
        final Reader reader = new FileReader(testMatrixFile);
        final TestMatrixArtifact testMatrixArtifact = proctorLoader.loadJsonTestMatrix(reader);
        assertThat(testMatrixArtifact.getAudit().getVersion()).isEqualTo("1524");
        assertThat(testMatrixArtifact.getAudit().getUpdated()).isEqualTo(1313525000000L);
        assertThat(testMatrixArtifact.getAudit().getUpdatedBy()).isEqualTo("shoichi");
        assertThat(testMatrixArtifact.getTests()).hasSize(4);
        assertThat(testMatrixArtifact.getTests()).containsKeys(
                "exampletst",
                "sometst",
                "null_tst",
                "meta_tags_tst");

        final ConsumableTestDefinition testDefinition = testMatrixArtifact.getTests().get("exampletst");
        assertThat(testDefinition.getBuckets().get(0).getName()).isEqualTo("control");
        assertThat(testDefinition.getBuckets().get(1).getName()).isEqualTo("test");
        assertThat(testDefinition.getAllocations()).hasSize(2);
        assertThat(testDefinition.getAllocations().get(0).getRule()).isEqualTo("${lang == ENGLISH}");
        assertThat(testDefinition.getAllocations().get(0).getRanges().get(0).getLength())
                .isCloseTo(0.25d, offset(1e-6));
        assertThat(testDefinition.getAllocations().get(0).getRanges().get(1).getLength())
                .isCloseTo(0.75d, offset( 1e-6));

        assertThat(testMatrixArtifact.getTests().get("null_tst")).isNull();
    }

    @Test
<<<<<<< HEAD
    public void testLoadJsonTestMatrixWithUnrecognizedPayloadType() throws IOException, TestMatrixOutdatedException {
        proctorLoader = new ExampleJsonProctorLoader(
                ImmutableSet.of("exampletst"),
                Collections.emptySet()
        );
||||||| parent of 1ef67212 (PROC-960: Create gradlew and build files, working compile and test)
    public void testLoadJsonTestMatrixWithUnrecognizedPayloadType() throws IOException {
        proctorLoader = new ExampleJsonProctorLoader(
                ImmutableSet.of("exampletst"),
                Collections.emptySet()
        );
=======
    public void testLoadJsonTestMatrixWithUnrecognizedPayloadType() throws IOException {
<<<<<<< HEAD
        proctorLoader =
                new ExampleJsonProctorLoader(ImmutableSet.of("exampletst"), Collections.emptySet());
>>>>>>> 1ef67212 (PROC-960: Create gradlew and build files, working compile and test)
||||||| parent of a496e85b (PROC-960: Remove autostyle code)
        proctorLoader =
                new ExampleJsonProctorLoader(ImmutableSet.of("exampletst"), Collections.emptySet());
=======
        proctorLoader = new ExampleJsonProctorLoader(
                ImmutableSet.of("exampletst"),
                Collections.emptySet()
        );
>>>>>>> a496e85b (PROC-960: Remove autostyle code)

        final String path = getClass().getResource("unrecognized-payload-test-matrix.json").getPath();
        final File testMatrixFile = new File(path);
        final Reader reader = new FileReader(testMatrixFile);
        final TestMatrixArtifact testMatrixArtifact = proctorLoader.loadJsonTestMatrix(reader);
        assertThat(testMatrixArtifact.getAudit().getVersion()).isEqualTo("1524");
        assertThat(testMatrixArtifact.getAudit().getUpdated()).isEqualTo(1313525000000L);
        assertThat(testMatrixArtifact.getAudit().getUpdatedBy()).isEqualTo("shoichi");
        assertThat(testMatrixArtifact.getTests()).hasSize(1);
        assertThat(testMatrixArtifact.getTests()).containsKeys(
                "exampletst");

        final ConsumableTestDefinition testDefinition = testMatrixArtifact.getTests().get("exampletst");
        assertThat(testDefinition.getBuckets().get(0).getName()).isEqualTo("control");
        assertThat(testDefinition.getBuckets().get(1).getName()).isEqualTo("test");
        assertThat(testDefinition.getAllocations()).hasSize(2);
        assertThat(testDefinition.getAllocations().get(0).getRule()).isEqualTo("${lang == ENGLISH}");
        assertThat(testDefinition.getAllocations().get(0).getRanges().get(0).getLength())
                .isCloseTo(0.25d, offset(1e-6));
        assertThat(testDefinition.getAllocations().get(0).getRanges().get(1).getLength())
                .isCloseTo(0.75d, offset( 1e-6));
        assertThat(testDefinition.getBuckets().get(0).getPayload()).isEqualTo(Payload.EMPTY_PAYLOAD);
    }

    @Test
<<<<<<< HEAD
    public void testLoadJsonTestMatrixWithOneRequiredTest() throws IOException, TestMatrixOutdatedException {
        proctorLoader = new ExampleJsonProctorLoader(
                ImmutableSet.of("exampletst"),
                Collections.emptySet()
        );
||||||| parent of 1ef67212 (PROC-960: Create gradlew and build files, working compile and test)
    public void testLoadJsonTestMatrixWithOneRequiredTest() throws IOException {
        proctorLoader = new ExampleJsonProctorLoader(
                ImmutableSet.of("exampletst"),
                Collections.emptySet()
        );
=======
    public void testLoadJsonTestMatrixWithOneRequiredTest() throws IOException {
<<<<<<< HEAD
        proctorLoader =
                new ExampleJsonProctorLoader(ImmutableSet.of("exampletst"), Collections.emptySet());
>>>>>>> 1ef67212 (PROC-960: Create gradlew and build files, working compile and test)
||||||| parent of a496e85b (PROC-960: Remove autostyle code)
        proctorLoader =
                new ExampleJsonProctorLoader(ImmutableSet.of("exampletst"), Collections.emptySet());
=======
        proctorLoader = new ExampleJsonProctorLoader(
                ImmutableSet.of("exampletst"),
                Collections.emptySet()
        );
>>>>>>> a496e85b (PROC-960: Remove autostyle code)

        final String path = getClass().getResource("example-test-matrix.json").getPath();
        final File testMatrixFile = new File(path);
        final Reader reader = new FileReader(testMatrixFile);
        final TestMatrixArtifact testMatrixArtifact = proctorLoader.loadJsonTestMatrix(reader);

        // only verify test names because other checks are done in testLoadJsonTestMatrix()
        assertThat(testMatrixArtifact.getTests().keySet())
                .containsExactly("exampletst");
    }

    @Test
<<<<<<< HEAD
    public void testLoadJsonTestMatrixWithMetaTags() throws IOException, TestMatrixOutdatedException {
        proctorLoader = new ExampleJsonProctorLoader(
                Collections.emptySet(),
                ImmutableSet.of("sometag", "example_tag")
        );
||||||| parent of 1ef67212 (PROC-960: Create gradlew and build files, working compile and test)
    public void testLoadJsonTestMatrixWithMetaTags() throws IOException {
        proctorLoader = new ExampleJsonProctorLoader(
                Collections.emptySet(),
                ImmutableSet.of("sometag", "example_tag")
        );
=======
    public void testLoadJsonTestMatrixWithMetaTags() throws IOException {
<<<<<<< HEAD
        proctorLoader =
                new ExampleJsonProctorLoader(
                        Collections.emptySet(), ImmutableSet.of("sometag", "example_tag"));
>>>>>>> 1ef67212 (PROC-960: Create gradlew and build files, working compile and test)
||||||| parent of a496e85b (PROC-960: Remove autostyle code)
        proctorLoader =
                new ExampleJsonProctorLoader(
                        Collections.emptySet(), ImmutableSet.of("sometag", "example_tag"));
=======
        proctorLoader = new ExampleJsonProctorLoader(
                Collections.emptySet(),
                ImmutableSet.of("sometag", "example_tag")
        );
>>>>>>> a496e85b (PROC-960: Remove autostyle code)

        final String path = getClass().getResource("example-test-matrix.json").getPath();
        final File testMatrixFile = new File(path);
        final Reader reader = new FileReader(testMatrixFile);
        final TestMatrixArtifact testMatrixArtifact = proctorLoader.loadJsonTestMatrix(reader);

        // only verify test names because other checks are done in testLoadJsonTestMatrix()
        assertThat(testMatrixArtifact.getTests().keySet())
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

        TestMatrixArtifact loadTestMatrix() throws IOException, MissingTestMatrixException, TestMatrixOutdatedException{
            return null;
        }

        String getSource() {
            return null;
        }
    }
}
