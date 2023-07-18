package com.indeed.proctor.pipet.core.model;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.ProctorResult;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestBucket;
import org.junit.Test;

import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

public class TestJsonResult {

    @Test
    public void testGenerateJsonBuckets() {
        final Map<String, ConsumableTestDefinition> testDefinitions =
                ImmutableMap.of("control", stubDefinitionWithVersion("vControl"));
        final Map<String, TestBucket> buckets =
                ImmutableMap.of("control", new TestBucket("fooname", -1, "foodesc"));
        ProctorResult result = new ProctorResult("0", buckets, emptyMap(), testDefinitions);

        assertThat(JsonResult.generateJsonBuckets(result))
                .hasSize(1)
                .containsKey("control")
                .hasEntrySatisfying(
                        "control",
                        jsonTestBucket -> {
                            assertThat(jsonTestBucket.getVersion()).isEqualTo("vControl");
                        });

        result = new ProctorResult("0", emptyMap(), emptyMap(), emptyMap());
        assertThat(JsonResult.generateJsonBuckets(result)).isEqualTo(emptyMap());
    }

    private ConsumableTestDefinition stubDefinitionWithVersion(final String version) {
        final ConsumableTestDefinition testDefinition = new ConsumableTestDefinition();
        testDefinition.setVersion(version);
        return testDefinition;
    }
}
