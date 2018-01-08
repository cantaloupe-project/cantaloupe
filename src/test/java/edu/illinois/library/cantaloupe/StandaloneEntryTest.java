package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.DeletingFileVisitor;
import edu.illinois.library.cantaloupe.util.SocketUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class StandaloneEntryTest extends BaseTest {

    private static final PrintStream CONSOLE_OUTPUT = System.out;
    private static final PrintStream CONSOLE_ERROR = System.err;
    private static final int HTTP_PORT = SocketUtils.getOpenPort();

    private Client httpClient;

    private Path cacheDir;
    private final ByteArrayOutputStream redirectedOutput =
            new ByteArrayOutputStream();
    private final ByteArrayOutputStream redirectedError =
            new ByteArrayOutputStream();

    // http://stackoverflow.com/questions/6141252/dealing-with-system-exit0-in-junit-tests
    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    private ClientResource getClientResource() {
        final Reference url = new Reference("http://localhost:" + HTTP_PORT + "/");
        final ClientResource resource = new ClientResource(url);
        resource.setNext(httpClient);
        return resource;
    }

    private void deleteCacheDir() throws IOException {
        Files.walkFileTree(getCacheDir(), new DeletingFileVisitor());
    }

    private Path getCacheDir() throws IOException {
        if (cacheDir == null) {
            try {
                cacheDir = Files.createTempDirectory("test");
            } catch (NoSuchFileException e) { // TODO: why does this happen?
                cacheDir = Paths.get(System.getProperty("java.io.tmpdir"), "test");
                Files.createDirectories(cacheDir);
            }
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

    @Before
    public void setUp() throws Exception {
        super.setUp();

        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT,
                TestUtil.getFixture("config.properties").toString());
        System.setProperty(StandaloneEntry.TEST_VM_ARGUMENT, "true");

        ConfigurationFactory.clearInstance();

        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.HTTP_ENABLED, true);
        config.setProperty(Key.HTTP_PORT, HTTP_PORT);
        config.setProperty(Key.HTTPS_ENABLED, false);
        config.setProperty(Key.RESOLVER_STATIC, "FilesystemResolver");
        config.setProperty(Key.PROCESSOR_FALLBACK, "Java2dProcessor");

        httpClient = new Client(new Context(), Protocol.HTTP);
        httpClient.start();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        StandaloneEntry.getAppServer().stop();
        httpClient.stop();
        deleteCacheDir();
        System.clearProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT);
        System.clearProperty(StandaloneEntry.LIST_FONTS_VM_ARGUMENT);
        resetOutput();
    }

    // list fonts

    @Test
    public void mainWithListFontsOption() throws Exception {
        redirectOutput();
        System.setProperty(StandaloneEntry.LIST_FONTS_VM_ARGUMENT, "");
        StandaloneEntry.main("");
        assertTrue(redirectedOutput.toString().contains("SansSerif"));
    }

    @Test
    public void mainWithListFontsOptionExits() throws Exception {
        exit.expectSystemExitWithStatus(0);
        System.clearProperty(StandaloneEntry.TEST_VM_ARGUMENT);

        System.setProperty(StandaloneEntry.LIST_FONTS_VM_ARGUMENT, "");
        StandaloneEntry.main("");
    }

    // missing config

    @Test
    public void mainWithMissingConfigOptionPrintsUsage() throws Exception {
        redirectOutput();
        System.clearProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT);
        StandaloneEntry.main("");
        assertEquals(StandaloneEntry.usage().trim(),
                redirectedOutput.toString().trim());
    }

    @Test
    public void mainWithMissingConfigOptionExits() throws Exception {
        System.clearProperty(StandaloneEntry.TEST_VM_ARGUMENT);
        exit.expectSystemExitWithStatus(-1);
        System.clearProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT);
        StandaloneEntry.main("");
    }

    // empty config VM option

    @Test
    public void mainWithEmptyConfigOptionPrintsUsage() throws Exception {
        redirectOutput();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "");
        StandaloneEntry.main("");
    }

    @Test
    public void mainWithEmptyConfigOptionExits() throws Exception {
        System.clearProperty(StandaloneEntry.TEST_VM_ARGUMENT);
        exit.expectSystemExitWithStatus(-1);
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "");
        StandaloneEntry.main("");
    }

    // missing config file

    @Test
    public void mainWithInvalidConfigFileArgumentPrintsUsage() throws Exception {
        redirectOutput();
        String path = "/bla/bla/bla";
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, path);
        StandaloneEntry.main("");
        assertEquals("Does not exist: " + path + "\n\n" + StandaloneEntry.usage().trim(),
                redirectedOutput.toString().trim());
    }

    @Test
    public void mainWithInvalidConfigFileArgumentExits() throws Exception {
        System.clearProperty(StandaloneEntry.TEST_VM_ARGUMENT);
        exit.expectSystemExitWithStatus(-1);
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "/bla/bla/bla");
        StandaloneEntry.main("");
    }

    // config file is a directory

    @Test
    public void mainWithDirectoryConfigFileArgumentPrintsUsage() throws Exception {
        redirectOutput();
        String path = TestUtil.getFixture("bla").getParent().toString();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, path);
        StandaloneEntry.main("");
        assertEquals("Not a file: " + path + "\n\n" + StandaloneEntry.usage().trim(),
                redirectedOutput.toString().trim());
    }

    @Test
    public void mainWithDirectoryConfigFileArgumentExits() throws Exception {
        System.clearProperty(StandaloneEntry.TEST_VM_ARGUMENT);
        exit.expectSystemExitWithStatus(-1);
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT,
                TestUtil.getFixture("bla").getParent().toString());
        StandaloneEntry.main("");
    }

    // valid config file

    @Test
    public void mainWithValidConfigFileArgumentStartsServer() throws Exception {
        StandaloneEntry.main("");
        ClientResource resource = getClientResource();
        resource.get();
        assertEquals(Status.SUCCESS_OK, resource.getResponse().getStatus());
    }

}
