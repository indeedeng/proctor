package com.indeed.proctor.consumer.gen.ant;

import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.consumer.gen.CodeGenException;
import com.indeed.proctor.consumer.gen.TestGroupsGenerator;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.codehaus.jackson.JsonNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class TestGroupsGeneratorTask extends Task {
    private final TestGroupsGenerator gen = new TestGroupsGenerator();

    private String input;
    private String target;
    private String packageName;
    private String groupsClass;
    private String groupsManagerClass;
    private static List<String> accessed;
    private static final Logger LOGGER = Logger.getLogger(TestGroupsGeneratorTask.class);

    public String getInput() {
        return input;
    }

    public void setInput(final String input) {
        this.input = input;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(final String target) {
        this.target = target;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(final String packageName) {
        this.packageName = packageName;
    }

    public String getGroupsClass() {
        return groupsClass;
    }

    public void setGroupsClass(final String groupsClass) {
        this.groupsClass = groupsClass;
    }

    public String getGroupsManagerClass() {
        return groupsManagerClass;
    }

    public void setGroupsManagerClass(final String groupsManagerClass) {
        this.groupsManagerClass = groupsManagerClass;
    }

    /*
     * traverse through main specification folder to find large proctor specifications (determined if they have the test
     * attribute) or individual test specifications (if they do not have this attribute).
     */
    private void recursiveSpecificationsFinder(File dir) throws CodeGenException {
        if (dir.equals(null)) {
            throw new CodeGenException("searchDirectory called with null pointer");
        }
        final File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for(File entry : files) {
            if (entry.isDirectory()) {
                recursiveSpecificationsFinder(entry);
            } else {
                if (entry.getName().endsWith(".json")) {
                    final JsonNode fullTest;
                    try {
                        fullTest = ProctorUtils.readJsonFromFile(entry);
                    } catch (IOException e) {
                        throw new CodeGenException("Could not read json correctly " + entry.getAbsolutePath(),e);
                    }
                    if (fullTest.has("tests")) {
                        gen.generate(entry.getAbsolutePath(),
                                target,
                                packageName,
                                groupsClass,
                                groupsManagerClass);
                    } else {
                        String filePath = entry.getAbsolutePath().substring(0, entry.getAbsolutePath().lastIndexOf(File.separator));
                        if (!accessed.contains(filePath)) {
                            final File newInput = gen.makeTotalSpecification(new File(filePath));
                            gen.generate(newInput.getAbsolutePath(),
                                    target,
                                    packageName,
                                    dir.getName()+"Groups",
                                    dir.getName()+"GroupsManager");
                            accessed.add(filePath);
                        }
                    }

                }
            }
        }
    }

    @Override
    public void execute() throws BuildException {
        //  TODO: validate
        accessed = new ArrayList<String>();
        File topDirectory = (new File(input)).getParentFile();
        if (topDirectory == null || !topDirectory.isDirectory()) {
            LOGGER.error("input not substituted with configured value");
            return;
        }
        try {
            recursiveSpecificationsFinder(topDirectory);
        } catch (final CodeGenException ex) {
            throw new BuildException("Unable to generate code " + ex.toString(), ex);
        }
    }
}
