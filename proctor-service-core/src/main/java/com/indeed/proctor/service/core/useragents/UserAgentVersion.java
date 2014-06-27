package com.indeed.proctor.service.core.useragents;

import com.google.common.base.Strings;
import eu.bitwalker.useragentutils.Version;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Shim class so that Indeed internal code can refer to an indeed class, in case we need to tweak the
 *  version detection later.
 *
 * @author matts
 */
public class UserAgentVersion {
    @Nonnull
    private final String version;
    private final int major;
    private final int minor;

    public UserAgentVersion(
            @Nullable final String version,
            final int major,
            final int minor
    ) {
        this.version = Strings.nullToEmpty(version);
        this.major = major;
        this.minor = minor;
    }

    @Nonnull
    public String getVersion() {
        return version;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public static UserAgentVersion from(@Nullable final Version version) {
        if (null == version) {
            return UNKNOWN;
        }

        final String majorVersion = version.getMajorVersion();
        final int majorVersionNumber = null == majorVersion ? -1 : UserAgent.getIntegerParameter(majorVersion, -1);

        final String minorVersion = version.getMinorVersion();
        final int minorVersionNumber= null == minorVersion ? -1 : UserAgent.getIntegerParameter(minorVersion, -1);

        return new UserAgentVersion(version.getVersion(), majorVersionNumber, minorVersionNumber);
    }

    public static final Version UNKNOWN_VERSION = new Version(null, null, null);
    public static final UserAgentVersion UNKNOWN = new UserAgentVersion(null, -1, -1);
}
