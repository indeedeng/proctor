package com.indeed.proctor.common;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.indeed.proctor.common.el.MulticontextReadOnlyVariableMapper;
import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestBucket;
import org.apache.el.ExpressionFactoryImpl;
import org.junit.Assert;
import org.junit.Test;

import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.PropertyNotFoundException;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertTrue;

/**
 *
 */
public class TestRuleVerifyUtils {

    private String testRule = "${browser != 'IE9'}";
    private ExpressionFactory expressionFactory = new ExpressionFactoryImpl();

    private ELContext setUpElContextWithContext(final Map<String, Object> context) {
        final List<TestBucket> buckets = TestProctorUtils.fromCompactBucketFormat("inactive:-1,control:0,test:1");
        final ConsumableTestDefinition testDefVal1 = TestProctorUtils.constructDefinition(buckets,
                TestProctorUtils.fromCompactAllocationFormat(String.format("%s|-1:0.5,0:0.5,1:0.0", testRule), "-1:0.25,0:0.5,1:0.25"));
        final Map<String, ValueExpression> testConstants = ProctorUtils.convertToValueExpressionMap(expressionFactory, testDefVal1.getConstants());

        final ProvidedContext providedContext = new ProvidedContext(ProctorUtils.convertToValueExpressionMap(expressionFactory, context), true);
        final VariableMapper variableMapper = new MulticontextReadOnlyVariableMapper(testConstants, providedContext.getContext());

        final RuleEvaluator ruleEvaluator = new RuleEvaluator(expressionFactory, RuleEvaluator.FUNCTION_MAPPER, testDefVal1.getConstants());
        return ruleEvaluator.createELContext(variableMapper);
    }

    @Test
    public void testVerifyRulesNormal() throws IncompatibleTestMatrixException {
        final Map<String, Object> context = Maps.newHashMap();
        context.put("browser", "IE");
        final ELContext elContext = setUpElContextWithContext(context);
        final Set<String> absentIdentifiers = Sets.newHashSet();

        RuleVerifyUtils.verifyRule(testRule, true, expressionFactory, elContext, absentIdentifiers);
        /** no exception was thrown **/
        assertTrue(true);
    }

    @Test
    public void testVerifyRulesWithoutContext() throws IncompatibleTestMatrixException {
        final Map<String, Object> context = Maps.newHashMap();
        final ELContext elContext = setUpElContextWithContext(context);
        final Set<String> absentIdentifiers = Sets.newHashSet();
        try {
            RuleVerifyUtils.verifyRule(testRule, true, expressionFactory, elContext, absentIdentifiers);
            Assert.fail();
        } catch (final PropertyNotFoundException el) {
            /** expected **/
            assertTrue(true);
        }
    }

    @Test
    public void testVerifyRulesWithAbsentIdentifiers() throws IncompatibleTestMatrixException {
        final ELContext elContext = setUpElContextWithContext(Maps.<String, Object>newHashMap());
        final Set<String> absentIdentifiers = Sets.newHashSet("browser");
        RuleVerifyUtils.verifyRule(testRule, true, expressionFactory, elContext, absentIdentifiers);
        /** no exception was thrown **/
        assertTrue(true);
    }
}