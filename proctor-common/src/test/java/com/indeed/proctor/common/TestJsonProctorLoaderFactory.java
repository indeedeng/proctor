package com.indeed.proctor.common;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.model.TestType;
import com.indeed.util.varexport.VarExporter;
import org.assertj.core.api.Condition;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestJsonProctorLoaderFactory {

    @Test
    public void testDynamicFilterFromSpecification() {
        final JsonProctorLoaderFactory factory = new JsonProctorLoaderFactory();
        factory.setFilePath(getClass().getResource("example-test-matrix.json").getPath());
        factory.setSpecificationResource("classpath:specification-with-filter.json");
        final AbstractProctorLoader proctorLoader = factory.getLoader();
        proctorLoader.load();
        final Proctor proctor = proctorLoader.get();

        assertNotNull(
                "exampletst should be loaded with a TestNamePrefixFilter",
                proctor.getTestDefinition("exampletst"));
        assertNotNull(
                "meta_tags_tst should be loaded with a MetaTagsFilter",
                proctor.getTestDefinition("meta_tags_tst"));
        assertNull(
                "sometst should not be loaded with a filter", proctor.getTestDefinition("sometst"));

        // test case: no testNameFilter => all tests should be determined
        testDynamicFilterFromSpecification(
                proctor,
                Collections.emptyList(),
                Arrays.asList("exampletst", "meta_tags_tst"),
                Arrays.asList("sometst"));

        // test case: filter exampletst => only exampletst should be determined
        testDynamicFilterFromSpecification(
                proctor,
                Arrays.asList("exampletst"),
                Arrays.asList("exampletst"),
                Arrays.asList("meta_tags_tst", "sometst"));

        // test case: filter an unknown test => no tests should be determined
        testDynamicFilterFromSpecification(
                proctor,
                Arrays.asList("sometst"),
                Collections.emptyList(),
                Arrays.asList("exampletst", "meta_tags_tst", "sometst"));
    }

    private void testDynamicFilterFromSpecification(
            @Nonnull final Proctor proctor,
            @Nonnull final Collection<String> testNameFilter,
            @Nonnull final Collection<String> expectedTestNamesInResult,
            @Nonnull final Collection<String> expectedTestNamesNotInResult) {
        final ProctorResult result =
                proctor.determineTestGroups(
                        Identifiers.of(TestType.ANONYMOUS_USER, "user"),
                        ImmutableMap.<String, Object>of("lang", "en"),
                        Collections.<String, Integer>emptyMap(),
                        testNameFilter);

        for (final String testName : expectedTestNamesInResult) {
            assertTrue(
                    "an allocation of " + testName + " should be determined",
                    result.getAllocations().containsKey(testName));
            assertTrue(
                    "a bucket of " + testName + " should be determined",
                    result.getBuckets().containsKey(testName));
        }
        for (final String testName : expectedTestNamesNotInResult) {
            assertFalse(
                    testName + " should not be determined",
                    result.getAllocations().containsKey(testName));
            assertFalse(
                    testName + " should not be determined",
                    result.getBuckets().containsKey(testName));
        }
    }

    @Test
    public void testExportedVariableForSpecification() {
        final JsonProctorLoaderFactory factory = new JsonProctorLoaderFactory();
        factory.setFilePath(getClass().getResource("example-test-matrix.json").getPath());
        factory.setSpecificationResource("classpath:specification-with-filter.json");
        assertThat(
                        VarExporter.forNamespace("JsonProctorLoaderFactory")
                                .<String>getValue("specification-specification-with-filter.json"))
                .contains("example");
    }

    @Test
    public void testExportedVariableForSpecificationFromObject() {
        final JsonProctorLoaderFactory factory = new JsonProctorLoaderFactory();
        factory.setFilePath(getClass().getResource("example-test-matrix.json").getPath());
        factory.setSpecification(new ProctorSpecification());
        assertThat(VarExporter.forNamespace("JsonProctorLoaderFactory").getVariables())
                .haveExactly(
                        1,
                        new Condition<>(
                                e -> e.getName().startsWith("specification-anonymous-"),
                                "variable name has the expected prefix"));
    }
}
