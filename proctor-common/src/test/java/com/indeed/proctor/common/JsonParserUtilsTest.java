package com.indeed.proctor.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonParserUtilsTest {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String EMPTY = "{}";
    private static final String TWO_VALUES = "" +
            "{" +
            "   \"object1\": {" +
            "       \"a\": 1," +
            "       \"b\": [1, 2, 3]" +
            "   }," +
            "   \"object2\": {" +
            "       \"a\": 3," +
            "       \"b\": []" +
            "   }" +
            "}";
    private static final String TWO_VALUES_WITH_ONE_NULL = "" +
            "{" +
            "   \"object1\": null," +
            "   \"object2\": {" +
            "       \"a\": 4," +
            "       \"b\": [1, 2]" +
            "   }" +
            "}";
    private static final List<String> TEST_OBJECT_MAP_TEST_CASES = ImmutableList.of(
            EMPTY,
            TWO_VALUES,
            TWO_VALUES_WITH_ONE_NULL
    );

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
    public void testConsumeJsonWithTestObjectMap() throws IOException {
        for (final String testCase : TEST_OBJECT_MAP_TEST_CASES) {
            verifyTestObjectMap(testCase);
        }
    }

    private static void verifyTestObjectMap(final String testObjectMapAsJsonString) throws IOException {
        final Map<String, TestObject> expected = OBJECT_MAPPER.readValue(
                testObjectMapAsJsonString,
                new TypeReference<Map<String, TestObject>>() {
                }
        );

        final JsonFactory jsonFactory = new JsonFactory();
        final JsonParser jsonParser = jsonFactory.createParser(testObjectMapAsJsonString);

        jsonParser.nextToken();

        final Map<String, TestObject> actual = new HashMap<>();
        JsonParserUtils.consumeJson(
                jsonParser,
                (key, parser) -> {
                    final TestObject testObject = OBJECT_MAPPER.readValue(parser, TestObject.class);

                    actual.put(key, testObject);
                }
        );

        assertThat(actual).isEqualTo(expected);
    }
}
