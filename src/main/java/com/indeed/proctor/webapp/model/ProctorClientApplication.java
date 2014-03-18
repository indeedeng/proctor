package com.indeed.proctor.webapp.model;

import java.util.Date;

public class ProctorClientApplication {
    private final String application;
    private final String baseApplicationUrl;
    private final String address;
    private final Date lastUpdate;
    private final int version;

    public ProctorClientApplication(final String application, final String baseApplicationUrl, final String address, final Date lastUpdate, final int version) {
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

    public int getVersion() {
        return version;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + address.hashCode();
        result = prime * result + application.hashCode();
        result = prime * result + baseApplicationUrl.hashCode();
        result = prime * result + version;
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
                && (version == that.version);
    }

    @Override
    public String toString() {
        return baseApplicationUrl + " @ r" + version;
    }
}
