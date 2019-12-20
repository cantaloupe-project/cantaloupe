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

import static org.junit.jupiter.api.Assertions.*;

public class StandaloneEntryTest extends BaseTest {

    private static final PrintStream CONSOLE_OUTPUT = System.out;
    private static final PrintStream CONSOLE_ERROR = System.err;
    private static final int HTTP_PORT = SocketUtils.getOpenPort();

    private Client httpClient = new Client();

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
    void mainWithListFontsOption() throws Exception {
        redirectOutput();
        StandaloneEntry.main(StandaloneEntry.LIST_FONTS_ARGUMENT);
        assertTrue(redirectedOutput.toString().contains("SansSerif"));
    }

    @Test
    @Disabled // broken in junit 5
    void mainWithListFontsOptionExits() throws Exception {
        //exit.expectSystemExitWithStatus(0);
        System.clearProperty(Application.TEST_VM_ARGUMENT);

        System.setProperty(StandaloneEntry.LIST_FONTS_ARGUMENT, "");
        StandaloneEntry.main("");
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
    @Disabled // broken in junit 5
    void mainWithMissingConfigOptionExits() throws Exception {
        System.clearProperty(Application.TEST_VM_ARGUMENT);
        //exit.expectSystemExitWithStatus(-1);
        System.clearProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT);
        StandaloneEntry.main("");
    }

    // empty config VM option

    @Test
    void mainWithEmptyConfigOptionPrintsUsage() throws Exception {
        redirectOutput();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "");
        StandaloneEntry.main("");
    }

    @Test
    @Disabled // broken in junit 5
    void mainWithEmptyConfigOptionExits() throws Exception {
        System.clearProperty(Application.TEST_VM_ARGUMENT);
        //exit.expectSystemExitWithStatus(-1);
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "");
        StandaloneEntry.main("");
    }

    // missing config file

    @Test
    void mainWithInvalidConfigFileArgumentPrintsUsage() throws Exception {
        redirectOutput();
        String path = "/bla/bla/bla";
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, path);
        StandaloneEntry.main("");
        assertEquals("Does not exist: " + path + "\n\n" + StandaloneEntry.usage().trim(),
                redirectedOutput.toString().trim());
    }

    @Test
    @Disabled // broken in junit 5
    void mainWithInvalidConfigFileArgumentExits() throws Exception {
        System.clearProperty(Application.TEST_VM_ARGUMENT);
        //exit.expectSystemExitWithStatus(-1);
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "/bla/bla/bla");
        StandaloneEntry.main("");
    }

    // config file is a directory

    @Test
    void mainWithDirectoryConfigFileArgumentPrintsUsage() throws Exception {
        redirectOutput();
        String path = TestUtil.getFixture("bla").getParent().toString();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, path);
        StandaloneEntry.main("");
        assertEquals("Not a file: " + path + "\n\n" + StandaloneEntry.usage().trim(),
                redirectedOutput.toString().trim());
    }

    @Test
    @Disabled // broken in junit 5
    void mainWithDirectoryConfigFileArgumentExits() throws Exception {
        System.clearProperty(Application.TEST_VM_ARGUMENT);
        //exit.expectSystemExitWithStatus(-1);
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT,
                TestUtil.getFixture("bla").getParent().toString());
        StandaloneEntry.main("");
    }

    // valid config file

    @Test
    void mainWithValidConfigFileArgumentStartsServer() throws Exception {
        StandaloneEntry.main("");
        Response response = httpClient.send();
        assertEquals(200, response.getStatus());
    }

}
