package com.indeed.proctor.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonParserUtilsTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final File JSON_FILE = new File(
            JsonParserUtilsTest.class.getResource("test-case-for-json-parser-utils.json").getPath()
    );
    private static final Map<String, TestObject> ALL_OBJECTS;

    static {
        try {
            ALL_OBJECTS = OBJECT_MAPPER.readValue(
                    JSON_FILE,
                    new TypeReference<Map<String, TestObject>>() {
                    }
            );
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class TestObject {
        private final int a;
        private final List<Integer> b;

        @JsonCreator
        public TestObject(
                @JsonProperty("a") final int a,
                @JsonProperty("b") final List<Integer> b
        ) {
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final TestObject that = (TestObject) o;
            return a == that.a &&
                    b.equals(that.b);
        }

        @Override
        public int hashCode() {
            return Objects.hash(a, b);
        }
    }

    @Test
    public void testConsumeJson() throws IOException {
        final JsonFactory jsonFactory = new JsonFactory();
        final JsonParser jsonParser = jsonFactory.createParser(JSON_FILE);

        jsonParser.nextToken();

        final Map<String, TestObject> testObjectMap = new HashMap<>();
        JsonParserUtils.consumeJson(
                jsonParser,
                (key, parser) -> {
                    final TestObject testObject = OBJECT_MAPPER.readValue(parser, TestObject.class);

                    testObjectMap.put(key, testObject);
                }
        );

        assertThat(testObjectMap).isEqualTo(ALL_OBJECTS);
    }
}
