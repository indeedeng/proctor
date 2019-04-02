package com.indeed.proctor.common.el;


import javax.el.ValueExpression;
import javax.el.VariableMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

/**
 * Provides services for the Unified Expression Language to look up variables in priority order given the input contexts.
 * @author ketan
 */
public class MulticontextReadOnlyVariableMapper extends VariableMapper {
    @Nonnull
    private final Map<String, ValueExpression>[] contexts;
    public MulticontextReadOnlyVariableMapper(@Nonnull final Map<String, ValueExpression>...contexts) {
        this.contexts = contexts;
    }

    @Nullable
    @Override
    public ValueExpression resolveVariable(final String name) {
        //noinspection unchecked
        return MulticontextReadOnlyVariableMapper.resolve(name, contexts);
    }

    @Nonnull
    @Override
    public ValueExpression setVariable(final String name, final ValueExpression expression) {
        throw new IllegalStateException("Setting variables is not allowed");
    }

    @Nullable
    public static ValueExpression resolve(final String variableName, @Nonnull final Map<String, ValueExpression>...contexts) {
        for (final Map<String, ValueExpression> context : contexts) {
            final ValueExpression ve = context.get(variableName);
            if (ve != null) {
                return ve;
            }
        }
        return null;
    }
}
