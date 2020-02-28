package edu.illinois.library.cantaloupe.util;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class CommandLocatorTest {

    @Test
    public void locateWithUnixPath() {
        assertEquals("/opt/bin" + File.separator + "bin1",
                CommandLocator.locate("bin1", "/opt/bin"));
    }

    @Test
    public void locateWithUnixPathAndTrailingSlash() {
        assertEquals("/opt/bin" + File.separator + "bin1",
                CommandLocator.locate("bin1", "/opt/bin" + File.separator));
    }

    @Test
    public void locateWithWindowsPath() {
        assertEquals("c:\\Program Files\\dir" + File.separator + "bin1",
                CommandLocator.locate("bin1", "c:\\Program Files\\dir"));
    }

    @Test
    public void locateWithWindowsPathAndTrailingSlash() {
        assertEquals("c:\\Program Files\\dir" + File.separator + "bin1",
                CommandLocator.locate("bin1", "c:\\Program Files\\dir" + File.separator));
    }

}