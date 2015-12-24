package edu.illinois.library.cantaloupe.image;

import org.junit.Test;

import static org.junit.Assert.*;

public class NumberUtilTest {

    @Test
    public void testRemoveTrailingZeroes() {
        assertEquals("0", NumberUtil.removeTrailingZeroes(0.0f));
        assertEquals("0.5", NumberUtil.removeTrailingZeroes(0.5f));
        assertEquals("50", NumberUtil.removeTrailingZeroes(50.0f));
        assertEquals("50.5", NumberUtil.removeTrailingZeroes(50.5f));
        assertEquals("50.5", NumberUtil.removeTrailingZeroes(50.50f));
        assertTrue(NumberUtil.removeTrailingZeroes(50.5555555555555f).length() <= 13);
    }

}
