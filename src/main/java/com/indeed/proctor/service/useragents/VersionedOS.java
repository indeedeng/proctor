package com.indeed.proctor.service.useragents;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import nl.bitwalker.useragentutils.OperatingSystem;
import nl.bitwalker.useragentutils.Version;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;

/**
 * @author matts
 */
public class VersionedOS {
    @Nonnull
    private final OperatingSystem os;
    @Nonnull
    private final String version;
    private final int majorVersion;
    private final int minorVersion;

    private VersionedOS(@Nonnull final OperatingSystem os, @Nonnull final String version, int majorVersion, int minorVersion) {
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        this.os = null == os.getGroup() ? os : os.getGroup();
        this.version = version;
    }

    @Nonnull
    public String getFamily() {
        return os.name().toLowerCase();
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    @Nonnull
    public String getVersion() {
        return version;
    }

    @Nonnull
    public static VersionedOS fromUserAgent(@Nonnull final UserAgent agent) {
        final OperatingSystem os = agent.getOperatingSystem();
        final Version version = getVersion(agent);
        final int majorVersion = parseVersionNumberFrom(version.getMajorVersion());
        final int minorVersion = parseVersionNumberFrom(version.getMinorVersion());

        @Nonnull final String fullVersion = Strings.nullToEmpty(version.getVersion());
        return new VersionedOS(os, fullVersion, majorVersion, minorVersion);
    }

    private static int parseVersionNumberFrom(final String versionNumber) {
        if (Strings.isNullOrEmpty(versionNumber)) {
            return -1;
        }

        try {
            return Integer.parseInt(versionNumber);
        } catch(NumberFormatException e) {
            return -1;
        }
    }

    @Nonnull
    private static Version getVersion(@Nonnull final UserAgent userAgent) {
        String userAgentString = userAgent.getUserAgentString();

        if (userAgent.isIOS()) {
            return parseOsVersion(Browser.getIOSVersion(userAgentString));
        }
        else if (userAgent.isAndroid()) {
            return parseOsVersion(Browser.getAndroidVersion(userAgentString));
        }
        else if (userAgent.isWindowsPhone()) {
            return parseOsVersion(Browser.getWindowsPhoneVersion(userAgentString));
        }

        return UNKNOWN_VERSION;
    }

    @Nonnull
    private static Version parseOsVersion(@Nullable final String osVersionString) {
        if (Strings.isNullOrEmpty(osVersionString)) {
            return UNKNOWN_VERSION;
        }

        final Iterator<String> it = Splitter.on('.').split(osVersionString).iterator();
        final String majorVersion = it.next();
        final String minorVersion = it.hasNext() ? it.next() : "";

        return new Version(osVersionString, majorVersion, minorVersion);
    }

    public static final Version UNKNOWN_VERSION = new Version(null, null, null);
}
