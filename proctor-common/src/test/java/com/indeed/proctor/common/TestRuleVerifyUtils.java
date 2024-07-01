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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TestRuleVerifyUtils {

    private final ExpressionFactory expressionFactory = new ExpressionFactoryImpl();

    public static class TestClass {
        public boolean matches() {
            return true;
        }

        public boolean doesNotMatch() {
            return false;
        }

        public boolean isFortyTwo(final String s) {
            return "42".equals(s);
        }
    }

    private ELContext setUpElContextWithContext(
            final Map<String, Object> context, final String testRule) {
        final List<TestBucket> buckets =
                TestProctorUtils.fromCompactBucketFormat("inactive:-1,control:0,test:1");
        final ConsumableTestDefinition testDefVal1 =
                TestProctorUtils.constructDefinition(
                        buckets,
                        TestProctorUtils.fromCompactAllocationFormat(
                                String.format("%s|-1:0.5,0:0.5,1:0.0", testRule),
                                "-1:0.25,0:0.5,1:0.25"));
        final Map<String, ValueExpression> testConstants =
                ProctorUtils.convertToValueExpressionMap(
                        expressionFactory, testDefVal1.getConstants());

        final ProvidedContext providedContext =
                ProvidedContext.forValueExpressionMap(
                        ProctorUtils.convertConstantsToValueExpressionMap(
                                expressionFactory, context),
                        Collections.emptySet());
        final VariableMapper variableMapper =
                new MulticontextReadOnlyVariableMapper(testConstants, providedContext.getContext());

        final RuleEvaluator ruleEvaluator =
                new RuleEvaluator(
                        expressionFactory,
                        RuleEvaluator.FUNCTION_MAPPER,
                        testDefVal1.getConstants());
        return ruleEvaluator.createELContext(variableMapper);
    }

    private void verifyRule(
            final String testRule, final Object[][] context, final String[] absentIdentifiers)
            throws InvalidRuleException {
        final Map<String, Object> contextMap = new HashMap<>();
        for (final Object[] c : context) {
            contextMap.put((String) c[0], c[1]);
        }
        final ELContext elContext = setUpElContextWithContext(contextMap, testRule);
        RuleVerifyUtils.verifyRule(
                testRule, true, expressionFactory, elContext, Sets.newHashSet(absentIdentifiers));
    }

    private void expectValidRule(
            final String testRule, final Object[][] context, final String[] absentIdentifiers) {
        try {
            verifyRule(testRule, context, absentIdentifiers);
        } catch (final InvalidRuleException e) {
            /* exception should not be thrown. **/
            throw new RuntimeException("validation failed", e);
        }
    }

    private InvalidRuleException expectInvalidRule(
            final String testRule, final Object[][] context, final String[] absentIdentifiers) {
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
                new Object[][] {
                    {"browser", "IE"},
                },
                new String[] {});
    }

    @Test
    public void testVerifyRulesNestedMapBoolean() {
        expectValidRule(
                "${foo.bar}",
                new Object[][] {
                    {"foo", Collections.singletonMap("bar", true)},
                },
                new String[] {});
    }

    @Test
    public void testVerifyRulesProctorVersion() {
        expectInvalidRule(
                "${version != proctor:version('213.0')}",
                new Object[][] {
                    {"version", ""},
                },
                new String[] {});
        expectValidRule(
                "${version != proctor:version('213.0.0.0')}",
                new Object[][] {
                    {"version", ""},
                },
                new String[] {});
    }

    @Test
    public void testValidRulesWithMethodCall() {
        expectValidRule(
                "${browser == 'IE9' && obj.matches()}",
                new Object[][] {
                    {"browser", "IE"},
                    {"obj", new TestClass()},
                },
                new String[] {});
        expectValidRule(
                "${browser == 'IE9' && not obj.doesNotMatch()}",
                new Object[][] {
                    {"browser", "IE"},
                    {"obj", new TestClass()},
                },
                new String[] {});
        expectValidRule(
                "${browser == 'IE9' && obj.isFortyTwo('42')}",
                new Object[][] {
                    {"browser", "IE"},
                    {"obj", new TestClass()},
                },
                new String[] {});

        expectValidRule(
                "${browser == 'IE9' && not obj.isFortyTwo('49')}",
                new Object[][] {
                    {"browser", "IE"},
                    {"obj", new TestClass()},
                },
                new String[] {});
    }

    @Test
    public void testInvalidRulesWithMethodCall() {
        InvalidRuleException invalidRuleException =
                expectInvalidRule(
                        "${browser == 'IE' && obj.notExists()}",
                        new Object[][] {
                            {"browser", "IE"},
                            {"obj", new TestClass()},
                        },
                        new String[] {});
        assertThat(invalidRuleException.getMessage()).contains("Method not found");

        invalidRuleException =
                expectInvalidRule(
                        "${browser == 'IE' && obj.isFortyTwo(['42'])}",
                        new Object[][] {
                            {"browser", "IE"},
                            {"obj", new TestClass()},
                        },
                        new String[] {});
        assertThat(invalidRuleException.getMessage()).contains("syntax error");
    }

    @Test
    public void testValidRulesWithMissingExperimentalUnitNotIncludedInLocalContext() {
        expectValidRule(
                "${missingExperimentalUnit && browser == 'IE'}",
                new Object[][] {{"browser", "IE"}},
                new String[] {});

        expectValidRule(
                "${!missingExperimentalUnit && browser == 'IE'}",
                new Object[][] {{"browser", "IE"}},
                new String[] {});

        expectValidRule("${missingExperimentalUnit}", new Object[][] {}, new String[] {});
    }

    @Test
    public void
            testInvalidRulesWithMissingExperimentalUnitNotIncludedInLocalContext_MissingOtherVariables() {
        InvalidRuleException invalidRuleException =
                expectInvalidRule(
                        "${missingExperimentalUnit && browser == 'IE' && obj.foobar()}",
                        new Object[][] {
                            {"browser", "IE"},
                            {"obj", new TestClass()},
                        },
                        new String[] {});
        assertThat(invalidRuleException.getMessage()).contains("Method not found");

        invalidRuleException =
                expectInvalidRule(
                        "${missingExperimentalUnit && browser == 'IE' && obj.isFortyTwo(['42'])}",
                        new Object[][] {
                            {"browser", "IE"},
                            {"obj", new TestClass()},
                        },
                        new String[] {});
        assertThat(invalidRuleException.getMessage()).contains("syntax error");
    }

    @Test
    public void testVerifyRulesWithoutContext() {
        final InvalidRuleException e =
                expectInvalidRule("${browser != 'IE9'}", new Object[][] {}, new String[] {});
        assertThat(e).hasMessageContaining("not defined in the application's test specification");
    }

    @Test
    public void testVerifyRulesWithAbsentIdentifiers() {
        expectValidRule(
                "${browser != 'IE9'}",
                new Object[][] {},
                new String[] {
                    "browser",
                });
    }

    @Test
    public void testVerifyRulesWithList() {
        final InvalidRuleException e =
                expectInvalidRule(
                        "${[browser]}",
                        new Object[][] {
                            {"browser", "IE"},
                        },
                        new String[] {});
        assertThat(e)
                .hasMessageContaining(
                        "syntax error. Check that your rule is in the correct format and returns a boolean");
    }

    @Test
    public void testVerifyRulesWithSet() {
        final InvalidRuleException e =
                expectInvalidRule(
                        "${{browser}.size() > 0}",
                        new Object[][] {
                            {"browser", "IE"},
                        },
                        new String[] {});
        assertThat(e)
                .hasMessageContaining(
                        "syntax error. Check that your rule is in the correct format and returns a boolean");
    }

    @Test
    public void testVerifyRulesWithMap() {
        final InvalidRuleException e =
                expectInvalidRule(
                        "${{'Foo': true}.size() > 0}", new Object[][] {}, new String[] {});
        assertThat(e)
                .hasMessageContaining(
                        "syntax error. Check that your rule is in the correct format and returns a boolean");
    }

    @Test
    public void testVerifyRulesWithSemicolon() {
        final InvalidRuleException e =
                expectInvalidRule("${false;true}", new Object[][] {}, new String[] {});
        assertThat(e)
                .hasMessageContaining(
                        "syntax error. Check that your rule is in the correct format and returns a boolean");
    }

    @Test
    public void testVerifyRulesWithMismatchedCurlys() {
        final InvalidRuleException e =
                expectInvalidRule("${true}}", new Object[][] {}, new String[] {});
        assertThat(e)
                .hasMessageContaining(
                        "syntax error. Check that your rule is in the correct format and returns a boolean");
    }

    @Test
    public void testVerifyRulesWithLambda() {
        final InvalidRuleException e =
                expectInvalidRule(
                        "${(x -> 42);true}",
                        new Object[][] {
                            {"browser", "IE"},
                        },
                        new String[] {});
        assertThat(e)
                .hasMessageContaining(
                        "syntax error. Check that your rule is in the correct format and returns a boolean");
    }

    @Test
    public void testVerifyAndRuleNormal() {
        expectValidRule(
                "${browser == 'IE9' && country == 'US'}",
                new Object[][] {
                    {"browser", "IE"},
                    {"country", "JP"},
                },
                new String[] {});
    }

    @Test
    public void testVerifyAndRuleWithoutContext() {
        final String testRule = "${browser == 'IE9' && country == 'US'}";
        InvalidRuleException e =
                expectInvalidRule(
                        testRule,
                        new Object[][] {
                            {"browser", "IE"},
                        },
                        new String[] {});
        assertThat(e).hasMessageContaining("not defined in the application's test specification");
        e =
                expectInvalidRule(
                        testRule,
                        new Object[][] {
                            {"country", "US"},
                        },
                        new String[] {});
        assertThat(e).hasMessageContaining("not defined in the application's test specification");
        e =
                expectInvalidRule(
                        testRule,
                        new Object[][] {},
                        new String[] {
                            "browser",
                        });
        assertThat(e).hasMessageContaining("not defined in the application's test specification");
        e =
                expectInvalidRule(
                        testRule,
                        new Object[][] {},
                        new String[] {
                            "country",
                        });
        assertThat(e).hasMessageContaining("not defined in the application's test specification");
    }

    @Test
    public void testVerifyAndRuleWithAbsentIdentifers() {
        final String testRule = "${browser == 'IE9' && country == 'US'}";
        expectValidRule(
                testRule,
                new Object[][] {
                    {"browser", "IE"},
                },
                new String[] {
                    "country",
                });
        expectValidRule(
                testRule,
                new Object[][] {
                    {"country", "JP"},
                },
                new String[] {
                    "browser",
                });
        expectValidRule(
                testRule,
                new Object[][] {},
                new String[] {
                    "browser", "country",
                });
    }

    @Test
    public void testInvalidSyntaxRuleWithConcat() {
        final InvalidRuleException e =
                expectInvalidRule(
                        "${browser == 'IE9' && country += 'US'}",
                        new Object[][] {
                            {"country", "US"},
                            {"browser", "chrome"}
                        },
                        new String[] {});
        assertThat(e)
                .hasMessageContaining(
                        "syntax error. Check that your rule is in the correct format and returns a boolean");
    }

    @Test
    public void testInvalidSyntaxRuleWithArrow() {
        final InvalidRuleException e =
                expectInvalidRule(
                        "${browser == 'IE9' && country -> 'US'}",
                        new Object[][] {
                            {"country", "US"},
                            {"browser", "chrome"}
                        },
                        new String[] {});
        assertThat(e)
                .hasMessageContaining(
                        "syntax error. Check that your rule is in the correct format and returns a boolean");
    }

    @Test
    public void testInvalidSyntaxRuleWithAssignment() {
        final String testRuleUsingValid =
                "${lang=='en' && ((companyUrl != null && country == 'US' && indeed:contains(EC_COMPANY_URLS, companyUrl)))}";
        final String testRuleUsingAssign =
                "${lang=='en' && ((companyUrl != null && country = 'US' && indeed:contains(EC_COMPANY_URLS, companyUrl)))}";

        expectValidRule(
                testRuleUsingValid,
                new Object[][] {{"EC_COMPANY_URLS", "foo"}},
                new String[] {
                    "lang", "companyUrl", "country",
                });

        InvalidRuleException e =
                expectInvalidRule(
                        testRuleUsingAssign,
                        new Object[][] {
                            {"companyUrl", "indeed.com"},
                            {"country", "JP"},
                            {"lang", "en"},
                            {"EC_COMPANY_URLS", "foo"}
                        },
                        new String[] {});
        assertThat(e)
                .hasMessageContaining(
                        "syntax error. Check that your rule is in the correct format and returns a boolean");
        e =
                expectInvalidRule(
                        testRuleUsingAssign,
                        new Object[][] {{"EC_COMPANY_URLS", "foo"}},
                        new String[] {
                            "lang", "companyUrl", "country",
                        });
        assertThat(e)
                .hasMessageContaining(
                        "syntax error. Check that your rule is in the correct format and returns a boolean");
    }
}
