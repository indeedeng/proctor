package com.indeed.proctor.groups;

import junit.framework.Assert;
import org.junit.Test;

import static com.indeed.proctor.groups.UppercaseFirstCharacterGroups.Test.UPPERCASE_FIRST_CHARACTER_TEST;

/**
 *
 */
public class TestUppercaseFirstCharacterGroups {

    @Test
    public void testUppercaseFirstCharacter() {
        Assert.assertEquals("Uppercase_first_character_test", UPPERCASE_FIRST_CHARACTER_TEST.getName());
    }
}
