package com.indeed.proctor.webapp.tags;

import com.google.common.collect.ImmutableMap;
import com.indeed.proctor.common.EnvironmentVersion;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.webapp.db.Environment;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;
import java.util.Map;

/**
 * Tests for tag functions : TestDefinitionFunctions.java
 */
public class TestTestDefinitionFunctions {

    /* Example Commit History:
        r134 - INDAPPW-537: Feature flag for html5 history based question navigation
        r135 - Promoting iahtml5qsthisttst (trunk r134) to qa
        r136 - Promoting iahtml5qsthisttst (qa r135) to production
               Promoting iahtml5qsthisttst (trunk r134) to qa
        r137 - INDAPPW-537: Adjusting test ratio allocations
        r138 - Promoting iahtml5qsthisttst (trunk r137) to production
        r139 - Promoting iahtml5qsthisttst (trunk r137) to qa
        r140 - Promoting iahtml5qsthisttst (qa r135) to production
               Promoting iahtml5qsthisttst (trunk r134) to qa
     */
    private static final String WHO = "developer-A";
    private static final Date WHEN = new Date(2012, 01, 12);
    private static final Map<String, Revision> PROMOTED_REVISIONS = ImmutableMap.<String, Revision>builder()
        .put("134", new Revision("134", WHO, WHEN, "INDAPPW-537: Feature flag for html5 history based question navigation"))
        .put("135", new Revision("135", WHO, WHEN, "Promoting iahtml5qsthisttst (trunk r134) to qa"))
        .put("136", new Revision("136", WHO, WHEN, "Promoting iahtml5qsthisttst (qa r135) to production\nPromoting iahtml5qsthisttst (trunk r134) to qa"))
        .put("137", new Revision("137", WHO, WHEN, "INDAPPW-537: Adjusting test ratio allocations"))
        .put("138", new Revision("138", WHO, WHEN, "Promoting iahtml5qsthisttst (trunk r137) to production"))
        .put("139", new Revision("139", WHO, WHEN, "Promoting iahtml5qsthisttst (trunk r137) to qa"))
        .put("140", new Revision("140", WHO, WHEN, "Promoting iahtml5qsthisttst (qa r135) to production\nPromoting iahtml5qsthisttst (trunk r134) to qa"))
        .build();
    private static final EnvironmentVersion PROMOTED_STATE_OF_THE_WORLD = new EnvironmentVersion("iahtml5qsthisttst",
                                                                      // current trunk revision
                                                                      PROMOTED_REVISIONS.get("137"),
                                                                      // QA revision = 139, promoted from Trunk r137 (effective version)
                                                                      PROMOTED_REVISIONS.get("139"), "137",
                                                                      // PROD revision = 140, promoted from Trunk r134 (effective version)
                                                                      PROMOTED_REVISIONS.get("140"), "134");

    /*
        Example Charm Commit history:
        (trunk branch)
        r178090 - COMP-1654 - changed the rule to check userlanguage, rather than jasxlanguage
        r182781 - COMP-1683 - Roll out the test 100% globally

        (qa branch)
        r178100 - merged r178090: COMP-1654 - changed the rule to check userlanguage, rather than jasxlanguage
        r182787 - merged r182781: COMP-1683 - Roll out the test 100% globally

        (production branch)
        r178374 - merged r178090: COMP-1654 - changed the rule to check userlanguage, rather than jasxlanguage
        ( not yet merged r182781)
     */
    private static final Map<String, Revision> CHARMED_REVISIONS = ImmutableMap.<String, Revision>builder()
        // trunk
        .put("178090", new Revision("178090", WHO, WHEN, "COMP-1654 - changed the rule to check userlanguage, rather than jasxlanguage"))
        .put("182781", new Revision("182781", WHO, WHEN, "COMP-1683 - Roll out the test 100% globally"))

        // QA
        .put("178100", new Revision("178100", WHO, WHEN, "merged r178090: COMP-1654 - changed the rule to check userlanguage, rather than jasxlanguage"))
        .put("182787", new Revision("182787", WHO, WHEN, "merged r182781: COMP-1683 - Roll out the test 100% globally"))

        // production
        .put("178374", new Revision("178374", WHO, WHEN, "merged r178090: COMP-1654 - changed the rule to check userlanguage, rather than jasxlanguage"))
        .build();
    private static final EnvironmentVersion CHARMED_STATE_OF_THE_WORLD = new EnvironmentVersion("acmecmpattrtst",
                                                                      // current trunk revision
                                                                      CHARMED_REVISIONS.get("182781"),
                                                                      // QA revision = 182787, last merged trunk revision r182781 (effective version)
                                                                      CHARMED_REVISIONS.get("182787"), "182781",
                                                                      // PROD revision = 178374, merged from trunk revision 178090 (effective version)
                                                                      CHARMED_REVISIONS.get("178374"), "178090");



    @Test
    public void testIsCurrentVersionViewingTrunkPromoted() {
        final Revision r134 = PROMOTED_REVISIONS.get("134");
        final Revision r137 = PROMOTED_REVISIONS.get("137");

        final Environment viewing = Environment.WORKING;
        Assert.assertFalse("r134 is not current trunk revision", TestDefinitionFunctions.isCurrentVersionOnTrunk(viewing, r134, PROMOTED_STATE_OF_THE_WORLD));
        Assert.assertTrue("r134 is current trunk revision", TestDefinitionFunctions.isCurrentVersionOnTrunk(viewing, r137, PROMOTED_STATE_OF_THE_WORLD));

        // r134 is not current qa version
        Assert.assertFalse("r134 is not current qa version", TestDefinitionFunctions.isCurrentVersionOnQa(viewing, r134, PROMOTED_STATE_OF_THE_WORLD));
        // r137 is current QA.version
        Assert.assertTrue("r137 is current qa version", TestDefinitionFunctions.isCurrentVersionOnQa(viewing, r137, PROMOTED_STATE_OF_THE_WORLD));

        // r134 is current production version (promoted from QA r135 (trunk r134)
        Assert.assertTrue("r134 is current production version", TestDefinitionFunctions.isCurrentVersionOnProduction(viewing, r134, PROMOTED_STATE_OF_THE_WORLD));
        // r137 is current QA.version
        Assert.assertFalse("r137 is not current production version", TestDefinitionFunctions.isCurrentVersionOnProduction(viewing, r137, PROMOTED_STATE_OF_THE_WORLD));
    }

    @Test
    public void testIsCurrentVersionViewingQaPromoted() {
        final Revision r135 = PROMOTED_REVISIONS.get("135");
        final Revision r139 = PROMOTED_REVISIONS.get("139");

        final Environment viewing = Environment.QA;

        Assert.assertFalse("r135 is promoted version of trunk revision r134 (not current trunk)", TestDefinitionFunctions.isCurrentVersionOnTrunk(viewing, r135, PROMOTED_STATE_OF_THE_WORLD));
        Assert.assertTrue("r139 is promoted version of trunk revision r137 (current trunk)", TestDefinitionFunctions.isCurrentVersionOnTrunk(viewing, r139, PROMOTED_STATE_OF_THE_WORLD));

        Assert.assertFalse("r135 is not current qa revision", TestDefinitionFunctions.isCurrentVersionOnQa(viewing, r135, PROMOTED_STATE_OF_THE_WORLD));
        Assert.assertTrue("r139 is current qa revision", TestDefinitionFunctions.isCurrentVersionOnQa(viewing, r139, PROMOTED_STATE_OF_THE_WORLD));

        // r135 promoted to production (r140) (trunk r134)
        Assert.assertTrue("r135 is promoted version of trunk r134 which is current production.version", TestDefinitionFunctions.isCurrentVersionOnProduction(viewing, r135, PROMOTED_STATE_OF_THE_WORLD));
        Assert.assertFalse("r139 is not promoted version of production version", TestDefinitionFunctions.isCurrentVersionOnProduction(viewing, r139, PROMOTED_STATE_OF_THE_WORLD));
    }

    @Test
    public void testIsCurrentVersionViewingProductionPromoted() {
        final Revision r136 = PROMOTED_REVISIONS.get("136");
        final Revision r138 = PROMOTED_REVISIONS.get("138");
        final Revision r140 = PROMOTED_REVISIONS.get("140");

        final Environment viewing = Environment.PRODUCTION;

        Assert.assertFalse("r136 is promoted version of trunk revision r134 (not current trunk)", TestDefinitionFunctions.isCurrentVersionOnTrunk(viewing, r136, PROMOTED_STATE_OF_THE_WORLD));
        Assert.assertTrue("r138 is promoted version of trunk revision r137 (current trunk.revision)", TestDefinitionFunctions.isCurrentVersionOnTrunk(viewing, r138, PROMOTED_STATE_OF_THE_WORLD));
        Assert.assertFalse("r140 is promoted version of trunk revision r134 (not current trunk)", TestDefinitionFunctions.isCurrentVersionOnTrunk(viewing, r140, PROMOTED_STATE_OF_THE_WORLD));

        // r136 promoted to production (r140) (trunk r134)
        Assert.assertFalse("r136 is qa r135, trunk r134 neither are current QA.versions", TestDefinitionFunctions.isCurrentVersionOnQa(viewing, r136, PROMOTED_STATE_OF_THE_WORLD));
        Assert.assertTrue("r138 is promoted version of trunk revision r137 (current qa.version)", TestDefinitionFunctions.isCurrentVersionOnQa(viewing, r138, PROMOTED_STATE_OF_THE_WORLD));
        Assert.assertFalse("r140 is qa r135, trunk r134 neither are current QA.versions", TestDefinitionFunctions.isCurrentVersionOnQa(viewing, r140, PROMOTED_STATE_OF_THE_WORLD));

        Assert.assertFalse("r136 is not current production.revision (r140)", TestDefinitionFunctions.isCurrentVersionOnProduction(viewing, r136, PROMOTED_STATE_OF_THE_WORLD));
        Assert.assertFalse("r138 is not current production.revision (r140)", TestDefinitionFunctions.isCurrentVersionOnProduction(viewing, r138, PROMOTED_STATE_OF_THE_WORLD));
        Assert.assertTrue("r140 is current production.revision", TestDefinitionFunctions.isCurrentVersionOnProduction(viewing, r140, PROMOTED_STATE_OF_THE_WORLD));
    }

    @Test
    public void testIsCurrentVersionViewingTrunkCharmed() {
        final Revision r178090 = CHARMED_REVISIONS.get("178090");
        final Revision r182781 = CHARMED_REVISIONS.get("182781");

        final Environment viewing = Environment.WORKING;
        Assert.assertFalse("r178090 is not current trunk revision", TestDefinitionFunctions.isCurrentVersionOnTrunk(viewing, r178090, CHARMED_STATE_OF_THE_WORLD));
        Assert.assertTrue("r182781 is current trunk revision", TestDefinitionFunctions.isCurrentVersionOnTrunk(viewing, r182781, CHARMED_STATE_OF_THE_WORLD));

        Assert.assertFalse("r178090 is not current qa.version (r182781)", TestDefinitionFunctions.isCurrentVersionOnQa(viewing, r178090, CHARMED_STATE_OF_THE_WORLD));
        Assert.assertTrue("r182781 is current qa.version (r182781)", TestDefinitionFunctions.isCurrentVersionOnQa(viewing, r182781, CHARMED_STATE_OF_THE_WORLD));

        Assert.assertTrue("r178090 is current production.version (r178090)", TestDefinitionFunctions.isCurrentVersionOnProduction(viewing, r178090, CHARMED_STATE_OF_THE_WORLD));
        Assert.assertFalse("r182781 is not current production.version (r178090)", TestDefinitionFunctions.isCurrentVersionOnProduction(viewing, r182781, CHARMED_STATE_OF_THE_WORLD));
    }

    @Test
    public void testIsCurrentVersionViewingQaCharmed() {
        final Revision r178100 = CHARMED_REVISIONS.get("178100");
        final Revision r182787 = CHARMED_REVISIONS.get("182787");

        final Environment viewing = Environment.QA;
        Assert.assertFalse("r178100 is merged r178090 (not current trunk revision)", TestDefinitionFunctions.isCurrentVersionOnTrunk(viewing, r178100, CHARMED_STATE_OF_THE_WORLD));
        Assert.assertTrue("r182787 is merged r182781 (current trunk revision)", TestDefinitionFunctions.isCurrentVersionOnTrunk(viewing, r182787, CHARMED_STATE_OF_THE_WORLD));

        Assert.assertFalse("r178100 is not current qa.revision", TestDefinitionFunctions.isCurrentVersionOnQa(viewing, r178100, CHARMED_STATE_OF_THE_WORLD));
        Assert.assertTrue("r182787 is current qa.revision", TestDefinitionFunctions.isCurrentVersionOnQa(viewing, r182787, CHARMED_STATE_OF_THE_WORLD));

        Assert.assertTrue("r178100 is merged r178090 (current production.version r178090)", TestDefinitionFunctions.isCurrentVersionOnProduction(viewing, r178100, CHARMED_STATE_OF_THE_WORLD));
        Assert.assertFalse("r182787 is merged r182781 (current production.version r178090)", TestDefinitionFunctions.isCurrentVersionOnProduction(viewing, r182787, CHARMED_STATE_OF_THE_WORLD));
    }

    @Test
    public void testIsCurrentVersionViewingProductionCharmed() {
        final Revision r178374 = CHARMED_REVISIONS.get("178374");

        final Environment viewing = Environment.PRODUCTION;
        Assert.assertFalse("r178374 is merged r178090 (not current trunk revision)", TestDefinitionFunctions.isCurrentVersionOnTrunk(viewing, r178374, CHARMED_STATE_OF_THE_WORLD));

        Assert.assertFalse("r178374 is merged r178090 not current qa.version (r182781)", TestDefinitionFunctions.isCurrentVersionOnQa(viewing, r178374, CHARMED_STATE_OF_THE_WORLD));

        Assert.assertTrue("r178374 current production.revision", TestDefinitionFunctions.isCurrentVersionOnProduction(viewing, r178374, CHARMED_STATE_OF_THE_WORLD));
    }
}
