package com.indeed.proctor.common;

import com.google.common.collect.Lists;
import org.apache.el.lang.ExpressionBuilder;
import org.apache.el.parser.AstIdentifier;
import org.apache.el.parser.Node;
import org.apache.log4j.Logger;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import java.util.List;
import java.util.Set;

import static com.indeed.proctor.common.ProctorUtils.isEmptyWhitespace;
import static com.indeed.proctor.common.ProctorUtils.removeElExpressionBraces;

public class RuleVerifyUtils {

    private static final Logger LOGGER = Logger.getLogger(RuleVerifyUtils.class);

    private RuleVerifyUtils() {
    }

    public static void verifyRule(
            final String testRule,
            final boolean shouldEvaluate,
            final ExpressionFactory expressionFactory,
            final ELContext elContext,
            final Set<String> absentIdentifiers
    ) throws InvalidRuleException {
        final String bareRule = removeElExpressionBraces(testRule);
        if (!isEmptyWhitespace(bareRule)) {
            final ValueExpression valueExpression;
            try {
                valueExpression = expressionFactory.createValueExpression(elContext, testRule, Boolean.class);
            } catch (final ELException e) {
                throw new InvalidRuleException(e, String.format("Rule %s has invalid syntax.", testRule));
            }

            if (shouldEvaluate) {
                /**
                 * must have a context to test against, even if it's "Collections.emptyMap()", how to
                 * tell if this method is used for ProctorBuilder or during load of the testMatrix.
                 * also used to check to make sure any classes included in the EL can be found.
                 * Class
                 */

                // Parse test rule as expression language and create AST
                final Node root;
                try {
                    root = ExpressionBuilder.createNode(testRule);
                } catch (final ELException e) {
                    throw new InvalidRuleException(e, String.format("Rule %s has invalid syntax.", testRule));
                }

                // Check identifiers in the AST and verify variable names
                final Node undefinedIdentifier = checkUndefinedIdentifier(root, elContext, absentIdentifiers);
                if (undefinedIdentifier != null) {
                    throw new InvalidRuleException(String.format("Rule %s contains undefined identifier : %s", testRule, undefinedIdentifier.getImage()));
                }

                // Evaluate rule with given context
                try {
                    valueExpression.getValue(elContext);
                } catch (final ELException e) {
                    if (isIgnorable(root, absentIdentifiers)) {
                        LOGGER.debug(String.format("Rule %s contains uninstantiated identifier(s) in %s, ignore the failure", testRule, absentIdentifiers), e);
                    } else {
                        throw new InvalidRuleException(e, String.format("Failed to evaluate a rule %s", testRule));
                    }
                }
            }
        }
    }

    private static boolean isIgnorable(final Node node, final Set<String> absentIdentifiers) {
        final List<Node> leaves = getLeafNodes(node);
        for (final Node n : leaves) {
            final String image = n.getImage();
            if (absentIdentifiers.contains(image)) {
                /** we can ignore this test failure since the identifier context is not provided **/
                return true;
            }
        }
        return false;
    }

    private static Node checkUndefinedIdentifier(final Node node, final ELContext elContext, final Set<String> absentIdentifiers) {
        if (node instanceof AstIdentifier) {
            final String name = node.getImage();
            final boolean hasVariable = elContext.getVariableMapper().resolveVariable(name) != null;
            if (!hasVariable && !absentIdentifiers.contains(name)) {
                return node;
            }
        } else {
            for (int i = 0; i < node.jjtGetNumChildren(); i++) {
                final Node result = checkUndefinedIdentifier(node.jjtGetChild(i), elContext, absentIdentifiers);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static List<Node> getLeafNodes(final Node node) {
        final List<Node> nodes = Lists.newArrayList();
        find(node, nodes);
        return nodes;
    }

    private static void find(final Node node, final List<Node> res) {
        if (node.jjtGetNumChildren() > 0) {
            for (int i = 0; i < node.jjtGetNumChildren(); i++) {
                find(node.jjtGetChild(i), res);
            }
        } else {
            res.add(node);
        }
    }
}
