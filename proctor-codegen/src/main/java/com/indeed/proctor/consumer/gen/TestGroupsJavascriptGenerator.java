package com.indeed.proctor.consumer.gen;


import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.indeed.proctor.common.PayloadType;
import com.indeed.proctor.common.model.Payload;

import java.util.Map;

/**
 * Generates a Javascript test groups file.
 *
 * @author andrewk
 */
public class TestGroupsJavascriptGenerator extends TestGroupsGenerator {

    public void generate(final String input, final String target, final String packageName, final String groupsClass, final boolean useClosure) throws CodeGenException {
        final String templatePath = "/com/indeed/proctor/consumer/ant/";
        final String jsTemplateName = "groups-js.ftl";
        final String fileExtension = ".js";
        final Map<String, Object> baseContext = Maps.newHashMap();
        baseContext.put("groupsClassName", groupsClass);
        baseContext.put("useClosure", useClosure);
        if (!Strings.isNullOrEmpty(packageName)) {
            generate(input, target, baseContext, packageName, groupsClass, templatePath, jsTemplateName, fileExtension);
        }

    }

    protected void addPayloadToTestDef(final Map<String, Object> testDef, final PayloadType specifiedPayloadType) {
        testDef.put("payloadJavascriptType", specifiedPayloadType.javascriptTypeName);
        testDef.put("payloadDefaultValue", specifiedPayloadType.getDefaultJavascriptValue());
    }

    public static void main(final String[] args) throws CodeGenException {
        if (args.length != 5) {
            System.err.println("java " + TestGroupsJavascriptGenerator.class.getCanonicalName() + " input.json outputDirectory packageName groupsClassName useClosure");
            System.exit(-4);
        }
        final TestGroupsJavascriptGenerator generator = new TestGroupsJavascriptGenerator();
        final String input = args[0];
        final String target = args[1];
        final String packageName = args[2];
        final String groupsClass = args[3];
        final boolean useClosure = Boolean.parseBoolean(args[4]);
        generator.generate(input, target, packageName, groupsClass, useClosure);
    }
}
