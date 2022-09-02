package com.indeed.proctor.common;

import org.junit.Test;

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
}
