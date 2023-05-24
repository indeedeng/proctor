import com.indeed.proctor.consumer.gen.CodeGenException;
import org.apache.maven.model.Resource;
import org.apache.maven.project.MavenProject;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

class JavaProctorTestMojo extends AbstractJavaProctorMojo {

    @Parameter(property = "project", defaultValue = "${project}", required = true)
    private static MavenProject project;

    @Parameter(property = "topDirectory", defaultValue = "${basedir}/src/main/proctor", required = true)
    private static File topDirectory;

    static File getTopDirectory() {
        return topDirectory;
    }

    @Parameter(property = "outputDirectory", defaultValue = "${project.build.directory}/generated-sources/proctor", required = true)
    private static File outputDirectory;

    static File getOutputDirectory() {
        return outputDirectory;
    }

    @Parameter(property = "specificationOutput", defaultValue = "${project.build.directory}/generated-resources/proctor", required = true)
    private static File specificationOutput;

    static File getSpecificationOutput() {
        return specificationOutput;
    }
    public static void main(final String[] args) throws IllegalArgumentException, CodeGenException {
        project.addTestCompileSourceRoot(getOutputDirectory().getPath());
        createTotalSpecifications(getTopDirectory(),null);
        final Resource[] resources = getResources();
        for (final Resource resource : resources) {
            project.addTestResource(resource);
        }
        exec();
    }
}
