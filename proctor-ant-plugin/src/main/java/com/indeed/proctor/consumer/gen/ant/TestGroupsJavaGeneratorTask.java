package com.indeed.proctor.consumer.gen.ant;

import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.consumer.gen.CodeGenException;
import com.indeed.proctor.consumer.gen.TestGroupsJavaGenerator;

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

    @Override
    protected void generateSourceFiles(final ProctorSpecification specification)
            throws CodeGenException {
        gen.generate(
                specification, target, packageName, groupsClass, groupsManagerClass, contextClass);
    }
}
