package com.indeed.proctor.common;

import com.google.common.collect.Sets;
import com.indeed.proctor.common.el.MulticontextReadOnlyVariableMapper;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestBucket;
import junit.framework.AssertionFailedError;
import org.apache.el.ExpressionFactoryImpl;
import org.junit.Test;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRuleVerifyUtils {

    private final ExpressionFactory expressionFactory = new ExpressionFactoryImpl();

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
            contextMap.put((String) c[0], c[1]);
        }
        final ELContext elContext = setUpElContextWithContext(contextMap, testRule);
        RuleVerifyUtils.verifyRule(testRule, true, expressionFactory, elContext, Sets.newHashSet(absentIdentifiers));
    }

    private void expectValidRule(final String testRule, final Object[][] context, final String[] absentIdentifiers) {
        try {
            verifyRule(testRule, context, absentIdentifiers);
        } catch (final InvalidRuleException e) {
            /* exception should not be thrown. **/
            throw new RuntimeException("validation failed", e);
        }

    }

    private InvalidRuleException expectInvalidRule(final String testRule, final Object[][] context, final String[] absentIdentifiers) {
        try {
            verifyRule(testRule, context, absentIdentifiers);

            /* exception should be thrown until here. **/
            throw new AssertionFailedError("invalid rule exception should be thrown");
        } catch (final InvalidRuleException e) {
            /* expected **/
            return e;
        }
    }

    @Test
    public void testVerifyRulesNormal() {
        expectValidRule(
                "${browser != 'IE9'}",
                new Object[][]{
                        {"browser", "IE"},
                },
                new String[]{
                }
        );

    }

    @Test
    public void testVerifyRulesWithoutContext() {
        final InvalidRuleException e = expectInvalidRule(
                "${browser != 'IE9'}",
                new Object[][]{
                },
                new String[]{
                }
        );
        assertThat(e).hasMessageContaining("contains undefined identifier");
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
    public void testVerifyRulesWithList() {
        final InvalidRuleException e = expectInvalidRule(
                "${[browser]}",
                new Object[][]{
                        {"browser", "IE"},
                },
                new String[]{
                }
        );
        assertThat(e).hasMessageContaining("has invalid syntax");
    }

    @Test
    public void testVerifyRulesWithSet() {
        final InvalidRuleException e = expectInvalidRule(
                "${{browser}.size() > 0}",
                new Object[][]{
                        {"browser", "IE"},
                },
                new String[]{
                }
        );
        assertThat(e).hasMessageContaining("has invalid syntax");
    }

    @Test
    public void testVerifyRulesWithMap() {
        final InvalidRuleException e = expectInvalidRule(
                "${{'Foo': true}.size() > 0}",
                new Object[][]{
                },
                new String[]{
                }
        );
        assertThat(e).hasMessageContaining("has invalid syntax");
    }

    @Test
    public void testVerifyRulesWithSemicolon() {
        final InvalidRuleException e = expectInvalidRule(
                "${false;true}",
                new Object[][]{
                },
                new String[]{
                }
        );
        assertThat(e).hasMessageContaining("has invalid syntax");
    }

    @Test
    public void testVerifyRulesWithLambda() {
        final InvalidRuleException e = expectInvalidRule(
                "${(x -> 42);true}",
                new Object[][]{
                        {"browser", "IE"},
                },
                new String[]{
                }
        );
        assertThat(e).hasMessageContaining("has invalid syntax");
    }

    @Test
    public void testVerifyAndRuleNormal() {
        expectValidRule(
                "${browser == 'IE9' && country == 'US'}",
                new Object[][]{
                        {"browser", "IE"},
                        {"country", "JP"},
                },
                new String[]{
                }
        );
    }

    @Test
    public void testVerifyAndRuleWithoutContext() {
        final String testRule = "${browser == 'IE9' && country == 'US'}";
        InvalidRuleException e = expectInvalidRule(
                testRule,
                new Object[][]{
                        {"browser", "IE"},
                },
                new String[]{
                }
        );
        assertThat(e).hasMessageContaining("contains undefined identifier");
        e = expectInvalidRule(
                testRule,
                new Object[][]{
                        {"country", "US"},
                },
                new String[]{
                }
        );
        assertThat(e).hasMessageContaining("contains undefined identifier");
        e = expectInvalidRule(
                testRule,
                new Object[][]{
                },
                new String[]{
                        "browser",
                }
        );
        assertThat(e).hasMessageContaining("contains undefined identifier");
        e = expectInvalidRule(
                testRule,
                new Object[][]{
                },
                new String[]{
                        "country",
                }
        );
        assertThat(e).hasMessageContaining("contains undefined identifier");
    }

    @Test
    public void testVerifyAndRuleWithAbsentIdentifers() {
        final String testRule = "${browser == 'IE9' && country == 'US'}";
        expectValidRule(
                testRule,
                new Object[][]{
                        {"browser", "IE"},
                },
                new String[]{
                        "country",
                }
        );
        expectValidRule(
                testRule,
                new Object[][]{
                        {"country", "JP"},
                },
                new String[]{
                        "browser",
                }
        );
        expectValidRule(
                testRule,
                new Object[][]{
                },
                new String[]{
                        "browser",
                        "country",
                }
        );
    }

    @Test
    public void testInvalidSyntaxRuleWithConcat() {
        final InvalidRuleException e = expectInvalidRule(
                "${browser == 'IE9' && country += 'US'}",
                new Object[][]{
                        {"country", "US"},
                        {"browser", "chrome"}
                },
                new String[]{
                }
        );
        assertThat(e).hasMessageContaining("has invalid syntax");
    }

    @Test
    public void testInvalidSyntaxRuleWithArrow() {
        final InvalidRuleException e = expectInvalidRule(
                "${browser == 'IE9' && country -> 'US'}",
                new Object[][]{
                        {"country", "US"},
                        {"browser", "chrome"}
                },
                new String[]{
                }
        );
        assertThat(e).hasMessageContaining("has invalid syntax");
    }

    @Test
    public void testInvalidSyntaxRuleWithAssignment() {
        final String testRuleUsingValid = "${lang=='en' && ((companyUrl != null && country == 'US' && indeed:contains(EC_COMPANY_URLS, companyUrl)))}";
        final String testRuleUsingAssign = "${lang=='en' && ((companyUrl != null && country = 'US' && indeed:contains(EC_COMPANY_URLS, companyUrl)))}";

        expectValidRule(
                testRuleUsingValid,
                new Object[][]{
                        {"EC_COMPANY_URLS", "foo"}
                },
                new String[]{
                        "lang",
                        "companyUrl",
                        "country",
                }
        );

        InvalidRuleException e = expectInvalidRule(
                testRuleUsingAssign,
                new Object[][]{
                        {"companyUrl", "indeed.com"},
                        {"country", "JP"},
                        {"lang", "en"},
                        {"EC_COMPANY_URLS", "foo"}
                },
                new String[]{
                }
        );
        assertThat(e).hasMessageContaining("has invalid syntax");
        e = expectInvalidRule(
                testRuleUsingAssign,
                new Object[][]{
                        {"EC_COMPANY_URLS", "foo"}
                },
                new String[]{
                        "lang",
                        "companyUrl",
                        "country",
                }
        );
        assertThat(e).hasMessageContaining("has invalid syntax");
    }
}
