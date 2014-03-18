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
    private static final Map<Long, Revision> PROMOTED_REVISIONS = ImmutableMap.<Long, Revision>builder()
        .put(134L, new Revision(134L, WHO, WHEN, "INDAPPW-537: Feature flag for html5 history based question navigation"))
        .put(135L, new Revision(135L, WHO, WHEN, "Promoting iahtml5qsthisttst (trunk r134) to qa"))
        .put(136L, new Revision(136L, WHO, WHEN, "Promoting iahtml5qsthisttst (qa r135) to production\nPromoting iahtml5qsthisttst (trunk r134) to qa"))
        .put(137L, new Revision(137L, WHO, WHEN, "INDAPPW-537: Adjusting test ratio allocations"))
        .put(138L, new Revision(138L, WHO, WHEN, "Promoting iahtml5qsthisttst (trunk r137) to production"))
        .put(139L, new Revision(139L, WHO, WHEN, "Promoting iahtml5qsthisttst (trunk r137) to qa"))
        .put(140L, new Revision(140L, WHO, WHEN, "Promoting iahtml5qsthisttst (qa r135) to production\nPromoting iahtml5qsthisttst (trunk r134) to qa"))
        .build();
    private static final EnvironmentVersion PROMOTED_STATE_OF_THE_WORLD = new EnvironmentVersion("iahtml5qsthisttst",
                                                                      // current trunk revision
                                                                      PROMOTED_REVISIONS.get(137L),
                                                                      // QA revision = 139, promoted from Trunk r137 (effective version)
                                                                      PROMOTED_REVISIONS.get(139L), 137L,
                                                                      // PROD revision = 140, promoted from Trunk r134 (effective version)
                                                                      PROMOTED_REVISIONS.get(140L), 134L);

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
    private static final Map<Long, Revision> CHARMED_REVISIONS = ImmutableMap.<Long, Revision>builder()
        // trunk
        .put(178090L, new Revision(178090L, WHO, WHEN, "COMP-1654 - changed the rule to check userlanguage, rather than jasxlanguage"))
        .put(182781L, new Revision(182781L, WHO, WHEN, "COMP-1683 - Roll out the test 100% globally"))

        // QA
        .put(178100L, new Revision(178100L, WHO, WHEN, "merged r178090: COMP-1654 - changed the rule to check userlanguage, rather than jasxlanguage"))
        .put(182787L, new Revision(182787L, WHO, WHEN, "merged r182781: COMP-1683 - Roll out the test 100% globally"))

        // production
        .put(178374L, new Revision(178374L, WHO, WHEN, "merged r178090: COMP-1654 - changed the rule to check userlanguage, rather than jasxlanguage"))
        .build();
    private static final EnvironmentVersion CHARMED_STATE_OF_THE_WORLD = new EnvironmentVersion("acmecmpattrtst",
                                                                      // current trunk revision
                                                                      CHARMED_REVISIONS.get(182781L),
                                                                      // QA revision = 182787, last merged trunk revision r182781 (effective version)
                                                                      CHARMED_REVISIONS.get(182787L), 182781L,
                                                                      // PROD revision = 178374, merged from trunk revision 178090 (effective version)
                                                                      CHARMED_REVISIONS.get(178374L), 178090);



    @Test
    public void testIsCurrentVersionViewingTrunkPromoted() {
        final Revision r134 = PROMOTED_REVISIONS.get(134L);
        final Revision r137 = PROMOTED_REVISIONS.get(137L);

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
        final Revision r135 = PROMOTED_REVISIONS.get(135L);
        final Revision r139 = PROMOTED_REVISIONS.get(139L);

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
        final Revision r136 = PROMOTED_REVISIONS.get(136L);
        final Revision r138 = PROMOTED_REVISIONS.get(138L);
        final Revision r140 = PROMOTED_REVISIONS.get(140L);

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
        final Revision r178090 = CHARMED_REVISIONS.get(178090L);
        final Revision r182781 = CHARMED_REVISIONS.get(182781L);

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
        final Revision r178100 = CHARMED_REVISIONS.get(178100L);
        final Revision r182787 = CHARMED_REVISIONS.get(182787L);

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
        final Revision r178374 = CHARMED_REVISIONS.get(178374L);

        final Environment viewing = Environment.PRODUCTION;
        Assert.assertFalse("r178374 is merged r178090 (not current trunk revision)", TestDefinitionFunctions.isCurrentVersionOnTrunk(viewing, r178374, CHARMED_STATE_OF_THE_WORLD));

        Assert.assertFalse("r178374 is merged r178090 not current qa.version (r182781)", TestDefinitionFunctions.isCurrentVersionOnQa(viewing, r178374, CHARMED_STATE_OF_THE_WORLD));

        Assert.assertTrue("r178374 current production.revision", TestDefinitionFunctions.isCurrentVersionOnProduction(viewing, r178374, CHARMED_STATE_OF_THE_WORLD));
    }
}
