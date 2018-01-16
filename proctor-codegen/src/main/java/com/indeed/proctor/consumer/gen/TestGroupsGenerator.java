package com.indeed.proctor.consumer.gen;

import com.google.common.annotations.VisibleForTesting;
import com.indeed.proctor.common.PayloadType;
import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.common.Serializers;
import com.indeed.proctor.common.TestSpecification;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringEscapeUtils;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Handles combining multiple proctor specs into one and building a root map for generating a
 * Proctor test groups file.
 *
 * Note: heavily based off of the original TestGroupsJavaGenerator. populateRootMap
 * may still have some code only needed for Java.
 *
 * @author andrewk
 */
public abstract class TestGroupsGenerator extends FreeMarkerCodeGenerator {
    private static final ObjectMapper OBJECT_MAPPER = Serializers.lenient();
    /*
     * If a folder of split jsons defining a proctor specification is provided, this method iterates over the folder
     * contents, using the individual TestDefinition jsons and a providedcontext.json to create one large
     * temporary ProctorSpecification json to be used for code generation
     */
    public static File makeTotalSpecification(File dir, String targetDir) throws CodeGenException {
        //If no name is provided use the name of the containing folder
        return makeTotalSpecification(dir, targetDir,dir.getPath().substring(dir.getPath().lastIndexOf(File.separator) + 1) + "Groups.json");
    }

    public static File makeTotalSpecification(File dir, String targetDir, String name) throws CodeGenException {
        final File[] dirFiles = dir.listFiles();
        return makeTotalSpecification(Arrays.asList(dirFiles), targetDir, name);
    }

    public static File makeTotalSpecification(List<File> files, String targetDir, String name) throws CodeGenException {
        Map<String, TestSpecification> testSpec = new LinkedHashMap<String, TestSpecification>();
        Map<String, String> providedContext = new LinkedHashMap<String,String>();
        for(File file : files) {
            final String fileName = file.getName();
            if(fileName.equals("providedcontext.json")){
                try {
                    providedContext = OBJECT_MAPPER.readValue(file, Map.class);
                } catch (IOException e) {
                    throw new CodeGenException("Could not read json correctly " + file.getAbsolutePath(), e);
                }
            }
            else if (fileName.endsWith(".json")){
                final TestSpecification spec;
                try {
                    spec = OBJECT_MAPPER.readValue(file, TestSpecification.class);
                } catch (IOException e) {
                    throw new CodeGenException("Could not read json correctly " + file.getAbsolutePath(),e);
                }
                final String specName = fileName.substring(0, fileName.indexOf(".json"));
                if (testSpec.containsKey(specName)) {
                    throw new CodeGenException("Multiple " + fileName + " found, each test should only have 1 spec file");
                }
                testSpec.put(specName, spec);
            }
        }
        final ProctorSpecification proctorSpecification = new ProctorSpecification();
        proctorSpecification.setTests(testSpec);
        proctorSpecification.setProvidedContext(providedContext);

        final File output =  new File(targetDir, name);
        try {
            OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValue(output, proctorSpecification);
        } catch (IOException e) {
            throw new CodeGenException("Could not write to temp file " + output.getAbsolutePath(),e);
        }
        return output;
    }

    @Override
    protected Map<String, Object> populateRootMap(final String input, final Map<String, Object> baseContext, final String packageName, final String className) {
        final File inputFile = new File(input);
        final ProctorSpecification spec = ProctorUtils.readSpecification(inputFile);

        return populateRootMap(spec, baseContext, packageName, className);
    }

    @VisibleForTesting
    Map<String, Object> populateRootMap(final ProctorSpecification spec, final Map<String, Object> baseContext, final String packageName, final String className) {
        final Map<String, Object> rootMap = Maps.newHashMap(baseContext);

        final Map<String, TestSpecification> tests = spec.getTests();

        final List<Object> testDefs = Lists.newArrayListWithCapacity(tests.size());

        // Sort buckets and test names, to have consistent iterator
        final SortedSet<String> sortedTestNames = new TreeSet<String>(tests.keySet());
        for (String testName : sortedTestNames) {
            final Set<Map<String, ?>> buckets = Sets.newLinkedHashSet();

            final TestSpecification testSpecification = tests.get(testName);
            final Map.Entry<String, Integer>[] sortedBuckets = testSpecification.getBuckets().entrySet().toArray(new Map.Entry[testSpecification.getBuckets().size()]);

            Arrays.sort(sortedBuckets, new Comparator<Map.Entry<String, Integer>>() {
                public int compare(final Map.Entry<String, Integer> e0, final Map.Entry<String, Integer> e1) {
                    if (e0.getValue().intValue() < e1.getValue().intValue()) {
                        return -1;
                    }
                    return e0.getValue() == e1.getValue() ? 0 : 1;
                }
            });

            boolean foundFallbackValue = false;
            for (final Map.Entry<String, Integer> bucket : sortedBuckets) {
                final String bucketName = bucket.getKey();
                final String enumName = toEnumName(bucketName);

                final Map<String, Object> bucketDef = Maps.newHashMap();
                final String normalizedBucketName = toJavaIdentifier(bucketName);
                bucketDef.put("value", bucket.getValue());
                bucketDef.put("name", bucketName);
                bucketDef.put("normalizedName", normalizedBucketName);
                bucketDef.put("enumName", enumName);
                bucketDef.put("javaClassName", uppercaseFirstChar(normalizedBucketName));

                buckets.add(bucketDef);

                foundFallbackValue = foundFallbackValue || (bucket.getValue() == testSpecification.getFallbackValue());
            }

                if (!buckets.isEmpty() && !foundFallbackValue) {
                    throw new IllegalArgumentException("Specified fallback value " + testSpecification.getFallbackValue() + " for test " + testName + " is not in the list of standard values: " + Arrays.toString(sortedBuckets));
                }

            final String name = toJavaIdentifier(testName);
            final String enumName = toEnumName(name);

            final Map<String, Object> testDef = Maps.newHashMap();
            testDef.put("name", testName);
            testDef.put("normalizedName", name);
            testDef.put("enumName", enumName);

            testDef.put("javaClassName", uppercaseFirstChar(name));
            testDef.put("buckets", buckets);
            testDef.put("defaultValue", testSpecification.getFallbackValue());
            final List<Map<String, String>> nestedPayloadsList = new ArrayList<Map<String,String>>();
            // Only define testDef.payloadJavaClass if the API user has
            // claimed she expects a payload.
            if (testSpecification.getPayload() != null) {
                final String specifiedPayloadTypeName = testSpecification.getPayload().getType();
                final PayloadType specifiedPayloadType = PayloadType.payloadTypeForName(specifiedPayloadTypeName);
                if(specifiedPayloadType == PayloadType.MAP) {
                    testDef.put("isMap","true");
                    for(Map.Entry<String,String> entry : testSpecification.getPayload().getSchema().entrySet()) {
                        final Map<String,String> nestedPayloadsMap = Maps.newHashMap();
                        nestedPayloadsMap.put("key",entry.getKey());
                        final PayloadType payloadTypeForValue = PayloadType.payloadTypeForName(entry.getValue());
                        if(payloadTypeForValue != PayloadType.MAP) {
                            nestedPayloadsMap.put("value", payloadTypeForValue.javaClassName);
                            nestedPayloadsMap.put("valueWithoutArray",
                                    payloadTypeForValue.javaClassName.substring(0, payloadTypeForValue.javaClassName.length() - 2));
                            if (PayloadType.STRING_ARRAY == payloadTypeForValue) {
                                nestedPayloadsMap.put("notANumber", "true");
                            }
                            nestedPayloadsMap.put("payloadTypeName", payloadTypeForValue.payloadTypeName);
                            nestedPayloadsList.add(nestedPayloadsMap);
                        } else {
                            throw new IllegalArgumentException("Nested Map Payloads are not allowed");
                        }
                    }
                }
                addPayloadToTestDef(testDef, specifiedPayloadType);
            }

            if (testSpecification.getDescription() != null) {
                testDef.put("description", StringEscapeUtils.escapeJava(testSpecification.getDescription()));
            }
            testDef.put("nestedPayloadsList",nestedPayloadsList);

            testDefs.add(testDef);
        }

        rootMap.put("contextArguments", spec.getProvidedContext());
        rootMap.put("mainClassName", className);
        rootMap.put("packageName", packageName);
        rootMap.put("testEnumName", "Test");
        rootMap.put("testDefs", testDefs);

        return rootMap;
    }

    abstract void addPayloadToTestDef(final Map<String, Object> testDef, final PayloadType specifiedPayloadType);
}
