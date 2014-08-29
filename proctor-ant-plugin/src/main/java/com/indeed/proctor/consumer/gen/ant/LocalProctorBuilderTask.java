package com.indeed.proctor.consumer.gen.ant;

import com.indeed.proctor.builder.LocalProctorBuilder;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * Executes the LocalProctorBuilder
 * destdir  -   The output directory to write the matrix json file.
 *              If set to '-', output is written to STDOUT
 * destfile -   The name of the generated test-matrix JSON file.
 * srcdir   -   The source directory containing the test-definitions directory
 *              ${srcdir}/test-definitions
 * author   -   Optional author string to be used for the test-matrix audit.author
 * version  -   Optional version to be used for the test-matrix audit.version
 * @author parker
 */
public class LocalProctorBuilderTask extends Task {
    private String destdir;
    private String destfile = "proctor-tests-matrix.json";
    private String srcdir;

    // Artifact overrides
    private String author = null;
    private String version = null;

    public String getDestdir() {
        return destdir;
    }

    public void setDestdir(String destdir) {
        this.destdir = destdir;
    }

    public String getSrcdir() {
        return srcdir;
    }

    public void setSrcdir(String srcdir) {
        this.srcdir = srcdir;
    }

    public String getDestfile() {
        return destfile;
    }

    public void setDestfile(String destfile) {
        this.destfile = destfile;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public void execute() throws BuildException {
        try {
            if(srcdir == null) {
                throw new BuildException("srcdir is required");
            }
            if(destdir == null) {
                throw new BuildException("destdir is required");
            }
            new LocalProctorBuilder(
                    new File(srcdir),
                    "-".equals(destdir) ?
                        new PrintWriter(System.out) :
                        new FileWriter(new File(destdir, destfile)),
                    author,
                    version).execute();
        } catch (Exception e) {
            if(e instanceof BuildException) {
                throw (BuildException) e;
            }
            throw new BuildException("Failed to create test matrix: " + e.getMessage(), e);
        }

    }
}
