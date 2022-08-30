package com.indeed.proctor.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.indeed.proctor.common.dynamic.DynamicFilters;
import com.indeed.proctor.common.dynamic.MetaTagsFilter;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestMatrixArtifact;
import org.junit.Test;

import java.io.Reader;
import java.io.StringReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class TestAbstractJsonProctorLoader {
    private static final Set<String> TESTS_IN_EXAMPLE_TEST_MATRIX = ImmutableSet.of(
            "exampletst",
            "sometst",
            "null_tst",
            "meta_tags_tst"
    );
    private ExampleJsonProctorLoader proctorLoader;

    private final String CONSTANT_SIZE_EXCEED = "The size of the field \"constant\" is too large, exceeds 1.5 Mb";
    private final String PAYLOAD_SIZE_EXCEED = "The size of the field \"payload\" is too large, exceeds 1.8 Kb";

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
                .isCloseTo(0.75d, offset(1e-6));

        assertThat(testMatrixArtifact.getTests().get("null_tst")).isNull();
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
        assertThat(testMatrixArtifact.getTests().keySet())
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
        assertThat(testMatrixArtifact.getTests().keySet())
                .containsExactlyInAnyOrder("sometst", "meta_tags_tst");
    }

    @Test
    public void testLoadJsonTestMatrix_givenExceedSizeConstant_thenThrowException() throws IOException {
        proctorLoader = new ExampleJsonProctorLoader(
                TESTS_IN_EXAMPLE_TEST_MATRIX,
                Collections.emptySet()
        );

        final StringBuilder sb = new StringBuilder("{\n" +
                "  \"audit\" : {\n" +
                "    \"version\" : \"1524\",\n" +
                "    \"updated\" : 1313525000000,\n" +
                "    \"updatedDate\" : \"2022-08-30T15:03-0500\",\n" +
                "    \"updatedBy\" : \"vanguyen\"\n" +
                "  },\n" +
                "  \"unknown_field\": \"should be ignored\",\n" +
                "  \"tests\" : {\n" +
                "    \"exampletst\" : {\n" +
                "      \"constants\" : {\n" +
                "        \"ALLOWED_ADVERTISER_IDS\" : [");
        for (int i = 0; i < 15000; i++) {
            sb.append("27831689, 4903639, 11940909, 19771536, 2829486, 27021644, 27831689, 4903639, 11940909, 19771536, 2829486, 27021644,");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("]\n" +
                "      },\n" +
                "      \"version\" : \"1\",\n" +
                "      \"salt\" : \"exampletst\",\n" +
                "      \"rule\" : null,\n" +
                "      \"buckets\" : [ {\n" +
                "        \"name\" : \"control\",\n" +
                "        \"value\" : 0\n" +
                "      }, {\n" +
                "        \"name\" : \"test\",\n" +
                "        \"value\" : 1\n" +
                "      } ],\n" +
                "      \"allocations\" : [ {\n" +
                "        \"rule\" : \"${lang == ENGLISH}\",\n" +
                "        \"ranges\" : [ {\n" +
                "          \"bucketValue\" : 0,\n" +
                "          \"length\" : 0.25\n" +
                "        }, {\n" +
                "          \"bucketValue\" : 1,\n" +
                "          \"length\" : 0.75\n" +
                "        } ],\n" +
                "        \"id\" : \"\"\n" +
                "      }, {\n" +
                "        \"rule\" : null,\n" +
                "        \"ranges\" : [ {\n" +
                "          \"bucketValue\" : 0,\n" +
                "          \"length\" : 0.1\n" +
                "        }, {\n" +
                "          \"bucketValue\" : 1,\n" +
                "          \"length\" : 0.9\n" +
                "        } ],\n" +
                "        \"id\" : \"\"\n" +
                "      } ],\n" +
                "      \"silent\" : false,\n" +
                "      \"testType\" : \"USER\",\n" +
                "      \"description\" : \"An example test\"\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
        final Reader reader = new StringReader(sb.toString());

        try {
            proctorLoader.loadJsonTestMatrix(reader);
            fail("Expected RTE");
        } catch (final RuntimeException rte) {
            assertEquals(CONSTANT_SIZE_EXCEED, rte.getMessage());
        }
    }

    @Test
    public void testLoadJsonTestMatrix_givenExceedSizePayload_thenThrowException() throws IOException {
        proctorLoader = new ExampleJsonProctorLoader(
                TESTS_IN_EXAMPLE_TEST_MATRIX,
                Collections.emptySet()
        );

        final StringBuilder sb = new StringBuilder("{\n" +
                "  \"audit\": {\n" +
                "    \"version\": \"1524\",\n" +
                "    \"updated\": 1313525000000,\n" +
                "    \"updatedDate\": \"2022-08-30T15:03-0500\",\n" +
                "    \"updatedBy\": \"vanguyen\"\n" +
                "  },\n" +
                "  \"tests\": {\n" +
                "    \"exampletst\": {\n" +
                "      \"constants\": {},\n" +
                "      \"version\": \"1\",\n" +
                "      \"salt\": \"exampletst\",\n" +
                "      \"rule\": null,\n" +
                "      \"buckets\": [\n" +
                "        {\n" +
                "          \"name\": \"control\",\n" +
                "          \"value\": 0\n" +
                "        },");
        for (int i = 0; i < 600; i++) {
            sb.append("{\n" +
                    "          \"name\": \"test5\",\n" +
                    "          \"value\": 5,\n" +
                    "          \"description\": \"BTFYT-4023: AB-test combined SJ/OJ non-US applyperseen MOB model\",\n" +
                    "          \"payload\": {\n" +
                    "            \"stringValue\": \"{\\\"model_name\\\":\\\"applyperseen_mob_rotw_dd826b0\\\",\\\"calibrator_name\\\":\\\"applyperseen_mob_rotw_dd826b0_dataset_calib_test_validation\\\",\\\"field_unaware_sparse_binary_tensorflow\\\":null,\\\"field_aware_sparse_binary_tensorflow\\\":null,\\\"logistic_regression\\\":{\\\"model_weight_property\\\":\\\"q2.5\\\"},\\\"old_format_logistic_regression_payload\\\":null}\"\n" +
                    "          }\n" +
                    "        },");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("],\n" +
                "      \"allocations\": [\n" +
                "        {\n" +
                "          \"rule\": \"${lang == ENGLISH}\",\n" +
                "          \"ranges\": [\n" +
                "            {\n" +
                "              \"bucketValue\": 0,\n" +
                "              \"length\": 0.25\n" +
                "            },\n" +
                "            {\n" +
                "              \"bucketValue\": 1,\n" +
                "              \"length\": 0.75\n" +
                "            }\n" +
                "          ],\n" +
                "          \"id\": \"\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"rule\": null,\n" +
                "          \"ranges\": [\n" +
                "            {\n" +
                "              \"bucketValue\": 0,\n" +
                "              \"length\": 0.1\n" +
                "            },\n" +
                "            {\n" +
                "              \"bucketValue\": 1,\n" +
                "              \"length\": 0.9\n" +
                "            }\n" +
                "          ],\n" +
                "          \"id\": \"\"\n" +
                "        }\n" +
                "      ],\n" +
                "      \"silent\": false,\n" +
                "      \"testType\": \"USER\",\n" +
                "      \"description\": \"An example test\"\n" +
                "    }\n" +
                "  }\n" +
                "}");
        final Reader reader = new StringReader(sb.toString());

        try {
            proctorLoader.loadJsonTestMatrix(reader);
            fail("Expected RTE");
        } catch (final RuntimeException rte) {
            assertEquals(PAYLOAD_SIZE_EXCEED, rte.getMessage());
        }
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
