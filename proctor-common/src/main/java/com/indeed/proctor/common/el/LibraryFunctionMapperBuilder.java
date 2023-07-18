package com.indeed.proctor.common.el;

import com.google.common.collect.Maps;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * Enables easier construction of a {@link LibraryFunctionMapper}
 *
 * @author ketan
 */
public class LibraryFunctionMapperBuilder {
    @Nonnull private final Map<String, Class<?>> libraryClasses = Maps.newHashMap();

    @Nonnull
    public LibraryFunctionMapperBuilder add(
            @Nonnull final String namespace, @Nonnull final Class<?> library) {
        libraryClasses.put(namespace, library);
        return this;
    }

    @Nonnull
    public LibraryFunctionMapper build() {
        return new LibraryFunctionMapper(libraryClasses);
    }
}
