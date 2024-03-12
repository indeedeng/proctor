package com.indeed.proctor.common;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestRuleFilter {

    private RuleFilter ruleFilter;
    private String rule;

    @Before
    public void setUp() throws Exception {
        final Map<String, Object> testConstants = ImmutableMap.of(
                "US_REMOTE_Q", Lists.newArrayList("work from home remote", "remote work from home", "remote work from home jobs on indeed", "at home work", "online", "work home online", "online work from home", "work at home", "at home", "online", "work from home", "remote"),
                "US_REMOTE_L", Lists.newArrayList("work at home", "at home", "online", "work from home", "remote", "remote", "us", "remote,us", ""));
        ruleFilter = RuleFilter.createDefaultRuleFilter(testConstants);
    }

    @Test
    public void testPartialMatchEmptyContextMatch() throws Exception {
        rule = "${3<4 && (2 > 1 || proctor:versionInRange(version, '1.2.0.0', '2.4.0.0'))}";
        assertMatches(ImmutableMap.of());
    }

    @Test
    public void testPartialMatchEmptyContextNoMatch() throws Exception {
        rule = "${3<4 && 2<1 && proctor:versionInRange(version, '1.2.0.0', '2.4.0.0')}";
        assertDoesNotMatch(ImmutableMap.of());
    }

    @Test
    public void testPartialMatchComplexRule() throws Exception {
        rule = "${country == 'US' && adFormat == 'web' && hasAccount && (indeed:contains(US_REMOTE_Q, fn:toLowerCase(fn:trim(query))) || indeed:contains(US_REMOTE_L, searchLocation))}";
        assertMatches(ImmutableMap.of());
        assertMatches(ImmutableMap.of("country", "US", "adFormat", "web"));
        assertDoesNotMatch(ImmutableMap.of("country", "GB", "adFormat", "web"));
        assertDoesNotMatch(ImmutableMap.of("country", "US", "adFormat", "mob"));
        assertDoesNotMatch(ImmutableMap.of("query", "software engineer", "searchLocation", "Austin"));
        assertMatches(ImmutableMap.of("query", "remote", "searchLocation", "Austin"));
        assertMatches(ImmutableMap.of("query", "software engineer", "searchLocation", "remote"));
        assertMatches(ImmutableMap.of("query", "software engineer"));
        assertMatches(ImmutableMap.of("searchLocation", "Austin"));
        assertDoesNotMatch(ImmutableMap.of("hasAccount", false, "searchLocation", "Austin"));
    }

    @Test
    public void testPartialFalseSometimesHelps() throws Exception {
        rule = "${!(country == 'US' && adFormat == 'web')}";
        assertDoesNotMatch(ImmutableMap.of("country", "US", "adFormat", "web"));
        assertMatches(ImmutableMap.of("country", "US"));
        assertMatches(ImmutableMap.of("adFormat", "web"));
        assertMatches(ImmutableMap.of("country", "GB"));
        assertMatches(ImmutableMap.of("country", "mob"));
        assertMatches(ImmutableMap.of("country", "US", "adFormat", "mob"));
    }

    private void assertMatches(final Map<String, Object> context) {
        assertTrue(ruleFilter.ruleCanMatch(rule, context));
    }

    private void assertDoesNotMatch(final Map<String, Object> context) {
        assertFalse(ruleFilter.ruleCanMatch(rule, context));
    }
}
