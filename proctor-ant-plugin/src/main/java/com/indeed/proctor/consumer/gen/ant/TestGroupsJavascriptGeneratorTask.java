package com.indeed.proctor.consumer.gen.ant;

import com.indeed.proctor.consumer.gen.CodeGenException;
import com.indeed.proctor.consumer.gen.TestGroupsJavascriptGenerator;
import org.apache.log4j.Logger;

import java.io.File;

/**
 * Ant task for generating Javascript Proctor test groups files.
 *
 * @author andrewk
 */
public class TestGroupsJavascriptGeneratorTask extends TestGroupsGeneratorTask {
    private static final Logger LOGGER = Logger.getLogger(TestGroupsJavascriptGeneratorTask.class);
    private final TestGroupsJavascriptGenerator gen = new TestGroupsJavascriptGenerator();

    private boolean useClosure;

    public boolean isUseClosure() {
        return useClosure;
    }

    public void setUseClosure(final boolean useClosure) {
        this.useClosure = useClosure;
    }

    protected void generateTotalSpecification(final File dir, final File specificationOutputFile) throws CodeGenException {
        final File output = gen.makeTotalSpecification(dir, specificationOutputFile.getParent(), specificationOutputFile.getName());
        gen.generate(output.getPath(),target,packageName,groupsClass, useClosure);
    }

    protected void generateFile() throws CodeGenException {
        gen.generate(input, target, packageName, groupsClass, useClosure);
    }
}
