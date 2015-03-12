package com.indeed.proctor.consumer.gen.maven;

import java.io.File;
import java.io.IOException;

import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.consumer.gen.CodeGenException;
import com.indeed.proctor.consumer.gen.TestGroupsGenerator;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Created by andrewk on 3/12/15.
 */
public abstract class AbstractProctorMojo extends AbstractMojo {
    abstract File getOutputDirectory();

    abstract File getTopDirectory();

    abstract File getSpecificationOutput();

    protected abstract void processFile(File file, String packageName, String className) throws CodeGenException;

    /*
         * traverse through main specification folder to find large proctor specifications (determined if they have the test
         * attribute)
         */
    private void recursiveSpecificationsFinder(final File dir, final String packageNamePrefix) throws CodeGenException {
        if (dir.equals(null)) {
            throw new CodeGenException("recursiveSpecificationsFinder called with null pointer");
        }
        final File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for(final File entry : files) {
            try {
                if (entry.isDirectory()) {
                    recursiveSpecificationsFinder(entry, (packageNamePrefix == null) ? entry.getName() : packageNamePrefix + "/" + entry.getName());
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
    private void addNonPartialsToResources(final File dir, final Resource resource) throws CodeGenException {
        if (dir.equals(null)) {
            throw new CodeGenException("Could not read from directory " + dir.getPath());
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
                    resource.addInclude(entry.getPath().substring(getTopDirectory().getPath().length() + 1));
                }
            } catch (IOException e) {
                throw new CodeGenException("Could not read from file " + entry.getName(),e);
            }

        }
    }

    /*
         * Finds any partial specifications and creates a generated specification
         */
    void createTotalSpecifications(final File dir, final String packageDirectory) throws MojoExecutionException {

        if (dir.equals(null)) {
            throw new MojoExecutionException("Could not read from directory " + dir.getPath());
        }
        final File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        final File[] providedContextFiles = dir.listFiles((java.io.FileFilter) FileFilterUtils.andFileFilter(FileFilterUtils.fileFileFilter(), FileFilterUtils.nameFileFilter("providedcontext.json")));
        if (providedContextFiles.length == 1) {
            final File parent = providedContextFiles[0].getParentFile();
            final File outputDir = new File(getSpecificationOutput() + File.separator + packageDirectory.substring(0,packageDirectory.lastIndexOf(File.separator)));
            outputDir.mkdirs();
            try {
                generateTotalSpecification(parent, outputDir);
            } catch (final CodeGenException e) {
                throw new MojoExecutionException("Couldn't create total specification",e);
            }
        }
        for (File entry : dir.listFiles()) {
            if (entry.isDirectory()) {
                createTotalSpecifications(entry, (packageDirectory == null) ? entry.getName() : packageDirectory + File.separator + entry.getName());
            }
        }
    }

    abstract void generateTotalSpecification(final File parent, final File outputDir) throws CodeGenException;

    Resource[] getResources() throws MojoExecutionException {
        final Resource resourceNonGenerated = new Resource();
        resourceNonGenerated.setDirectory(getTopDirectory().getPath());
        try {
            addNonPartialsToResources(getTopDirectory(),resourceNonGenerated);
        } catch (CodeGenException e) {
            throw new MojoExecutionException("Couldn't add non partial specifications to resources");
        }
        if (resourceNonGenerated.getIncludes().isEmpty()) {
            resourceNonGenerated.addExclude("**/*");
        }
        Resource resourceGenerated = new Resource();
        final File specificationOutputDir = getSpecificationOutput();
        resourceGenerated.setDirectory(specificationOutputDir.getPath());
        resourceGenerated.addInclude("**/*.json");
        final Resource[] resources = {resourceNonGenerated,resourceGenerated};
        return resources;

    }

    public void execute() throws MojoExecutionException {
        File topDirectory = getTopDirectory();
        if(topDirectory == null) {
            getLog().error("topDirectory not substituted with configured value?");
            return;
        }
        File specificationOutput = getSpecificationOutput();
        try {
            recursiveSpecificationsFinder(topDirectory, null);
            if(specificationOutput!=null) {
                recursiveSpecificationsFinder(specificationOutput,null);
            }
        } catch (final CodeGenException ex) {
            throw new MojoExecutionException("Unable to generate code", ex);
        }
    }
}
