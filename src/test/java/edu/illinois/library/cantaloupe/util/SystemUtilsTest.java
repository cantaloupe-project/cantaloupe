package edu.illinois.library.cantaloupe.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class SystemUtilsTest {

    @Test
    public void testGetJavaVersion() {
        final String versionStr = System.getProperty("java.version");
        int pos = versionStr.indexOf('.');
        pos = versionStr.indexOf('.', pos + 1);
        double expected = Double.parseDouble(versionStr.substring (0, pos));

        assertEquals(expected, SystemUtils.getJavaVersion(), 0.000001f);
    }

}