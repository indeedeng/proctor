package com.indeed.proctor.common;

import com.google.common.collect.Sets;
import com.indeed.proctor.common.el.MulticontextReadOnlyVariableMapper;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestBucket;
import org.apache.el.ExpressionFactoryImpl;
import org.junit.Assert;
import org.junit.Test;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;

public class TestRuleVerifyUtils {

    private ExpressionFactory expressionFactory = new ExpressionFactoryImpl();

    private ELContext setUpElContextWithContext(final Map<String, Object> context, final String testRule) {
        final List<TestBucket> buckets = TestProctorUtils.fromCompactBucketFormat("inactive:-1,control:0,test:1");
        final ConsumableTestDefinition testDefVal1 = TestProctorUtils.constructDefinition(buckets,
                TestProctorUtils.fromCompactAllocationFormat(String.format("%s|-1:0.5,0:0.5,1:0.0", testRule), "-1:0.25,0:0.5,1:0.25"));
        final Map<String, ValueExpression> testConstants = ProctorUtils.convertToValueExpressionMap(expressionFactory, testDefVal1.getConstants());

        final ProvidedContext providedContext = new ProvidedContext(ProctorUtils.convertToValueExpressionMap(expressionFactory, context), true);
        final VariableMapper variableMapper = new MulticontextReadOnlyVariableMapper(testConstants, providedContext.getContext());

        final RuleEvaluator ruleEvaluator = new RuleEvaluator(expressionFactory, RuleEvaluator.FUNCTION_MAPPER, testDefVal1.getConstants());
        return ruleEvaluator.createELContext(variableMapper);
    }

    private void verifyRule(final String testRule, final Object[][] context, final String[] absentIdentifiers) throws InvalidRuleException {
        final Map<String, Object> contextMap = new HashMap<>();
        for (final Object[] c : context) {
            contextMap.put((String)c[0], c[1]);
        }
        final ELContext elContext = setUpElContextWithContext(contextMap, testRule);
        RuleVerifyUtils.verifyRule(testRule, true, expressionFactory, elContext, Sets.newHashSet(absentIdentifiers));
    }

    private void expectValidRule(final String testRule, final Object[][] context, final String[] absentIdentifiers) {
        try {
            verifyRule(testRule, context, absentIdentifiers);

            /** no exception was thrown **/
            assertTrue(true);
        } catch (final InvalidRuleException el) {
            /** exception should not be thrown. **/
            Assert.fail();
        }

    }
    private void expectInvalidRule(final String testRule, final Object[][] context, final String[] absentIdentifiers) {
        try {
            verifyRule(testRule, context, absentIdentifiers);

            /** exception should be thrown until here. **/
            Assert.fail();
        } catch (final InvalidRuleException el) {
            /** expected **/
            assertTrue(true);
        }
    }

    @Test
    public void testVerifyRulesNormal() {
        expectValidRule(
                "${browser != 'IE9'}",
                new Object[][] {
                        {"browser", "IE"},
                },
                new String[] {
                }
        );

    }

    @Test
    public void testVerifyRulesWithoutContext() {
        expectInvalidRule(
                "${browser != 'IE9'}",
                new Object[][] {
                },
                new String[] {
                }
        );
    }

    @Test
    public void testVerifyRulesWithAbsentIdentifiers() {
        expectValidRule(
                "${browser != 'IE9'}",
                new Object[][]{
                },
                new String[]{
                        "browser",
                }
        );
    }

    @Test
    public void testVerifyAndRuleNormal() {
        expectValidRule(
                "${browser == 'IE9' && country == 'US'}",
                new Object[][]{
                        { "browser", "IE" },
                        { "country", "JP" },
                },
                new String[]{
                }
        );
    }

    @Test
    public void testVerifyAndRuleWithoutContext() {
        expectInvalidRule(
                "${browser == 'IE9' && country == 'US'}",
                new Object[][]{
                        { "browser", "IE" },
                },
                new String[]{
                }
        );
        expectInvalidRule(
                "${browser == 'IE9' && country == 'US'}",
                new Object[][]{
                        { "country", "US" },
                },
                new String[]{
                }
        );
        expectInvalidRule(
                "${browser == 'IE9' && country == 'US'}",
                new Object[][]{
                },
                new String[]{
                        "browser",
                }
        );
        expectInvalidRule(
                "${browser == 'IE9' && country == 'US'}",
                new Object[][]{
                },
                new String[]{
                        "country",
                }
        );
    }

    @Test
    public void testVerifyAndRuleWithAbsentIdentifers() {
        expectValidRule(
                "${browser == 'IE9' && country == 'US'}",
                new Object[][]{
                        { "browser", "IE" },
                },
                new String[]{
                        "country",
                }
        );
        expectValidRule(
                "${browser == 'IE9' && country == 'US'}",
                new Object[][]{
                        { "country", "JP" },
                },
                new String[]{
                        "browser",
                }
        );
        expectValidRule(
                "${browser == 'IE9' && country == 'US'}",
                new Object[][]{
                },
                new String[]{
                        "browser",
                        "country",
                }
        );
    }

    @Test
    public void testInvalidSyntaxRule() {
        expectInvalidRule(
                "${browser == 'IE9' && country = 'US'}",
                new Object[][] {
                        { "browser", "IE" },
                        { "country", "JP" },
                },
                new String[] {
                }
        );
    }
}