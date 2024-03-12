package com.indeed.proctor.common;

import com.indeed.proctor.common.el.filter.PartialExpressionFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import javax.el.FunctionMapper;
import javax.el.ValueExpression;

import java.util.Map;

/**
 * Leverages RuleEvaluator to determine whether a given rule *could* match the
 * given context; useful for tools that search allocations.
 **/
@NotThreadSafe
public class RuleFilter {
    @Nonnull
    private final PartialExpressionFactory expressionFactory;
    private final RuleEvaluator ruleEvaluator;

    RuleFilter(
            final FunctionMapper functionMapper,
            @Nonnull final Map<String, Object> testConstantsMap
    ) {
        this.expressionFactory = new PartialExpressionFactory(testConstantsMap.keySet());
        this.ruleEvaluator = new RuleEvaluator(
                expressionFactory,
                functionMapper,
                testConstantsMap);
    }

    public static RuleFilter createDefaultRuleFilter(final Map<String, Object> testConstantsMap) {
        return new RuleFilter(RuleEvaluator.FUNCTION_MAPPER, testConstantsMap);
    }

    public boolean ruleCanMatch(final String rule, final Map<String, Object> values) {
        expressionFactory.setContextVariables(values.keySet());
        final Map<String, ValueExpression> localContext = ProctorUtils
                .convertToValueExpressionMap(expressionFactory, values);
        return ruleEvaluator.evaluateBooleanRuleWithValueExpr(rule, localContext);
    }
}
