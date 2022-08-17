package com.indeed.proctor.common;

import com.google.common.collect.ImmutableSet;
import com.indeed.proctor.common.model.Payload;
import org.junit.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class TestForceGroupsOptionStrings {
    static Set<String> forcePayloadTests = ImmutableSet.of("abc", "def", "xyz");
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
    public void testGenerateForceGroupsString_withForcePayload() {
        assertThat(ForceGroupsOptionsStrings.generateForceGroupsString(
                ForceGroupsOptions.builder()
                        .putForceGroup("abc", 1)
                        .putForcePayload("abc", new Payload(0.2))
                        .build())
        ).isEqualTo("abc1;doubleValue:0.2");

        assertThat(ForceGroupsOptionsStrings.generateForceGroupsString(
                ForceGroupsOptions.builder()
                        .putForceGroup("abc", 1)
                        .putForcePayload("abc", new Payload(new Double[]{0.2, 0.4, 0.6}))
                        .build())
        ).isEqualTo("abc1;doubleArray:[0.2,0.4,0.6]");

        assertThat(ForceGroupsOptionsStrings.generateForceGroupsString(
                ForceGroupsOptions.builder()
                        .putForceGroup("abc", 1)
                        .putForcePayload("abc", new Payload(2L))
                        .build())
        ).isEqualTo("abc1;longValue:2");

        assertThat(ForceGroupsOptionsStrings.generateForceGroupsString(
                ForceGroupsOptions.builder()
                        .putForceGroup("abc", 1)
                        .putForcePayload("abc", new Payload(new Long[]{2L, 4L, 6L}))
                        .build())
        ).isEqualTo("abc1;longArray:[2,4,6]");

        assertThat(ForceGroupsOptionsStrings.generateForceGroupsString(
                ForceGroupsOptions.builder()
                        .putForceGroup("abc", 1)
                        .putForcePayload("abc", new Payload("test,with,comma"))
                        .build())
        ).isEqualTo("abc1;stringValue:\"test,with,comma\"");

        assertThat(ForceGroupsOptionsStrings.generateForceGroupsString(
                ForceGroupsOptions.builder()
                        .putForceGroup("abc", 1)
                        .putForcePayload("abc", new Payload("foo\"bar"))
                        .build())
        ).isEqualTo("abc1;stringValue:\"foo\\\"bar\"");

        assertThat(ForceGroupsOptionsStrings.generateForceGroupsString(
                ForceGroupsOptions.builder()
                        .putForceGroup("abc", 1)
                        .putForcePayload("abc", new Payload(new String[]{"test1", "test2", "test3"}))
                        .build())
        ).isEqualTo("abc1;stringArray:[\"test1\",\"test2\",\"test3\"]");
    }

    @Test
    public void testParseForcePayloadString() {
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

        final String testString2 = "foo\"bar";
        assertThat(ForceGroupsOptionsStrings.parseForcePayloadString("stringValue:\"foo\\\"bar\""))
                .isEqualTo(
                        new Payload(testString2)
                );

        final String[] testStringArr = {"test1", "test2", "test3"};
        assertThat(ForceGroupsOptionsStrings.parseForcePayloadString("stringArray:[\"test1\",\"test2\",\"test3\"]"))
                .isEqualTo(
                        new Payload(testStringArr)
                );
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
        final String crashTextInputExample = "[\\\"key1\\\":\\\"w1/w2\\\",\\\"key2\\\":\\\"foo bar\\\",\\\"key3\\\":[\\\"Veröffentlicht von\\\"]]";
        final String crashTextExample = "[\"key1\":\"w1/w2\",\"key2\":\"foo bar\",\"key3\":[\"Veröffentlicht von\"]]";

        final String test1 = "abc1;stringValue:\"test,with,comma\",def2;doubleValue:0.2,xyz1;stringValue:\"" + crashTextInputExample + "\"";

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

        final String test2 = "abc1;stringArray:[\"" + crashTextInputExample + "\",\"" + crashTextInputExample + "\"]";

        final ForceGroupsOptions testOptions2 = ForceGroupsOptions.builder()
                .putForceGroup("abc", 1)
                .putForcePayload("abc",new Payload(new String[]{crashTextExample, crashTextExample}))
                .build();

        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString(test2, forcePayloadTests))
                .isEqualTo(testOptions2);

        assertThat(
                ForceGroupsOptionsStrings.generateForceGroupsString(testOptions2))
                .isEqualTo(test2);
    }
}
