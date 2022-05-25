package com.indeed.proctor.common;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestForceGroupsDefaultMode {
    @Test
    public void testGetToken_InitialHasNoToken() {
        assertThat(ForceGroupsDefaultMode.getInitial().getToken())
                .isEmpty();
    }

    @Test
    public void testGetToken_NonInitialHasToken() {
        for (final ForceGroupsDefaultMode mode : ForceGroupsDefaultMode.values()) {
            if (!mode.equals(ForceGroupsDefaultMode.getInitial())) {
                assertThat(mode.getToken())
                        .isNotEmpty();
            }
        }
    }

    @Test
    public void testGetToken_TokenShouldStartWithDefault_to_() {
        // weak enforcement for consistency. this can be removed.
        for (final ForceGroupsDefaultMode mode : ForceGroupsDefaultMode.values()) {
            if (!mode.equals(ForceGroupsDefaultMode.getInitial())) {
                assertThat(mode.getToken())
                        .hasValueSatisfying(token -> assertThat(token).startsWith("default_to_"));
            }
        }
    }

    @Test
    public void testGetToken_Fallback() {
        assertThat(ForceGroupsDefaultMode.FALLBACK.getToken())
                .hasValue("default_to_fallback");
    }

    @Test
    public void testFromToken_Fallback() {
        assertThat(ForceGroupsDefaultMode.fromToken("default_to_fallback"))
                .hasValue(ForceGroupsDefaultMode.FALLBACK);
    }

    @Test
    public void testFromToken_FallbackIgnoreCase() {
        assertThat(ForceGroupsDefaultMode.fromToken("dEfaUlt_to_faLlbAck"))
                .hasValue(ForceGroupsDefaultMode.FALLBACK);
    }

    @Test
    public void testGetToken_MinLive() {
        assertThat(ForceGroupsDefaultMode.MIN_LIVE.getToken())
                .hasValue("default_to_min_live");
    }

    @Test
    public void testFromToken_MinLive() {
        assertThat(ForceGroupsDefaultMode.fromToken("default_to_min_live"))
                .hasValue(ForceGroupsDefaultMode.MIN_LIVE);
    }

    @Test
    public void testFromToken_MinLiveIgnoreCase() {
        assertThat(ForceGroupsDefaultMode.fromToken("dEfaUlt_to_MiN_lIvE"))
                .hasValue(ForceGroupsDefaultMode.MIN_LIVE);
    }

    @Test
    public void testFromToken_EmptyString() {
        assertThat(ForceGroupsDefaultMode.fromToken(""))
                .isEmpty();
    }

    @Test
    public void testFromToken_Null() {
        assertThat(ForceGroupsDefaultMode.fromToken(null))
                .isEmpty();
    }

    @Test
    public void testFromToken_UnknownToken() {
        assertThat(ForceGroupsDefaultMode.fromToken("here is unknown token"))
                .isEmpty();
    }
}
