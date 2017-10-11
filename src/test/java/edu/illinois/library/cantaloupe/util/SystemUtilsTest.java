package edu.illinois.library.cantaloupe.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class SystemUtilsTest {

    @Test
    public void testGetJavaVersion() {
        int expected;
        final String versionStr = System.getProperty("java.version");
        if (versionStr.contains(".")) {
            int pos = versionStr.indexOf('.');
            expected = Integer.parseInt(versionStr.substring(0, pos));
        } else {
            expected = Integer.parseInt(versionStr);
        }

        assertEquals(expected, SystemUtils.getJavaVersion());
    }

}