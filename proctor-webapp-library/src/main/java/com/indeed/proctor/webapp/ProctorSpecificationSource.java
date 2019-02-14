package com.indeed.proctor.webapp;

import com.indeed.proctor.common.ProctorSpecification;
import com.indeed.proctor.webapp.db.Environment;
import com.indeed.proctor.webapp.model.AppVersion;
import com.indeed.proctor.webapp.model.RemoteSpecificationResult;

import java.util.Map;
import java.util.Set;

/**
 * @author parker
 */
public interface ProctorSpecificationSource {

    Map<AppVersion,RemoteSpecificationResult> loadAllSpecifications(Environment environment);

    Map<AppVersion, ProctorSpecification> loadAllSuccessfulSpecifications(Environment environment);

    Set<AppVersion> activeClients(Environment environment, String testName);

    Set<String> activeTests(Environment environment);

    RemoteSpecificationResult getRemoteResult(Environment environment, AppVersion version);
}
