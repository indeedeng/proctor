package com.indeed.proctor.consumer.gen.ant;

import com.indeed.proctor.consumer.gen.CodeGenException;
import com.indeed.proctor.consumer.gen.TestGroupsGenerator;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

public class TestGroupsGeneratorTask extends Task {
    private final TestGroupsGenerator gen = new TestGroupsGenerator();

    private String input;
    private String target;
    private String packageName;
    private String groupsClass;
    private String groupsManagerClass;

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

    @Override
    public void execute() throws BuildException {
        //  TODO: validate  
        try {
            gen.generate(input, target, packageName, groupsClass, groupsManagerClass);
        } catch (CodeGenException ex) {
            throw new BuildException(ex.toString(), ex);
        }
    }
}
