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

package com.indeed.proctor.common.el;

import org.apache.el.ValueExpressionImpl;
import org.apache.el.lang.FunctionMapperFactory;
import org.apache.el.lang.VariableMapperFactory;
import org.apache.el.parser.AstDeferredExpression;
import org.apache.el.parser.AstDynamicExpression;
import org.apache.el.parser.AstFunction;
import org.apache.el.parser.AstIdentifier;
import org.apache.el.parser.AstLiteralExpression;
import org.apache.el.parser.ELParser;
import org.apache.el.parser.Node;
import org.apache.el.parser.NodeVisitor;
import org.apache.el.parser.ParseException;
import org.apache.el.util.MessageFactory;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.FunctionMapper;
import javax.el.ValueExpression;
import javax.el.VariableMapper;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.Set;

/** Like ExpressionBuilder except removes nodes referring to missing variables */
public final class PartialExpressionBuilder implements NodeVisitor {

    private FunctionMapper fnMapper;

    private VariableMapper varMapper;

    private ELContext ctx;

    private String expression;

    private final Set<String> variablesDefined;
    /** */
    public PartialExpressionBuilder(String expression, ELContext ctx, Set<String> variablesDefined)
            throws ELException {
        this.expression = expression;

        FunctionMapper ctxFn = ctx.getFunctionMapper();
        VariableMapper ctxVar = ctx.getVariableMapper();
        this.ctx = ctx;

        if (ctxFn != null) {
            this.fnMapper = new FunctionMapperFactory(ctxFn);
        }
        if (ctxVar != null) {
            this.varMapper = new VariableMapperFactory(ctxVar);
        }
        this.variablesDefined = variablesDefined;
    }

    private static Node createNodeInternal(String expr) throws ELException {
        if (expr == null) {
            throw new ELException(MessageFactory.get("error.null"));
        }

        // Removed caching as we translate nodes differently depending on variables defined
        Node node;
        try {
            node = (new ELParser(new StringReader(expr))).CompositeExpression();

            // validate composite expression
            int numChildren = node.jjtGetNumChildren();
            if (numChildren == 1) {
                node = node.jjtGetChild(0);
            } else {
                Class<?> type = null;
                Node child = null;
                for (int i = 0; i < numChildren; i++) {
                    child = node.jjtGetChild(i);
                    if (child instanceof AstLiteralExpression) continue;
                    if (type == null) type = child.getClass();
                    else {
                        if (!type.equals(child.getClass())) {
                            throw new ELException(MessageFactory.get("error.mixed", expr));
                        }
                    }
                }
            }

            if (node instanceof AstDeferredExpression || node instanceof AstDynamicExpression) {
                node = node.jjtGetChild(0);
            }
        } catch (ParseException pe) {
            throw new ELException("Error Parsing: " + expr, pe);
        }
        return node;
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
        Node node = createNodeInternal(this.expression);
        try {
            node = NodeHunter.destroyUnknowns(node, variablesDefined);
        } catch (final Exception e) {
            throw new ELException(e);
        }
        this.prepare(node);
        if (node instanceof AstDeferredExpression || node instanceof AstDynamicExpression) {
            node = node.jjtGetChild(0);
        }
        return node;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.sun.el.parser.NodeVisitor#visit(com.sun.el.parser.Node)
     */
    @Override
    public void visit(Node node) throws ELException {
        if (node instanceof AstFunction) {

            final AstFunction funcNode = (AstFunction) node;

            if (this.fnMapper == null) {
                throw new ELException(MessageFactory.get("error.fnMapper.null"));
            }
            Method function =
                    fnMapper.resolveFunction(funcNode.getPrefix(), funcNode.getLocalName());
            if (function == null) {
                throw new ELException(
                        MessageFactory.get("error.fnMapper.method", funcNode.getOutputName()));
            }
            int pcnt = function.getParameterTypes().length;
            if (node.jjtGetNumChildren() != pcnt) {
                throw new ELException(
                        MessageFactory.get(
                                "error.fnMapper.paramcount",
                                funcNode.getOutputName(),
                                "" + pcnt,
                                "" + node.jjtGetNumChildren()));
            }
        } else if (node instanceof AstIdentifier && this.varMapper != null) {
            String variable = node.getImage();

            this.varMapper.resolveVariable(variable);
        }
    }

    public ValueExpression createValueExpression(Class<?> expectedType) throws ELException {
        final Node node = this.build();
        return new ValueExpressionImpl(
                this.expression, node, this.fnMapper, this.varMapper, expectedType);
    }
}
