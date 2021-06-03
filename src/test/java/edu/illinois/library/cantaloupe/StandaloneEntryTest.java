package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Client;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.DeletingFileVisitor;
import edu.illinois.library.cantaloupe.util.SocketUtils;
import edu.illinois.library.cantaloupe.util.SystemUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

public class StandaloneEntryTest extends BaseTest {

    private static final PrintStream CONSOLE_OUTPUT = System.out;
    private static final PrintStream CONSOLE_ERROR  = System.err;
    private static final int HTTP_PORT              = SocketUtils.getOpenPort();
    private static final String NEWLINE             = System.getProperty("line.separator");

    private final Client httpClient = new Client();

    private Path cacheDir;
    private final ByteArrayOutputStream redirectedOutput =
            new ByteArrayOutputStream();
    private final ByteArrayOutputStream redirectedError =
            new ByteArrayOutputStream();

    private void deleteCacheDir() throws IOException {
        Files.walkFileTree(getCacheDir(), new DeletingFileVisitor());
    }

    private Path getCacheDir() throws IOException {
        if (cacheDir == null) {
            cacheDir = Files.createTempDirectory("test");
        }
        return cacheDir;
    }

    /**
     * Redirects stdout/stderr output to byte arrays for analysis.
     */
    private void redirectOutput() {
        System.setOut(new PrintStream(redirectedOutput));
        System.setErr(new PrintStream(redirectedError));
    }

    private void resetOutput() {
        System.setOut(CONSOLE_OUTPUT);
        System.setErr(CONSOLE_ERROR);
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();

        SystemUtils.clearExitRequest();

        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT,
                TestUtil.getFixture("config.properties").toString());

        ConfigurationFactory.clearInstance();

        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTP_ENABLED, true);
        config.setProperty(Key.HTTP_PORT, HTTP_PORT);
        config.setProperty(Key.HTTPS_ENABLED, false);
        config.setProperty(Key.SOURCE_STATIC, "FilesystemSource");
        config.setProperty(Key.PROCESSOR_FALLBACK, "Java2dProcessor");

        httpClient.setURI(new URI("http://localhost:" + HTTP_PORT + "/"));
    }

    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        StandaloneEntry.getAppServer().stop();
        httpClient.stop();
        deleteCacheDir();
        System.clearProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT);
        System.clearProperty(StandaloneEntry.LIST_FONTS_ARGUMENT);
        resetOutput();
    }

    // list fonts

    @Test
    void mainWithListFontsArgument() throws Exception {
        redirectOutput();
        StandaloneEntry.main(StandaloneEntry.LIST_FONTS_ARGUMENT);
        assertTrue(redirectedOutput.toString().contains("SansSerif"));
    }

    @Test
    void mainWithListFontsArgumentExits() throws Exception {
        StandaloneEntry.main(StandaloneEntry.LIST_FONTS_ARGUMENT);
        assertTrue(SystemUtils.exitRequested());
        assertEquals(0, SystemUtils.requestedExitCode());
    }

    // missing config

    @Test
    void mainWithMissingConfigOptionPrintsUsage() throws Exception {
        redirectOutput();
        System.clearProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT);
        StandaloneEntry.main("");
        assertEquals(StandaloneEntry.usage().trim(),
                redirectedOutput.toString().trim());
    }

    @Test
    void mainWithMissingConfigOptionExits() throws Exception {
        System.clearProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT);

        StandaloneEntry.main("");
        assertTrue(SystemUtils.exitRequested());
        assertEquals(-1, SystemUtils.requestedExitCode());
    }

    // empty config VM option

    @Test
    void mainWithEmptyConfigOptionPrintsUsage() throws Exception {
        // TODO: why does this test fail in Windows with a NullPointerException?
        assumeFalse(org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS);

        redirectOutput();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "");
        StandaloneEntry.main("");

        String message = redirectedOutput.toString();
        assertTrue(message.contains("Usage:"));
    }

    @Test
    void mainWithEmptyConfigOptionExits() throws Exception {
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "");

        StandaloneEntry.main("");
        assertTrue(SystemUtils.exitRequested());
        assertEquals(-1, SystemUtils.requestedExitCode());
    }

    // missing config file

    @Test
    void mainWithInvalidConfigFileArgumentPrintsUsage() throws Exception {
        redirectOutput();
        String path = Paths.get("bla").toAbsolutePath().toString();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, path);

        StandaloneEntry.main("");
        assertEquals("Does not exist: " + path + NEWLINE + NEWLINE +
                        StandaloneEntry.usage() + NEWLINE,
                redirectedOutput.toString());
    }

    @Test
    void mainWithInvalidConfigFileArgumentExits() throws Exception {
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "/bla/bla/bla");

        StandaloneEntry.main("");
        assertTrue(SystemUtils.exitRequested());
        assertEquals(-1, SystemUtils.requestedExitCode());
    }

    // config file is a directory

    @Test
    void mainWithDirectoryConfigFileArgumentPrintsUsage() throws Exception {
        redirectOutput();
        String path = TestUtil.getFixture("bla").getParent().toString();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, path);
        StandaloneEntry.main("");

        String expected = "Not a file: " + path + NEWLINE + NEWLINE +
                StandaloneEntry.usage() + NEWLINE;
        String actual = redirectedOutput.toString();
        assertEquals(expected, actual);
    }

    @Test
    void mainWithDirectoryConfigFileArgumentExits() throws Exception {
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT,
                TestUtil.getFixture("bla").getParent().toString());

        StandaloneEntry.main("");
        assertTrue(SystemUtils.exitRequested());
        assertEquals(-1, SystemUtils.requestedExitCode());
    }

    // valid config file

    @Test
    void mainWithValidConfigFileArgumentStartsServer() throws Exception {
        StandaloneEntry.main("");
        Response response = httpClient.send();
        assertEquals(200, response.getStatus());
    }

    @Disabled // TODO: this sometimes passes and sometimes fails
    @Test
    void mainWithFailingToBindToPortExits() throws Exception {
        final Configuration config = Configuration.getInstance();
        int port = SocketUtils.getUsedPort();
        config.setProperty(Key.HTTP_PORT, port);

        StandaloneEntry.main("");
        assertEquals(-1, SystemUtils.requestedExitCode());
    }

}
