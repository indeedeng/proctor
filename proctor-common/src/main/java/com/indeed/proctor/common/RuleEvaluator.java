package com.indeed.proctor.common;

import com.indeed.proctor.common.el.LibraryFunctionMapperBuilder;
import com.indeed.proctor.common.el.MulticontextReadOnlyVariableMapper;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.el.ExpressionFactoryImpl;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.el.ArrayELResolver;
import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ListELResolver;
import javax.el.MapELResolver;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import java.util.Map;

/**
 * A nice tidy packaging of javax.el stuff.
 *
 * @author ketan
 * @author pwp
 *
 */
public class RuleEvaluator {
    private static final Logger LOGGER = LogManager.getLogger(RuleEvaluator.class);

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

    public static RuleEvaluator createDefaultRuleEvaluator(final Map<String, Object> testConstantsMap) {
        return new RuleEvaluator(EXPRESSION_FACTORY, FUNCTION_MAPPER, testConstantsMap);
    }

    @Nonnull
    private static CompositeELResolver constructStandardElResolver() {
        final CompositeELResolver elResolver = new CompositeELResolver();
        elResolver.add(new ArrayELResolver());
        elResolver.add(new ListELResolver());
        elResolver.add(new MapELResolver());
        elResolver.add(new BeanELResolver()); // this must be last, because it throws Exception
        return elResolver;
    }


    public static LibraryFunctionMapperBuilder defaultFunctionMapperBuilder() {
        final LibraryFunctionMapperBuilder builder = new LibraryFunctionMapperBuilder()
                                                .add("indeed", ProctorRuleFunctions.class) //backwards compatibility
                                                .add("fn", LegacyTaglibFunctions.class)
                                                .add("proctor", ProctorRuleFunctions.class);
        return builder;
    }

    @Nonnull
    ELContext createElContext(@Nonnull final Map<String, Object> values) {
        final Map<String, ValueExpression> localContext = ProctorUtils.convertToValueExpressionMap(expressionFactory, values);
        @SuppressWarnings("unchecked")
        final VariableMapper variableMapper = new MulticontextReadOnlyVariableMapper(testConstants, localContext);
        return createELContext(variableMapper);
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
        if (StringUtils.isBlank(rule)) {
            return true;
        }
        if (!rule.startsWith("${") || !rule.endsWith("}")) {
            LOGGER.error("Invalid rule '" +  rule + "'");   //  TODO: should this be an exception?
            return false;
        }
        final String bareRule = ProctorUtils.removeElExpressionBraces(rule);
        if (StringUtils.isBlank(bareRule) || "true".equalsIgnoreCase(bareRule)) {
            return true;    //  always passes
        }
        if ("false".equalsIgnoreCase(bareRule)) {
            return false;
        }

        final ELContext elContext = createElContext(values);
        final ValueExpression ve = expressionFactory.createValueExpression(elContext, rule, String.class);
        final String result = (String) ve.getValue(elContext);
        checkRuleIsBooleanType(rule, result);

        return Boolean.parseBoolean(result);
    }

    /**
     * @throws IllegalArgumentException if type of expression is not boolean
     */
    static void checkRuleIsBooleanType(final String rule, final String value) {
        // apache-el is an expression language, not a rule language, and it is very lenient
        // sadly that means it will just evaluate to false when users make certain mistakes, e.g. by
        // coercing String value "xyz" to boolean false, instead of throwing an exception.
        // To support users writing rules, be more strict here in requiring the type of the
        // value to be expected before coercion
        if (StringUtils.isBlank(value) || "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return;
        }
        throw new IllegalArgumentException("Received non-boolean return value: " + value + " from rule " + rule);
    }

}
