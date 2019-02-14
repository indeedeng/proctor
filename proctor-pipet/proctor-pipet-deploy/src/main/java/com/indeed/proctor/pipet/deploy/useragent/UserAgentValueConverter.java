package com.indeed.proctor.pipet.deploy.useragent;

import com.indeed.proctor.pipet.core.var.ValueConverter;

import javax.annotation.Nonnull;

/** @author parker */
public final class UserAgentValueConverter implements ValueConverter<UserAgent> {
    private UserAgentValueConverter() { }

    public UserAgent convert(@Nonnull String rawValue) {
        return UserAgent.parseUserAgentStringSafely(rawValue);
    }
    @Override
    public Class<UserAgent> getType() {
        return UserAgent.class;
    }

    public static UserAgentValueConverter userAgentValueConverter() {
        return new UserAgentValueConverter();
    }
}
