package com.indeed.proctor.consumer.gen.maven;

import com.indeed.proctor.consumer.gen.CodeGenException;
import com.indeed.proctor.consumer.gen.TestGroupsJavaGenerator;

import java.io.File;

/**
 * A Mojo that can combine multiple Proctor specifications and generate a Java file.
 *
 * @author andrewk
 */
public abstract class AbstractJavaProctorMojo extends AbstractProctorMojo {

    private final TestGroupsJavaGenerator gen = new TestGroupsJavaGenerator();

    protected void processFile(
            final File file,
            final String packageName,
            final String className
    ) throws CodeGenException {
        getLog().info(String.format("Building resources for %s", packageName));
        gen.generate(
                file.getPath(),
                getOutputDirectory().getPath(),
                packageName,
                className,
                className + "Manager",
                className + "Context"
        );
    }

    protected void generateTotalSpecification(
            final File parent,
            final File outputDir
    ) throws CodeGenException {
        gen.makeTotalSpecification(parent, outputDir.getPath());
    }
}
