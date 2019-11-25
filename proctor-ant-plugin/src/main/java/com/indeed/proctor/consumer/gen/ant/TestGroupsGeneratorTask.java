package com.indeed.proctor.consumer.gen.ant;

import com.google.common.base.Strings;
import com.indeed.proctor.consumer.gen.CodeGenException;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.indeed.proctor.consumer.gen.TestGroupsGenerator.DYNAMIC_FILTERS_FILENAME;
import static com.indeed.proctor.consumer.gen.TestGroupsGenerator.PROVIDED_CONTEXT_FILENAME;

/**
 * Ant task for generating Proctor test groups files.
 *
 * @author andrewk
 */
public abstract class TestGroupsGeneratorTask extends Task {
    protected static final Logger LOGGER = Logger.getLogger(TestGroupsGeneratorTask.class);
    /**
     * Paths to input specification files separated by ","
     * It allows two types of inputs
     *
     * 1. A single path to json file that is not named
     * providedcontext.json or dynamicfilters.json
     * The json file holds all tests specifications (total specification).
     *
     * 2. All other cases. one or more paths to json file
     * or directory including json files, separated by comma.
     * each json file is a single test specification
     * or providedcontext.json or dynamicfilters.json.
     *
     * FIXME: current implementation is not strictly following this at this moment.
     * Current limitations are
     * * single path to "providedcontext.json" is handled as Type 1.
     * * single path to "dynamicfilters.json" is handled as Type 1.
     * * multiple paths to json files is handled as Type 1 and fails to parse.
     */
    protected String input;
    /**
     * Target base directory to generate files
     */
    protected String target;
    /**
     * Package of generated source files
     */
    protected String packageName;
    /**
     * Name of generated group class
     */
    protected String groupsClass;
    /**
     * Paths to generate a total specification json file.
     * This is ignored when `input` specifies total specification json file.
     */
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
    protected void totalSpecificationGenerator(final List<File> files) throws CodeGenException {
        if (files == null || files.size() == 0) {
            throw new CodeGenException("No specifications file input");
        }
        final List<File> providedContextFiles = new ArrayList<>();
        final List<File> dynamicFiltersFiles = new ArrayList<>();
        for (final File file : files) {
            if (PROVIDED_CONTEXT_FILENAME.equals(file.getName())) {
                providedContextFiles.add(file);
            } else if (DYNAMIC_FILTERS_FILENAME.equals(file.getName())) {
                dynamicFiltersFiles.add(file);
            }
        }
        if (providedContextFiles.size() != 1) {
            throw new CodeGenException("Incorrect amount of " + PROVIDED_CONTEXT_FILENAME + " in specified input folder");
        } else if (dynamicFiltersFiles.size() > 1) {
            throw new CodeGenException("Incorrect amount of " + DYNAMIC_FILTERS_FILENAME + " in specified input folder");
        } else {
            //make directory if it doesn't exist
            new File(specificationOutput.substring(0, specificationOutput.lastIndexOf(File.separator))).mkdirs();
            final File specificationOutputFile = new File(specificationOutput);
            generateTotalSpecification(files, specificationOutputFile);
        }
    }

    @Override
    public void execute() throws BuildException {
        if (input == null) {
            throw new BuildException("Undefined input files for code generation from specification");
        }
        if (target == null) {
            throw new BuildException("Undefined target directory for code generation from specification");
        }

        final String[] inputs = input.split(",");

        if (inputs.length == 0) {
            LOGGER.error("input shouldn't be empty");
            return;
        } else {
            final List<File> files = new ArrayList<>();

            boolean isSingleSpecificationFile = true;
            for (final String input : inputs) {
                final File inputFile = new File(input.trim());
                if (inputFile == null) {
                    LOGGER.error("input not substituted with configured value");
                    return;
                }
                if (inputFile.isDirectory()) {
                    files.addAll(Arrays.asList(inputFile.listFiles()));
                    isSingleSpecificationFile = false;
                } else {
                    files.add(inputFile);
                }
            }
            if (isSingleSpecificationFile) {
                try {
                    generateFile();
                } catch (final CodeGenException ex) {
                    throw new BuildException("Unable to generate code: " + ex.getMessage(), ex);
                }
            } else {
                if (!Strings.isNullOrEmpty(getSpecificationOutput())) {
                    try {
                        totalSpecificationGenerator(files);
                    } catch (final CodeGenException e) {
                        throw new BuildException("Unable to generate total specification: " + e.getMessage(), e);
                    }
                } else {
                    throw new BuildException("Undefined output folder for generated specification");
                }
            }
        }
    }

    protected abstract void generateTotalSpecification(
            final List<File> files,
            final File specificationOutputFile
    ) throws CodeGenException;

    protected abstract void generateFile() throws CodeGenException;
}
