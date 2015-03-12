package com.indeed.proctor.consumer.gen.maven;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;

@Mojo(name = "generate-test-js", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES)
public class JavascriptProctorTestMojo extends AbstractJavaProctorMojo {

    @Parameter(property = "project", defaultValue = "${project}", required = true)
    private MavenProject project;

    @Parameter(property = "topDirectory", defaultValue = "${basedir}/src/test/proctor", required = true)
    private File topDirectory;

    File getTopDirectory() {
        return topDirectory;
    }

    @Parameter(property = "outputDirectory", defaultValue = "js/generated-test-sources/proctor", required = true)
    protected File outputDirectory;

    File getOutputDirectory() {
        return outputDirectory;
    }

    @Parameter(property = "specificationOutput", defaultValue = "${project.build.directory}/generated-test-resources/proctor", required = true)
    private File specificationOutput;

    File getSpecificationOutput() {
        return specificationOutput;
    }

    public void execute() throws MojoExecutionException {
        project.addTestCompileSourceRoot(getOutputDirectory().getPath());
        super.createTotalSpecifications(getTopDirectory(),null);
        final Resource[] resources = getResources();
        for(final Resource resource : resources) {
            project.addTestResource(resource);
        }
        super.execute();
    }
}
