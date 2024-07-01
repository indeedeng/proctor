package com.indeed.proctor.common;

import javax.el.ValueExpression;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/** May provide a map of identifiers to use for rule evaluation */
public class ProvidedContext {
    enum ProvidedContextEvaluationMode {
        EVALUATE_WITH_CONTEXT,
        EVALUATE_WITHOUT_CONTEXT,
        SKIP_EVALUATION;
    }
    /** @deprecated Use factory method nonEvaluableContext() */
    @Deprecated
    public static final Map<String, ValueExpression> EMPTY_CONTEXT = Collections.emptyMap();

    private static final ProvidedContext NON_EVALUABLE =
            new ProvidedContext(ProvidedContext.EMPTY_CONTEXT, false);
    private static final ProvidedContext EMPTY_EVALUABLE =
            new ProvidedContext(
                    ProvidedContext.EMPTY_CONTEXT,
                    ProvidedContextEvaluationMode.EVALUATE_WITHOUT_CONTEXT);
    private final Map<String, ValueExpression> context;
    private final Set<String> uninstantiatedIdentifiers;
    /** if false, indicates that for rule verification, this context is unusable (empty) */
    private final boolean shouldEvaluate;

    private final ProvidedContextEvaluationMode mode;

    /** @deprecated Use factory methods */
    @Deprecated
    public ProvidedContext(
            final Map<String, ValueExpression> context,
            final boolean shouldEvaluate,
            final Set<String> uninstantiatedIdentifiers) {
        this.context = context;
        this.shouldEvaluate = shouldEvaluate;
        this.uninstantiatedIdentifiers = uninstantiatedIdentifiers;
        this.mode =
                shouldEvaluate
                        ? ProvidedContextEvaluationMode.EVALUATE_WITH_CONTEXT
                        : ProvidedContextEvaluationMode.SKIP_EVALUATION;
    }

    /** @deprecated Use factory methods */
    @Deprecated
    public ProvidedContext(
            final Map<String, ValueExpression> context, final ProvidedContextEvaluationMode mode) {
        this.context = context;
        this.shouldEvaluate = mode == ProvidedContextEvaluationMode.SKIP_EVALUATION;
        this.uninstantiatedIdentifiers = Collections.emptySet();
        this.mode = mode;
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

    public static ProvidedContext evaluableContext() {
        return EMPTY_EVALUABLE;
    }

    public Map<String, ValueExpression> getContext() {
        return context;
    }

    public Set<String> getUninstantiatedIdentifiers() {
        return uninstantiatedIdentifiers;
    }

    public ProvidedContextEvaluationMode getEvaluationMode() {
        return mode;
    }

    public boolean shouldEvaluate() {
        return shouldEvaluate;
    }
}
