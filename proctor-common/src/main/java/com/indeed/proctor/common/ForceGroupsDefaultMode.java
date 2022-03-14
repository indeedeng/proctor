package com.indeed.proctor.common;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * Options to define the behavior of what bucket to force when it's not explicitly given.
 */
public enum ForceGroupsDefaultMode {
    /**
     * The initial option. It doesn't force any bucket.
     */
    NONE(null),
    /**
     * It forces to assign no bucket. In most situations when you use AbstractGroups, it leads to using the fallback buckets.
     */
    FALLBACK("default_to_fallback"),
    ;

    @Nullable
    private final String token;

    ForceGroupsDefaultMode(@Nullable final String token) {
        this.token = token;
    }

    /**
     * Returns the token of the mode, used to specify the mode in a force groups string.
     * Empty if the mode is the initial option (i.e., NONE)
     */
    public Optional<String> getToken() {
        return Optional.ofNullable(token);
    }

    public static ForceGroupsDefaultMode getInitial() {
        return NONE;
    }

    public static Optional<ForceGroupsDefaultMode> fromToken(final String token) {
        for (final ForceGroupsDefaultMode mode : values()) {
            if ((mode.token != null) && mode.token.equalsIgnoreCase(token)) {
                return Optional.of(mode);
            }
        }
        return Optional.empty();
    }
}
