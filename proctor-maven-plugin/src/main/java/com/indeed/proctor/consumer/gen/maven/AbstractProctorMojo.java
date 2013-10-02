package com.indeed.proctor.consumer.gen.maven;

import com.indeed.proctor.consumer.gen.CodeGenException;
import com.indeed.proctor.consumer.gen.TestGroupsGenerator;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.File;
import java.io.IOException;

public abstract class AbstractProctorMojo extends AbstractMojo {

    private final TestGroupsGenerator gen = new TestGroupsGenerator();

    abstract File getOutputDirectory();
    abstract File getTopDirectory();

    private JsonNode readJsonFromFile(File input) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readValue(input, JsonNode.class);
        return rootNode;
    }

    private void processFile(File file, String packageName, String className) throws CodeGenException {
        getLog().info(String.format("Building resources for %s", packageName));
        gen.generate(file.getPath(), getOutputDirectory().getPath(), packageName, className, className + "Manager");
    }

    private void searchDirectory(File dir, String packageNamePrefix) throws CodeGenException {
        if(dir.equals(null)) {
            getLog().error("searchDirectory called with null pointer");
            return;
        }
        File[] files = dir.listFiles();
        if(files == null) {
            return;
        }
        for(File entry : files) {
            if(entry.isDirectory()) {
                searchDirectory(entry, (packageNamePrefix == null) ? entry.getName() : packageNamePrefix + "/" + entry.getName());
            } else {
                if(entry.getName().endsWith(".json")) {
                    processFile(
                            entry,
                            packageNamePrefix == null ? "" : packageNamePrefix.replace("/", "."),
                            entry.getName().substring(0, entry.getName().lastIndexOf(".json")));
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
        File topDirectory = getTopDirectory();
        if(topDirectory == null) {
            getLog().error("topDirectory not substituted with configured value?");
            return;
        }
        try {
            searchDirectory(topDirectory, null);
        } catch (CodeGenException ex) {
            throw new MojoExecutionException("Unable to generate code", ex);
        }
    }
}
