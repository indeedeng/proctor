package com.indeed.proctor.consumer.gen.ant;

import com.indeed.proctor.consumer.gen.CodeGenException;
import com.indeed.proctor.consumer.gen.TestGroupsJavaGenerator;

import java.io.File;

/**
 * Ant task for generating Java Proctor test groups files.
 *
 * @author andrewk
 */
public class TestGroupsJavaGeneratorTask extends TestGroupsGeneratorTask {
    private final TestGroupsJavaGenerator gen = new TestGroupsJavaGenerator();

    private String groupsManagerClass;
    private String contextClass;

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

    protected void generateTotalSpecification(final File dir, final File specificationOutputFile) throws CodeGenException {
        final File output = gen.makeTotalSpecification(dir, specificationOutputFile.getParent(), specificationOutputFile.getName());
        gen.generate(output.getPath(), target, packageName, groupsClass, groupsManagerClass, contextClass);
    }

    protected void generateFile() throws CodeGenException {
        gen.generate(input, target, packageName, groupsClass, groupsManagerClass, contextClass);
    }
}
