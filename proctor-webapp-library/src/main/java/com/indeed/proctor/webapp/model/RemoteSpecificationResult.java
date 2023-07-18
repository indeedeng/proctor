package com.indeed.proctor.webapp.model;

import com.google.common.base.Strings;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A result of attempt to fetch specifications of a single app version from multiple instances This
 * class has either of the following two states
 *
 * <p>- succeed to fetch specifications from one of instances. `clientApplication` and
 * `specifications` is not null
 *
 * <p>- failed to fetch specifications from all instances. `clientApplication` and `specifications`
 * is null `remaining` may be non-empty when it's identified that no instance likely doesn't expose
 * specifications
 *
 * <p>This class is exposed in a endpoint /proctor/specification?branch=qa&version=***&app=***
 */
public class RemoteSpecificationResult {
    private final AppVersion version;

    private final Map<ProctorClientApplication, Throwable> failures;

    @Nullable private final ProctorClientApplication clientApplication;
    @Nullable private final ProctorSpecifications specifications;

    private RemoteSpecificationResult(
            final AppVersion version,
            final Map<ProctorClientApplication, Throwable> failures,
            @Nullable final ProctorClientApplication clientApplication,
            @Nullable final ProctorSpecifications specifications) {
        this.version = version;
        this.failures = failures;
        this.clientApplication = clientApplication;
        this.specifications = specifications;
    }

    public boolean isSuccess() {
        return specifications != null;
    }

    public AppVersion getVersion() {
        return version;
    }

    public Map<ProctorClientApplication, String> getFailures() {
        return failures.entrySet().stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                // getMessage() may return null
                                e -> Strings.nullToEmpty(e.getValue().getMessage())));
    }

    // exposed as json
    @CheckForNull
    public ProctorClientApplication getClientApplication() {
        return clientApplication;
    }

    @CheckForNull
    public ProctorSpecifications getSpecifications() {
        return specifications;
    }

    public static RemoteSpecificationResult success(
            final AppVersion version,
            final ProctorClientApplication client,
            final ProctorSpecifications result) {
        return new RemoteSpecificationResult(version, Collections.emptyMap(), client, result);
    }

    public static RemoteSpecificationResult failures(
            final AppVersion version, final Map<ProctorClientApplication, Throwable> failures) {
        return new RemoteSpecificationResult(version, failures, null, null);
    }
}
