package com.indeed.proctor.codegen.test;

import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.consumer.gen.CodeGenException;
import com.indeed.proctor.consumer.gen.TestGroupsGenerator;
import com.indeed.proctor.consumer.gen.TestGroupsJavaGenerator;
import com.indeed.proctor.consumer.gen.TestGroupsJavascriptGenerator;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

import java.io.File;
import java.io.IOException;

/**
 * A Mojo that can combine multiple Proctor specifications and generate some type of file.
 *
 * @author andrewk
 */
public abstract class ProctorGradleAutogenTask extends DefaultTask {
    private static final File outputDirectory = new File("./");
    private static final File topDirectory = new File("../test/proctor");
    private static final File specificationOutput = new File("../test/resources");
    private static final boolean useClosure = false;
    private final TestGroupsJavaGenerator gen = new TestGroupsJavaGenerator();
    private static final TestGroupsJavaGenerator javaGenerator = new TestGroupsJavaGenerator();
    private static final TestGroupsJavascriptGenerator javascriptGenerator = new TestGroupsJavascriptGenerator();

    boolean isUseClosure() {
        return useClosure;
    }
    protected static void processJavaFile(
            final File file,
            final String packageName,
            final String className
    ) throws CodeGenException {
        javaGenerator.generate(
                ProctorUtils.readSpecification(file),
                outputDirectory.getPath(),
                packageName,
                className,
                className + "Manager",
                className + "Context"
        );
    }
    protected static void processJavaScriptFile(
            final File file,
            final String packageName,
            final String className
    ) throws CodeGenException {
        javascriptGenerator.generate(
                ProctorUtils.readSpecification(file),
                outputDirectory.getPath(),
                packageName,
                className,
                useClosure
        );
    }

    public static void generateTotalSpecification(
            final File parent,
            final File outputDir
    ) throws CodeGenException {
        TestGroupsGenerator.makeTotalSpecification(parent, outputDir.getPath());
    }

    /*
     * traverse through main specification folder to find large proctor specifications (determined if they have the test
     * attribute)
     */
    private static void recursiveSpecificationsFinder(final File dir, final String packageNamePrefix) throws CodeGenException {
        if (dir.equals(null)) {
            throw new CodeGenException("recursiveSpecificationsFinder called with null pointer");
        }
        final File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (final File entry : files) {
            try {
                if (entry.isDirectory()) {
                    recursiveSpecificationsFinder(entry, (packageNamePrefix == null) ? entry.getName() : packageNamePrefix + "/" + entry.getName());
                } else if (entry.getName().endsWith(".json") && ProctorUtils.readJsonFromFile(entry).has("tests")) {
                    processJavaFile(
                            entry,
                            packageNamePrefix == null ? "" : packageNamePrefix.replace("/", "."),
                            entry.getName().substring(0, entry.getName().lastIndexOf(".json")));
                    processJavaScriptFile(
                            entry,
                            packageNamePrefix == null ? "" : packageNamePrefix.replace("/", "."),
                            entry.getName().substring(0, entry.getName().lastIndexOf(".json")));
                }

            } catch (final IOException e) {
                throw new CodeGenException("Could not read from file " + entry.getName(),e);
            }
        }
    }

    /*
     * Finds any partial specifications and creates a generated specification
     */
    static void createTotalSpecifications(final File dir, final String packageDirectory) throws RuntimeException {

        if (dir.equals(null)) {
            throw new RuntimeException("Could not read from directory " + dir.getPath());
        }
        final File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        final File[] providedContextFiles = dir.listFiles((java.io.FileFilter) FileFilterUtils.andFileFilter(FileFilterUtils.fileFileFilter(), FileFilterUtils.nameFileFilter("providedcontext.json")));
        if (providedContextFiles.length == 1) {
            final File parent = providedContextFiles[0].getParentFile();
            final File outputDir = new File(specificationOutput + File.separator + packageDirectory.substring(0,packageDirectory.lastIndexOf(File.separator)));
            outputDir.mkdirs();
            try {
                generateTotalSpecification(parent, outputDir);
            } catch (final CodeGenException e) {
                throw new RuntimeException("Couldn't create total specification",e);
            }
        }
        for (final File entry : dir.listFiles()) {
            if (entry.isDirectory()) {
                createTotalSpecifications(entry, (packageDirectory == null) ? entry.getName() : packageDirectory + File.separator + entry.getName());
            }
        }
    }

    public static void main(final String[] args) {
        final String testCompileSourceRoot = outputDirectory.getPath();
        createTotalSpecifications(topDirectory,null);
        if (topDirectory == null) {
            return;
        }
        try {
            recursiveSpecificationsFinder(topDirectory, null);
            if (specificationOutput!=null) {
                recursiveSpecificationsFinder(specificationOutput,null);
            }
        } catch (final CodeGenException ex) {
            throw new RuntimeException("Unable to generate code", ex);
        }
    }
}