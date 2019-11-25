package com.indeed.proctor.consumer.gen;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.indeed.proctor.common.PayloadType;

import java.util.Map;

/**
 * Generates a Java test groups file.
 *
 * @author andrewk
 */

public class TestGroupsJavaGenerator extends TestGroupsGenerator {

    public void generate(
            final String input,
            final String target,
            final String packageName,
            final String groupsClass,
            final String groupsManagerClass,
            final String contextClass
    ) throws CodeGenException {
        final String templatePath = "/com/indeed/proctor/consumer/ant/";
        final String groupsTemplateName = "groups.ftl";
        final String groupsManagerTemplateName = "groups-manager.ftl";
        final String payloadTemplateName = "payload.ftl";
        final String contextTemplateName = "context.ftl";
        final String payloadClass = groupsClass + "Payload"; // must be same as in groups.ftl: "${mainClassName}Payload"
        final String fileExtension = ".java";
        final Map<String, Object> baseContext = Maps.newHashMap();
        baseContext.put("groupsClassName", groupsClass);
        baseContext.put("groupsManagerClassName", groupsManagerClass);
        baseContext.put("payloadClassName", payloadClass);
        if (!Strings.isNullOrEmpty(groupsClass)) {
            generate(
                    input,
                    target,
                    baseContext,
                    packageName,
                    groupsClass,
                    templatePath,
                    groupsTemplateName,
                    fileExtension
            );
        }
        if (!Strings.isNullOrEmpty(groupsManagerClass)) {
            generate(
                    input,
                    target,
                    baseContext,
                    packageName,
                    groupsManagerClass,
                    templatePath,
                    groupsManagerTemplateName,
                    fileExtension
            );
        }
        if (!Strings.isNullOrEmpty(groupsClass)) {
            generate(
                    input,
                    target,
                    baseContext,
                    packageName,
                    payloadClass,
                    templatePath,
                    payloadTemplateName,
                    fileExtension
            );
        }
        if (!Strings.isNullOrEmpty(contextClass)) {
            generate(
                    input,
                    target,
                    baseContext,
                    packageName,
                    contextClass,
                    templatePath,
                    contextTemplateName,
                    fileExtension
            );
        }

    }

    protected void addPayloadToTestDef(
            final Map<String, Object> testDef,
            final PayloadType specifiedPayloadType
    ) {
        testDef.put("payloadJavaClass", specifiedPayloadType.javaClassName);
        testDef.put("payloadAccessorName", specifiedPayloadType.javaAccessorName);
    }

    public static void main(final String[] args) throws CodeGenException {
        if (args.length != 6) {
            System.err.println("java "
                    + TestGroupsJavaGenerator.class.getCanonicalName()
                    + " input.json outputDirectory packageName groupsClassName groupsManagerClassName contextClassName"
            );
            System.exit(-4);
        }
        final TestGroupsJavaGenerator generator = new TestGroupsJavaGenerator();
        final String input = args[0];
        final String target = args[1];
        final String packageName = args[2];
        final String groupsClass = args[3];
        final String groupsManagerClass = args[4];
        final String contextClass = args[5];
        generator.generate(input, target, packageName, groupsClass, groupsManagerClass, contextClass);
    }
}
