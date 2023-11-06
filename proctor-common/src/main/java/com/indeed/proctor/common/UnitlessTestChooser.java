package com.indeed.proctor.common;

import com.indeed.proctor.common.model.ConsumableTestDefinition;
import com.indeed.proctor.common.model.TestBucket;
import com.indeed.proctor.common.model.TestType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ValueExpression;
import java.util.Map;
import java.util.Set;

public class UnitlessTestChooser extends StandardTestChooser {
    public UnitlessTestChooser(
            @Nonnull final ExpressionFactory expressionFactory,
            @Nonnull final FunctionMapper functionMapper,
            @Nonnull final String testName,
            @Nonnull final ConsumableTestDefinition testDefinition,
            @Nonnull final IdentifierValidator identifierValidator) {
        super(
                new UnitlessAllocationRangeSelector(
                        expressionFactory,
                        functionMapper,
                        testName,
                        testDefinition,
                        identifierValidator));
    }

    @Nonnull
    @Override
    public TestChooser.Result choose(
            @Nullable final String identifier,
            @Nonnull final Map<String, ValueExpression> localContext,
            @Nonnull final Map<String, TestBucket> testGroups,
            @Nonnull final ForceGroupsOptions forceGroupsOptions,
            @Nonnull final Set<TestType> testTypesWithInvalidIdentifier,
            final boolean isRandomEnabled) {
        // null is not valid identifier for unitless
        if (identifier == null) {
            return Result.EMPTY;
        }
        return super.choose(identifier, localContext, testGroups, forceGroupsOptions);
    }
}
