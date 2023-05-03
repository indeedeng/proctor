package com.indeed.proctor.consumer;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.model.NameObfuscator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A convenience class meant to provide easy functions for creating the various javascript payloads proctor requires.
 */
public class ProctorJavascriptPayloadBuilder {

    private final AbstractGroups testGroups;
    private final NameObfuscator testNameObfuscator;

    public ProctorJavascriptPayloadBuilder(final AbstractGroups testGroups, final NameObfuscator testNameObfuscator) {
        this.testGroups = testGroups;
        this.testNameObfuscator = testNameObfuscator;
    }

    public ProctorJavascriptPayloadBuilder(final AbstractGroups testGroups) {
        this(testGroups, new NameObfuscator());
    }


    /**
     * Generates a list of [bucketValue, payloadValue]'s for each test in the input array
     * The purpose is to provide payloads in the order of tests defined in the client app's proctor spec.
     *
     * To be used with generated javascript files from 'ant generate-proctor-js' by serializing the list
     * to a string and passing it to {packageName}.init();
     *
     * @param <E> Generic Type of Test
     * @param tests an alphabetical list of Test enums from your generated proctor java subclass of {@link com.indeed.proctor.consumer.AbstractGroups}.
     * @return a list of 2-element lists that hold the bucketValue and payloadValue for each test in the same order as the input
     */
    @SuppressWarnings("deprecation")
    public final <E extends Test> List<List<Object>> buildAlphabetizedListJavascriptConfig(final E... tests) {
        return testGroups.getJavaScriptConfig(tests);
    }

    /**
     * Generates a Map that be serialized to JSON and used with
     * indeed.proctor.groups.init and
     * indeed.proctor.groups.inGroup(tstName, bucketValue)
     * tstName is obfuscated by hashing the tstName
     *
     * @param <E> Generic Type of Test
     * @param tests an alphabetical list of Test enums from your generated proctor java subclass of {@link com.indeed.proctor.consumer.AbstractGroups}.
     * @return a {@link Map} of config JSON
     */
    public final <E extends Test> Map<String, List<Object>> buildObfuscatedJavaScriptConfigMap(final E... tests) {
        final ImmutableMap.Builder<String, List<Object>> result = ImmutableMap.builder();
        for (final Test test : tests) {
            final List<Object> testValueAndPayload = Arrays.asList(
                    testGroups.getValue(test.getName(), test.getFallbackValue()),
                    testGroups.getPayload(test.getName(), test.getFallbackValue()).fetchAValue());
            final String hashedName = testNameObfuscator.obfuscateTestName(test.getName());
            result.put(hashedName, testValueAndPayload);
        }
        return result.build();
    }
}
