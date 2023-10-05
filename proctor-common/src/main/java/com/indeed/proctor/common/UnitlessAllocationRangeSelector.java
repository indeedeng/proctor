package com.indeed.proctor.common;

import com.indeed.proctor.common.model.ConsumableTestDefinition;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import java.util.function.Function;

public class UnitlessAllocationRangeSelector extends TestRangeSelector {
    UnitlessAllocationRangeSelector(
            @Nonnull final ExpressionFactory expressionFactory,
            @Nonnull final FunctionMapper functionMapper,
            final String testName,
            @Nonnull final ConsumableTestDefinition testDefinition,
            @Nonnull final IdentifierValidator identifierValidator) {
        super(expressionFactory, functionMapper, testName, testDefinition, identifierValidator);
    }

    UnitlessAllocationRangeSelector(
            @Nonnull final RuleEvaluator ruleEvaluator,
            @Nonnull final String testName,
            @Nonnull final ConsumableTestDefinition testDefinition,
            @Nonnull final IdentifierValidator identifierValidator) {
        super(ruleEvaluator, testName, testDefinition, identifierValidator);
    }

    @Override
    protected int getMatchingAllocation(
            final Function<String, Boolean> evaluator, @Nullable final String identifier) {
        final String[] rules = getRules();
        for (int i = 0; i < rules.length; i++) {
            final String rule = rules[i];
            if (isValidAllocation(identifier, rule) && evaluator.apply(rule)) {
                return i;
            }
        }
        return -1;
    }

    private boolean isValidAllocation(final String identifier, final String rule) {
        return isNormalAllocation(identifier) || isUnitlessAllocation(rule);
    }

    private boolean isNormalAllocation(final String identifier) {
        return identifier != null
                && getIdentifierValidator().validate(getTestDefinition().getTestType(), identifier);
    }

    private boolean isUnitlessAllocation(final String rule) {
        return rule != null
                && getTestDefinition().getContainsUnitlessAllocation()
                && rule.contains("missingExperimentalUnit");
    }
}
