package com.indeed.proctor.consumer.gen.ant;

import com.google.common.base.Strings;
import com.indeed.proctor.consumer.gen.CodeGenException;
import com.indeed.proctor.consumer.gen.TestGroupsGenerator;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class TestGroupsGeneratorTask extends Task {
    private final TestGroupsGenerator gen = new TestGroupsGenerator();

    private String input;
    private String target;
    private String packageName;
    private String groupsClass;
    private String groupsManagerClass;
    private String contextClass;
    private String specificationOutput;
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

    public String getContextClass() {
        return contextClass;
    }

    public void setContextClass(final String contextClass) {
        this.contextClass = contextClass;
    }

    public String getSpecificationOutput() {
        return specificationOutput;
    }

    public void setSpecificationOutput(final String specificationOutput) {
        this.specificationOutput = specificationOutput;
    }

    /*
     * Generates total specifications from any partial specifications found
     */
    private void totalSpecificationGenerator(final File dir) throws CodeGenException {
        if (dir.equals(null)) {
            throw new CodeGenException("input directory creates null file");
        }
        final File[] providedContextFiles = dir.listFiles((java.io.FileFilter) FileFilterUtils.andFileFilter(FileFilterUtils.fileFileFilter(), FileFilterUtils.nameFileFilter("providedcontext.json")));
        if (providedContextFiles.length == 1) {
            //make directory if it doesn't exist
            (new File(specificationOutput.substring(0,specificationOutput.lastIndexOf(File.separator)))).mkdirs();
            final File specificationOutputFile = new File(specificationOutput);
            final File output = gen.makeTotalSpecification(dir, specificationOutputFile.getParent(), specificationOutputFile.getName());
            gen.generate(output.getPath(),target,packageName,groupsClass,groupsManagerClass,contextClass);
        } else {
            throw new CodeGenException("Incorrect amount of providedcontext.json in specified input folder");
        }
    }

    @Override
    public void execute() throws BuildException {
        //  TODO: validate
        final File inputFile = new File(input);
        if (inputFile == null) {
            LOGGER.error("input not substituted with configured value");
            return;
        }
        if (inputFile.isDirectory()) {
            if(!Strings.isNullOrEmpty(getSpecificationOutput())) {
                try {
                    totalSpecificationGenerator(inputFile);
                } catch (final CodeGenException e) {
                    throw new BuildException("Could not create total specification", e);
                }
            } else {
                throw new BuildException("Undefined output folder for generated specification");
            }
        } else {
            try {
                gen.generate(input, target, packageName, groupsClass, groupsManagerClass, contextClass);
            } catch (final CodeGenException ex) {
                throw new BuildException("Unable to generate code " + ex.toString(), ex);
            }
        }
    }
}
