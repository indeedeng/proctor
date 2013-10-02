package com.indeed.proctor;

import com.indeed.proctor.common.el.LibraryFunctionMapper;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.Test;
import static org.junit.Assert.assertNotNull;

import com.google.common.collect.Maps;


public class TestLibraryFunctionMapper {
    public static void findMe() {
        /* intentionally empty */
    }

    public static Object findMeToo(final String s) {
        return s;
    }

    @Test
    public void testConstruction() {
        final Map<String, Class<?>> libraryClasses = Maps.newHashMap();
        libraryClasses.put("tlfm", TestLibraryFunctionMapper.class);
        final LibraryFunctionMapper mapper = new LibraryFunctionMapper(libraryClasses);
        final Method foundMe = mapper.resolveFunction("tlfm", "findMe");
        assertNotNull(foundMe);
        final Method foundMeToo = mapper.resolveFunction("tlfm", "findMeToo");
        assertNotNull(foundMeToo);
    }
}
