package com.indeed.proctor.common;

import com.indeed.proctor.common.el.LibraryFunctionMapperBuilder;
import com.indeed.proctor.common.el.MulticontextReadOnlyVariableMapper;
import com.indeed.proctor.common.el.PartialExpressionBuilder;
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
import javax.el.ValueReference;
import javax.el.VariableMapper;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A nice tidy packaging of javax.el stuff.
 *
 * @author ketan
 * @author pwp
 */
public class RuleEvaluator {
    private static final Logger LOGGER = LogManager.getLogger(RuleEvaluator.class);

    static final FunctionMapper FUNCTION_MAPPER = defaultFunctionMapperBuilder().build();

    static final ExpressionFactory EXPRESSION_FACTORY = new ExpressionFactoryImpl();

    @Nonnull final ExpressionFactory expressionFactory;
    @Nonnull final CompositeELResolver elResolver;
    @Nonnull private final Map<String, ValueExpression> testConstants;
    @Nonnull private final FunctionMapper functionMapper;

    RuleEvaluator(
            @Nonnull final ExpressionFactory expressionFactory,
            @Nonnull final FunctionMapper functionMapper,
            @Nonnull final Map<String, Object> testConstantsMap) {
        this.expressionFactory = expressionFactory;

        this.functionMapper = functionMapper;

        elResolver = constructStandardElResolver();

        testConstants =
                ProctorUtils.convertToValueExpressionMap(expressionFactory, testConstantsMap);
    }

    public static RuleEvaluator createDefaultRuleEvaluator(
            final Map<String, Object> testConstantsMap) {
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
        final LibraryFunctionMapperBuilder builder =
                new LibraryFunctionMapperBuilder()
                        .add("indeed", ProctorRuleFunctions.class) // backwards compatibility
                        .add("fn", LegacyTaglibFunctions.class)
                        .add("proctor", ProctorRuleFunctions.class);
        return builder;
    }

    @Nonnull
    ELContext createElContext(@Nonnull final Map<String, ValueExpression> localContext) {
        @SuppressWarnings("unchecked")
        final VariableMapper variableMapper =
                new MulticontextReadOnlyVariableMapper(testConstants, localContext);
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

    /**
     * @deprecated Use evaluateBooleanRuleWithValueExpr(String, Map) instead, it's more efficient
     */
    @Deprecated
    public boolean evaluateBooleanRule(final String rule, @Nonnull final Map<String, Object> values)
            throws IllegalArgumentException {
        final Map<String, ValueExpression> localContext =
                ProctorUtils.convertToValueExpressionMap(expressionFactory, values);
        return evaluateBooleanRuleWithValueExpr(rule, localContext);
    }

    public boolean evaluateBooleanRuleWithValueExpr(
            final String rule, @Nonnull final Map<String, ValueExpression> localContext)
            throws IllegalArgumentException {
        if (StringUtils.isBlank(rule)) {
            return true;
        }
        if (!rule.startsWith("${") || !rule.endsWith("}")) {
            LOGGER.error("Invalid rule '" + rule + "'"); //  TODO: should this be an exception?
            return false;
        }
        final ProctorUtils.ElExpressionClassification ec =
                ProctorUtils.clasifyElExpression(rule, true);
        if (ec == ProctorUtils.ElExpressionClassification.EMPTY
                || ec == ProctorUtils.ElExpressionClassification.CONSTANT_TRUE) {
            return true; //  always passes
        }
        if (ec == ProctorUtils.ElExpressionClassification.CONSTANT_FALSE) {
            return false;
        }

        final ELContext elContext = createElContext(localContext);
        final ValueExpression ve =
                expressionFactory.createValueExpression(elContext, rule, boolean.class);
        checkRuleIsBooleanType(rule, elContext, ve);

        final Object result = ve.getValue(elContext);

        if (result instanceof Boolean) {
            return ((Boolean) result);
        }
        // this should never happen, evaluateRule throws ELException when it cannot coerce to
        // Boolean
        throw new IllegalArgumentException(
                "Received non-boolean return value: "
                        + (result == null ? "null" : result.getClass().getCanonicalName())
                        + " from rule "
                        + rule);
    }
    /**
     * @deprecated Use evaluateBooleanRulePartialWithValueExpr(String, Map) instead, it's more
     *     efficient
     */
    @Deprecated
    public boolean evaluateBooleanRulePartial(
            final String rule, @Nonnull final Map<String, Object> values)
            throws IllegalArgumentException {
        final Map<String, ValueExpression> localContext =
                ProctorUtils.convertToValueExpressionMap(expressionFactory, values);
        return evaluateBooleanRulePartialWithValueExpr(rule, localContext);
    }

    /**
     * This method should only be used for partial matching with proctor rules
     *
     * @return Evaluates a partial rule
     */
    public boolean evaluateBooleanRulePartialWithValueExpr(
            final String rule, @Nonnull final Map<String, ValueExpression> values)
            throws IllegalArgumentException {
        if (StringUtils.isBlank(rule)) {
            return true;
        }
        if (!rule.startsWith("${") || !rule.endsWith("}")) {
            LOGGER.error("Invalid rule '" + rule + "'"); //  TODO: should this be an exception?
            return false;
        }
        final String bareRule = ProctorUtils.removeElExpressionBraces(rule);
        if (StringUtils.isBlank(bareRule) || "true".equalsIgnoreCase(bareRule)) {
            return true; //  always passes
        }
        if ("false".equalsIgnoreCase(bareRule)) {
            return false;
        }

        final ELContext elContext = createElContext(values);
        final Set<String> variablesDefined = new HashSet<>();
        variablesDefined.addAll(values.keySet());
        variablesDefined.addAll(testConstants.keySet());
        final PartialExpressionBuilder builder =
                new PartialExpressionBuilder(rule, elContext, variablesDefined);
        final ValueExpression ve = builder.createValueExpression(boolean.class);
        checkRuleIsBooleanType(rule, elContext, ve);

        final Object result = ve.getValue(elContext);

        if (result instanceof Boolean) {
            return ((Boolean) result);
        }
        // this should never happen, evaluateRule throws ELException when it cannot coerce to
        // Boolean
        throw new IllegalArgumentException(
                "Received non-boolean return value: "
                        + (result == null ? "null" : result.getClass().getCanonicalName())
                        + " from rule "
                        + rule);
    }

    /** @throws IllegalArgumentException if type of expression is not boolean */
    static void checkRuleIsBooleanType(
            final String rule, final ELContext elContext, final ValueExpression ve) {
        // apache-el is an expression language, not a rule language, and it is very lenient
        // sadly that means it will just evaluate to false when users make certain mistakes, e.g. by
        // coercing String value "xyz" to boolean false, instead of throwing an exception.
        // To support users writing rules, be more strict here in requiring the type of the
        // value to be expected before coercion
        Class<?> type = ve.getType(elContext);

        // if object is map check what the base value type is
        if (Objects.equals(type, Object.class)) {
            final ValueReference valueReference = ve.getValueReference(elContext);
            final Object base = valueReference.getBase();
            final Object property = valueReference.getProperty();
            try {
                if (base instanceof Map<?, ?>) {
                    final Map<?, ?> map = (Map<?, ?>) base;
                    type = map.get(property).getClass();
                }
            } catch (final NullPointerException e) {
                throw new IllegalArgumentException(
                        "Received nested value which does not exist in context map: value "
                                + property
                                + " in rule "
                                + rule);
            }
        }

        if (ClassUtils.isPrimitiveWrapper(type)) {
            type = ClassUtils.wrapperToPrimitive(type);
        }
        // allow null to be coerced for historic reasons
        if ((type != null) && (type != boolean.class)) {
            throw new IllegalArgumentException(
                    "Received non-boolean return value: " + type + " from rule " + rule);
        }
    }

    /**
     * @param expectedType class to coerce result to, use primitive instead of wrapper, e.g.
     *     boolean.class instead of Boolean.class.
     * @return null or a Boolean value representing the expression evaluation result
     * @throws RuntimeException: E.g. PropertyNotFound or other ELException when not of expectedType
     * @deprecated Use evaluateBooleanRule() instead, it checks against more errors
     */
    @CheckForNull
    @Deprecated
    public Object evaluateRule(
            final String rule, final Map<String, Object> values, final Class expectedType) {
        final ELContext elContext =
                createElContext(
                        ProctorUtils.convertToValueExpressionMap(expressionFactory, values));
        final ValueExpression ve =
                expressionFactory.createValueExpression(elContext, rule, expectedType);
        return ve.getValue(elContext);
    }
}
