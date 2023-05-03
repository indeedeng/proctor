package com.indeed.proctor.consumer;

import java.util.Map;

public interface ProctorContextDescriptor {
    /**
     * A Map mapping variable names to datatype names.
     * This can be used to introspect the context an application will provide to resolve rules in test definitions.
     */
    Map<String, String> getProvidedContext();
}
