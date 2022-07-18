package com.indeed.proctor.common;

import com.indeed.proctor.common.model.Payload;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TestForceGroupsOptionStrings {
    @Test
    public void testParseForceGroupsString_Empty() {
        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString(""))
                .isEqualTo(ForceGroupsOptions.empty());
    }

    @Test
    public void testParseForceGroupsString_SingleOption() {
        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString("default_to_fallback"))
                .isEqualTo(
                        ForceGroupsOptions.builder()
                                .setDefaultMode(ForceGroupsDefaultMode.FALLBACK)
                                .build()
                );

        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString("default_to_min_live"))
                .isEqualTo(
                        ForceGroupsOptions.builder()
                                .setDefaultMode(ForceGroupsDefaultMode.MIN_LIVE)
                                .build()
                );
    }

    @Test
    public void testParseForceGroupsString_OptionAndGroup() {
        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString("default_to_fallback,abc1"))
                .isEqualTo(
                        ForceGroupsOptions.builder()
                                .putForceGroup("abc", 1)
                                .setDefaultMode(ForceGroupsDefaultMode.FALLBACK)
                                .build()
                );

        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString("abc1,default_to_min_live"))
                .isEqualTo(
                        ForceGroupsOptions.builder()
                                .putForceGroup("abc", 1)
                                .setDefaultMode(ForceGroupsDefaultMode.MIN_LIVE)
                                .build()
                );
    }

    @Test
    public void testParseForceGroupsString_MultipleOptions_ShouldTakeLast() {
        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString("default_to_fallback,default_to_min_live"))
                .isEqualTo(
                        ForceGroupsOptions.builder()
                                .setDefaultMode(ForceGroupsDefaultMode.MIN_LIVE)
                                .build()
                );
    }

    @Test
    public void testParseForceGroupsString_GroupTwice() {
        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString("abc1,abc2"))
                .isEqualTo(
                        ForceGroupsOptions.builder()
                                .putForceGroup("abc", 2)
                                .build()
                );
    }

    @Test
    public void testGenerateForceGroupsString_Empty() {
        assertThat(
                ForceGroupsOptionsStrings.generateForceGroupsString(
                        ForceGroupsOptions.empty()
                )
        )
                .isEqualTo("");
    }


    @Test
    public void testGenerateForceGroupsString_SingleOption() {
        assertThat(
                ForceGroupsOptionsStrings.generateForceGroupsString(
                        ForceGroupsOptions.builder()
                                .setDefaultMode(ForceGroupsDefaultMode.FALLBACK)
                                .build())
        )
                .isEqualTo("default_to_fallback");

        assertThat(
                ForceGroupsOptionsStrings.generateForceGroupsString(
                        ForceGroupsOptions.builder()
                                .setDefaultMode(ForceGroupsDefaultMode.MIN_LIVE)
                                .build())
        )
                .isEqualTo("default_to_min_live");
    }

    @Test
    public void testGenerateForceGroupsString_OptionAndBucket() {
        assertThat(
                ForceGroupsOptionsStrings.generateForceGroupsString(
                        ForceGroupsOptions.builder()
                                .setDefaultMode(ForceGroupsDefaultMode.FALLBACK)
                                .putForceGroup("abc", 1)
                                .build())
        )
                .isEqualTo("default_to_fallback,abc1");

        assertThat(
                ForceGroupsOptionsStrings.generateForceGroupsString(
                        ForceGroupsOptions.builder()
                                .setDefaultMode(ForceGroupsDefaultMode.MIN_LIVE)
                                .putForceGroup("abc", 1)
                                .build())
        )
                .isEqualTo("default_to_min_live,abc1");
    }

    @Test
    public void testParseForceGroupsString_parseForcePayloadString() {
        final Double testDouble = 0.2;
        assertThat(ForceGroupsOptionsStrings.parseForcePayloadString("doubleValue:0.2"))
                .isEqualTo(
                        new Payload(testDouble)
                );

        final Double[] testDoubleArr = {0.2, 0.4, 0.6};
        assertThat(ForceGroupsOptionsStrings.parseForcePayloadString("doubleArray:[0.2 0.4 0.6]"))
                .isEqualTo(
                        new Payload(testDoubleArr)
                );

        final Long testLong = 2L;
        assertThat(ForceGroupsOptionsStrings.parseForcePayloadString("longValue:2"))
                .isEqualTo(
                        new Payload(testLong)
                );

        final Long[] testLongArr = {2L, 4L, 6L};
        assertThat(ForceGroupsOptionsStrings.parseForcePayloadString("longArray:[2 4 6]"))
                .isEqualTo(
                        new Payload(testLongArr)
                );

        final String testString = "test";
        assertThat(ForceGroupsOptionsStrings.parseForcePayloadString("stringValue:\"test\""))
                .isEqualTo(
                        new Payload(testString)
                );

        final String[] testStringArr = {"test1", "test2", "test3"};
        assertThat(ForceGroupsOptionsStrings.parseForcePayloadString("stringArray:[\"test1\" \"test2\" \"test3\"]"))
                .isEqualTo(
                        new Payload(testStringArr)
                );

        final Map<String, Object> testMap = new HashMap<>();
        testMap.put("testKey1", "1");
        testMap.put("testKey2", "value2");
        testMap.put("testKey3", "3.0");
        testMap.put("testKey4", "v1 v2");
        testMap.put("testKey5", "1.0 2.0");
        testMap.put("testKey6", "1 2");
        assertThat(ForceGroupsOptionsStrings.parseForcePayloadString("map:[\"testKey1\":1 \"testKey2\":\"value2\" \"testKey3\":3.0 \"testKey4\":[\"v1\" \"v2\"] \"testKey5\":[1.0 2.0] \"testKey6\":[1 2]]"))
                .isEqualTo(
                        new Payload(testMap)
                );
    }

    @Test
    public void testParseForceGroupsString_GroupAndPayload() {
        assertThat(
                ForceGroupsOptionsStrings.generateForceGroupsString(
                        ForceGroupsOptions.builder()
                                .putForceGroup("abc", 1)
                                .putForcePayload("abc", new Payload("test"))
                                .build())
        )
                .isEqualTo("abc1;{ stringValue : \"test\" }");

        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString("abc1;stringValue:\"test\""))
                .isEqualTo(
                        ForceGroupsOptions.builder()
                                .putForceGroup("abc", 1)
                                .putForcePayload("abc",new Payload("test"))
                                .build()
                );
    }

    @Test
    public void testParseForceGroupsString_MultipleGroupsAndPayloads() {
        assertThat(
                ForceGroupsOptionsStrings.generateForceGroupsString(
                        ForceGroupsOptions.builder()
                                .putForceGroup("abc", 1)
                                .putForcePayload("abc", new Payload("test"))
                                .putForceGroup("def", 2)
                                .putForcePayload("def",new Payload("test2"))
                                .build())
        )
                .isEqualTo("abc1;{ stringValue : \"test\" },def2;{ stringValue : \"test2\" }");

        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString("abc1;stringValue:\"test\",def2;stringValue:\"test2\""))
                .isEqualTo(
                        ForceGroupsOptions.builder()
                                .putForceGroup("abc", 1)
                                .putForcePayload("abc",new Payload("test"))
                                .putForceGroup("def", 2)
                                .putForcePayload("def",new Payload("test2"))
                                .build()
                );
    }
}
