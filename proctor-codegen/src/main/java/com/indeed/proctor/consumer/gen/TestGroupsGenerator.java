package com.indeed.proctor.consumer.gen;


import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.indeed.proctor.common.PayloadType;
import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.common.Serializers;
import com.indeed.proctor.common.TestSpecification;
import org.apache.commons.lang.StringEscapeUtils;
import org.codehaus.jackson.map.ObjectMapper;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;


public class TestGroupsGenerator extends FreeMarkerCodeGenerator {
    private static final ObjectMapper OBJECT_MAPPER = Serializers.lenient();

    public void generate(final String input, final String target, final String packageName, final String groupsClass, final String groupsManagerClass, final String contextClass) throws CodeGenException {
        final String templatePath = "/com/indeed/proctor/consumer/ant/";
        final String groupsTemplateName = "groups.ftl";
        final String groupsManagerTemplateName = "groups-manager.ftl";
        final String payloadTemplateName = "payload.ftl";
        final String contextTemplateName = "context.ftl";
        final String payloadClass = groupsClass + "Payload";
        final Map<String, Object> baseContext = Maps.newHashMap();
        baseContext.put("groupsClassName", groupsClass);
        baseContext.put("groupsManagerClassName", groupsManagerClass);
        baseContext.put("payloadClassName",groupsClass + "Payload");
        if (groupsClass != null) {
            generate(input, target, baseContext, packageName, groupsClass, templatePath, groupsTemplateName);
        }
        if (groupsManagerClass != null) {
            generate(input, target, baseContext, packageName, groupsManagerClass, templatePath, groupsManagerTemplateName);
        }
        if (groupsClass != null) {
            generate(input, target, baseContext, packageName, payloadClass, templatePath, payloadTemplateName);
        }
        if (contextClass != null) {
            generate(input, target, baseContext, packageName, contextClass, templatePath, contextTemplateName);
        }

    }
    /*
     * If a folder of split jsons defining a proctor specification is provided, this method iterates over the folder
     * contents, using the individual TestDefinition jsons and a providedcontext.json to create one large
     * temporary ProctorSpecification json to be used for code generation
     */
    public static File makeTotalSpecification(File dir, String targetDir) throws CodeGenException {
        //If no name is provided use the name of the containing folder
        return makeTotalSpecification(dir,targetDir,dir.getPath().substring(dir.getPath().lastIndexOf(File.separator) + 1) + "Groups.json");
    }
    public static File makeTotalSpecification(File dir, String targetDir, String name) throws CodeGenException {
        final File[] dirFiles = dir.listFiles();
        Map<String,TestSpecification> testSpec = new LinkedHashMap<String, TestSpecification>();
        Map<String,String> providedContext = new LinkedHashMap<String,String>();
        for(File child : dirFiles) {
            final String childName = child.getName();
            if(childName.equals("providedcontext.json")){
                try {
                    providedContext = OBJECT_MAPPER.readValue(child,Map.class);
                } catch (IOException e) {
                    throw new CodeGenException("Could not read json correctly " + child.getAbsolutePath(), e);
                }
            }
            else if (childName.endsWith(".json")){
                final TestSpecification spec;
                try {
                    spec = OBJECT_MAPPER.readValue(child,TestSpecification.class);
                } catch (IOException e) {
                    throw new CodeGenException("Could not read json correctly " + child.getAbsolutePath(),e);
                }
                testSpec.put(childName.substring(0, childName.indexOf(".json")),spec);
            }
        }
        final ProctorSpecification proctorSpecification = new ProctorSpecification();
        proctorSpecification.setTests(testSpec);
        proctorSpecification.setProvidedContext(providedContext);

        final File output =  new File(targetDir, name);
        try {
            OBJECT_MAPPER.defaultPrettyPrintingWriter().writeValue(output, proctorSpecification);
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
    static Map<String, Object> populateRootMap(final ProctorSpecification spec, final Map<String, Object> baseContext, final String packageName, final String className) {
        final Map<String, Object> rootMap = Maps.newHashMap(baseContext);

        final Map<String, TestSpecification> tests = spec.getTests();

        final List<Object> testDefs = Lists.newArrayListWithCapacity(tests.size());

        // Sort buckets and test names, to have consistent iterator
        final SortedSet<String> sortedTestNames = new TreeSet<String>(tests.keySet());
        for (String testName : sortedTestNames) {
            final Set<Map<String, ?>> buckets = Sets.newLinkedHashSet();

            final TestSpecification testSpecification = tests.get(testName);
            final Entry<String, Integer>[] sortedBuckets = testSpecification.getBuckets().entrySet().toArray(new Map.Entry[testSpecification.getBuckets().size()]);

            Arrays.sort(sortedBuckets, new Comparator<Entry<String, Integer>>() {
                public int compare(final Entry<String, Integer> e0, final Entry<String, Integer> e1) {
                    if(e0.getValue().intValue() < e1.getValue().intValue()) {
                        return -1;
                    }
                    return e0.getValue() == e1.getValue() ? 0 : 1;
                }
            });

            boolean foundFallbackValue = false;
            for (final Entry<String, Integer> bucket : sortedBuckets) {
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

            if (! foundFallbackValue) {
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
                testDef.put("payloadJavaClass", specifiedPayloadType.javaClassName);
                testDef.put("payloadAccessorName", specifiedPayloadType.javaAccessorName);
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

    public static void main(final String[] args) throws CodeGenException {
        if (args.length != 5) {
            System.err.println("java " + TestGroupsGenerator.class.getCanonicalName() + " input.json outputDirectory packageName groupsClassName");
            System.exit(-4);
        }
        final TestGroupsGenerator generator = new TestGroupsGenerator();
        final String input = args[0];
        final String target = args[1];
        final String packageName = args[2];
        final String groupsClass = args[3];
        final String groupsManagerClass = args[4];
        final String contextClass = args[5];
        generator.generate(input, target, packageName, groupsClass, groupsManagerClass, contextClass);
    }
}
