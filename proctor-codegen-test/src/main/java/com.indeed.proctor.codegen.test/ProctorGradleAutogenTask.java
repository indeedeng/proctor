package com.indeed.proctor.codegen.test;

import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.consumer.gen.CodeGenException;
import com.indeed.proctor.consumer.gen.TestGroupsGenerator;
import com.indeed.proctor.consumer.gen.TestGroupsJavaGenerator;
import com.indeed.proctor.consumer.gen.TestGroupsJavascriptGenerator;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;

import java.io.File;
import java.io.IOException;

/**
 * A Java application that can combine multiple Proctor specifications and generate some type of
 * file. Used to test code generation in proctor-codegen. Generate Java and JS code then clean up.
 * This is minimal viable solution. During upgrade from maven to gradle turned maven MOJO gen
 * plugins into a java app that accomplishes the use needed for proctor OSS. Instead of duplicating
 * proctor-gradle-plugin just generate the code needed for testing proctor-codegen and clean up
 * after.
 *
 * @author andrewk / zgoodwin
 */
public abstract class ProctorGradleAutogenTask {
    private static final File outputDirectory = new File("../proctor-codegen-test/src/test/");
    private static final File topDirectory = new File("../proctor-codegen-test/src/main");
    private static final File specificationOutput =
            new File("../proctor-codegen-test/src/test/resources/com.indeed.proctor.groups");
    private static final File resourceOutput =
            new File("../proctor-codegen-test/src/test/resources");
    private static final boolean useClosure = false;
    private static final TestGroupsJavaGenerator javaGenerator = new TestGroupsJavaGenerator();
    private static final TestGroupsJavascriptGenerator javascriptGenerator =
            new TestGroupsJavascriptGenerator();

    protected static void processJavaFile(
            final File file, final String packageName, final String className)
            throws CodeGenException {
        javaGenerator.generate(
                ProctorUtils.readSpecification(file),
                outputDirectory.getPath(),
                packageName,
                className,
                className + "Manager",
                className + "Context");
    }

    protected static void processJavaScriptFile(final File file, final String className)
            throws CodeGenException {
        javascriptGenerator.generate(
                ProctorUtils.readSpecification(file),
                resourceOutput.getPath(),
                "com.indeed.proctor.groups",
                className,
                useClosure);
    }

    public static void generateTotalSpecification(final File parent, final File outputDir)
            throws CodeGenException {
        TestGroupsGenerator.makeTotalSpecification(parent, outputDir.getPath());
    }

    /*
     * traverse through main specification folder to find large proctor specifications (determined if they have the test
     * attribute)
     */
    private static void recursiveSpecificationsFinder(
            final File dir, final String packageNamePrefix) throws CodeGenException {
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
                    recursiveSpecificationsFinder(
                            entry,
                            (packageNamePrefix == null)
                                    ? entry.getName()
                                    : packageNamePrefix + "/" + entry.getName());
                } else if (entry.getName().endsWith(".json")
                        && ProctorUtils.readJsonFromFile(entry).has("tests")) {
                    processJavaFile(
                            entry,
                            packageNamePrefix == null ? "" : packageNamePrefix.replace("/", "."),
                            entry.getName().substring(0, entry.getName().lastIndexOf(".json")));
                    processJavaScriptFile(
                            entry,
                            entry.getName().substring(0, entry.getName().lastIndexOf(".json")));
                }

            } catch (final IOException e) {
                throw new CodeGenException("Could not read from file " + entry.getName(), e);
            }
        }
    }

    /*
     * Finds any partial specifications and creates a generated specification
     */
    static void createTotalSpecifications(final File dir, final String packageDirectory)
            throws RuntimeException {
        if (dir.equals(null)) {
            throw new RuntimeException("Could not read from directory " + dir.getPath());
        }
        final File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        final File[] providedContextFiles =
                dir.listFiles(
                        (java.io.FileFilter)
                                FileFilterUtils.andFileFilter(
                                        FileFilterUtils.fileFileFilter(),
                                        FileFilterUtils.nameFileFilter("providedcontext.json")));
        if (providedContextFiles.length == 1) {
            final File parent = providedContextFiles[0].getParentFile();
            final File outputDir =
                    new File(
                            specificationOutput
                                    + File.separator
                                    + packageDirectory.substring(
                                            0, packageDirectory.lastIndexOf(File.separator)));
            outputDir.mkdirs();
            try {
                generateTotalSpecification(parent, outputDir);
            } catch (final CodeGenException e) {
                throw new RuntimeException("Couldn't create total specification", e);
            }
        }
        for (final File entry : dir.listFiles()) {
            if (entry.isDirectory()) {
                createTotalSpecifications(
                        entry,
                        (packageDirectory == null)
                                ? entry.getName()
                                : packageDirectory + File.separator + entry.getName());
            }
        }
    }

    public static void main(final String[] args) throws IOException {
        try {
            FileUtils.deleteDirectory(
                    FileUtils.getFile("src/test/resources/com/indeed/proctor/groups/temp"));
        } catch (final Exception e) {
        }

        createTotalSpecifications(topDirectory, null);
        if (topDirectory == null) {
            return;
        }
        try {
            recursiveSpecificationsFinder(topDirectory, null);
            if (specificationOutput != null) {
                recursiveSpecificationsFinder(specificationOutput, null);
            }
        } catch (final CodeGenException ex) {
            throw new RuntimeException("Unable to generate code", ex);
        }
        final File dest = new File("src/test/resources/com/indeed/proctor/groups/temp");
        FileUtils.moveFileToDirectory(
                new File(
                        "src/test/resources/com.indeed.proctor.groups/java/com.indeed.proctor.codegen.test/groups/SplitSpecificationTestGroups.json"),
                dest,
                true);
        FileUtils.moveFileToDirectory(
                new File(
                        "src/test/resources/com.indeed.proctor.groups/java/com.indeed.proctor.codegen.test/groups/SplitSpecificationTestWithFiltersGroups.json"),
                dest,
                false);
        FileUtils.deleteDirectory(
                FileUtils.getFile("src/test/resources/com.indeed.proctor.groups"));
    }
}
