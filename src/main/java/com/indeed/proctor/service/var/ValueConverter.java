package com.indeed.proctor.service.var;

public interface ValueConverter<T> {
    public T convert(String rawValue) throws ConversionException;
}
