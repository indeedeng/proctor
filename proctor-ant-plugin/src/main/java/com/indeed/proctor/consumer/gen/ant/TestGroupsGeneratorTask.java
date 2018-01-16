package com.indeed.proctor.consumer.gen.ant;

import com.google.common.base.Strings;
import com.indeed.proctor.consumer.gen.CodeGenException;
import org.apache.log4j.Logger;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    /**
     * Use {@link TestGroupsGeneratorTask#totalSpecificationGenerator(List)} instead
     * @deprecated
     */
    @Deprecated
    protected void totalSpecificationGenerator(final File dir) throws CodeGenException {
        totalSpecificationGenerator(Arrays.asList(dir.listFiles()));
    }
    /*
     * Generates total specifications from any partial specifications found
     */
    protected void totalSpecificationGenerator(final List<File> files) throws CodeGenException {
        if (files == null || files.size() == 0) {
            throw new CodeGenException("No specifications file input");
        }
        final List<File> providedContextFiles =  new ArrayList<>();
        for (final File file : files) {
            if ("providedcontext.json".equals(file.getName())) {
                providedContextFiles.add(file);
            }
        }
        if (providedContextFiles.size() == 1) {
            //make directory if it doesn't exist
            (new File(specificationOutput.substring(0, specificationOutput.lastIndexOf(File.separator)))).mkdirs();
            final File specificationOutputFile = new File(specificationOutput);
            generateTotalSpecification(files, specificationOutputFile);
        } else {
            throw new CodeGenException("Incorrect amount of providedcontext.json in specified input folder");
        }
    }

    /**
     * Use {@link TestGroupsGeneratorTask#generateTotalSpecification(List, File)} instead
     * @deprecated
     */
    @Deprecated
    protected abstract void generateTotalSpecification(final File dir, final File specificationOutputFile) throws CodeGenException;

    protected abstract void generateTotalSpecification(final List<File> files, final File specificationOutputFile) throws CodeGenException;

    public void execute() throws BuildException {
        final String[] inputs = input.split(",");

        if (inputs.length == 0) {
            LOGGER.error("input shouldn't be empty");
            return;
        } else {
            final List<File> files = new ArrayList<>();

            for (final String input : inputs) {
                final File inputFile = new File(input);
                if (inputFile == null) {
                    LOGGER.error("input not substituted with configured value");
                    return;
                }
                if (inputFile.isDirectory()) {
                    files.addAll(Arrays.asList(inputFile.listFiles()));
                } else {
                    files.add(inputFile);
                }
            }

            if (files.size() == 1) {
                try {
                    generateFile();
                } catch (final CodeGenException ex) {
                    throw new BuildException("Unable to generate code " + ex.toString(), ex);
                }
            } else {
                if (!Strings.isNullOrEmpty(getSpecificationOutput())) {
                    try {
                        totalSpecificationGenerator(files);
                    } catch (final CodeGenException e) {
                        throw new BuildException("Could not create total specification", e);
                    }
                } else {
                    throw new BuildException("Undefined output folder for generated specification");
                }
            }
        }
    }

    protected abstract void generateFile() throws CodeGenException;
}
