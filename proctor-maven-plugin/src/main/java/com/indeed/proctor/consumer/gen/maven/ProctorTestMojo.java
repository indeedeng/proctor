package com.indeed.proctor.consumer.gen.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;

@Mojo(name = "generate-test", defaultPhase = LifecyclePhase.GENERATE_TEST_SOURCES)
public class ProctorTestMojo extends AbstractProctorMojo {

    @Parameter(property = "project", defaultValue = "${project}", required = true)
    private MavenProject project;

    @Parameter(property = "topDirectory", defaultValue = "${basedir}/src/test/proctor", required = true)
    private File topDirectory;

    File getTopDirectory() {
        return topDirectory;
    }

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/generated-test-sources/proctor", required = true)
    protected File outputDirectory;

    File getOutputDirectory() {
        return outputDirectory;
    }

    public void execute() throws MojoExecutionException {
        project.addTestCompileSourceRoot(getOutputDirectory().getPath());
        project.addTestResource(getResource());
        super.execute();
    }
}
