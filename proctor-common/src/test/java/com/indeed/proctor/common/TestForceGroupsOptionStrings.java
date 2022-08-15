package com.indeed.proctor.common;

import com.indeed.proctor.common.model.Payload;
import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class TestForceGroupsOptionStrings {
    static Set<String> forcePayloadTests;

    static {
        forcePayloadTests = new HashSet<>();
        forcePayloadTests.add("abc");
        forcePayloadTests.add("def");
        forcePayloadTests.add("xyz");
    }

    @Test
    public void testParseForceGroupsString_Empty() {
        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString("", forcePayloadTests))
                .isEqualTo(ForceGroupsOptions.empty());
    }

    @Test
    public void testParseForceGroupsString_SingleOption() {
        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString("default_to_fallback", forcePayloadTests))
                .isEqualTo(
                        ForceGroupsOptions.builder()
                                .setDefaultMode(ForceGroupsDefaultMode.FALLBACK)
                                .build()
                );

        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString("default_to_min_live", forcePayloadTests))
                .isEqualTo(
                        ForceGroupsOptions.builder()
                                .setDefaultMode(ForceGroupsDefaultMode.MIN_LIVE)
                                .build()
                );
    }

    @Test
    public void testParseForceGroupsString_OptionAndGroup() {
        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString("default_to_fallback,abc1", forcePayloadTests))
                .isEqualTo(
                        ForceGroupsOptions.builder()
                                .putForceGroup("abc", 1)
                                .setDefaultMode(ForceGroupsDefaultMode.FALLBACK)
                                .build()
                );

        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString("abc1,default_to_min_live", forcePayloadTests))
                .isEqualTo(
                        ForceGroupsOptions.builder()
                                .putForceGroup("abc", 1)
                                .setDefaultMode(ForceGroupsDefaultMode.MIN_LIVE)
                                .build()
                );
    }

    @Test
    public void testParseForceGroupsString_MultipleOptions_ShouldTakeLast() {
        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString("default_to_fallback,default_to_min_live", forcePayloadTests))
                .isEqualTo(
                        ForceGroupsOptions.builder()
                                .setDefaultMode(ForceGroupsDefaultMode.MIN_LIVE)
                                .build()
                );
    }

    @Test
    public void testParseForceGroupsString_GroupTwice() {
        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString("abc1,abc2", forcePayloadTests))
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
        assertThat(ForceGroupsOptionsStrings.parseForcePayloadString("doubleArray:[0.2,0.4,0.6]"))
                .isEqualTo(
                        new Payload(testDoubleArr)
                );

        final Long testLong = 2L;
        assertThat(ForceGroupsOptionsStrings.parseForcePayloadString("longValue:2"))
                .isEqualTo(
                        new Payload(testLong)
                );

        final Long[] testLongArr = {2L, 4L, 6L};
        assertThat(ForceGroupsOptionsStrings.parseForcePayloadString("longArray:[2,4,6]"))
                .isEqualTo(
                        new Payload(testLongArr)
                );

        final String testString = "test,with,comma";
        assertThat(ForceGroupsOptionsStrings.parseForcePayloadString("stringValue:\"test,with,comma\""))
                .isEqualTo(
                        new Payload(testString)
                );

        final String[] testStringArr = {"test1", "test2", "test3"};
        assertThat(ForceGroupsOptionsStrings.parseForcePayloadString("stringArray:[\"test1\",\"test2\",\"test3\"]"))
                .isEqualTo(
                        new Payload(testStringArr)
                );

        final Map<String, Object> testMap = new HashMap<>();
        testMap.put("testKey1", "1");
        testMap.put("testKey2", "\"value2\"");
        testMap.put("testKey3", "3.0");
        testMap.put("testKey4", "[\"v1\", \"v2\"]");
        testMap.put("testKey5", "[1.0, 2.0]");
        testMap.put("testKey6", "1.0");
        final Payload testP = ForceGroupsOptionsStrings.parseForcePayloadString("map:[\"testKey1\":1, \"testKey2\":\"value2\", \"testKey3\":3.0, \"testKey4\":[\"v1\", \"v2\"], \"testKey5\":[1.0, 2.0], \"testKey6\":1.0]");

        assertThat(testP).isEqualTo(new Payload(testMap));
    }

    @Test
    public void testParseForceGroupsString_GroupAndPayload() {
        final String generatedForceGroup = ForceGroupsOptionsStrings.generateForceGroupsString(
                ForceGroupsOptions.builder()
                        .putForceGroup("abc", 1)
                        .putForcePayload("abc", new Payload("test,with,comma"))
                        .build());

        assertThat(generatedForceGroup)
                .isEqualTo("abc1;stringValue:\"test,with,comma\"");

        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString("abc1;stringValue:\"test,with,comma\"", forcePayloadTests))
                .isEqualTo(
                        ForceGroupsOptions.builder()
                                .putForceGroup("abc", 1)
                                .putForcePayload("abc",new Payload("test,with,comma"))
                                .build()
                );

        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString(generatedForceGroup, forcePayloadTests))
                .isEqualTo(
                        ForceGroupsOptions.builder()
                                .putForceGroup("abc", 1)
                                .putForcePayload("abc",new Payload("test,with,comma"))
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
                                .putForcePayload("def", new Payload("test2"))
                                .build())
        )
                .isEqualTo("abc1;stringValue:\"test\",def2;stringValue:\"test2\"");

        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString("abc1;stringValue:\"test\",def2;stringValue:\"test2\"", forcePayloadTests))
                .isEqualTo(
                        ForceGroupsOptions.builder()
                                .putForceGroup("abc", 1)
                                .putForcePayload("abc", new Payload("test"))
                                .putForceGroup("def", 2)
                                .putForcePayload("def", new Payload("test2"))
                                .build()
                );

        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString("abc1;stringValue:\"test\",def2;doubleValue:0.2", forcePayloadTests))
                .isEqualTo(
                        ForceGroupsOptions.builder()
                                .putForceGroup("abc", 1)
                                .putForcePayload("abc", new Payload("test"))
                                .putForceGroup("def", 2)
                                .putForcePayload("def", new Payload(0.2))
                                .build()
                );
    }

    @Test
    public void testParseForceGroupsString_JSONinString() {
        final String crashTextExample = "[\"key1\":\"w1\\/w2\",\"key2\":\"foo bar\",\"key3\":[\"Veröffentlicht von\"]]";

        final String test1 = "abc1;stringValue:\"test,with,comma\",def2;doubleValue:0.2,xyz1;stringValue:\"" + crashTextExample + "\"";

        final ForceGroupsOptions testOptions1 = ForceGroupsOptions.builder()
                .putForceGroup("abc", 1)
                .putForcePayload("abc",new Payload("test,with,comma"))
                .putForceGroup("def", 2)
                .putForcePayload("def",new Payload(0.2))
                .putForceGroup("xyz", 1)
                .putForcePayload("xyz", new Payload(crashTextExample))
                .build();

        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString(test1, forcePayloadTests))
                .isEqualTo(testOptions1);

        assertThat(ForceGroupsOptionsStrings.generateForceGroupsString(testOptions1))
                .isEqualTo(test1);

        final String test2 = "abc1;stringArray:[\"" + crashTextExample + "\",\"" + crashTextExample + "\"]";

        final ForceGroupsOptions testOptions2 = ForceGroupsOptions.builder()
                .putForceGroup("abc", 1)
                .putForcePayload("abc",new Payload(new String[]{crashTextExample, crashTextExample}))
                .build();

        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString(test2, forcePayloadTests))
                .isEqualTo(testOptions2);

        assertThat(
                ForceGroupsOptionsStrings.generateForceGroupsString(testOptions2))
                .isEqualTo(test2);

        final Map<String, Object> testMap3 = new HashMap<>();
        testMap3.put("key1", "\"w1\\/w2\"");
        testMap3.put("key2", "\"foo bar\"");
        testMap3.put("key3", "[\"Veröffentlicht von\"]");

        final String test3 = "abc1;map:" + crashTextExample;

        final ForceGroupsOptions testOptions3 = ForceGroupsOptions.builder()
                .putForceGroup("abc", 1)
                .putForcePayload("abc",new Payload(testMap3))
                .build();

        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString(test3, forcePayloadTests))
                .isEqualTo(testOptions3);

        assertThat(
                ForceGroupsOptionsStrings.generateForceGroupsString(testOptions3))
                .isEqualTo(test3);

        final Map<String, Object> testMap4 = new HashMap<>();
        testMap4.put("c1", "\"" + crashTextExample + "\"");
        testMap4.put("c2", "\"" + crashTextExample + "\"");

        final String test4 = "abc1;map:[\"c1\":\"" + crashTextExample + "\",\"c2\":\"" + crashTextExample + "\"]";

        final ForceGroupsOptions testOptions4 = ForceGroupsOptions.builder()
                .putForceGroup("abc", 1)
                .putForcePayload("abc",new Payload(testMap4))
                .build();


        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString(test4, forcePayloadTests))
                .isEqualTo(testOptions4);

        assertThat(
                ForceGroupsOptionsStrings.generateForceGroupsString(testOptions4))
                .isEqualTo(test4);
    }
}
