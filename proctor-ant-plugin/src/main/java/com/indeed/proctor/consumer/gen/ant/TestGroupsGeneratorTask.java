package com.indeed.proctor.consumer.gen.ant;

import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.consumer.gen.CodeGenException;
import com.indeed.proctor.consumer.gen.TestGroupsGenerator;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

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
    private String name;
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
     * Generates total specifications from any partial specifications found
     */
    private void totalSpecificationGenerator(File dir) throws CodeGenException {
        if (dir.equals(null)) {
            throw new CodeGenException("searchDirectory called with null pointer");
        }
        final File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for(File entry : files) {
            if (entry.isDirectory()) {
                totalSpecificationGenerator(entry);
            } else {
                try {
                    if(entry.getName().endsWith(".json") && !ProctorUtils.readJsonFromFile(entry).has("tests")) {
                        final String filePath = entry.getAbsolutePath().substring(0, entry.getAbsolutePath().lastIndexOf(File.separator));
                        if (!accessed.contains(filePath)) {
                            gen.makeTotalSpecification(new File(filePath), filePath.substring(0,filePath.lastIndexOf(File.separator)), name);
                            accessed.add(filePath);
                        }
                    }
                } catch (final IOException e) {
                    throw new CodeGenException("Could not create total specification file ",e);
                }
            }
        }
    }

    @Override
    public void execute() throws BuildException {
        //  TODO: validate
        accessed = new ArrayList<String>();
        final File bottom = new File(input);
        name = bottom.getName();
        final File topDirectory = bottom.getParentFile();
        if (topDirectory == null || !topDirectory.isDirectory()) {
            LOGGER.error("input not substituted with configured value");
            return;
        }
        try {
            totalSpecificationGenerator(topDirectory);
            gen.generate(input, target, packageName, groupsClass, groupsManagerClass);
        } catch (final CodeGenException ex) {
            throw new BuildException("Unable to generate code " + ex.toString(), ex);
        }
    }
}
