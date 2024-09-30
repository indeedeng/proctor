/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.indeed.proctor.common.el.filter;

import com.google.common.collect.ImmutableSet;
import org.apache.el.ValueExpressionLiteral;
import org.apache.el.lang.ELSupport;
import org.apache.el.util.MessageFactory;

import javax.annotation.concurrent.NotThreadSafe;
import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.el.ValueExpression;

import java.util.HashSet;
import java.util.Set;

/**
 * Like ExpressionFactoryImpl except removes nodes referring to missing variables
 */
@NotThreadSafe
public class PartialExpressionFactory extends ExpressionFactory {

    private final Set<String> testConstants;
    private final Set<String> contextVariables;

    public PartialExpressionFactory(final Set<String> testConstants) {
        super();
        this.testConstants = testConstants;
        this.contextVariables = new HashSet<>();
    }

    @Override
    public Object coerceToType(Object obj, Class<?> type) {
        return ELSupport.coerceToType(obj, type);
    }

    @Override
    public MethodExpression createMethodExpression(ELContext context,
            String expression, Class<?> expectedReturnType,
            Class<?>[] expectedParamTypes) {
        PartialExpressionBuilder builder = new PartialExpressionBuilder(expression, context, getAllDefinedVariables());
        return builder.createMethodExpression(expectedReturnType,
                expectedParamTypes);
    }

    @Override
    public ValueExpression createValueExpression(ELContext context,
            String expression, Class<?> expectedType) {
        if (expectedType == null) {
            throw new NullPointerException(MessageFactory
                    .get("error.value.expectedType"));
        }
        PartialExpressionBuilder builder = new PartialExpressionBuilder(expression, context, getAllDefinedVariables());
        return builder.createValueExpression(expectedType);
    }

    @Override
    public ValueExpression createValueExpression(Object instance,
            Class<?> expectedType) {
        if (expectedType == null) {
            throw new NullPointerException(MessageFactory
                    .get("error.value.expectedType"));
        }
        return new ValueExpressionLiteral(instance, expectedType);
    }

    public void setContextVariables(final Set<String> contextVariables) {
        this.contextVariables.clear();
        this.contextVariables.addAll(contextVariables);
    }

    private Set<String> getAllDefinedVariables() {
        return ImmutableSet.<String>builder()
                .addAll(testConstants)
                .addAll(contextVariables)
                .build();
    }
}
