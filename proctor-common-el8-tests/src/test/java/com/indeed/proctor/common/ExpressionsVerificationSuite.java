package com.indeed.proctor.common;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Runs same tests as in commons, but with different jasper-el version in classpath (see pom.xml).
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        TestProctorUtils.class,
        TestRuleEvaluator.class,
        TestRuleVerifyUtils.class
})
public class ExpressionsVerificationSuite {
}
