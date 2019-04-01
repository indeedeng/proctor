package com.indeed.proctor.common;

import com.indeed.proctor.common.el.LibraryFunctionMapperBuilder;
import com.indeed.proctor.common.el.MulticontextReadOnlyVariableMapper;
import com.indeed.shaded.javax.el7.ArrayELResolver;
import com.indeed.shaded.javax.el7.BeanELResolver;
import com.indeed.shaded.javax.el7.CompositeELResolver;
import com.indeed.shaded.javax.el7.ELContext;
import com.indeed.shaded.javax.el7.ELResolver;
import com.indeed.shaded.javax.el7.ExpressionFactory;
import com.indeed.shaded.javax.el7.FunctionMapper;
import com.indeed.shaded.javax.el7.ListELResolver;
import com.indeed.shaded.javax.el7.MapELResolver;
import com.indeed.shaded.javax.el7.ValueExpression;
import com.indeed.shaded.javax.el7.VariableMapper;
import com.indeed.shaded.org.apache.el7.ExpressionFactoryImpl;
import org.apache.log4j.Logger;
import org.apache.taglibs.standard.functions.Functions;

import javax.annotation.Nonnull;
import java.util.Map;

/**
 * A nice tidy packaging of javax.el stuff.
 *
 * @author ketan
 * @author pwp
 *
 */
public class RuleEvaluator {
    private static final Logger LOGGER = Logger.getLogger(RuleEvaluator.class);

    static final FunctionMapper FUNCTION_MAPPER = defaultFunctionMapperBuilder().build();

    static final ExpressionFactory EXPRESSION_FACTORY = new ExpressionFactoryImpl();

    @Nonnull
    final ExpressionFactory expressionFactory;
    @Nonnull
    final CompositeELResolver elResolver;
    @Nonnull
    private final Map<String, ValueExpression> testConstants;
    @Nonnull
    private final FunctionMapper functionMapper;

    RuleEvaluator(
            @Nonnull final ExpressionFactory expressionFactory,
            @Nonnull final FunctionMapper functionMapper,
            @Nonnull final Map<String, Object> testConstantsMap
    ) {
        this.expressionFactory = expressionFactory;

        this.functionMapper = functionMapper;

        elResolver = constructStandardElResolver();

        testConstants = ProctorUtils.convertToValueExpressionMap(expressionFactory, testConstantsMap);
    }

    @Nonnull
    private static CompositeELResolver constructStandardElResolver() {
        final CompositeELResolver elResolver = new CompositeELResolver();
        elResolver.add(new ArrayELResolver());
        elResolver.add(new ListELResolver());
        elResolver.add(new BeanELResolver());
        elResolver.add(new MapELResolver());
        return elResolver;
    }


    public static LibraryFunctionMapperBuilder defaultFunctionMapperBuilder() {
        final LibraryFunctionMapperBuilder builder = new LibraryFunctionMapperBuilder()
                                                .add("indeed", ProctorRuleFunctions.class) //backwards compatibility
                                                .add("fn", Functions.class)
                                                .add("proctor", ProctorRuleFunctions.class);
        return builder;
    }

    @Nonnull
    ELContext createELContext(@Nonnull final VariableMapper variableMapper) {
        return new ELContext() {
            @Nonnull
            @Override
            public ELResolver getELResolver() {
                return elResolver;
            }

            @Nonnull
            @Override
            public FunctionMapper getFunctionMapper() {
                return functionMapper;
            }

            @Nonnull
            @Override
            public VariableMapper getVariableMapper() {
                return variableMapper;
            }
        };
    }

    public boolean evaluateBooleanRule(final String rule, @Nonnull final Map<String, Object> values) throws IllegalArgumentException {
        if (ProctorUtils.isEmptyWhitespace(rule)) {
            return true;
        }
        if (!rule.startsWith("${") || !rule.endsWith("}")) {
            LOGGER.error("Invalid rule '" +  rule + "'");   //  TODO: should this be an exception?
            return false;
        }
        final String bareRule = ProctorUtils.removeElExpressionBraces(rule);
        if (ProctorUtils.isEmptyWhitespace(bareRule) || "true".equalsIgnoreCase(bareRule)) {
            return true;    //  always passes
        }
        if ("false".equalsIgnoreCase(bareRule)) {
            return false;
        }
        final Map<String, ValueExpression> localContext = ProctorUtils.convertToValueExpressionMap(expressionFactory, values);
        //noinspection unchecked
        final VariableMapper variableMapper = new MulticontextReadOnlyVariableMapper(testConstants, localContext);
        final ELContext elContext = createELContext(variableMapper);

        final ValueExpression ve = expressionFactory.createValueExpression(elContext, rule, Boolean.class);
        final Object result = ve.getValue(elContext);
        if (result instanceof Boolean) {
            return ((Boolean) result);
        }

        throw new IllegalArgumentException("Received non-boolean return value: " + result.getClass().getCanonicalName() + " from rule " + rule);
    }
}
