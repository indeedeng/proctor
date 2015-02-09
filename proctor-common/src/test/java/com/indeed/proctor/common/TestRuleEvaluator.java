package com.indeed.proctor.common;

import com.indeed.util.core.ReleaseVersion;
import com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

/**
 * @author parker
 */
public class TestRuleEvaluator {

    private RuleEvaluator ruleEvaluator;

    @Before
    public void setUp() throws Exception {
        final Map<String, Object> testConstants = Collections.<String, Object>singletonMap("LANGUAGES_ENABLED", Lists.newArrayList("en", "fr", "de"));
        ruleEvaluator = new RuleEvaluator(RuleEvaluator.EXPRESSION_FACTORY, RuleEvaluator.FUNCTION_MAPPER, testConstants);
    }

    @Test
    public void testNullRule() {
        Assert.assertTrue("null rule should be true", ruleEvaluator.evaluateBooleanRule(null, Collections.<String, Object>emptyMap()));
    }

    @Test
    public void testEmptyRule() {
        for(final String rule : new String[] { "", " ", "${}", "${   }"}) {
            Assert.assertTrue("empty rule '" + rule + "' should be true", ruleEvaluator.evaluateBooleanRule(rule, Collections.<String, Object>emptyMap()));
        }
    }

    @Test
    public void testLiteralRule() {
        for(final String rule : new String[] { "${true}", "${TRUE}", "${ TRUE }" }) {
            Assert.assertTrue("rule '" + rule + "' should be true", ruleEvaluator.evaluateBooleanRule(rule, Collections.<String, Object>emptyMap()));
        }
        for(final String rule : new String[] { "${false}", "${FALSE}", "${ FALSE }" }) {
            Assert.assertFalse("rule '" + rule + "' should be false", ruleEvaluator.evaluateBooleanRule(rule, Collections.<String, Object>emptyMap()));
        }
    }

    @Test
    public void testMalformedRuleShouldBeFalse() {
        for(final String rule : new String[] { "true", "TRUE", "FALSE", "false", " ${true} ", " ${ true } " }) {
            Assert.assertFalse("malformed rule '" + rule + "' should be false", ruleEvaluator.evaluateBooleanRule(rule, Collections.<String, Object>emptyMap()));
        }
    }

    @Test
    public void testElExpressionsShouldBeAvailable() {
        final Map<String, Object> values = Collections.<String, Object>singletonMap("lang", "en");
        {
            final String rule = "${lang == 'en'}";
            Assert.assertTrue("rule '" + rule + "' should be true for " + values, ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final String rule = "${lang == 'fr'}";
            Assert.assertFalse("rule '" + rule + "' should be false for " + values, ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testUtilTagFunctionsShouldBeAvailable() {
        final String rule = "${proctor:contains(LANGUAGES_ENABLED, lang)}";
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("lang", "en");
            Assert.assertTrue("rule '" + rule + "' should be true for " + values, ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("lang", "it");
            Assert.assertFalse("rule '" + rule + "' should be false for " + values, ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testStandardLibFunctionsBeAvailable() {
        final String rule = "${fn:startsWith(lang, 'en')}";
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("lang", "en_US");
            Assert.assertTrue("rule '" + rule + "' should be true for " + values, ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("lang", "en");
            Assert.assertTrue("rule '" + rule + "' should be true for " + values, ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("lang", "fr");
            Assert.assertFalse("rule '" + rule + "' should be false for " + values, ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testRegexMatches() {
        final String rule = "${proctor:matches(value, '^#[0-9a-fA-F]{3,6}$')}";
        {
            for (final String validHex : new String[]{"#000", "#0000", "#00000", "#AEFFB3", "#aE3FB3"}) {
                final Map<String, Object> values = Collections.<String, Object>singletonMap("value", validHex);
                Assert.assertTrue("rule '" + rule + "' should be true for " + values, ruleEvaluator.evaluateBooleanRule(rule, values));
            }
        }
        {
            for (final String invalidHex : new String[]{"#00", "abc3f", "#000000d", "abe4z ", ""}) {
                final Map<String, Object> values = Collections.<String, Object>singletonMap("value", invalidHex);
                Assert.assertFalse("rule '" + rule + "' should be false for " + values, ruleEvaluator.evaluateBooleanRule(rule, values));
            }
        }
    }

    @Test
    public void testVersionLessThanWildCard() {
        final String rule = "${version < proctor:version('1.2.x')}";
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("1.1.0.0"));
            Assert.assertTrue("1.1 < 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("1.1.9.500"));
            Assert.assertTrue("1.1.9.500 < 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("1.2.0.1"));
            Assert.assertFalse("1.2.0.1 !< 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("1.3.0.0"));
            Assert.assertFalse("1.3 !< 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("2.0.0.0"));
            Assert.assertFalse("2.0 !< 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testVersionLessThan() {
        final String rule = "${version < proctor:version('1.2.0.0')}";
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("1.1.0.0"));
            Assert.assertTrue("1.1 < 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("1.1.9.500"));
            Assert.assertTrue("1.1.9.500 < 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("1.2.0.1"));
            Assert.assertFalse("1.2.0.1 !< 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("1.3.0.0"));
            Assert.assertFalse("1.3 !< 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("2.0.0.0"));
            Assert.assertFalse("2.0 !< 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testVersionLessThanEqualWildCard() {
        final String rule = "${version <= proctor:version('1.2.x')}";
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("1.2.0.0"));
            Assert.assertTrue("1.2.0.0 <= 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testVersionLessThanEqual() {
        final String rule = "${version <= proctor:version('1.2.0.0')}";
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("1.2.0.0"));
            Assert.assertTrue("1.2.0.0 <= 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testVersionEqualWildCard() {
        final String rule = "${version == proctor:version('5.4.3.x')}";
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("5.4.3.0"));
            Assert.assertTrue("5.4.3.0 == 5.4.3.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("5.4.3.200"));
            Assert.assertTrue("5.4.3.200 == 5.4.3.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testVersionEqual() {
        final String rule = "${version == proctor:version('5.4.3.0')}";
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("5.4.3.0"));
            Assert.assertTrue("5.4.3.0 == 5.4.3", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testVersionGreaterThanWildCard() {
        final String rule = "${version > proctor:version('1.2.x')}";
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("1.1.0.0"));
            Assert.assertFalse("1.1 !> 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("1.1.9.500"));
            Assert.assertFalse("1.1.9.500 !> 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            // THIS IS THE ONE INTERESTING DIFFERENCE TO NOTE FROM THE 1.2.0.0 COMPARISON
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("1.2.0.1"));
            Assert.assertFalse("1.2.0.1 !> 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("1.3.0.0"));
            Assert.assertTrue("1.3 > 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("2.0.0.0"));
            Assert.assertTrue("2.0 > 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testVersionGreaterThan() {
        final String rule = "${version > proctor:version('1.2.0.0')}";
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("1.1.0.0"));
            Assert.assertFalse("1.1 !> 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("1.1.9.500"));
            Assert.assertFalse("1.1.9.500 !> 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            // THIS IS THE ONE INTERESTING DIFFERENCE TO NOTE FROM THE 1.2.+ COMPARISON
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("1.2.0.1"));
            Assert.assertTrue("1.2.0.1 > 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("1.3.0.0"));
            Assert.assertTrue("1.3 > 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("2.0.0.0"));
            Assert.assertTrue("2.0 > 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testVersionGreaterThanEqualWildCard() {
        final String rule = "${version >= proctor:version('1.2.x')}";
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("1.2.0.0"));
            Assert.assertTrue("1.2.0.0 >= 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("1.2.0.1"));
            Assert.assertTrue("1.2.0.1 >= 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testVersionGreaterThanEqual() {
        final String rule = "${version >= proctor:version('1.2.0.0')}";
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("1.2.0.0"));
            Assert.assertTrue("1.2.0.0 >= 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("1.2.0.1"));
            Assert.assertTrue("1.2.0.1 >= 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testVersionInRangeWildCard() {
        final String rule = "${proctor:versionInRange(version, '1.2.x', '2.4.0.0')}";
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("1.2.0.0"));
            Assert.assertTrue("1.2 in range", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("2.0.0.0"));
            Assert.assertTrue("2.0 in range", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("2.3.9.1024"));
            Assert.assertTrue("2.3.9.1024 in range", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("2.4.0.0"));
            Assert.assertFalse("2.4 not in range", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("2.4.1.0"));
            Assert.assertFalse("2.4.1 not in range", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("0.9.0.0"));
            Assert.assertFalse("0.9 not in range", ruleEvaluator.evaluateBooleanRule(rule, values));
        }

        final String badRule = "${proctor:versionInRange(version, '1.2.x', '2.4.x')}";
        try {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("0.9.0.0"));
            ruleEvaluator.evaluateBooleanRule(badRule, values);
            Assert.fail("wild card should not be allowed as open upper bound");
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    public void testVersionInRange() {
        final String rule = "${proctor:versionInRange(version, '1.2.0.0', '2.4.0.0')}";
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("1.2.0.0"));
            Assert.assertTrue("1.2 in range", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("2.0.0.0"));
            Assert.assertTrue("2.0 in range", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("2.3.9.1024"));
            Assert.assertTrue("2.3.9.1024 in range", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("2.4.0.0"));
            Assert.assertFalse("2.4 not in range", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("2.4.1.0"));
            Assert.assertFalse("2.4.1 not in range", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = Collections.<String, Object>singletonMap("version", ReleaseVersion.fromString("0.9.0.0"));
            Assert.assertFalse("0.9 not in range", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

}
