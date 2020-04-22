package com.indeed.proctor.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.indeed.proctor.common.Serializers;
import org.junit.Test;

import java.io.IOException;

public class TestEditTest {
    private static final ObjectMapper OBJECT_MAPPER = Serializers.lenient();

    private static final String TEST_EDIT_JSON_STRING =
            "{" +
                    "\"revision\":{" +
                            "\"revision\":\"1213134\"," +
                            "\"author\":\"parker\"," +
                            "\"date\":1340055596000," +
                            "\"message\":\"message\"" +
                    "}," +
                    "\"definition\":{" +
                            "\"version\":\"1\"," +
                            "\"constants\":{}," +
                            "\"specialConstants\":{}," +
                            "\"salt\":\"iapreloadxpctst\"," +
                            "\"rule\":null," +
                            "\"buckets\":[{" +
                                    "\"name\":\"inactive\"," +
                                    "\"value\":-1" +
                            "}]," +
                            "\"allocations\":[{" +
                                    "\"rule\":null," +
                                    "\"ranges\":[{" +
                                            "\"bucketValue\":-1," +
                                            "\"length\":1.0" +
                                    "}]," +
                                    "\"id\":\"A1\"" +
                            "}]," +
                            "\"silent\":false," +
                            "\"metaTags\":[]," +
                            "\"testType\":\"USER\"," +
                            "\"description\":\"description\"" +
                    "}" +
            "}";

    private static final String TEST_EDIT_WITH_NULL_DEFINITION_JSON_STRING =
            "{" +
                    "\"revision\":{" +
                            "\"revision\":\"1213134\"," +
                            "\"author\":\"parker\"," +
                            "\"date\":1340055596000," +
                            "\"message\":\"message\"" +
                    "}," +
                    "\"definition\":null" +
            "}";

    @Test
    public void testDeserialize() throws IOException {
        OBJECT_MAPPER.readValue(TEST_EDIT_JSON_STRING, TestEdit.class);
        OBJECT_MAPPER.readValue(TEST_EDIT_WITH_NULL_DEFINITION_JSON_STRING, TestEdit.class);
    }
}
