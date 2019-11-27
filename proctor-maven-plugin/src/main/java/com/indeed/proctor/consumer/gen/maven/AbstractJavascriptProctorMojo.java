package com.indeed.proctor.consumer.gen.maven;

import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.consumer.gen.CodeGenException;
import com.indeed.proctor.consumer.gen.TestGroupsGenerator;
import com.indeed.proctor.consumer.gen.TestGroupsJavascriptGenerator;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * A Mojo that can combine multiple Proctor specifications and generate a Javascript file.
 *
 * @author andrewk
 */
public abstract class AbstractJavascriptProctorMojo extends AbstractProctorMojo {

    private final TestGroupsJavascriptGenerator gen = new TestGroupsJavascriptGenerator();

    @Parameter(property = "useClosure", defaultValue = "false", required = true)
    private boolean useClosure;

    boolean isUseClosure() {
        return useClosure;
    }

    protected void processFile(
            final File file,
            final String packageName,
            final String className
    ) throws CodeGenException {
        getLog().info(String.format("Building resources for %s", packageName));
        gen.generate(
                ProctorUtils.readSpecification(file),
                getOutputDirectory().getPath(),
                packageName,
                className,
                useClosure
        );
    }

    protected void generateTotalSpecification(
            final File parent,
            final File outputDir
    ) throws CodeGenException {
        TestGroupsGenerator.makeTotalSpecification(parent, outputDir.getPath());
    }
}
