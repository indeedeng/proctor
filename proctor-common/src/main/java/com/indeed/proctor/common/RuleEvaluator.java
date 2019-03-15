package com.indeed.proctor.common;

import com.indeed.proctor.common.el.LibraryFunctionMapperBuilder;
import com.indeed.proctor.common.el.MulticontextReadOnlyVariableMapper;
import org.apache.el.ExpressionFactoryImpl;
import org.apache.el.lang.ELSupport;
import org.apache.log4j.Logger;
import org.apache.taglibs.standard.functions.Functions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
        final CompositeELResolver elResolver = new LoggingCompositeELResolver();
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

            @Override
            public Object convertToType(Object obj, Class<?> type) {
                return ELSupport.coerceToType(null, obj, type);
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

        final long start = System.currentTimeMillis();

        final Object result = ve.getValue(elContext);

        final long end = System.currentTimeMillis();
        final long diff = end - start;
        if (diff > 500) {
            LOGGER.info(String.format("Tomcat 8: end evaluation for rule: %s, values: %s in %dms", rule, values, diff));
        }

        if (result instanceof Boolean) {
            return ((Boolean) result);
        }

        throw new IllegalArgumentException("Received non-boolean return value: " + result.getClass().getCanonicalName() + " from rule " + rule);
    }

    private static class LoggingCompositeELResolver extends CompositeELResolver {
        private int size = 0;
        private ELResolver[] resolvers = new ELResolver[8];

        public LoggingCompositeELResolver() {
        }

        @Override
        public void add(ELResolver elResolver) {
            Objects.requireNonNull(elResolver);
            if (this.size >= this.resolvers.length) {
                ELResolver[] nr = new ELResolver[this.size * 2];
                System.arraycopy(this.resolvers, 0, nr, 0, this.size);
                this.resolvers = nr;
            }

            this.resolvers[this.size++] = elResolver;
        }

        @Override
        @Nullable
        public Object getValue(@Nullable final ELContext context, @Nullable final Object base, @Nullable final Object property) {
            context.setPropertyResolved(false);
            int sz = this.size;

            for(int i = 0; i < sz; ++i) {
                final long start = System.currentTimeMillis();
                Object result = this.resolvers[i].getValue(context, base, property);
                final long end = System.currentTimeMillis();
                final long diff = end - start;
                //LOGGER.info(String.format("Resolver: %s ends resolving for base: %s, property: %s in %dms", this.resolvers[i].getClass().getSimpleName(), base, property, diff));
                if (context.isPropertyResolved()) {
                    return result;
                }
            }

            return null;
        }
    }
}
