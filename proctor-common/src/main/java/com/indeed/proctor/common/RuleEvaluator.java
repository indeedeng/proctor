package com.indeed.proctor.common;

import com.indeed.proctor.common.el.LibraryFunctionMapperBuilder;
import com.indeed.proctor.common.el.MulticontextReadOnlyVariableMapper;
import org.apache.el.ExpressionFactoryImpl;
import org.apache.log4j.Logger;
import org.apache.taglibs.standard.functions.Functions;

import javax.annotation.Nonnull;
import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import javax.servlet.jsp.el.ScopedAttributeELResolver;
import java.util.Map;
import java.util.Objects;

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
        elResolver.add(new UndefinedIdentifierELResolver());
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

    /**
     * An ELResolver for resolving undefined variables as null without checking static field and method reference
     */
    private static class UndefinedIdentifierELResolver extends ScopedAttributeELResolver {
        @Override
        public Object getValue(final ELContext context, final Object base, final Object property)
                throws NullPointerException, ELException {
            Objects.requireNonNull(context);

            if (base == null) {
                context.setPropertyResolved(true);
                LOGGER.info("Resolved as null for the property: " + property);
            }

            return null;
        }
    }
}
