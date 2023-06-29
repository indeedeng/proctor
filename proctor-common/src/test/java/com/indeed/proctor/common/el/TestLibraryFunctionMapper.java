package com.indeed.proctor.common.el;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;


public class TestLibraryFunctionMapper {
    public static void findMe() {
        /* intentionally empty */
    }

    public static Object findMeToo(final String s) {
        return s;
    }

    @Test
    public void testConstruction() {
        final String key = "tlfm";
        final LibraryFunctionMapper mapper = new LibraryFunctionMapper(
                ImmutableMap.of(key, TestLibraryFunctionMapper.class));

        final Method foundMe = mapper.resolveFunction(key, "findMe");
        assertNotNull(foundMe);

        final Method foundMeToo = mapper.resolveFunction(key, "findMeToo");
        assertNotNull(foundMeToo);

        final Method notFound = mapper.resolveFunction(key, "toString");
        assertNull(notFound);
    }
}
