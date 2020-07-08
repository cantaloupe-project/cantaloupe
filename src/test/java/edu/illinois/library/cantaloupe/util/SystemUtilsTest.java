package edu.illinois.library.cantaloupe.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class SystemUtilsTest {

    @Test
    public void testGetJavaMajorVersion() {
        // This will be tested indirectly in testParseJavaMajorVersion().
        assertTrue(SystemUtils.getJavaMajorVersion() >= 8);
    }

    @Test
    public void testParseJavaMajorVersionWithLegalVersion() {
        assertEquals(8, SystemUtils.parseJavaMajorVersion("1.8.0_63"));
        assertEquals(9, SystemUtils.parseJavaMajorVersion("9"));
        assertEquals(9, SystemUtils.parseJavaMajorVersion("9.0.1"));
        assertEquals(9, SystemUtils.parseJavaMajorVersion("9.1.0"));
        assertEquals(10, SystemUtils.parseJavaMajorVersion("10"));
        assertEquals(10, SystemUtils.parseJavaMajorVersion("10.0.1"));
        assertEquals(11, SystemUtils.parseJavaMajorVersion("11-ea"));
        assertEquals(11, SystemUtils.parseJavaMajorVersion("11.0.1_34-ea"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseJavaMajorVersionWithIllegalVersion() {
        SystemUtils.parseJavaMajorVersion("cats.0.2");
    }

    @Test
    public void testIsALPNAvailable() {
        boolean result = SystemUtils.isALPNAvailable();
        if (SystemUtils.getJavaMajorVersion() >= 9) {
            assertTrue(result);
        } else {
            assertFalse(result);
        }
    }

}