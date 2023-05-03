package com.indeed.proctor.common.el;

import com.google.common.collect.Maps;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.el.FunctionMapper;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Unified Expression Language support class for importing public static library functions for use in a UEL expression
 * @author ketan
 */
public class LibraryFunctionMapper extends FunctionMapper {
    @Nonnull
    private final Map<String, Map<String, Method>> libraries = Maps.newHashMap();
    private static final int PUBLIC_AND_STATIC = Modifier.PUBLIC | Modifier.STATIC;

    public LibraryFunctionMapper(@Nonnull final Map<String, Class<?>> libraryClasses) {
        for (final Entry<String, Class<?>> entry : libraryClasses.entrySet()) {
            final String namespace = entry.getKey();
            final Map<String, Method> functions = extractFunctions(entry.getValue());
            libraries.put(namespace, functions);
        }
    }

    private Map<String, Method> extractFunctions(@Nonnull final Class<?> c) {
        final Map<String, Method> libraryFunctions = Maps.newHashMap();
        for (final Method m : c.getMethods()) {
            final int modifiers = m.getModifiers();
            if ((modifiers & PUBLIC_AND_STATIC) == 0) {
                continue;
            }
            libraryFunctions.put(m.getName(), m);
        }
        return libraryFunctions;
    }

    @Nullable
    @Override
    public Method resolveFunction(@Nonnull final String namespace, @Nonnull final String functionName) {
        final Map<String, Method> library = libraries.get(namespace);
        if (library == null) {
            return null;
        }
        return library.get(functionName);
    }
}
