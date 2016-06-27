package com.indeed.proctor.consumer.gen.ant;

import com.google.common.base.Strings;
import com.indeed.proctor.consumer.gen.CodeGenException;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.log4j.Logger;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import java.io.File;

/**
 * Ant task for generating Proctor test groups files.
 *
 * @author andrewk
 */
public abstract class TestGroupsGeneratorTask extends Task {
    protected static final Logger LOGGER = Logger.getLogger(TestGroupsGeneratorTask.class);
    protected String input;
    protected String target;
    protected String packageName;
    protected String groupsClass;
    protected String specificationOutput;

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

    public String getSpecificationOutput() {
        return specificationOutput;
    }

    public void setSpecificationOutput(final String specificationOutput) {
        this.specificationOutput = specificationOutput;
    }

    /*
     * Generates total specifications from any partial specifications found
     */
    protected void totalSpecificationGenerator(final File dir) throws CodeGenException {
        if (dir == null) {
            throw new CodeGenException("input directory creates null file");
        }
        final File[] providedContextFiles = dir.listFiles((java.io.FileFilter) FileFilterUtils.andFileFilter(FileFilterUtils.fileFileFilter(), FileFilterUtils.nameFileFilter("providedcontext.json")));
        if (providedContextFiles.length == 1) {
            //make directory if it doesn't exist
            final File specificationOutputFile = new File(specificationOutput);
            final File parent = specificationOutputFile.getParentFile();
            if (!parent.isDirectory() && !parent.mkdirs()) {
                throw new CodeGenException("Unable to create directory: " + parent.getPath());
            }
            generateTotalSpecification(dir, specificationOutputFile);
        } else {
            throw new CodeGenException("Incorrect amount of providedcontext.json in specified input folder");
        }
    }

    protected abstract void generateTotalSpecification(final File dir, final File specificationOutputFile) throws CodeGenException;

    public void execute() throws BuildException {
        //  TODO: validate
        final File inputFile = new File(input);
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
                generateFile();
            } catch (final CodeGenException ex) {
                throw new BuildException("Unable to generate code " + ex.toString(), ex);
            }
        }
    }

    protected abstract void generateFile() throws CodeGenException;
}
