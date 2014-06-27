package com.indeed.proctor.service.core.useragents;

import com.google.common.base.Objects;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import eu.bitwalker.useragentutils.Browser;
import eu.bitwalker.useragentutils.DeviceType;
import eu.bitwalker.useragentutils.OperatingSystem;
import eu.bitwalker.useragentutils.Version;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;
import java.util.Iterator;

/**
 * Wrapper around bitwalker UserAgentUtils that provides lots of helpful convenience methods
 * for defining proctor rules.
 *
 * @see eu.bitwalker.useragentutils.UserAgent
 * @author gaurav
 * @author matts
 */
public class UserAgent {
    @Nonnull
    private final eu.bitwalker.useragentutils.UserAgent userAgent;
    @Nonnull
    private final VersionedOS os;
    @Nonnull
    private final UserAgentVersion version;
    @Nonnull
    private final String userAgentString;

    @Deprecated // Use UserAgent#parseUserAgentString instead
    public UserAgent(@Nonnull final String userAgentString) {
        this.userAgentString = userAgentString;
        this.userAgent = eu.bitwalker.useragentutils.UserAgent.parseUserAgentString(userAgentString);
        this.os = VersionedOS.fromUserAgent(this);
        this.version = UserAgentVersion.from(userAgent.getBrowserVersion());
    }

    private UserAgent(
            @Nullable final String userAgentHeader,
            @Nonnull final eu.bitwalker.useragentutils.UserAgent delegate,
            @Nonnull final UserAgentVersion version
    ) {
        this.userAgentString = Strings.nullToEmpty(userAgentHeader);
        this.userAgent = delegate;
        this.os = VersionedOS.fromUserAgent(this);
        this.version = version;
    }

    @Nonnull
    public Browser getBrowser() {
        return userAgent.getBrowser();
    }

    @Nonnull
    public static UserAgent extractUserAgent(@Nonnull final HttpServletRequest request) {
        return newBuilder().setRequest(request).build();
    }

    @Nonnull
    public static UserAgent parseUserAgentString(@Nullable final String userAgentString) {
        return newBuilder().setUserAgentString(userAgentString).build();
    }

    @Nonnull
    public static UserAgent extractUserAgentSafely(@Nonnull final HttpServletRequest request) {
        return newBuilder().setRequest(request).buildSafely();
    }

    @Nonnull
    public static UserAgent parseUserAgentStringSafely(@Nullable final String userAgentString) {
        return newBuilder().setUserAgentString(userAgentString).buildSafely();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        UserAgent temp = (UserAgent) o;
        final eu.bitwalker.useragentutils.UserAgent other = temp.userAgent;

        return userAgent.equals(other);
    }

    @Override
    public int hashCode() {
        return userAgent.hashCode();
    }

    /**
     * Delegate method
     * Detects the detailed version information of the browser. Depends on the userAgent to be available.
     * Use it only after using UserAgent(String) or UserAgent.parseUserAgent(String).
     * Returns UserAgent.UNKNOWN_VERSION if it can not detect the version information.
     * @return Version
     */
    @Nonnull
    public Version getBrowserVersion() {
        return Objects.firstNonNull(userAgent.getBrowserVersion(), UserAgentVersion.UNKNOWN_VERSION);
    }

    @Nonnull
    public UserAgentVersion getVersion() {
        return version;
    }

    @Nonnull
    public OperatingSystem getOperatingSystem() {
        return Objects.firstNonNull(userAgent.getOperatingSystem(), OperatingSystem.UNKNOWN);
    }

    @Nonnull
    public VersionedOS getOS() {
        // TODO This will eventually subsume and replace the less useful getOperatingSystem
        return this.os;
    }

    @Nonnull
    public DeviceType getDeviceType() {
        return Objects.firstNonNull(getOperatingSystem().getDeviceType(), DeviceType.UNKNOWN);
    }

    @Nonnull
    public String getUserAgentString() {
        return this.userAgentString;
    }

    /**
     * Determines whether user agent is a tablet
     * @return true if tablet
     */
    public boolean isTablet() {
        return DeviceType.TABLET.equals(getDeviceType()) ||
                userAgentString.contains(NEXUS_7_SIGNATURE) ||
                userAgentString.contains(KINDLE_FIRE_SIGNATURE);
    }

    public boolean isIOS() {
        final OperatingSystem operatingSystemGroup = getOperatingSystem().getGroup();

        return isMobileDevice() &&
                OperatingSystem.IOS.equals(operatingSystemGroup);
    }

    public boolean isChromeForIOS() {
        return isIOS() && userAgentString.contains("CriOS");
    }

    public boolean isAndroid() {
        final OperatingSystem operatingSystemGroup = getOperatingSystem().getGroup();

        return OperatingSystem.ANDROID.equals(operatingSystemGroup);
    }

    public boolean isWindowsPhone() {
        return OperatingSystem.WINDOWS_MOBILE7.equals(getOperatingSystem());
    }

    public boolean isMobileDevice() {
        return getOperatingSystem().isMobileDevice() || isTablet();
    }

    public boolean isSmartPhone() {
        OperatingSystem operatingSystemGroup = getOperatingSystem().getGroup();
        return isMobileDevice() &&
                (OperatingSystem.IOS.equals(operatingSystemGroup)
                || OperatingSystem.ANDROID.equals(operatingSystemGroup)
                || OperatingSystem.WINDOWS_MOBILE7.equals(getOperatingSystem()));
    }

    /**
     * Mobile devices that are not tablets are regarded as phones.
     * It is difficult to distinguish non-cellular handhelds
     * @return true if mobile device and not a tablet
     */
    public boolean isPhone() {
        return getOperatingSystem().isMobileDevice() && !isTablet();
    }

    public boolean isDumbPhone() {
        return isPhone() && !isSmartPhone();
    }

    private boolean meetsMinimumVersion(final int minMajorVersion, final int minMinorVersion,
                                     final int majorVersion, final int minorVersion) {
        return (majorVersion > minMajorVersion) || (majorVersion == minMajorVersion && minorVersion >= minMinorVersion);
    }

    @Override
    public String toString() {
        return userAgent.toString();
    }

    private static final String NEXUS_7_SIGNATURE = "Nexus 7";
    private static final String KINDLE_FIRE_SIGNATURE = "Kindle Fire";

    private static final eu.bitwalker.useragentutils.UserAgent UNKNOWN_USER_AGENT =
            new eu.bitwalker.useragentutils.UserAgent(OperatingSystem.UNKNOWN, Browser.UNKNOWN);

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        private HttpServletRequest request;
        private String userAgentString;

        private Builder() { }

        public Builder setRequest(@Nonnull final HttpServletRequest request) {
            this.request = request;
            this.userAgentString = request.getHeader("User-Agent");
            return this;
        }

        public Builder setUserAgentString(@Nullable final String userAgentString) {
            this.userAgentString = userAgentString;
            return this;
        }

        @Nullable
        private static UserAgentVersion parseVersion(@Nullable final String versionString) {
            if (null == versionString) {
                return null;
            }

            final Iterator<String> splits = Splitter.on('.').split(versionString).iterator();
            final int majorVersion = splits.hasNext() ? getIntegerParameter(splits.next(), -1) : -1;
            final int minorVersion = splits.hasNext() ? getIntegerParameter(splits.next(), -1) : -1;

            return new UserAgentVersion(versionString, majorVersion, minorVersion);
        }

        @Nonnull
        public UserAgent build() {
            if (null == userAgentString) {
                return new UserAgent(null, UNKNOWN_USER_AGENT, UserAgentVersion.UNKNOWN);
            }

            @Nullable
            final eu.bitwalker.useragentutils.UserAgent userAgent =
                    eu.bitwalker.useragentutils.UserAgent.parseUserAgentString(userAgentString);

            final eu.bitwalker.useragentutils.UserAgent userAgentToUse = Objects.firstNonNull(userAgent, UNKNOWN_USER_AGENT);
            final UserAgentVersion version = UserAgentVersion.from(userAgent.getBrowserVersion());

            return new UserAgent(userAgentString, userAgentToUse, version);
        }

        public UserAgent buildSafely() {
            try {
                return build();

            } catch (RuntimeException e) {
                return new UserAgent(userAgentString, UNKNOWN_USER_AGENT, UserAgentVersion.UNKNOWN);
            }
        }

    }

    /**
     * reading valid integer parameter
     *
     * @param argument expected value
     * @param defaultValue default value
     * @return valid integer or default value
     */
    protected static int getIntegerParameter(String argument, int defaultValue) {
        try {
            return argument != null
                   ? Integer.parseInt(argument)
                   : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

}
