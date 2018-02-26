package com.indeed.proctor.common;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.dynamic.TestNamePrefixFilter;
import com.indeed.proctor.common.model.TestType;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TestJsonProctorLoaderFactory {
    @Test
    public void testDynamicFilterFromMethod() {
        final JsonProctorLoaderFactory factory = new JsonProctorLoaderFactory();
        factory.setFilePath(getClass().getResource("example-test-matrix.json").getPath());
        factory.setDynamicFilters(Collections.singletonList(new TestNamePrefixFilter("example")));
        factory.setSpecificationResource("classpath:example-empty-specification.json");
        final AbstractProctorLoader proctorLoader = factory.getLoader();
        proctorLoader.load();
        final Proctor proctor = proctorLoader.get();
        final ProctorResult result = proctor.determineTestGroups(
                Identifiers.of(TestType.ANONYMOUS_USER, "user"),
                ImmutableMap.<String, Object>of("lang", "en"),
                Collections.<String, Integer>emptyMap()
        );

        assertNotNull("exampletst should be loaded with a filter",
                proctor.getTestDefinition("exampletst"));
        assertTrue("an allocation of exampletst should be determined",
                result.getAllocations().containsKey("exampletst"));
        assertTrue("a bucket of exampletst should be determined",
                result.getBuckets().containsKey("exampletst"));
    }

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

        assertNotNull("exampletst should be loaded with a filter",
                proctor.getTestDefinition("exampletst"));
        assertTrue("an allocation of exampletst should be determined",
                result.getAllocations().containsKey("exampletst"));
        assertTrue("a bucket of exampletst should be determined",
                result.getBuckets().containsKey("exampletst"));
    }

    @Test
    public void testDynamicFilterFromMethodAndSpecification() {
        final JsonProctorLoaderFactory factory = new JsonProctorLoaderFactory();
        factory.setFilePath(getClass().getResource("example-test-matrix.json").getPath());
        factory.setDynamicFilters(Collections.singletonList(new TestNamePrefixFilter("some")));
        factory.setSpecificationResource("classpath:specification-with-filter.json");
        final AbstractProctorLoader proctorLoader = factory.getLoader();
        proctorLoader.load();
        final Proctor proctor = proctorLoader.get();
        final ProctorResult result = proctor.determineTestGroups(
                Identifiers.of(TestType.ANONYMOUS_USER, "user"),
                ImmutableMap.<String, Object>of("lang", "en"),
                Collections.<String, Integer>emptyMap()
        );

        assertNotNull("exampletst should be loaded with a filter",
                proctor.getTestDefinition("exampletst"));
        assertNotNull("sometst should be loaded with a filter",
                proctor.getTestDefinition("sometst"));
        assertTrue("an allocation of exampletst should be determined",
                result.getAllocations().containsKey("exampletst"));
        assertTrue("a bucket of exampletst should be determined",
                result.getBuckets().containsKey("exampletst"));
        assertTrue("an allocation of sometst should be determined",
                result.getAllocations().containsKey("sometst"));
        assertTrue("a bucket of sometst should be determined",
                result.getBuckets().containsKey("sometst"));
    }
}