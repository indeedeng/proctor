package com.indeed.proctor.common;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.indeed.proctor.common.el.LibraryFunctionMapperBuilder;
import com.indeed.proctor.common.el.MulticontextReadOnlyVariableMapper;
import com.indeed.proctor.shaded.javax.el.ArrayELResolver;
import com.indeed.proctor.shaded.javax.el.BeanELResolver;
import com.indeed.proctor.shaded.javax.el.CompositeELResolver;
import com.indeed.proctor.shaded.javax.el.ELContext;
import com.indeed.proctor.shaded.javax.el.ELResolver;
import com.indeed.proctor.shaded.javax.el.ExpressionFactory;
import com.indeed.proctor.shaded.javax.el.FunctionMapper;
import com.indeed.proctor.shaded.javax.el.ListELResolver;
import com.indeed.proctor.shaded.javax.el.MapELResolver;
import com.indeed.proctor.shaded.javax.el.ValueExpression;
import com.indeed.proctor.shaded.javax.el.VariableMapper;
import com.indeed.proctor.shaded.org.apache.el.ExpressionFactoryImpl;

import java.util.Map;

public class BenchmarkEl {
    public static void main(final String[] args) {
        final FunctionMapper functionMapper = new LibraryFunctionMapperBuilder().add("proctor", ProctorRuleFunctions.class).build();

        final ExpressionFactory expressionFactory = new ExpressionFactoryImpl();

        final CompositeELResolver elResolver = new CompositeELResolver();
        elResolver.add(new ArrayELResolver());
        elResolver.add(new ListELResolver());
        elResolver.add(new BeanELResolver());
        elResolver.add(new MapELResolver());

        final Map<String, Object> values = Maps.newLinkedHashMap();
        values.put("countries", Sets.newHashSet("AA", "BB", "CC", "DD", "EE", "FF", "GG", "HH", "II", "JJ", "KK", "LL", "MM"));
        values.put("AA", "AA");
        values.put("CC", "CC");
        values.put("NN", "NN");
        values.put("ZZ", "ZZ");
        values.put("I1", 239235);
        values.put("I2", 569071142);
        values.put("I3", -189245);
        values.put("D1", 129835.12512);
        values.put("D2", -9582.9385);
        values.put("D3", 98982223.598731412);
        values.put("BT", Boolean.TRUE);
        values.put("BF", Boolean.FALSE);
        values.put("GLOOP", "");

        final String[] expressions = {
                "${proctor:contains(countries, AA) || proctor:contains(countries, CC) || D2 < I3 && BF}",
                "${! proctor:contains(countries, ZZ) && I1 < I2 && empty GLOOP}",
                "${I2 - I3 + D3 - D1}",
                "${NN == '0' && ZZ == 'ZZ'}",
                "${BT != BF}",
        };

        final int iterations = 100*1000;
        long elapsed = -System.currentTimeMillis();
        for (int i = 0; i < iterations; i++) {
            final Map<String, ValueExpression> localContext = ProctorUtils.convertToValueExpressionMap(expressionFactory, values);
            final VariableMapper variableMapper = new MulticontextReadOnlyVariableMapper(localContext);

            final ELContext elContext = new ELContext() {
                @Override
                public ELResolver getELResolver() {
                    return elResolver;
                }

                @Override
                public FunctionMapper getFunctionMapper() {
                    return functionMapper;
                }

                @Override
                public VariableMapper getVariableMapper() {
                    return variableMapper;
                }
            };

            for (int j = 0; j < expressions.length; j++) {
                final ValueExpression ve = expressionFactory.createValueExpression(elContext, expressions[j], Object.class);
                final Object result = ve.getValue(elContext);
                if (i % 10000 == 0) {
                    System.out.println(result);
                }
            }
        }
        elapsed += System.currentTimeMillis();

        final int total = iterations * expressions.length;
        System.out.println(total + " expressions in " + elapsed + " ms (average " + (elapsed/(((double) total))) + " ms/expression)");
    }
}
