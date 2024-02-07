package com.indeed.proctor.common;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.indeed.util.core.ReleaseVersion;
import org.junit.Before;
import org.junit.Test;

import javax.el.ELException;
import java.util.Arrays;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** @author parker */
public class TestRuleEvaluator {

    private RuleEvaluator ruleEvaluator;

    @Before
    public void setUp() throws Exception {
        final Map<String, Object> testConstants =
                ImmutableMap.of(
                        "LANGUAGES_ENABLED", Lists.newArrayList("en", "fr", "de"),
                        "US_REMOTE_Q",
                                Lists.newArrayList(
                                        "work from home remote",
                                        "remote work from home",
                                        "remote work from home jobs on indeed",
                                        "at home work",
                                        "online",
                                        "work home online",
                                        "online work from home",
                                        "work at home",
                                        "at home",
                                        "online",
                                        "work from home",
                                        "remote"),
                        "US_REMOTE_L",
                                Lists.newArrayList(
                                        "work at home",
                                        "at home",
                                        "online",
                                        "work from home",
                                        "remote",
                                        "remote",
                                        "us",
                                        "remote,us",
                                        ""));
        ruleEvaluator =
                new RuleEvaluator(
                        RuleEvaluator.EXPRESSION_FACTORY,
                        RuleEvaluator.FUNCTION_MAPPER,
                        testConstants);
    }

    @Test
    public void testNullRule() {
        assertTrue("null rule should be true", ruleEvaluator.evaluateBooleanRule(null, emptyMap()));
    }

    @Test
    public void testNullReferenceRule() {
        assertFalse(
                "null rule should be false",
                ruleEvaluator.evaluateBooleanRule("${null}", emptyMap()));
    }

    @Test
    public void testEmptyRule() {
        for (final String rule : new String[] {"", " ", "${}", "${   }"}) {
            assertTrue(
                    "empty rule '" + rule + "' should be true",
                    ruleEvaluator.evaluateBooleanRule(rule, emptyMap()));
        }
    }

    @Test
    public void testLiteralRule() {
        for (final String rule : new String[] {"${true}", "${TRUE}", "${ TRUE }"}) {
            assertTrue(
                    "rule '" + rule + "' should be true",
                    ruleEvaluator.evaluateBooleanRule(rule, emptyMap()));
        }
        for (final String rule : new String[] {"${false}", "${FALSE}", "${ FALSE }"}) {
            assertFalse(
                    "rule '" + rule + "' should be false",
                    ruleEvaluator.evaluateBooleanRule(rule, emptyMap()));
        }
    }

    @Test
    public void testMalformedRuleShouldBeFalse() {
        for (final String rule :
                new String[] {"true", "TRUE", "FALSE", "false", " ${true} ", " ${ true } "}) {
            assertFalse(
                    "malformed rule '" + rule + "' should be false",
                    ruleEvaluator.evaluateBooleanRule(rule, emptyMap()));
        }
    }

    @Test
    public void testNonBooleanShouldFail() {
        {
            final String rule = "${1 == 1}";
            assertTrue(
                    "rule '" + rule + "' should be true",
                    ruleEvaluator.evaluateBooleanRule(rule, emptyMap()));
        }
        {
            // mismatched parens make this a String value "true}"
            assertThatThrownBy(() -> ruleEvaluator.evaluateBooleanRule("${1 == 1}}", emptyMap()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Received non-boolean return value");
        }
        {
            // numeric result becomes String, not boolean
            assertThatThrownBy(() -> ruleEvaluator.evaluateBooleanRule("${1 + 1}}", emptyMap()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Received non-boolean return value");
        }
        {
            // numeric result becomes String, not boolean
            assertThatThrownBy(
                            () -> ruleEvaluator.evaluateBooleanRule("${'tr'}${'ue'}", emptyMap()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Received non-boolean return value");
        }
    }

    @Test
    public void testMethodCalls() {
        final Map<String, Object> context = ImmutableMap.of("context", new Temp(), "country", "US");
        for (String rule :
                ImmutableList.of(
                        "${country == 'US' && context.isValid()}",
                        "${country == 'US' && context.isFortyTwo('42')}")) {
            assertTrue(
                    "rule '" + rule + "' should be true for " + context,
                    ruleEvaluator.evaluateBooleanRule(rule, context));
        }

        for (String rule :
                ImmutableList.of(
                        "${!context.isValid()}",
                        "${country == 'US' && context.isFortyTwo('47')}")) {
            assertFalse(
                    "rule '" + rule + "' should be true for " + context,
                    ruleEvaluator.evaluateBooleanRule(rule, context));
        }

        assertThatThrownBy(
                        () ->
                                ruleEvaluator.evaluateBooleanRule(
                                        "${country == 'US' && context.isNotFortyTwo('42')}",
                                        context))
                .isInstanceOf(ELException.class)
                .hasMessageContaining("Method not found");
    }

    @Test
    public void testElExpressionsShouldBeAvailable() {
        final Map<String, Object> values = singletonMap("lang", "en");
        {
            final String rule = "${lang == 'en'}";
            assertTrue(
                    "rule '" + rule + "' should be true for " + values,
                    ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final String rule = "${!(lang == 'fr')}";
            assertTrue(
                    "rule '" + rule + "' should be true for " + values,
                    ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final String rule = "${lang == 'fr'}";
            assertFalse(
                    "rule '" + rule + "' should be false for " + values,
                    ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    public static class Temp {
        public String getY() {
            return "barY";
        }

        public boolean isValid() {
            return true;
        }

        public boolean isFortyTwo(String s) {
            return "42".equals(s);
        }
    }

    @Test
    public void testElPropertyResolvers() {
        // Map
        {
            final Map<String, Object> values =
                    singletonMap("context", ImmutableMap.of("foo", "bar"));
            String rule = "${context['foo'] == 'bar'}";
            assertTrue(
                    "rule '" + rule + "' should be true for " + values,
                    ruleEvaluator.evaluateBooleanRule(rule, values));
            rule = "${context.foo == 'bar'}";
            assertTrue(
                    "rule '" + rule + "' should be true for " + values,
                    ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        // Array
        {
            final Map<String, Object> values = singletonMap("context", new String[] {"foo", "bar"});
            final String rule = "${context[1] == 'bar'}";
            assertTrue(
                    "rule '" + rule + "' should be true for " + values,
                    ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        // List
        {
            final Map<String, Object> values = singletonMap("context", Arrays.asList("foo", "bar"));
            final String rule = "${context[1] == 'bar'}";
            assertTrue(
                    "rule '" + rule + "' should be true for " + values,
                    ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        // bean property (getter)
        {
            final Map<String, Object> values = singletonMap("context", new Temp());
            final String rule = "${context.y == 'barY'}";
            assertTrue(
                    "rule '" + rule + "' should be true for " + values,
                    ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testUtilTagFunctionsShouldBeAvailable() {
        final String rule = "${proctor:contains(LANGUAGES_ENABLED, lang)}";
        {
            final Map<String, Object> values = singletonMap("lang", "en");
            assertTrue(
                    "rule '" + rule + "' should be true for " + values,
                    ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = singletonMap("lang", "it");
            assertFalse(
                    "rule '" + rule + "' should be false for " + values,
                    ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testStandardLibFunctionsBeAvailable() {
        final String rule = "${fn:startsWith(lang, 'en')}";
        {
            final Map<String, Object> values = singletonMap("lang", "en_US");
            assertTrue(
                    "rule '" + rule + "' should be true for " + values,
                    ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = singletonMap("lang", "en");
            assertTrue(
                    "rule '" + rule + "' should be true for " + values,
                    ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values = singletonMap("lang", "fr");
            assertFalse(
                    "rule '" + rule + "' should be false for " + values,
                    ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testRegexMatches() {
        final String rule = "${proctor:matches(value, '^#[0-9a-fA-F]{3,6}$')}";
        {
            for (final String validHex :
                    new String[] {"#000", "#0000", "#00000", "#AEFFB3", "#aE3FB3"}) {
                final Map<String, Object> values = singletonMap("value", validHex);
                assertTrue(
                        "rule '" + rule + "' should be true for " + values,
                        ruleEvaluator.evaluateBooleanRule(rule, values));
            }
        }
        {
            for (final String invalidHex :
                    new String[] {"#00", "abc3f", "#000000d", "abe4z ", ""}) {
                final Map<String, Object> values = singletonMap("value", invalidHex);
                assertFalse(
                        "rule '" + rule + "' should be false for " + values,
                        ruleEvaluator.evaluateBooleanRule(rule, values));
            }
        }
    }

    @Test
    public void testVersionLessThanWildCard() {
        final String rule = "${version < proctor:version('1.2.x')}";
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("1.1.0.0"));
            assertTrue("1.1 < 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("1.1.9.500"));
            assertTrue("1.1.9.500 < 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("1.2.0.1"));
            assertFalse("1.2.0.1 !< 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("1.3.0.0"));
            assertFalse("1.3 !< 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("2.0.0.0"));
            assertFalse("2.0 !< 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testVersionLessThan() {
        final String rule = "${version < proctor:version('1.2.0.0')}";
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("1.1.0.0"));
            assertTrue("1.1 < 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("1.1.9.500"));
            assertTrue("1.1.9.500 < 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("1.2.0.1"));
            assertFalse("1.2.0.1 !< 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("1.3.0.0"));
            assertFalse("1.3 !< 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("2.0.0.0"));
            assertFalse("2.0 !< 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testVersionLessThanEqualWildCard() {
        final String rule = "${version <= proctor:version('1.2.x')}";
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("1.2.0.0"));
            assertTrue("1.2.0.0 <= 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testVersionLessThanEqual() {
        final String rule = "${version <= proctor:version('1.2.0.0')}";
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("1.2.0.0"));
            assertTrue("1.2.0.0 <= 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testVersionEqualWildCard() {
        final String rule = "${version == proctor:version('5.4.3.x')}";
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("5.4.3.0"));
            assertTrue("5.4.3.0 == 5.4.3.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("5.4.3.200"));
            assertTrue("5.4.3.200 == 5.4.3.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testVersionEqual() {
        final String rule = "${version == proctor:version('5.4.3.0')}";
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("5.4.3.0"));
            assertTrue("5.4.3.0 == 5.4.3", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testVersionGreaterThanWildCard() {
        final String rule = "${version > proctor:version('1.2.x')}";
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("1.1.0.0"));
            assertFalse("1.1 !> 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("1.1.9.500"));
            assertFalse("1.1.9.500 !> 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            // THIS IS THE ONE INTERESTING DIFFERENCE TO NOTE FROM THE 1.2.0.0 COMPARISON
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("1.2.0.1"));
            assertFalse("1.2.0.1 !> 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("1.3.0.0"));
            assertTrue("1.3 > 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("2.0.0.0"));
            assertTrue("2.0 > 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testVersionGreaterThan() {
        final String rule = "${version > proctor:version('1.2.0.0')}";
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("1.1.0.0"));
            assertFalse("1.1 !> 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("1.1.9.500"));
            assertFalse("1.1.9.500 !> 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            // THIS IS THE ONE INTERESTING DIFFERENCE TO NOTE FROM THE 1.2.+ COMPARISON
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("1.2.0.1"));
            assertTrue("1.2.0.1 > 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("1.3.0.0"));
            assertTrue("1.3 > 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("2.0.0.0"));
            assertTrue("2.0 > 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testVersionGreaterThanEqualWildCard() {
        final String rule = "${version >= proctor:version('1.2.x')}";
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("1.2.0.0"));
            assertTrue("1.2.0.0 >= 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("1.2.0.1"));
            assertTrue("1.2.0.1 >= 1.2.x", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testVersionGreaterThanEqual() {
        final String rule = "${version >= proctor:version('1.2.0.0')}";
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("1.2.0.0"));
            assertTrue("1.2.0.0 >= 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("1.2.0.1"));
            assertTrue("1.2.0.1 >= 1.2", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testVersionInRangeWildCard() {
        final String rule = "${proctor:versionInRange(version, '1.2.x', '2.4.0.0')}";
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("1.2.0.0"));
            assertTrue("1.2 in range", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("2.0.0.0"));
            assertTrue("2.0 in range", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("2.3.9.1024"));
            assertTrue("2.3.9.1024 in range", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("2.4.0.0"));
            assertFalse("2.4 not in range", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("2.4.1.0"));
            assertFalse("2.4.1 not in range", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("0.9.0.0"));
            assertFalse("0.9 not in range", ruleEvaluator.evaluateBooleanRule(rule, values));
        }

        assertThatThrownBy(
                        () ->
                                ruleEvaluator.evaluateBooleanRule(
                                        "${proctor:versionInRange(version, '1.2.x', '2.4.x')}",
                                        ImmutableMap.of(
                                                "version", ReleaseVersion.fromString("0.9.0.0"))))
                .isInstanceOf(ELException.class)
                .hasMessageContaining("proctor:versionInRange")
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    public void testVersionInRange() {
        final String rule = "${proctor:versionInRange(version, '1.2.0.0', '2.4.0.0')}";
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("1.2.0.0"));
            assertTrue("1.2 in range", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("2.0.0.0"));
            assertTrue("2.0 in range", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("2.3.9.1024"));
            assertTrue("2.3.9.1024 in range", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("2.4.0.0"));
            assertFalse("2.4 not in range", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("2.4.1.0"));
            assertFalse("2.4.1 not in range", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
        {
            final Map<String, Object> values =
                    singletonMap("version", ReleaseVersion.fromString("0.9.0.0"));
            assertFalse("0.9 not in range", ruleEvaluator.evaluateBooleanRule(rule, values));
        }
    }

    @Test
    public void testNonBooleanRule() {
        assertThat(ruleEvaluator.evaluateRule("${4}", emptyMap(), Integer.class)).isEqualTo(4);
        assertThat(ruleEvaluator.evaluateRule("${true}", emptyMap(), Boolean.class))
                .isEqualTo(true);
        assertThat(ruleEvaluator.evaluateRule("${4}", emptyMap(), String.class)).isEqualTo("4");
        assertThat(ruleEvaluator.evaluateRule("${true}", emptyMap(), String.class))
                .isEqualTo("true");
    }

    @Test
    public void testPartialMatchEmptyContextMatch() throws Exception {
        final String rule =
                "${3<4 && (2 > 1 || proctor:versionInRange(version, '1.2.0.0', '2.4.0.0'))}";
        final Map<String, Object> values = emptyMap();
        assertTrue(ruleEvaluator.evaluateBooleanRulePartial(rule, values));
    }

    @Test
    public void testPartialMatchEmptyContextNoMatch() throws Exception {
        final String rule =
                "${3<4 && 2<1 && proctor:versionInRange(version, '1.2.0.0', '2.4.0.0')}";
        final Map<String, Object> values = emptyMap();
        assertFalse(ruleEvaluator.evaluateBooleanRulePartial(rule, values));
    }

    @Test
    public void testPartialMatchComplexRule() throws Exception {
        final String rule =
                "${country == 'US' && adFormat == 'web' && hasAccount && (indeed:contains(US_REMOTE_Q, fn:toLowerCase(fn:trim(query))) || indeed:contains(US_REMOTE_L, searchLocation))}";
        assertTrue(ruleEvaluator.evaluateBooleanRulePartial(rule, ImmutableMap.of()));
        assertTrue(
                ruleEvaluator.evaluateBooleanRulePartial(
                        rule, ImmutableMap.of("country", "US", "adFormat", "web")));
        assertFalse(
                ruleEvaluator.evaluateBooleanRulePartial(
                        rule, ImmutableMap.of("country", "GB", "adFormat", "web")));
        assertFalse(
                ruleEvaluator.evaluateBooleanRulePartial(
                        rule, ImmutableMap.of("country", "US", "adFormat", "mob")));
        assertFalse(
                ruleEvaluator.evaluateBooleanRulePartial(
                        rule,
                        ImmutableMap.of("query", "software engineer", "searchLocation", "Austin")));
        assertTrue(
                ruleEvaluator.evaluateBooleanRulePartial(
                        rule, ImmutableMap.of("query", "remote", "searchLocation", "Austin")));
        assertTrue(
                ruleEvaluator.evaluateBooleanRulePartial(
                        rule,
                        ImmutableMap.of("query", "software engineer", "searchLocation", "remote")));
        assertTrue(
                ruleEvaluator.evaluateBooleanRulePartial(
                        rule, ImmutableMap.of("query", "software engineer")));
        assertTrue(
                ruleEvaluator.evaluateBooleanRulePartial(
                        rule, ImmutableMap.of("searchLocation", "Austin")));
        assertFalse(
                ruleEvaluator.evaluateBooleanRulePartial(
                        rule, ImmutableMap.of("hasAccount", false, "searchLocation", "Austin")));
    }

    @Test
    public void testPartialFalseSometimesHelps() throws Exception {
        final String rule = "${!(country == 'US' && adFormat == 'web')}";
        assertFalse(
                ruleEvaluator.evaluateBooleanRulePartial(
                        rule, ImmutableMap.of("country", "US", "adFormat", "web")));
        assertTrue(
                ruleEvaluator.evaluateBooleanRulePartial(rule, ImmutableMap.of("country", "US")));
        assertTrue(
                ruleEvaluator.evaluateBooleanRulePartial(rule, ImmutableMap.of("adFormat", "web")));
        assertTrue(
                ruleEvaluator.evaluateBooleanRulePartial(rule, ImmutableMap.of("country", "GB")));
        assertTrue(
                ruleEvaluator.evaluateBooleanRulePartial(rule, ImmutableMap.of("country", "mob")));
        assertTrue(
                ruleEvaluator.evaluateBooleanRulePartial(
                        rule, ImmutableMap.of("country", "US", "adFormat", "mob")));
    }
}
