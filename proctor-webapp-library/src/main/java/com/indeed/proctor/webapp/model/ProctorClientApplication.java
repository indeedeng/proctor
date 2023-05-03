package com.indeed.proctor.webapp.model;

import java.util.Date;

public class ProctorClientApplication {
    private final String application;
    private final String baseApplicationUrl;
    private final String address;
    private final Date lastUpdate;
    private final String version;

    public ProctorClientApplication(final String application,
                                    final String baseApplicationUrl,
                                    final String address,
                                    final Date lastUpdate,
                                    final String version) {
        this.application = application;
        this.baseApplicationUrl = baseApplicationUrl;
        this.address = address;
        this.lastUpdate = lastUpdate;
        this.version = version;
    }

    public String getApplication() {
        return application;
    }

    public String getBaseApplicationUrl() {
        return baseApplicationUrl;
    }

    public String getAddress() {
        return address;
    }

    public Date getLastUpdate() {
        return lastUpdate;
    }

    public String getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + address.hashCode();
        result = 31 * result + application.hashCode();
        result = 31 * result + baseApplicationUrl.hashCode();
        result = 31 * result + version.hashCode();
        return result;
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
        final ProctorClientApplication that = (ProctorClientApplication) obj;
        return address.equals(that.address)
                && application.equals(that.application)
                && baseApplicationUrl.equals(that.baseApplicationUrl)
                && (version.equals(that.version));
    }

    @Override
    public String toString() {
        return baseApplicationUrl + " @ r" + version;
    }
}
