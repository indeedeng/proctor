package com.indeed.proctor.webapp;

import com.indeed.proctor.webapp.model.ProctorClientApplication;
import com.indeed.proctor.webapp.model.ProctorSpecifications;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.InputStream;
import java.util.Date;

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
            final String content = IOUtils.toString(exportedVariables);
            final ProctorClientApplication client =
                    new ProctorClientApplication(
                            "testapp",
                            "http://example.com",
                            "http://example.com",
                            new Date(),
                            "version"
                    );
            final ProctorSpecifications result =
                    RemoteProctorSpecificationSource
                            .parseExportedVariables(content, client);

            assertThat(result.asSet())
                    .hasSize(1)
                    .first()
                    .satisfies(s ->
                            assertThat(s.getTests())
                                    .containsKey("my_tst")
                    );
        }
    }
}
