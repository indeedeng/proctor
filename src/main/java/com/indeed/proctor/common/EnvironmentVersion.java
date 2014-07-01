package com.indeed.proctor.common;

import com.indeed.proctor.store.Revision;
import com.indeed.proctor.webapp.db.Environment;

/**
* @author parker
*/
public class EnvironmentVersion {
    public static final String UNKNOWN_REVISION = "-1";
    public static final String UNKNOWN_VERSION = "-1";

    private final String testName;

    // @Nullable - The last commit on the trunk branch
    private final Revision trunk;
    // @Nullable - The last commit on the qa branch
    private final Revision qa;
    private final String qaEffectiveRevision; // "effective" revision, aka the 'version' number from the TestDefinition on the Production Branch. This should refer to a revision on the TRUNK branch

    // @Nullable - The last commit on the production
    private final Revision production;
    private final String productionEffectiveRevision; // "effective" revision, aka the 'version' number from the TestDefinition on the Production Branch. This should refer to a revision on the TRUNK branch

    public EnvironmentVersion(final String testName,
                              final Revision trunk,
                              final Revision qa,
                              final String qaEffectiveRevision,
                              final Revision production,
                              final String productionEffectiveRevision) {
        this.testName = testName;
        this.trunk = trunk;
        this.qa = qa;
        this.qaEffectiveRevision = qaEffectiveRevision;
        this.production = production;
        this.productionEffectiveRevision = productionEffectiveRevision;
    }

    /**
     * Creates a new EnvironmentVersion modifying the appropriate version and
     * effectiveRevision.
     *
     * @param branch
     * @param version
     * @param effectiveRevision
     * @return
     */
    public EnvironmentVersion update(final Environment branch, final Revision version, final String effectiveRevision ) {
        return new EnvironmentVersion(testName ,
                                      Environment.WORKING == branch ? version : this.trunk,
                                      Environment.QA == branch ? version : this.qa,
                                      Environment.QA == branch ? effectiveRevision : this.qaEffectiveRevision,
                                      Environment.PRODUCTION == branch ? version : this.production,
                                      Environment.PRODUCTION == branch ? effectiveRevision : this.productionEffectiveRevision
                                      );
    }

    public String getTestName() {
        return testName;
    }

    // @Nullable
    public Revision getTrunk() {
        return trunk;
    }

    public String getTrunkRevision() {
        return getRevision(trunk);
    }

    public String getTrunkVersion() {
        return getTrunkRevision();
    }

    // @Nullable
    public Revision getQa() {
        return qa;
    }

    public String getQaRevision() {
        return getRevision(qa);
    }

    public String getQaVersion() {
        return qaEffectiveRevision;
    }

    // @Nullable
    public Revision getProduction() {
        return production;
    }

    public String getProductionRevision() {
        return getRevision(production);
    }


    public String getProductionVersion() {
        return productionEffectiveRevision;
    }

    public String getRevision(final Environment branch) {
        switch (branch) {
            case WORKING:
                return getTrunkRevision();
            case QA:
                return getQaRevision();
            case PRODUCTION:
                return getProductionRevision();
            default:
                return UNKNOWN_REVISION;
        }
    }

    public String getVersion(final Environment branch) {
        switch (branch) {
            case WORKING:
                return getTrunkVersion();
            case QA:
                return getQaVersion();
            case PRODUCTION:
                return getProductionVersion();
            default:
                return UNKNOWN_VERSION;
        }
    }

    private static String getRevision(Revision version) {
        return version != null ? version.getRevision() : UNKNOWN_REVISION;
    }
}
