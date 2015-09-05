package edu.illinois.library.cantaloupe.util;

import junit.framework.TestCase;

public class NumberUtilTest extends TestCase {

    public void testRemoveTrailingZeroes() {
        assertEquals("0", NumberUtil.removeTrailingZeroes(new Float(0.0)));
        assertEquals("50", NumberUtil.removeTrailingZeroes(new Float(50.0)));
        assertEquals("50.5", NumberUtil.removeTrailingZeroes(new Float(50.5)));
        assertEquals("50.5", NumberUtil.removeTrailingZeroes(new Float(50.50)));
    }

}
