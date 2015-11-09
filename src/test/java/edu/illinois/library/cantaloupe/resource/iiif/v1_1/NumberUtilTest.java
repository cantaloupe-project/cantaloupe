package edu.illinois.library.cantaloupe.resource.iiif.v1_1;

import junit.framework.TestCase;

public class NumberUtilTest extends TestCase {

    public void testformatForUrl() {
        assertEquals("0", NumberUtil.formatForUrl(0.0f));
        assertEquals("0.5", NumberUtil.formatForUrl(0.5f));
        assertEquals("50", NumberUtil.formatForUrl(50.0f));
        assertEquals("50.5", NumberUtil.formatForUrl(50.5f));
        assertEquals("50.5", NumberUtil.formatForUrl(50.50f));
        assertTrue(NumberUtil.formatForUrl(50.5555555555555f).length() <= 13);
    }

}
