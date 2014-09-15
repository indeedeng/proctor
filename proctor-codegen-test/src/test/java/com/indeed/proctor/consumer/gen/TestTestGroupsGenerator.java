package com.indeed.proctor.consumer.gen;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.common.ProvidedContext;
import org.junit.Test;

import java.util.Map;

import static com.indeed.proctor.consumer.gen.TestGroupsGenerator.populateRootMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestTestGroupsGenerator {
    public static class Inner {
        public final String value;

        public Inner(final String value) {
            this.value = value;
        }
    }

    @Test
    public void testBinaryNameReplacement() throws Exception {
        final ProctorSpecification specification = new ProctorSpecification();

        final String classNameWithDollarSeparator = Inner.class.getName();
        assertTrue(
                "Bad classname: " + classNameWithDollarSeparator,
                classNameWithDollarSeparator.endsWith("$Inner"));

        specification.setProvidedContext(ImmutableMap.of("inner", classNameWithDollarSeparator));

        final ProvidedContext badContext = ProctorUtils.convertContextToTestableMap(specification.getProvidedContext());
        assertTrue(
                "Codebase evolution: the inner class is no longer unrecognized",
                badContext.getContext().isEmpty());

        final Inner inputObject = new Inner("runtimeValue");
        final Map<String, Object> rootMap = populateRootMap(
                specification,
                ImmutableMap.<String, Object>of("inner", inputObject),
                "com.indeed.proctor",
                "TestClass");

        final Inner inner = (Inner) rootMap.get("inner");
        assertEquals(
                "Expected to be able to retrieve the value of a field on the named inner class",
                "runtimeValue", inner.value);

        //noinspection unchecked
        final Map<String, String> contextArguments = (Map<String, String>)rootMap.get("contextArguments");
        assertEquals(
                "Expected the classname in the context arguments to be something that could appear in a java import or method call",
                Inner.class.getCanonicalName(), contextArguments.get("inner"));
    }
}
