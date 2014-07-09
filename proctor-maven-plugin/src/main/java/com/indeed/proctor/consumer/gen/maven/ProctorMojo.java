package com.indeed.proctor.consumer.gen.maven;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class ProctorMojo extends AbstractProctorMojo {

    @Parameter(property = "project", defaultValue = "${project}", required = true)
    private MavenProject project;

    @Parameter(property = "topDirectory", defaultValue = "${basedir}/src/main/proctor", required = true)
    private File topDirectory;

    File getTopDirectory() {
        return topDirectory;
    }

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/generated-sources/proctor", required = true)
    private File outputDirectory;

    File getOutputDirectory() {
        return outputDirectory;
    }

    @Override
    public void execute() throws MojoExecutionException {
        project.addCompileSourceRoot(getOutputDirectory().getPath());
        super.createTotalSpecifications(getTopDirectory());
        project.addResource(getResource());
        super.execute();
    }
}
