package edu.illinois.library.cantaloupe.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class StringUtilTest {

    @Test
    public void testRemoveTrailingZeroes() {
        assertEquals("0", StringUtil.removeTrailingZeroes(0.0f));
        assertEquals("0.5", StringUtil.removeTrailingZeroes(0.5f));
        assertEquals("50", StringUtil.removeTrailingZeroes(50.0f));
        assertEquals("50.5", StringUtil.removeTrailingZeroes(50.5f));
        assertEquals("50.5", StringUtil.removeTrailingZeroes(50.50f));
        assertTrue(StringUtil.removeTrailingZeroes(50.5555555555555f).length() <= 13);
    }

}
