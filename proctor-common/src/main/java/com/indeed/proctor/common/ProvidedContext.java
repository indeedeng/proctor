package com.indeed.proctor.common;

import javax.el.ValueExpression;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/** May provide a map of identifiers to use for rule evaluation */
public class ProvidedContext {
    /** @deprecated Use factory method nonEvaluableContext() */
    @Deprecated
    public static final Map<String, ValueExpression> EMPTY_CONTEXT = Collections.emptyMap();

    private static final ProvidedContext NON_EVALUABLE =
            new ProvidedContext(ProvidedContext.EMPTY_CONTEXT, false);
    private final Map<String, ValueExpression> context;
    private final Set<String> uninstantiatedIdentifiers;
    /** if false, indicates that for rule verification, this context is unusable (empty) */
    private final boolean shouldEvaluate;

    /** @deprecated Use factory methods */
    @Deprecated
    public ProvidedContext(
            final Map<String, ValueExpression> context,
            final boolean shouldEvaluate,
            final Set<String> uninstantiatedIdentifiers) {
        this.context = context;
        this.shouldEvaluate = shouldEvaluate;
        this.uninstantiatedIdentifiers = uninstantiatedIdentifiers;
    }

    /** @deprecated Use factory methods */
    @Deprecated
    public ProvidedContext(final Map<String, ValueExpression> context, final boolean evaluable) {
        this(context, evaluable, Collections.emptySet());
    }

    /**
     * Create a context to validate rules based on the given map of known identifiers and
     * uninstantiated identifiers
     */
    public static ProvidedContext forValueExpressionMap(
            final Map<String, ValueExpression> context,
            final Set<String> uninstantiatedIdentifiers) {
        return new ProvidedContext(context, true, uninstantiatedIdentifiers);
    }

    /**
     * Create a context indicating rule validation should not attempt to evaluate. This is a
     * fallback when no context can be provided (no proctor specification present).
     */
    public static ProvidedContext nonEvaluableContext() {
        return NON_EVALUABLE;
    }

    public Map<String, ValueExpression> getContext() {
        return context;
    }

    public Set<String> getUninstantiatedIdentifiers() {
        return uninstantiatedIdentifiers;
    }

    public boolean shouldEvaluate() {
        return shouldEvaluate;
    }
}
