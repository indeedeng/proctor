package com.indeed.proctor.common;

import com.indeed.proctor.store.Revision;
import com.indeed.proctor.webapp.db.Environment;

import java.util.Date;

/** @author parker */
public class EnvironmentVersion {
    public static final String UNKNOWN_REVISION = "-1";
    public static final String UNKNOWN_VERSION = "-1";
    public static final Revision FULL_UNKNOWN_REVISION =
            new Revision(UNKNOWN_REVISION, "[unknown]", new Date(0), "History unknown");

    private final String testName;

    // @Nullable - The last commit on the trunk branch
    private final Revision trunk;
    private final String
            trunkEffectiveRevision; // "effective" revision, used to bridge between different
    // Proctor stores. This will be an SVN revision if the commit
    // was copied from SVN.
    // @Nullable - The last commit on the qa branch
    private final Revision qa;
    private final String
            qaEffectiveRevision; // "effective" revision, aka the 'version' number from the
    // TestDefinition on the Production Branch. This should refer to a
    // revision on the TRUNK branch

    // @Nullable - The last commit on the production
    private final Revision production;
    private final String
            productionEffectiveRevision; // "effective" revision, aka the 'version' number from the
    // TestDefinition on the Production Branch. This should
    // refer to a revision on the TRUNK branch

    public EnvironmentVersion(
            final String testName,
            final Revision trunk,
            final String trunkEffectiveRevision,
            final Revision qa,
            final String qaEffectiveRevision,
            final Revision production,
            final String productionEffectiveRevision) {
        this.testName = testName;
        this.trunk = trunk;
        this.trunkEffectiveRevision = trunkEffectiveRevision;
        this.qa = qa;
        this.qaEffectiveRevision = qaEffectiveRevision;
        this.production = production;
        this.productionEffectiveRevision = productionEffectiveRevision;
    }

    public EnvironmentVersion(
            final String testName,
            final Revision trunk,
            final Revision qa,
            final String qaEffectiveRevision,
            final Revision production,
            final String productionEffectiveRevision) {
        this(
                testName,
                trunk,
                trunk.getRevision(),
                qa,
                qaEffectiveRevision,
                production,
                productionEffectiveRevision);
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
        return trunkEffectiveRevision;
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

    /**
     * Returns a String representing the revision associated with the branch.
     *
     * @param branch
     * @return
     */
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

    /**
     * Returns a {@link com.indeed.proctor.store.Revision} object associated with the branch.
     *
     * @param branch
     * @return
     */
    public Revision getFullRevision(final Environment branch) {
        switch (branch) {
            case WORKING:
                return trunk;
            case QA:
                return qa;
            case PRODUCTION:
                return production;
            default:
                return FULL_UNKNOWN_REVISION;
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
