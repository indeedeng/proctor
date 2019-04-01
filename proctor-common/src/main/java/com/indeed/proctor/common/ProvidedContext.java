package com.indeed.proctor.common;


import com.indeed.shaded.javax.el7.ValueExpression;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Created by rrice on 7/1/14.
 */
public class ProvidedContext {
    public static final Map<String,ValueExpression> EMPTY_CONTEXT = Collections.emptyMap();
    private final Map<String,ValueExpression> context;
    private final Set<String> uninstantiatedIdentifiers;
    private final boolean shouldEvaluate;
    public ProvidedContext(final Map<String, ValueExpression> context,
                           final boolean shouldEvaluate,
                           final Set<String> uninstantiatedIdentifiers) {
        this.context = context;
        this.shouldEvaluate = shouldEvaluate;
        this.uninstantiatedIdentifiers = uninstantiatedIdentifiers;
    }

    public ProvidedContext(final Map<String, ValueExpression> context, final boolean evaluable) {
        this(context, evaluable, Collections.<String>emptySet());
    }

    public Map<String,ValueExpression> getContext() {
        return context;
    }

    public Set<String> getUninstantiatedIdentifiers() {
        return uninstantiatedIdentifiers;
    }

    public boolean shouldEvaluate() {
        return shouldEvaluate;
    }
}
