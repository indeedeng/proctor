package com.indeed.proctor.webapp.tags;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;
import com.indeed.proctor.common.EnvironmentVersion;
import com.indeed.proctor.common.Serializers;
import com.indeed.proctor.common.model.Allocation;
import com.indeed.proctor.common.model.Range;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestDefinition;
import com.indeed.proctor.store.Revision;
import com.indeed.proctor.webapp.db.Environment;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.JsonParseException;


import java.io.IOException;
import java.util.Map;

/**
 * @author parker
 */
public final class TestDefinitionFunctions {
    private static final ObjectMapper objectMapper = Serializers.strict();

    private TestDefinitionFunctions() { throw new UnsupportedOperationException("Static class"); }

    public static TestBucket getTestBucketForRange(final TestDefinition definition, final Range range) {
        for(TestBucket tb : definition.getBuckets()) {
            if(tb.getValue() == range.getBucketValue()) {
                return tb;
            }
        }
        return null;
    }

    /**
     *
     * @param viewing
     * @param history
     * @param version
     * @return
     */
    public static boolean isCurrentVersionOnTrunk(final Environment viewing,
                                                  final Revision history,
                                                  final EnvironmentVersion version) {
        if(version == null) { return false; }
        switch (viewing) {
            case QA:
            case PRODUCTION:
                // When viewing QA/TRUNK, look for (trunk r{Revision})
                return isPromotedRevision(history, Environment.WORKING, version.getTrunkRevision()) ||
                       isCharmedRevision(history, version.getTrunkRevision());
            case WORKING:
            default:
                return history.getRevision() == version.getTrunkRevision();
        }
    }

    public static boolean isCurrentVersionOnQa(final Environment viewing,
                                               final Revision history,
                                               final EnvironmentVersion version) {
        if(version == null) { return false; }
        switch (viewing) {
            case WORKING:
                // trunk.revision gets set to qa.version during promotion
                return history.getRevision() == version.getQaVersion();
            case PRODUCTION:
                // viewing production history:
                // (qa r{qa.revision}) - this was a promotion from QA
                // or (trunk r{qa.version}) - this was a promotion from trunk that is the same as the current version running on QA
                return isPromotedRevision(history, Environment.QA, version.getQaRevision()) ||
                       isPromotedRevision(history, Environment.WORKING, version.getQaVersion()) ||
                       isCharmedRevision(history, version.getQaVersion());
            case QA:
            default:
                return history.getRevision() == version.getQaRevision();
        }
    }

    public static boolean isCurrentVersionOnProduction(final Environment viewing,
                                                       final Revision history,
                                                       final EnvironmentVersion version) {
        if(version == null) { return false; }
        switch (viewing) {
            case WORKING:
                // trunk.revision gets set to production.version during promotion
                return history.getRevision() == version.getProductionVersion();
            case QA:
                // viewing qa history:
                // (trunk r{qa.version}) - the same trunk revision was promoted to production, either by QA->PROD or TRUNK->PROD
                return isPromotedRevision(history, Environment.WORKING, version.getProductionVersion()) ||
                       isCharmedRevision(history, version.getProductionVersion());
            case PRODUCTION:
            default:
                return history.getRevision() == version.getProductionRevision();
        }
    }

    /**
     * Returns a flag whose value indicates if the history entry corresponds to the promoted version from the provided Branch+revision.
     *
     * When looking at the history of the trunk branch, because we track the trunk version as each test's 'Version' value (in the definition.json file),
     * it's sufficient to compare the history.revision == definition.version.
     *
     * When looking at the history on the qa branch, the history.revision will never equal the definition.version because definition.version corresponds to a revision on trunk.
     * When we promote from trunk -> QA, we format the commit message as "Promoting {testName} (trunk rXXX) to qa"
     * If we look for (trunk rXXX) we should be able to identify the commit on QA that of the current definition.version.
     *
     * @param history
     * @param source
     * @param revision
     * @return
     */
    private static boolean isPromotedRevision(Revision history, Environment source, String revision) {
        // Look for <branch> rXXXX in the commit message
        final String needle = String.format("%s r%s", source.getName(), revision);
        return history.getMessage().contains(needle);
    }

    private static boolean isCharmedRevision(Revision history, String revision) {
        // Look for "merged rXXXX: in the commit message
        final String needle = String.format("merged r%s:", revision);
        return history.getMessage().startsWith(needle);
    }


    // Annoyingly there isn't a way to check map.contains via jsp, you cannot even access the keys Collection directly
    public static boolean containsKey(Map m, Object key) {
        return m.containsKey(key);
    }

    public static TestDefinition parseTestDefinition(final String testDefinition) throws IOException, JsonParseException, JsonMappingException {
        final TestDefinition td = objectMapper.readValue(testDefinition, TestDefinition.class);
        // Until (PROC-72) is resolved, all of the 'empty' rules should get saved as NULL rules.
            if (CharMatcher.WHITESPACE.matchesAllOf(Strings.nullToEmpty(td.getRule()))) {
                td.setRule(null);
            }
        for (final Allocation ac : td.getAllocations()) {
            if (CharMatcher.WHITESPACE.matchesAllOf(Strings.nullToEmpty(ac.getRule()))) {
                ac.setRule(null);
            }
        }
        return td;
    }


}
