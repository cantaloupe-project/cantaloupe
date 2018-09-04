package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class ApplicationTest extends BaseTest {

    @Test
    public void testGetTempPathSetInConfiguration() throws IOException {
        Path expectedDir = Files.createTempDirectory("test");
        Configuration.getInstance().setProperty(Key.TEMP_PATHNAME, expectedDir);

        Path actualDir = Application.getTempPath();
        assertEquals(expectedDir, actualDir);
    }

    @Test
    public void testGetTempPathFallsBackToDefault() {
        Path expectedDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Configuration.getInstance().clearProperty(Key.TEMP_PATHNAME);

        Path actualDir = Application.getTempPath();
        assertEquals(expectedDir, actualDir);
    }

    @Test
    public void testGetTempPathCreatesDirectory() {
        Path expectedDir = Paths.get(System.getProperty("java.io.tmpdir"),
                "cats", "cats", "cats");
        Configuration.getInstance().setProperty(Key.TEMP_PATHNAME, expectedDir);

        Application.getTempPath();
        assertTrue(Files.exists(expectedDir));
    }

    /**
     * {@link Application#getVersion()} is not fully testable as it returns a
     * different value when the app is running from a .war.
     */
    @Test
    public void testGetVersion() {
        assertEquals("Unknown", Application.getVersion());
    }

    @Test
    public void testIsUsingSystemTempPath() {
        System.setProperty("java.io.tmpdir", "/default");
        final Configuration config = Configuration.getInstance();

        config.clearProperty(Key.TEMP_PATHNAME);
        assertTrue(Application.isUsingSystemTempPath());

        config.setProperty(Key.TEMP_PATHNAME, "/default");
        assertTrue(Application.isUsingSystemTempPath());

        config.setProperty(Key.TEMP_PATHNAME, System.getProperty("user.dir"));
        assertFalse(Application.isUsingSystemTempPath());
    }

}
