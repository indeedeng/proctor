package com.indeed.proctor.webapp.model;

import com.google.common.collect.ComparisonChain;

/**
 * @author parker
 */
public class AppVersion implements Comparable<AppVersion> {
    private final String app;
    private final String version;

    public AppVersion(final String app,
                      final String version) {
        this.app = app;
        this.version = version;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final AppVersion that = (AppVersion) o;

        if (!app.equals(that.app)) {
            return false;
        }
        if (!version.equals(that.version)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = app.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }

    @Override
    public int compareTo(AppVersion that) {
        if (this == that) {
            return 0;
        }
        return ComparisonChain.start()
                .compare(app, that.app)
                .compare(version, that.version)
                .result();
    }

    public String getApp() {
        return app;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return app + '@' + version;
    }

    public String toShortString() {
        final String shortVersionName = version.length() > 7 ? version.substring(0, 7) : version;
        return app + '@' + shortVersionName;
    }
}
