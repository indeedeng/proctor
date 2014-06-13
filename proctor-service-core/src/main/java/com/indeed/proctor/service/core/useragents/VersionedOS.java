package com.indeed.proctor.service.core.useragents;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import nl.bitwalker.useragentutils.OperatingSystem;
import nl.bitwalker.useragentutils.Version;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            return parseOsVersion(getIOSVersion(userAgentString));
        }
        else if (userAgent.isAndroid()) {
            return parseOsVersion(getAndroidVersion(userAgentString));
        }
        else if (userAgent.isWindowsPhone()) {
            return parseOsVersion(getWindowsPhoneVersion(userAgentString));
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

    private static final Pattern ANDROID_PATTERN = Pattern.compile("Android (\\d+(?:\\.\\d+)+)");
    public static String getAndroidVersion(String ua) {
        final Matcher m = ANDROID_PATTERN.matcher(ua);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    @Nullable
    public static String getIOSVersion(@Nullable final String ua) {
        if (ua == null) return null;
        StringBuilder versionBuilder = new StringBuilder(6);
        int index = ua.indexOf("OS ");
        if (index == -1) return null;
        index += 3; // skip over "OS ".
        if (!Character.isDigit(ua.charAt(index))) {
            return null;
        }
        while (index < ua.length() && ua.charAt(index) != ' ') {
            versionBuilder.append(ua.charAt(index));
            index++;
        }
        return versionBuilder.toString().replace('_', '.');
    }

    public static String getWindowsPhoneVersion(String ua) {
        if (ua == null) return null;
        StringBuilder versionBuilder = new StringBuilder(6);
        int index = ua.indexOf("OS ");
        if (index == -1) return null;
        index += 3; // skip over "OS ".
        if (!Character.isDigit(ua.charAt(index))) {
            return null;
        }
        while (index < ua.length() && ua.charAt(index) != ';') {
            versionBuilder.append(ua.charAt(index));
            index++;
        }
        return versionBuilder.toString();
    }

    public static final Version UNKNOWN_VERSION = new Version(null, null, null);
}
