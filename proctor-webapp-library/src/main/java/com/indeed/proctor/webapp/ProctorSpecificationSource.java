package com.indeed.proctor.webapp;

import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.model.AppVersion;
import com.indeed.proctor.webapp.model.ProctorSpecifications;
import com.indeed.proctor.webapp.model.RemoteSpecificationResult;

import java.util.Map;
import java.util.Set;

/** @author parker */
public interface ProctorSpecificationSource {
    /** @return a map from application version to load result of proctor specification */
    Map<AppVersion, RemoteSpecificationResult> loadAllSpecifications(Environment environment);

    /**
     * @return a map from application version to proctor specifications for instances that proctor
     *     webapp can successfully load from
     */
    Map<AppVersion, ProctorSpecifications> loadAllSuccessfulSpecifications(Environment environment);

    /** @return a set of applications with version that uses the test in the environment. */
    Set<AppVersion> activeClients(Environment environment, String testName);

    /**
     * @return a set of test names that are used in one of applications tracked from proctor webapp
     */
    Set<String> activeTests(Environment environment);

    /** @return a load result of proctor specification of the app version in the environment */
    RemoteSpecificationResult getRemoteResult(Environment environment, AppVersion version);
}
