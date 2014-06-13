package com.indeed.proctor.service.core.var;

import javax.annotation.Nonnull;

public interface ValueConverter<T> {
    public T convert(@Nonnull String rawValue) throws ValueConversionException;
}
