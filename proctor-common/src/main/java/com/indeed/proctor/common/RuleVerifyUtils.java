package com.indeed.proctor.common;

import com.google.common.collect.Lists;
import org.apache.el.lang.ExpressionBuilder;
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

/**
 *
 */
public class RuleVerifyUtils {

    private static final Logger LOGGER = Logger.getLogger(RuleVerifyUtils.class);

    private RuleVerifyUtils() {
    }

    public static void verifyRule(final String testRule,
                                  final boolean shouldEvaluate,
                                  final ExpressionFactory expressionFactory,
                                  final ELContext elContext,
                                  final Set<String> absentIdentifiers) throws IncompatibleTestMatrixException {
        final String bareRule = removeElExpressionBraces(testRule);
        if (!isEmptyWhitespace(bareRule)) {
            final ValueExpression valueExpression = expressionFactory.createValueExpression(elContext, testRule, Boolean.class);
            if (shouldEvaluate) {
                /**
                 * must have a context to test against, even if it's "Collections.emptyMap()", how to
                 * tell if this method is used for ProctorBuilder or during load of the testMatrix.
                 * also used to check to make sure any classes included in the EL can be found.
                 * Class
                 */

                try {
                    valueExpression.getValue(elContext);
                } catch (final ELException e) {
                    final Node node = ExpressionBuilder.createNode(testRule);
                    if (isIgnorable(node, absentIdentifiers)) {
                        LOGGER.debug(String.format("Rule %s contains uninstantiated identifier(s) in %s, ignore the failure", testRule, absentIdentifiers), e);
                    } else {
                        throw e;
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
