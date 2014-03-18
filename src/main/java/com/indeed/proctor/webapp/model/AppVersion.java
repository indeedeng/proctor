package com.indeed.proctor.webapp.model;

/**
* @author parker
*/
public class AppVersion implements Comparable<AppVersion> {
    private final String app;
    private final int version;
    public AppVersion(final String app, final int version) {
        this.app = app;
        this.version = version;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        return prime * (prime * 1 + app.hashCode())+ version;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AppVersion that = (AppVersion) obj;
        return (app.equals(that.app) && (version == that.version));
    }

    @Override
    public int compareTo(AppVersion that) {
        if (this == that) {
            return 0;
        }
        final int a = app.compareTo(that.app);
        if(a == 0) {
            if(version == that.version) {
                return 0;
            }
            return version < that.version ? -1 : 1;
        }
        return a;
    }

    public String getApp() {
        return app;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return app + '@' + version;
    }
}
