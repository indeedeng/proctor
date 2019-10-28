package com.indeed.proctor.common;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.model.TestType;
import com.indeed.util.varexport.VarExporter;
import org.junit.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
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
        final ProctorResult result = proctor.determineTestGroups(
                Identifiers.of(TestType.ANONYMOUS_USER, "user"),
                ImmutableMap.<String, Object>of("lang", "en"),
                Collections.<String, Integer>emptyMap()
        );

        assertNotNull("exampletst should be loaded with a TestNamePrefixFilter",
                proctor.getTestDefinition("exampletst"));
        assertNotNull("meta_tags_tst should be loaded with a MetaTagsFilter",
                proctor.getTestDefinition("meta_tags_tst"));
        assertNull("sometst should not be loaded with a filter",
                proctor.getTestDefinition("sometst"));

        assertTrue("an allocation of exampletst should be determined",
                result.getAllocations().containsKey("exampletst"));
        assertTrue("a bucket of exampletst should be determined",
                result.getBuckets().containsKey("exampletst"));
    }

    @Test
    public void testExportedVariableForSpecification() {
        final JsonProctorLoaderFactory factory = new JsonProctorLoaderFactory();
        factory.setFilePath(getClass().getResource("example-test-matrix.json").getPath());
        factory.setSpecificationResource("classpath:specification-with-filter.json");
        assertThat(
                VarExporter.forNamespace("JsonProctorLoaderFactory")
                        .<String>getValue("specification-specification-with-filter.json")
        ).contains("example");
    }
}
