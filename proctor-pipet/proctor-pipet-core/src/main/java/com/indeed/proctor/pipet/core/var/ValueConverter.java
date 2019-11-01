package com.indeed.proctor.pipet.core.var;

import javax.annotation.Nonnull;

public interface ValueConverter<T> {
    T convert(@Nonnull String rawValue) throws ValueConversionException;

    Class<T> getType();
}
