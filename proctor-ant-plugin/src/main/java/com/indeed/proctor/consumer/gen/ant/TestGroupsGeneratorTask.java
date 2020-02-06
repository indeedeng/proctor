package com.indeed.proctor.consumer.gen.ant;

import com.google.common.base.Strings;
import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.consumer.gen.CodeGenException;
import com.indeed.proctor.consumer.gen.TestGroupsGenerator;
import org.apache.log4j.Logger;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.LogLevel;

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

    /**
     * A period of sleep to display warning messages to users
     */
    private static final int SLEEP_TIME_FOR_WARNING = 3;

    /**
     * URL to the page for the dynamic filters migration
     */
    private static final String DYNAMIC_FILTERS_MIGRATION_URL = "https://github.com/indeedeng/proctor/issues/47";

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

    /**
     * Generates total specifications from any partial specifications found
     * Output generate total specification to `specificationOutput`
     */
    protected ProctorSpecification mergePartialSpecifications(final List<File> files) throws CodeGenException {
        if (files == null || files.size() == 0) {
            throw new CodeGenException("No specifications file input");
        }
        if (Strings.isNullOrEmpty(getSpecificationOutput())) {
            throw new CodeGenException("`specificationOutput` is not given");
        }

        // make directory if it doesn't exist
        new File(specificationOutput.substring(0, specificationOutput.lastIndexOf(File.separator))).mkdirs();
        final File specificationOutputFile = new File(specificationOutput);
        return TestGroupsGenerator.makeTotalSpecification(
                files,
                specificationOutputFile.getParent(),
                specificationOutputFile.getName()
        );
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

            final ProctorSpecification specification;
            if (isSingleSpecificationFile) {
                specification = ProctorUtils.readSpecification(new File(input));
            } else {
                try {
                    specification = mergePartialSpecifications(files);
                } catch (final CodeGenException e) {
                    throw new BuildException("Unable to generate total specification for inputs " + inputs + " : " + e.getMessage(), e);
                }
            }

            validateSpecification(specification);

            try {
                generateSourceFiles(specification);
            } catch (final CodeGenException e) {
                throw new BuildException("Unable to generate source file: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Validate input specification and log messages
     */
    private void validateSpecification(final ProctorSpecification specification) {
        final boolean hasDuplicatedFilters = specification.getDynamicFilters()
                .asCollection()
                .stream()
                .anyMatch(filter -> filter.getClass().isAnnotationPresent(Deprecated.class));

        if (hasDuplicatedFilters) {
            log(String.join("\n",
                    "=================================================================================",
                    "Warning: Proctor detected this application is using deprecated dynamic filters.",
                    "Please migrate to meta tags based filters.",
                    "See " + DYNAMIC_FILTERS_MIGRATION_URL + " for details.",
                    "",
                    "Sleeping " + SLEEP_TIME_FOR_WARNING + " seconds",
                    "================================================================================="
                    ), LogLevel.WARN.getLevel()
            );
        }
    }

    /**
     * Generate source files from given proctor specification
     *
     * @param specification a input specification for source code generation
     */
    protected abstract void generateSourceFiles(final ProctorSpecification specification) throws CodeGenException;
}
