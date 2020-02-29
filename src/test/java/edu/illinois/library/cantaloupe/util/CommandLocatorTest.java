package edu.illinois.library.cantaloupe.util;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class CommandLocatorTest {

    @Test
    void locateWithUnixPath() {
        assertEquals("/opt/bin" + File.separator + "bin1",
                CommandLocator.locate("bin1", "/opt/bin"));
    }

    @Test
    void locateWithUnixPathAndTrailingSlash() {
        assertEquals("/opt/bin" + File.separator + "bin1",
                CommandLocator.locate("bin1", "/opt/bin" + File.separator));
    }

    @Test
    void locateWithWindowsPath() {
        assertEquals("c:\\Program Files\\dir" + File.separator + "bin1",
                CommandLocator.locate("bin1", "c:\\Program Files\\dir"));
    }

    @Test
    void locateWithWindowsPathAndTrailingSlash() {
        assertEquals("c:\\Program Files\\dir" + File.separator + "bin1",
                CommandLocator.locate("bin1", "c:\\Program Files\\dir" + File.separator));
    }

}