package com.indeed.proctor.webapp;

import com.indeed.proctor.webapp.model.SpecificationResult;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author yiqing
 */

public class RemoteProctorSpecificationSourceTest {
    @Test
    public void testExportedVariableParser() throws Exception {
        final String exampleJson = "{\"providedContext\"\\:{\"country\"\\:\"String\",\"lang\"\\:\"String\"},\"tests\"\\:{\"my_tst\"\\:{\"fallbackValue\"\\:-1,\"buckets\"\\:{\"inactive\"\\:-1,\"control\"\\:0,\"sendReactivationEmail\"\\:1},\"description\"\\:\"KEY-45\\: text with \\\"double quote\\\"\"}}}";
        final InputStream stream = new ByteArrayInputStream(exampleJson.getBytes(StandardCharsets.UTF_8.name()));
        final SpecificationResult result = RemoteProctorSpecificationSource.EXPORTED_VARIABLE_PARSER.parse(stream);
        assertThat(result.getSpecifications().asSet())
                .hasSize(1)
                .first()
                .satisfies(s ->
                        assertThat(s.getTests())
                                .containsKey("my_tst")
                );
    }
}
