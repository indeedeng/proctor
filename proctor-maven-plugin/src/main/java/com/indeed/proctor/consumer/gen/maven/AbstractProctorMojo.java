package com.indeed.proctor.consumer.gen.maven;

import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.consumer.gen.CodeGenException;
import com.indeed.proctor.consumer.gen.TestGroupsGenerator;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractProctorMojo extends AbstractMojo {

    private final TestGroupsGenerator gen = new TestGroupsGenerator();
    private static List<String> accessed;
    abstract File getOutputDirectory();
    abstract File getTopDirectory();

    private void processFile(File file, String packageName, String className) throws CodeGenException {
        getLog().info(String.format("Building resources for %s", packageName));
        gen.generate(file.getAbsolutePath(), getOutputDirectory().getAbsolutePath(), packageName, className, className + "Manager");
    }
    /*
     * traverse through main specification folder to find large proctor specifications (determined if they have the test
     * attribute)
     */
    private void RecursiveSpecificationsFinder(File dir, String packageNamePrefix) throws CodeGenException {
        if (dir.equals(null)) {
            throw new CodeGenException("RecursiveSpecificationsFinder called with null pointer");
        }
        final File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for(File entry : files) {
            try {
                if (entry.isDirectory()) {
                    RecursiveSpecificationsFinder(entry, (packageNamePrefix == null) ? entry.getName() : packageNamePrefix + "/" + entry.getName());
                } else if (entry.getName().endsWith(".json") && ProctorUtils.readJsonFromFile(entry).has("tests")) {
                    processFile(
                            entry,
                            packageNamePrefix == null ? "" : packageNamePrefix.replace("/", "."),
                            entry.getName().substring(0, entry.getName().lastIndexOf(".json")));
                }

            } catch (IOException e) {
                throw new CodeGenException("Could not read from file " + entry.getName(),e);
            }
        }
    }

    /*
     * Adds any non partial specifications to resources
     */
    private void addNonPartialsToResources(File dir, Resource resource) throws CodeGenException {
        if (dir.equals(null)) {
            throw new CodeGenException("Could not read from directory " + dir.getAbsolutePath());
        }
        final File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for(File entry : files) {
            try {
                if (entry.isDirectory()) {
                    addNonPartialsToResources(entry, resource);
                } else if (entry.getName().endsWith(".json") && ProctorUtils.readJsonFromFile(entry).has("tests")) {
                    resource.addInclude(entry.getAbsolutePath().substring(getTopDirectory().getAbsolutePath().length() + 1));
                }
            } catch (IOException e) {
                throw new CodeGenException("Could not read from file " + entry.getName(),e);
            }

        }
    }


    /*
     * Finds any partial specifications and creates a generated specification
     */
    void createTotalSpecifications(File dir) throws MojoExecutionException {

        if (dir.equals(null)) {
            throw new MojoExecutionException("Could not read from directory " + dir.getAbsolutePath());
        }
        final File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File entry : files) {
            if (entry.isDirectory()) {
                createTotalSpecifications(entry);
            } else {
                try {
                    if (entry.getName().endsWith(".json") && !ProctorUtils.readJsonFromFile(entry).has("tests")) {
                        final File parent = entry.getParentFile();
                        if (!accessed.contains(parent.getAbsolutePath())) {
                            gen.makeTotalSpecification(entry.getParentFile(), parent.getParent());
                            accessed.add(parent.getAbsolutePath());
                        }
                    }
                } catch (final CodeGenException e) {
                    throw new MojoExecutionException("Could not create specification ",e);
                } catch (final IOException e) {
                    throw new MojoExecutionException("Could not read file " + entry.getName(),e);
                }
            }
        }
    }

    Resource getResource() throws MojoExecutionException {
        Resource resource = new Resource();
        resource.setDirectory(getTopDirectory().getAbsolutePath());

        final File topDir = new File(getTopDirectory().getPath());
        try {
            addNonPartialsToResources(topDir, resource);
        } catch (CodeGenException e) {
            throw new MojoExecutionException("Unable to read resources", e);
        }

        return resource;
    }

    @Override
    public void execute() throws MojoExecutionException {
        accessed = new ArrayList<String>();
        File topDirectory = getTopDirectory();
        if(topDirectory == null) {
            getLog().error("topDirectory not substituted with configured value?");
            return;
        }
        try {
            RecursiveSpecificationsFinder(topDirectory, null);
        } catch (final CodeGenException ex) {
            throw new MojoExecutionException("Unable to generate code", ex);
        }
    }
}
