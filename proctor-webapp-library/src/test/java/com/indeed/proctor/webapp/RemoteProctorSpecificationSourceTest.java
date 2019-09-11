package com.indeed.proctor.webapp;

import com.indeed.proctor.webapp.model.SpecificationResult;
import org.junit.Test;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author yiqing
 */

public class RemoteProctorSpecificationSourceTest {
    @Test
    public void testExportedVariableParser() throws Exception {
        try (final InputStream exportedVariables =
                     RemoteProctorSpecificationSourceTest.class
                             .getResourceAsStream("exportedVariables.txt")) {
            final SpecificationResult result =
                    RemoteProctorSpecificationSource
                            .parseExportedVariables(exportedVariables);

            assertThat(result.getSpecifications().asSet())
                    .hasSize(1)
                    .first()
                    .satisfies(s ->
                            assertThat(s.getTests())
                                    .containsKey("my_tst")
                    );
        }
    }
}
