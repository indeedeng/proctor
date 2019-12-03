package com.indeed.proctor.groups;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.model.NameObfuscator;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class TestUnitTestGroupsGeneratedJavascript {

    private static final ObjectMapper OBJECT_MAPPER =new ObjectMapper();
    private static final String UNIT_TEST_GROUPS_JS = "/UnitTestGroups.js";
    private ScriptEngine jsEngine;
    private static final NameObfuscator OBFUSCATOR = new NameObfuscator();
    private static String jsFile;

    @BeforeClass
    public static void setupClass() throws IOException {
        try (InputStream jsFileStream = TestUnitTestGroupsGeneratedJavascript.class.getResourceAsStream(UNIT_TEST_GROUPS_JS);
             BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(jsFileStream))) {
            jsFile = bufferedReader
                    .lines().collect(Collectors.joining("\n"));
        }
    }

    @Before
    public void setup() throws ScriptException {
        jsEngine = new ScriptEngineManager().getEngineByName("nashorn");
        jsEngine.eval(jsFile);
    }

    @Test
    public void testGeneratedJsCompilesAndInitializesWithNoTests() {
        // control is fallback bucket in UnitTestGroups.json
        assertThat(evaluateMethodWithMap(null, ".isBubbleControl()"))
                .isEqualTo("true");
        assertThat(evaluateMethodWithMap(emptyMap(), ".isBubbleControl()"))
                .isEqualTo("true");
        assertThat(evaluateMethodWithMap(emptyMap(), ".isBubbleTest()"))
                .isEqualTo("false");
        // inactive is fallback bucket in UnitTestGroups.json
        assertThat(evaluateMethodWithMap(emptyMap(), ".isPimpleInactive()"))
                .isEqualTo("true");
        assertThat(evaluateMethodWithMap(emptyMap(), ".isPimpleControl()"))
                .isEqualTo("false");
    }

    @Test
    public void testGeneratedJsCompilesAndThrowsErrorWhenProvidedIncorrectTests() {
        assertThatThrownBy(() -> {
            jsEngine.eval("UnitTestGroups.init([[\"\"]])");
        }).isInstanceOf(ScriptException.class);
    }

    @Test
    public void testGeneratedJsCompilesAndInitializesWhenProvidedIncorrectMap()  {
        assertThat(evaluateMethodWithMap(ImmutableMap.of("asdf",1), ".isBubbleControl()"))
            .isEqualTo("true");
    }

    @Test
    public void testGeneratedJsCompilesAndInitializesWhenProvidedMapWithOneExpectedValue()  {
        assertThat(evaluateMethodWithMap(
                ImmutableMap.of(OBFUSCATOR.obfuscateTestName("payloaded_excluded"),
                        singletonList(1)),
                ".isPayloaded_excludedTest()"))
                .isEqualTo("true");
    }

    @Test
    public void testGeneratedJsCompilesAndInitializesWhenProvidedTestWithPayload()  {
        assertThat(evaluateMethodWithMap(
                ImmutableMap.of(
                        OBFUSCATOR.obfuscateTestName("payloaded_excluded"),
                        Arrays.asList(1, true)),
                ".isPayloaded_excludedTest()"))
                .isEqualTo("true");
    }

    private String evaluateMethodWithMap(final Map<String, Object> context, final String method)  {
        try {
            jsEngine.eval("var map = " + (context == null ? "null" : OBJECT_MAPPER.writeValueAsString(context)));
            return String.valueOf(jsEngine.eval("UnitTestGroups.init(map)" + method));
        } catch (JsonProcessingException | ScriptException e) {
            throw new RuntimeException(e);
        }
    }
}
