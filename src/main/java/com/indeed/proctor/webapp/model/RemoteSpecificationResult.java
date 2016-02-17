package com.indeed.proctor.webapp.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.indeed.proctor.common.SpecificationResult;

import java.util.List;
import java.util.Map;

/**
* @author parker
*/
public class RemoteSpecificationResult {
    final AppVersion version;
    final Map<ProctorClientApplication, SpecificationResult> failures;
    final ProctorClientApplication skipped;
    final List<ProctorClientApplication> remaining;
    final ProctorClientApplication clientApplication;
    final SpecificationResult specificationResult;

    public RemoteSpecificationResult(final AppVersion version,
                                     final Map<ProctorClientApplication, SpecificationResult> failures,
                                     final ProctorClientApplication skipped,
                                     final List<ProctorClientApplication> remaining,
                                     final ProctorClientApplication _clientApplication,
                                     final SpecificationResult specificationResult) {
        this.version = version;
        this.failures = failures;
        this.skipped = skipped;
        this.remaining = remaining;
        this.clientApplication = _clientApplication;
        this.specificationResult = specificationResult;
    }

    public boolean isSuccess() {
        return specificationResult != null && specificationResult.getSpecification() != null;
    }

    public boolean isSkipped() {
        return skipped != null;
    }

    public AppVersion getVersion() {
        return version;
    }

    public Map<ProctorClientApplication, SpecificationResult> getFailures() {
        return failures;
    }

    public List<ProctorClientApplication> getRemaining() {
        return remaining;
    }

    public ProctorClientApplication getClientApplication() {
        return clientApplication;
    }

    public SpecificationResult getSpecificationResult() {
        return specificationResult;
    }

    public static Builder newBuilder(final AppVersion version) {
        return new Builder(version);
    }

    public static class Builder {
        final AppVersion version;
        final ImmutableMap.Builder<ProctorClientApplication, SpecificationResult> failures = ImmutableMap.builder();
        ProctorClientApplication skipped;
        ProctorClientApplication success;
        SpecificationResult result;

        private Builder(AppVersion version) {
            this.version = version;
        }

        public RemoteSpecificationResult build(List<ProctorClientApplication> remaining) {
            return new RemoteSpecificationResult(version, failures.build(), skipped, Lists.<ProctorClientApplication>newArrayList(remaining), success, result);
        }

        public Builder skipped(final ProctorClientApplication app, final SpecificationResult result) {
            this.skipped = app;
            this.result = result;
            return this;
        }

        public Builder failed(final ProctorClientApplication app, final SpecificationResult result) {
            failures.put(app, result);
            return this;
        }

        public Builder success(final ProctorClientApplication client, final SpecificationResult result) {
            this.success = client;
            this.result = result;
            return this;
        }

    }
}
