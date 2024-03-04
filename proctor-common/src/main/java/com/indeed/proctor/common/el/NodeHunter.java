package com.indeed.proctor.common.el;

import com.google.common.collect.ImmutableList;
import com.indeed.proctor.common.ProctorRuleFunctions.MaybeBool;
import org.apache.el.parser.AstAnd;
import org.apache.el.parser.AstFunction;
import org.apache.el.parser.AstIdentifier;
import org.apache.el.parser.AstLiteralExpression;
import org.apache.el.parser.AstNot;
import org.apache.el.parser.AstNotEqual;
import org.apache.el.parser.AstOr;
import org.apache.el.parser.ELParserTreeConstants;
import org.apache.el.parser.Node;
import org.apache.el.parser.NodeVisitor;
import org.apache.el.parser.SimpleNode;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

public class NodeHunter implements NodeVisitor {
    private static final List<String> NODE_TYPES =
            ImmutableList.copyOf(ELParserTreeConstants.jjtNodeName).stream()
                    .map(nodeName -> "Ast" + nodeName)
                    .collect(Collectors.toList());
    private static final Map<String, Integer> NODE_TYPE_IDS =
            NODE_TYPES.stream()
                    .collect(Collectors.toMap(nodeType -> nodeType, NODE_TYPES::indexOf));
    private static final int AST_FUNCTION_TYPE = 27;
    private static final int AST_NOT_EQUAL_TYPE = 9;

    private final Set<Node> initialUnknowns = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Node, Node> replacements = new IdentityHashMap<>();
    private final Set<String> variablesDefined;

    NodeHunter(final Set<String> variablesDefined) {
        this.variablesDefined = variablesDefined;
    }

    public static Node destroyUnknowns(final Node node, final Set<String> variablesDefined)
            throws Exception {
        final NodeHunter nodeHunter = new NodeHunter(variablesDefined);
        node.accept(nodeHunter);
        if (nodeHunter.initialUnknowns.isEmpty()) {
            // Nothing to do here
            return node;
        }
        nodeHunter.calculateReplacements();
        final Node result = nodeHunter.replaceNodes(node);
        // At this point result is a maybebool, we need to convert it to a bool
        final Node resultIsNotFalse = nodeHunter.wrapIsNotFalse(result);
        return resultIsNotFalse;
    }

    private void calculateReplacements() {
        final Stack<Node> nodesToDestroy = new Stack<>();
        initialUnknowns.forEach(nodesToDestroy::push);
        while (!nodesToDestroy.isEmpty()) {
            final Node nodeToDestroy = nodesToDestroy.pop();
            if (nodeToDestroy instanceof AstAnd) {
                // Replace simple "and" with maybeAnd
                replaceWithFunction(nodeToDestroy, "maybeAnd");
            } else if (nodeToDestroy instanceof AstOr) {
                // Replace simple "or" with maybeOr
                replaceWithFunction(nodeToDestroy, "maybeOr");
            } else if (nodeToDestroy instanceof AstNot) {
                // Replace simple "not" with maybeNot
                replaceWithFunction(nodeToDestroy, "maybeNot");
                // } else if (nodeToDestroy instanceof AstEqual || nodeToDestroy instanceof
                // AstNotEqual) {
                // TODO: if someone compares two bools using == that would be
                // weird, but we could handle it by making sure any cases that mix
                // maybeBool and bool are promoted to maybeBool like we do with the
                // other logical operators
            } else if (!replacements.containsKey(nodeToDestroy)) {
                // Anything else propagate the unknown value
                //
                // TODO: If a bool is used as an argument to a function we
                // could try and do the function if the maybeBool is true or
                // false, and only propagate the unknown if any argument is
                // unknown, but that seems rare and very complicated so I
                // haven't handled that case here.
                final AstLiteralExpression replacement = new AstLiteralExpression(1);
                replacement.setImage(MaybeBool.UNKNOWN.name());
                replacements.put(nodeToDestroy, replacement);
            }
            if (nodeToDestroy.jjtGetParent() != null) {
                nodesToDestroy.push(nodeToDestroy.jjtGetParent());
            }
        }
    }

    private AstFunction createFunctionReplacement(final Node node, final String function) {
        final AstFunction replacement = new AstFunction(AST_FUNCTION_TYPE);
        replacement.setPrefix("proctor");
        replacement.setLocalName(function);
        replacement.setImage("proctor:" + function);
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            final Node child = node.jjtGetChild(i);
            if (replacements.containsKey(child)) {
                replacement.jjtAddChild(replacements.get(child), i);
            } else {
                final AstFunction replacementChild = new AstFunction(AST_FUNCTION_TYPE);
                replacementChild.setPrefix("proctor");
                replacementChild.setLocalName("toMaybeBool");
                replacementChild.setImage("proctor:toMaybeBool");
                replacementChild.jjtAddChild(child, 0);
                replacement.jjtAddChild(replacementChild, i);
            }
        }

        return replacement;
    }

    private void replaceWithFunction(final Node node, final String function) {
        final AstFunction replacement = createFunctionReplacement(node, function);
        replacements.put(node, replacement);
    }

    private Node replaceNodes(final Node node)
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException,
                    InstantiationException {
        if (replacements.containsKey(node)) {
            Node newNode = node;
            while (replacements.containsKey(newNode)) {
                newNode = replacements.get(newNode);
            }
            return newNode;
        }
        final Class<?> nodeClass = node.getClass();
        final Constructor<?> constructor = nodeClass.getConstructor(int.class);
        final SimpleNode newNode =
                (SimpleNode) constructor.newInstance(NODE_TYPE_IDS.get(nodeClass.getSimpleName()));
        for (int i = 0; i < node.jjtGetNumChildren(); i++) {
            final Node newChild = replaceNodes(node.jjtGetChild(i));
            newChild.jjtSetParent(newNode);
            newNode.jjtAddChild(newChild, i);
        }
        newNode.jjtSetParent(node.jjtGetParent());
        newNode.setImage(node.getImage());
        if (newNode instanceof AstFunction) {
            ((AstFunction) newNode).setPrefix(((AstFunction) node).getPrefix());
            ((AstFunction) newNode).setLocalName(((AstFunction) node).getLocalName());
        }
        return newNode;
    }

    @Override
    public void visit(final Node node) throws Exception {
        if (node instanceof AstIdentifier) {
            String variable = node.getImage();
            if (!variablesDefined.contains(variable)) {
                initialUnknowns.add(node);
            }
        }
    }

    private Node wrapIsNotFalse(final Node node) {
        final Node resultIsNotFalse = new AstNotEqual(AST_NOT_EQUAL_TYPE);
        final AstLiteralExpression literalFalse = new AstLiteralExpression(1);
        literalFalse.setImage(MaybeBool.FALSE.name());
        literalFalse.jjtSetParent(resultIsNotFalse);
        resultIsNotFalse.jjtSetParent(node.jjtGetParent());
        node.jjtSetParent(resultIsNotFalse);
        resultIsNotFalse.jjtAddChild(node, 0);
        resultIsNotFalse.jjtAddChild(literalFalse, 1);
        return resultIsNotFalse;
    }
}
