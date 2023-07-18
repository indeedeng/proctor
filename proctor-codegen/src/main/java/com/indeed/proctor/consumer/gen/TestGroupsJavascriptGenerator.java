package com.indeed.proctor.consumer.gen;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.indeed.proctor.common.PayloadType;
import com.indeed.proctor.common.ProctorSpecification;

import java.util.Map;

/**
 * Generates a Javascript test groups file.
 *
 * @author andrewk
 */
public class TestGroupsJavascriptGenerator extends TestGroupsGenerator {

    public void generate(
            final ProctorSpecification specification,
            final String target,
            final String packageName,
            final String groupsClass,
            final boolean useClosure)
            throws CodeGenException {
        final String templatePath = "/com/indeed/proctor/consumer/ant/";
        final String jsTemplateName = "groups-js.ftl";
        final String fileExtension = ".js";
        final Map<String, Object> baseContext = Maps.newHashMap();
        baseContext.put("groupsClassName", groupsClass);
        baseContext.put("useClosure", useClosure);
        if (!Strings.isNullOrEmpty(packageName)) {
            generate(
                    specification,
                    target,
                    baseContext,
                    packageName,
                    groupsClass,
                    templatePath,
                    jsTemplateName,
                    fileExtension);
        }
    }

    protected void addPayloadToTestDef(
            final Map<String, Object> testDef, final PayloadType specifiedPayloadType) {
        testDef.put("payloadJavascriptType", specifiedPayloadType.javascriptTypeName);
        testDef.put("payloadDefaultValue", specifiedPayloadType.getDefaultJavascriptValue());
    }
}
