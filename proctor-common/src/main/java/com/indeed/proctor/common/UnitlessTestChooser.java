package com.indeed.proctor.common;

import com.indeed.proctor.common.model.ConsumableTestDefinition;

import javax.annotation.Nonnull;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;

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
}
