package com.indeed.proctor.common.el;

import com.google.common.collect.ImmutableSet;
import com.indeed.proctor.common.RuleEvaluator;

import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.ExpressionFactory;
import javax.el.FunctionMapper;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import java.util.HashSet;
import java.util.Set;

/** Util class for analyzing rule expressions */
public class RuleAnalyzer {

    /** @return variables contained in the expression */
    public static Set<String> getReferencedVariables(final String elString) {
        final NameGatheringVariableMapper variableMapper = new NameGatheringVariableMapper();
        // instances are not thread-safe, so creating new ones on every call.
        final ELResolver elResolver = new CompositeELResolver();
        final FunctionMapper functionMapper = RuleEvaluator.defaultFunctionMapperBuilder().build();
        ExpressionFactory.newInstance()
                .createValueExpression(
                        new ELContext() {
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
                        },
                        elString,
                        Void.class);
        return variableMapper.getGatheredVariables();
    }

    /**
     * Catches the Java Expression Language's events for variables to determine what variables are
     * mentioned in an el statement (which is what proctor rules are). Does not actually resolve
     * variables.
     */
    private static class NameGatheringVariableMapper extends VariableMapper {
        private final Set<String> variablesReferenced = new HashSet<>();

        @Override
        public ValueExpression resolveVariable(final String variable) {
            variablesReferenced.add(variable);
            return null;
        }

        @Override
        public ValueExpression setVariable(
                final String variable, final ValueExpression expression) {
            throw new UnsupportedOperationException();
        }

        private Set<String> getGatheredVariables() {
            return ImmutableSet.copyOf(variablesReferenced);
        }
    }
}
