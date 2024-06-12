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

import org.apache.el.MethodExpressionImpl;
import org.apache.el.MethodExpressionLiteral;
import org.apache.el.ValueExpressionImpl;
import org.apache.el.lang.FunctionMapperFactory;
import org.apache.el.lang.VariableMapperFactory;
import org.apache.el.parser.AstDeferredExpression;
import org.apache.el.parser.AstDynamicExpression;
import org.apache.el.parser.AstFunction;
import org.apache.el.parser.AstIdentifier;
import org.apache.el.parser.AstLiteralExpression;
import org.apache.el.parser.AstValue;
import org.apache.el.parser.ELParser;
import org.apache.el.parser.Node;
import org.apache.el.parser.NodeVisitor;
import org.apache.el.parser.ParseException;
import org.apache.el.util.MessageFactory;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.FunctionMapper;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.el.VariableMapper;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Like ExpressionBuilder except removes nodes referring to missing variables
 */
class PartialExpressionBuilder implements NodeVisitor {

    private FunctionMapper fnMapper;

    private VariableMapper varMapper;

    private String expression;

    private final Set<String> variablesDefined;

    public PartialExpressionBuilder(String expression, ELContext ctx, Set<String> variablesDefined)
            throws ELException {
        this.expression = expression;


        FunctionMapper ctxFn = ctx.getFunctionMapper();
        VariableMapper ctxVar = ctx.getVariableMapper();

        if (ctxFn != null) {
            this.fnMapper = new FunctionMapperFactory(ctxFn);
        }
        if (ctxVar != null) {
            this.varMapper = new VariableMapperFactory(ctxVar);
        }
        this.variablesDefined = variablesDefined;
    }

    public static final Node createNode(String expr) throws ELException {
        Node n = createNodeInternal(expr);
        return n;
    }

    private static final Node createNodeInternal(String expr)
            throws ELException {
        if (expr == null) {
            throw new ELException(MessageFactory.get("error.null"));
        }

        // Removed caching as we translate nodes differently depending on
        // variables defined
        Node n;
        try {
            n = (new ELParser(new StringReader(expr)))
                    .CompositeExpression();

            // validate composite expression
            int numChildren = n.jjtGetNumChildren();
            if (numChildren == 1) {
                n = n.jjtGetChild(0);
            } else {
                Class<?> type = null;
                Node child = null;
                for (int i = 0; i < numChildren; i++) {
                    child = n.jjtGetChild(i);
                    if (child instanceof AstLiteralExpression)
                        continue;
                    if (type == null)
                        type = child.getClass();
                    else {
                        if (!type.equals(child.getClass())) {
                            throw new ELException(MessageFactory.get(
                                    "error.mixed", expr));
                        }
                    }
                }
            }

            if (n instanceof AstDeferredExpression
                    || n instanceof AstDynamicExpression) {
                n = n.jjtGetChild(0);
            }
        } catch (ParseException pe) {
            throw new ELException("Error Parsing: " + expr, pe);
        }
        return n;
    }

    private void prepare(Node node) throws ELException {
        try {
            node.accept(this);
        } catch (Exception e) {
            if (e instanceof ELException) {
                throw (ELException) e;
            } else {
                throw (new ELException(e));
            }
        }
        if (this.fnMapper instanceof FunctionMapperFactory) {
            this.fnMapper = ((FunctionMapperFactory) this.fnMapper).create();
        }
        if (this.varMapper instanceof VariableMapperFactory) {
            this.varMapper = ((VariableMapperFactory) this.varMapper).create();
        }
    }

    private Node build() throws ELException {
        Node n = createNodeInternal(this.expression);
        try {
            n = NodeHunter.destroyUnknowns(n, variablesDefined);
        } catch (final Exception e) {
            throw new ELException(e);
        }
        this.prepare(n);
        if (n instanceof AstDeferredExpression
                || n instanceof AstDynamicExpression) {
            n = n.jjtGetChild(0);
        }
        return n;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.el.parser.NodeVisitor#visit(com.sun.el.parser.Node)
     */
    @Override
    public void visit(Node node) throws ELException {
        if (node instanceof AstFunction) {

            AstFunction funcNode = (AstFunction) node;

            if (this.fnMapper == null) {
                throw new ELException(MessageFactory.get("error.fnMapper.null"));
            }
            Method m = fnMapper.resolveFunction(funcNode.getPrefix(), funcNode
                    .getLocalName());
            if (m == null) {
                throw new ELException(MessageFactory.get(
                        "error.fnMapper.method", funcNode.getOutputName()));
            }
            int pcnt = m.getParameterTypes().length;
            if (node.jjtGetNumChildren() != pcnt) {
                throw new ELException(MessageFactory.get(
                        "error.fnMapper.paramcount", funcNode.getOutputName(),
                        "" + pcnt, "" + node.jjtGetNumChildren()));
            }
        } else if (node instanceof AstIdentifier && this.varMapper != null) {
            String variable = ((AstIdentifier) node).getImage();

            // simply capture it
            this.varMapper.resolveVariable(variable);
        }
    }

    public ValueExpression createValueExpression(Class<?> expectedType)
            throws ELException {
        Node n = this.build();
        return new ValueExpressionImpl(this.expression, n, this.fnMapper,
                this.varMapper, expectedType);
    }

    public MethodExpression createMethodExpression(Class<?> expectedReturnType,
            Class<?>[] expectedParamTypes) throws ELException {
        Node n = this.build();
        if (!n.isParametersProvided() && expectedParamTypes == null) {
            throw new NullPointerException(MessageFactory
                    .get("error.method.nullParms"));
        }
        if (n instanceof AstValue || n instanceof AstIdentifier) {
            return new MethodExpressionImpl(expression, n, this.fnMapper,
                    this.varMapper, expectedReturnType, expectedParamTypes);
        } else if (n instanceof AstLiteralExpression) {
            return new MethodExpressionLiteral(expression, expectedReturnType,
                    expectedParamTypes);
        } else {
            throw new ELException("Not a Valid Method Expression: "
                    + expression);
        }
    }
}

