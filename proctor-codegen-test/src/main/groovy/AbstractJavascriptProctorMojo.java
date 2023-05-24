import com.indeed.proctor.common.ProctorUtils;
import com.indeed.proctor.consumer.gen.CodeGenException;
import com.indeed.proctor.consumer.gen.TestGroupsGenerator;
import com.indeed.proctor.consumer.gen.TestGroupsJavascriptGenerator;

import java.io.File;

/**
 * A Gradle task that can combine multiple Proctor specifications and generate a Javascript file.
 *
 * @author andrewk
 */
abstract class AbstractJavascriptProctorMojo extends AbstractProctorGradleTask {

    private final TestGroupsJavascriptGenerator gen = new TestGroupsJavascriptGenerator();

    private final boolean useClosure = false;

    boolean isUseClosure() {
        return useClosure;
    }

    protected void processFile(
            final File file,
            final String packageName,
            final String className
    ) throws CodeGenException {
        gen.generate(
                ProctorUtils.readSpecification(file),
                getOutputDirectory().getPath(),
                packageName,
                className,
                useClosure
        );
    }

    public void generateTotalSpecification(
            final File parent,
            final File outputDir
    ) throws CodeGenException {
        TestGroupsGenerator.makeTotalSpecification(parent, outputDir.getPath());
    }
}
