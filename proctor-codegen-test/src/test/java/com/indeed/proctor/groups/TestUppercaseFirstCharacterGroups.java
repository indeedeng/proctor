package com.indeed.proctor.groups;

import org.junit.Assert;
import org.junit.Test;

import static java.com.indeed.proctor.codegen.test.groups.UppercaseFirstCharacterGroups.Test.UPPERCASE_FIRST_CHARACTER_TEST;

/**
 * Test to ensure that Proctor tests that start with an uppercase character retain an unnormalized name in the
 * generated groups class.
 */
public class TestUppercaseFirstCharacterGroups {

    @Test
    public void testUppercaseFirstCharacter() {
        Assert.assertEquals("Uppercase_first_character_test", UPPERCASE_FIRST_CHARACTER_TEST.getName());
    }
}
