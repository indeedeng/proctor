package com.indeed.proctor.consumer;

import java.util.Map;

public interface ProctorContextDescriptor {
    Map<String, String> getProvidedContext();
}