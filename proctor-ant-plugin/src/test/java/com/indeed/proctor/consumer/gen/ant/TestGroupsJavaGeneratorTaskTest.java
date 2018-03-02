package com.indeed.proctor.consumer.gen.ant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.common.TestSpecification;
import com.indeed.proctor.common.dynamic.DynamicFilters;
import com.indeed.proctor.common.dynamic.TestNamePrefixFilter;
import org.apache.tools.ant.BuildException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestGroupsJavaGeneratorTaskTest {
    private static final String PACKAGE_NAME = "gentst";
    private static final String GROUPS_CLASS = "groups";
    private static final String GROUPS_MANAGER_CLASS = "groupsManager";
    private static final String CONTEXT_CLASS = "context";
    private final ProctorSpecification specification = new ProctorSpecification();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Rule
    public TemporaryFolder inputDirectory = new TemporaryFolder();
    @Rule
    public TemporaryFolder outputDirectory = new TemporaryFolder();
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testSingleSpecification() throws IOException {
        final File singleSpecification = writeSingleSpecification(specification);
        execute(
                singleSpecification.getPath(),
                outputDirectory.getRoot().getPath(),
                null
        );
        assertAllGenerated(outputDirectory.getRoot());
    }

    @Test
    public void testNoInput() {
        thrown.expect(BuildException.class);
        execute(
                null,
                outputDirectory.getRoot().getPath(),
                null
        );
    }

    @Test
    public void testNoOutput() throws IOException {
        final File singleSpecification = writeSingleSpecification(specification);
        thrown.expect(BuildException.class);
        execute(
                singleSpecification.getPath(),
                null,
                null
        );
    }

    @Test
    public void testSingleSpecificationWithOutput() throws IOException {
        final File singleSpecification = writeSingleSpecification(specification);
        final File dummyOutput = new File(outputDirectory.getRoot(), "dummy.json");
        execute(
                singleSpecification.getPath(),
                outputDirectory.getRoot().getPath(),
                dummyOutput.getPath()
        );
        assertAllGenerated(outputDirectory.getRoot());
        assertFalse("specification output won't be generated for a single specification", dummyOutput.isFile());
    }

    @Test
    public void testMultiSpecifications() throws IOException {
        final File inputRoot = writeMultiSpecifications("spec", specification, false);
        final File outputSpecification = new File(outputDirectory.getRoot(), "spec.json");
        execute(
                inputRoot.getPath(),
                outputDirectory.getRoot().getPath(),
                outputSpecification.getPath()
        );
        assertAllGenerated(outputDirectory.getRoot());
        assertTrue("specification output should be generated", outputSpecification.isFile());
    }

    @Test
    public void testMultiSpecificationsWithFilter() throws IOException {
        final File inputRoot = writeMultiSpecifications("spec", specification, true);
        final File outputSpecification = new File(outputDirectory.getRoot(), "spec.json");
        execute(
                inputRoot.getPath(),
                outputDirectory.getRoot().getPath(),
                outputSpecification.getPath()
        );
        assertAllGenerated(outputDirectory.getRoot());
        assertTrue("specification output should be generated", outputSpecification.isFile());
    }

    @Test
    public void testMultiSpecificationsWithoutOutput() throws IOException {
        final File inputRoot = writeMultiSpecifications("spec", specification, true);
        thrown.expect(BuildException.class);
        execute(
                inputRoot.getPath(),
                outputDirectory.getRoot().getPath(),
                null
        );
    }

    @Before
    public void setUp() {
        final TestSpecification testSpecification = new TestSpecification();
        testSpecification.setBuckets(
                ImmutableMap.of(
                        "inactive", -1,
                        "control", 0,
                        "active", 1
                )
        );
        specification.setTests(
                ImmutableMap.of(
                        "one", testSpecification,
                        "three", testSpecification
                )
        );
        specification.setProvidedContext(
                ImmutableMap.of(
                        "country", "String"
                )
        );
        specification.setDynamicFilters(
                new DynamicFilters(
                        Collections.singletonList(new TestNamePrefixFilter("two"))
                )
        );
    }

    private void assertAllGenerated(final File root) {
        final File packageRoot = new File(root, PACKAGE_NAME);
        assertTrue("package root direct should be created", packageRoot.isDirectory());
        final File groupsClass = new File(packageRoot, GROUPS_CLASS + ".java");
        assertTrue("groups class should be generated", groupsClass.isFile());
        final File groupsManagerClass = new File(packageRoot, GROUPS_MANAGER_CLASS + ".java");
        assertTrue("groups manager class should be generated", groupsManagerClass.isFile());
        final File contextClass = new File(packageRoot, CONTEXT_CLASS + ".java");
        assertTrue("context class should be generated", contextClass.isFile());
    }

    private File writeSingleSpecification(final ProctorSpecification specification) throws IOException {
        final File file = inputDirectory.newFile("specification.json");
        objectMapper.writeValue(file, specification);
        return file;
    }

    private File writeMultiSpecifications(
            final String directoryName,
            final ProctorSpecification specification,
            final boolean writeFilter
    ) throws IOException {
        final File root = inputDirectory.newFolder(directoryName);
        final File providedContextFile = new File(root, "providedcontext.json");
        providedContextFile.createNewFile();
        objectMapper.writeValue(providedContextFile, specification.getProvidedContext());
        if (writeFilter) {
            final File dynamicFiltersFile = new File(root, "dynamicfilters.json");
            dynamicFiltersFile.createNewFile();
            objectMapper.writeValue(dynamicFiltersFile, specification.getDynamicFilters());
        }
        for (final Map.Entry<String, TestSpecification> entry : specification.getTests().entrySet()) {
            final String testName = entry.getKey();
            final TestSpecification testSpecification = entry.getValue();
            final File testSpecificationFile = new File(root, testName);
            testSpecificationFile.createNewFile();
            objectMapper.writeValue(testSpecificationFile, testSpecification);
        }
        return root;
    }

    private void execute(
            final String input,
            final String target,
            final String specificationOutput
    ) {
        execute(
                input,
                target,
                specificationOutput,
                PACKAGE_NAME,
                GROUPS_CLASS,
                GROUPS_MANAGER_CLASS,
                CONTEXT_CLASS
        );
    }

    private void execute(
            final String input,
            final String target,
            final String specificationOutput,
            final String packageName,
            final String groupsClass,
            final String groupsManagerClass,
            final String contextClass
    ) {
        final TestGroupsJavaGeneratorTask task = new TestGroupsJavaGeneratorTask();
        if (input != null) {
            task.setInput(input);
        }
        if (target != null) {
            task.setTarget(target);
        }
        if (packageName != null) {
            task.setPackageName(packageName);
        }
        if (groupsClass != null) {
            task.setGroupsClass(groupsClass);
        }
        if (groupsManagerClass != null) {
            task.setGroupsManagerClass(groupsManagerClass);
        }
        if (contextClass != null) {
            task.setContextClass(contextClass);
        }
        if (specificationOutput != null) {
            task.setSpecificationOutput(specificationOutput);
        }
        task.execute();
    }
}