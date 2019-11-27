package com.indeed.proctor.consumer.gen;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.indeed.proctor.common.PayloadType;
import com.indeed.proctor.common.ProctorSpecification;

import java.util.Map;

/**
 * Generates a Java test groups file.
 *
 * @author andrewk
 */

public class TestGroupsJavaGenerator extends TestGroupsGenerator {

    public void generate(
            final ProctorSpecification specification,
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
                    specification,
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
                    specification,
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
                    specification,
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
                    specification,
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
}
