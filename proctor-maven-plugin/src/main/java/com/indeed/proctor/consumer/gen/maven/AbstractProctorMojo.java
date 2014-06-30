package com.indeed.proctor.consumer.gen.maven;

import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.consumer.gen.CodeGenException;
import com.indeed.proctor.consumer.gen.TestGroupsGenerator;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public abstract class AbstractProctorMojo extends AbstractMojo {

    private final TestGroupsGenerator gen = new TestGroupsGenerator();
    private static List<String> accessed;
    abstract File getOutputDirectory();
    abstract File getTopDirectory();

    private void processFile(File file, String packageName, String className) throws CodeGenException {
        getLog().info(String.format("Building resources for %s", packageName));
        gen.generate(file.getPath(), getOutputDirectory().getPath(), packageName, className, className + "Manager");
    }
    /*
     * traverse through main specification folder to find large proctor specifications (determined if they have the test
     * attribute) or individual test specifications (if they do not have this attribute).
     */
    private void searchDirectory(File dir, String packageNamePrefix) throws CodeGenException {
        if (dir.equals(null)) {
            getLog().error("searchDirectory called with null pointer");
            return;
        }
        final File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for(File entry : files) {
            if (entry.isDirectory()) {
                searchDirectory(entry, (packageNamePrefix == null) ? entry.getName() : packageNamePrefix + "/" + entry.getName());
            } else {
                if (entry.getName().endsWith(".json")) {
                    final JsonNode fullTest;
                    try {
                        fullTest = ProctorUtils.readJsonFromFile(entry);
                    } catch (final IOException e) {
                        throw new CodeGenException("Could not read from file " + entry.getAbsolutePath(), e);
                    }
                    if (fullTest.has("tests")) {
                        processFile(
                                entry,
                                packageNamePrefix == null ? "" : packageNamePrefix.replace("/", "."),
                                entry.getName().substring(0, entry.getName().lastIndexOf(".json")));
                    } else {
                        String filePath = entry.getAbsolutePath().substring(0, entry.getAbsolutePath().lastIndexOf(File.separator));
                        if(!accessed.contains(filePath)) {
                            final File newInput = gen.makeTotalSpecification(entry.getParentFile());
                            processFile(
                                    newInput,
                                    packageNamePrefix == null ? "" : packageNamePrefix.substring(0,packageNamePrefix.lastIndexOf(File.separator)).replace(File.separator, "."),
                                    dir.getName()+"Groups");
                            accessed.add(filePath);
                        }
                    }
                }
            }
        }
    }

    Resource getResource() {
        Resource resource = new Resource();
        resource.setDirectory(getTopDirectory().getPath());
        resource.addInclude("**/*.json");
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
            searchDirectory(topDirectory, null);
        } catch (final CodeGenException ex) {
            throw new MojoExecutionException("Unable to generate code", ex);
        }
    }
}
