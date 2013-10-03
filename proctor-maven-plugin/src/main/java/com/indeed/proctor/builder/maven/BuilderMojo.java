package com.indeed.proctor.builder.maven;

import com.indeed.proctor.builder.LocalProctorBuilder;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileWriter;

@Mojo(name = "generate-matrix")
public class BuilderMojo extends AbstractMojo {

    @Parameter(property = "topDirectory", defaultValue = "${basedir}/src/main/proctor-data")
    private File topDirectory;

    @Parameter(property = "outputFile", defaultValue = "${project.build.directory}/proctor-test-matrix.json")
    private File outputFile;

    @Parameter(property = "author", defaultValue = "")
    private String author;

    @Parameter(property = "version", defaultValue = "-1")
    private long version;

    @Override
    public final void execute() throws MojoExecutionException {
        if(!topDirectory.exists()) {
            return;
        }
        try {
            new LocalProctorBuilder(topDirectory, new FileWriter(outputFile), "".equals(author) ? author : null, version).execute();
        } catch (Exception e) {
            throw new MojoExecutionException("Failure during builder execution", e);
        }
    }
}
