package edu.illinois.library.cantaloupe.util;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class CommandLocatorTest {

    @Test
    void locate() {
        // unix path, no trailing slash
        assertEquals("/opt/bin" + File.separator + "bin1",
                CommandLocator.locate("bin1", "/opt/bin"));

        // unix path, trailing slash
        assertEquals("/opt/bin" + File.separator + "bin1",
                CommandLocator.locate("bin1", "/opt/bin" + File.separator));

        // windows path, no trailing slash
        assertEquals("c:\\Program Files\\dir" + File.separator + "bin1",
                CommandLocator.locate("bin1", "c:\\Program Files\\dir"));

        // windows path, trailing slash
        assertEquals("c:\\Program Files\\dir" + File.separator + "bin1",
                CommandLocator.locate("bin1", "c:\\Program Files\\dir" + File.separator));
    }

}