package com.indeed.proctor.service.var;

import javax.annotation.Nonnull;

public interface ValueConverter<T> {
    public T convert(@Nonnull String rawValue) throws ValueConversionException;
}
