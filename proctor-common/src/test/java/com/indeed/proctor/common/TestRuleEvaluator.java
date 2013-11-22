package com.indeed.proctor.common;

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


}
