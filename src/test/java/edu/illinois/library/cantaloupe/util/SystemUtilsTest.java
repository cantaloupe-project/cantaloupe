package edu.illinois.library.cantaloupe.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class SystemUtilsTest {

    @Test
    public void testGetJavaMajorVersion() {
        int expected = 0;
        final String versionStr = System.getProperty("java.version");

        if (versionStr.startsWith("1")) { // < 9
            String[] parts = versionStr.split(Pattern.quote("."));
            if (parts.length > 1) {
                expected = Integer.parseInt(parts[1]);
            }
        }
        if (expected == 0) {
            expected = Integer.parseInt(versionStr.substring(0, versionStr.indexOf(".")));
        }

        int actual = SystemUtils.getJavaMajorVersion();
        assertEquals(expected, actual);
                                               // planning ahead...
        assertTrue(new HashSet<>(Arrays.asList(8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20)).contains(actual));
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