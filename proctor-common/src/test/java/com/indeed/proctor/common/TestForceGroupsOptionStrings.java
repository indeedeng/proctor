package com.indeed.proctor.common;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;

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
        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString("default_fallback"))
                .isEqualTo(
                        ForceGroupsOptions.builder()
                                .setDefaultMode(ForceGroupsDefaultMode.FALLBACK)
                                .build()
                );
    }

    @Test
    public void testParseForceGroupsString_OptionAndGroup() {
        assertThat(ForceGroupsOptionsStrings.parseForceGroupsString("default_fallback,abc1"))
                .isEqualTo(
                        ForceGroupsOptions.builder()
                                .putForceGroup("abc", 1)
                                .setDefaultMode(ForceGroupsDefaultMode.FALLBACK)
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
                .isEqualTo("default_fallback");
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
                .isEqualTo("default_fallback,abc1");
    }

    @Test
    public void testParseForceGroups() {
        //Test null string
        assertThat(parseForceGroups(null))
                .isEmpty();
        //Test empty string
        assertThat(parseForceGroups("")).isEmpty();
        //Test invalid string
        assertThat(parseForceGroups("fasdfasdf;zxcvwasdf")).isEmpty();
        //Test invalid numbers
        assertThat(parseForceGroups("fasdfasdf")).isEmpty();
        assertThat(parseForceGroups("test" + Integer.MAX_VALUE + "0")).isEmpty();
        assertThat(parseForceGroups("test-")).isEmpty();
        assertThat(parseForceGroups("test0-")).isEmpty();
        //Test single group
        assertThat(parseForceGroups("somerandomtst1"))
                .hasSize(1)
                .containsEntry("somerandomtst", 1);
        // not sure if this case needs to be supported...
        assertThat(parseForceGroups("somerandomtst*  1"))
                .hasSize(1)
                .containsEntry("somerandomtst*", 1);
        assertThat(parseForceGroups("somerandomtst" + Integer.MAX_VALUE))
                .hasSize(1)
                .containsEntry("somerandomtst", Integer.MAX_VALUE);
        assertThat(parseForceGroups("somerandomtst" + Integer.MIN_VALUE))
                .hasSize(1)
                .containsEntry("somerandomtst", Integer.MIN_VALUE);
        //Test multiple groups, multiple commas
        assertThat(parseForceGroups(",,somerandomtst1, \n,, someothertst0, notanothertst2,,"))
                .isEqualTo(ImmutableMap.builder()
                        .put("somerandomtst", 1)
                        .put("someothertst", 0)
                        .put("notanothertst", 2)
                        .build());
        //Test multiple, duplicate groups, last one wins
        assertThat(parseForceGroups("testA1, testA2, testB2"))
                .isEqualTo(ImmutableMap.builder()
                        .put("testA", 2)
                        .put("testB", 2)
                        .build());

        //Test multiple groups with some invalid stuff
        assertThat(parseForceGroups("somerandomtst1, someothertst0, notanothertst2,asdf;alksdfjzvc"))
                .isEqualTo(ImmutableMap.builder()
                        .put("somerandomtst", 1)
                        .put("someothertst", 0)
                        .put("notanothertst", 2)
                        .build());
    }

    private static Map<String, Integer> parseForceGroups(final String value) {
        return ForceGroupsOptionsStrings.parseForceGroupsString(value).getForceGroups();
    }
}
