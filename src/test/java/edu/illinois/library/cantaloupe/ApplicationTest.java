package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class ApplicationTest extends BaseTest {

    @Test
    void testGetTempPathSetInConfiguration() throws IOException {
        Path expectedDir = Files.createTempDirectory("test");
        Configuration.getInstance().setProperty(Key.TEMP_PATHNAME, expectedDir);

        Path actualDir = Application.getTempPath();
        assertEquals(expectedDir, actualDir);
    }

    @Test
    void testGetTempPathFallsBackToDefault() {
        Path expectedDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Configuration.getInstance().clearProperty(Key.TEMP_PATHNAME);

        Path actualDir = Application.getTempPath();
        assertEquals(expectedDir, actualDir);
    }

    @Test
    void testGetTempPathCreatesDirectory() {
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
    void testGetVersion() {
        assertEquals("Unknown", Application.getVersion());
    }

    @Test
    void testIsUsingSystemTempPath() {
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
