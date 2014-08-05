package com.indeed.proctor.common;


import javax.el.ValueExpression;
import java.util.Collections;
import java.util.Map;

/**
 * Created by rrice on 7/1/14.
 */
public class ProvidedContext {
    public static final Map<String,ValueExpression> EMPTY_CONTEXT = Collections.emptyMap();
    private final Map<String,ValueExpression> context;
    private final boolean evaluable;
    public ProvidedContext(final Map<String, ValueExpression> context,
                           boolean evaluable) {
        this.context = context;
        this.evaluable = evaluable;
    }
    public Map<String,ValueExpression> getContext() {
        return context;
    }
    public boolean isEvaluable() {
        return evaluable;
    }
}
