package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.Info;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.FileUtils;
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
import org.restlet.resource.ResourceException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class StandaloneEntryTest extends BaseTest {

    private static final PrintStream CONSOLE_OUTPUT = System.out;
    private static final PrintStream CONSOLE_ERROR = System.err;
    private static final int HTTP_PORT = TestUtil.getOpenPort();

    private Client httpClient;

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
        FileUtils.deleteDirectory(getCacheDir());
    }

    private File getCacheDir() throws IOException {
        Path directory = Files.createTempDirectory("test");
        return directory.toFile();
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
                TestUtil.getFixture("config.properties").getAbsolutePath());
        System.setProperty(StandaloneEntry.TEST_VM_OPTION, "true");

        ConfigurationFactory.clearInstance();
        final Configuration config = ConfigurationFactory.getInstance();

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
        StandaloneEntry.getWebServer().stop();
        httpClient.stop();
        deleteCacheDir();
        System.clearProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT);
        System.clearProperty(ApplicationInitializer.CLEAN_CACHE_VM_ARGUMENT);
        System.clearProperty(ApplicationInitializer.PURGE_CACHE_VM_ARGUMENT);
        System.clearProperty(ApplicationInitializer.PURGE_EXPIRED_FROM_CACHE_VM_ARGUMENT);
        System.clearProperty(StandaloneEntry.LIST_FONTS_VM_OPTION);
        resetOutput();
    }

    // list fonts

    @Test
    public void mainWithListFontsOption() throws Exception {
        redirectOutput();
        System.setProperty(StandaloneEntry.LIST_FONTS_VM_OPTION, "");
        StandaloneEntry.main(new String[] {});
        assertTrue(redirectedOutput.toString().contains("SansSerif"));
    }

    // missing config

    @Test
    public void mainWithMissingConfigOptionPrintsUsage() throws Exception {
        redirectOutput();
        System.clearProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT);
        StandaloneEntry.main(new String[] {});
        assertEquals(StandaloneEntry.usage().trim(),
                redirectedOutput.toString().trim());
    }

    @Test
    public void mainWithMissingConfigOptionExits() throws Exception {
        System.clearProperty(StandaloneEntry.TEST_VM_OPTION);
        exit.expectSystemExitWithStatus(-1);
        System.clearProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT);
        StandaloneEntry.main(new String[] {});
    }

    // empty config VM option

    @Test
    public void mainWithEmptyConfigOptionPrintsUsage() throws Exception {
        redirectOutput();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "");
        StandaloneEntry.main(new String[] {});
    }

    @Test
    public void mainWithEmptyConfigOptionExits() throws Exception {
        System.clearProperty(StandaloneEntry.TEST_VM_OPTION);
        exit.expectSystemExitWithStatus(-1);
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "");
        StandaloneEntry.main(new String[] {});
    }

    // missing config file

    @Test
    public void mainWithInvalidConfigFileArgumentPrintsUsage() throws Exception {
        redirectOutput();
        String path = "/bla/bla/bla";
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, path);
        StandaloneEntry.main(new String[] {});
        assertEquals("Does not exist: " + path + "\n\n" + StandaloneEntry.usage().trim(),
                redirectedOutput.toString().trim());
    }

    @Test
    public void mainWithInvalidConfigFileArgumentExits() throws Exception {
        System.clearProperty(StandaloneEntry.TEST_VM_OPTION);
        exit.expectSystemExitWithStatus(-1);
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "/bla/bla/bla");
        StandaloneEntry.main(new String[] {});
    }

    // config file is a directory

    @Test
    public void mainWithDirectoryConfigFileArgumentPrintsUsage() throws Exception {
        redirectOutput();
        String path = TestUtil.getFixture("bla").getParentFile().getAbsolutePath();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, path);
        StandaloneEntry.main(new String[] {});
        assertEquals("Not a file: " + path + "\n\n" + StandaloneEntry.usage().trim(),
                redirectedOutput.toString().trim());
    }

    @Test
    public void mainWithDirectoryConfigFileArgumentExits() throws Exception {
        System.clearProperty(StandaloneEntry.TEST_VM_OPTION);
        exit.expectSystemExitWithStatus(-1);
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT,
                TestUtil.getFixture("bla").getParentFile().getAbsolutePath());
        StandaloneEntry.main(new String[] {});
    }

    // valid config file

    @Test
    public void mainWithValidConfigFileArgumentStartsServer() throws Exception {
        StandaloneEntry.main(new String[] {});
        ClientResource resource = getClientResource();
        resource.get();
        assertEquals(Status.SUCCESS_OK, resource.getResponse().getStatus());
    }

    /**
     * Tests startup with the -Dcantaloupe.cache.clean VM option.
     */
    @Test
    public void mainWithCleanCacheArg() throws Exception {
        exit.expectSystemExitWithStatus(0);

        File cacheDir = getCacheDir();
        File imageDir = new File(cacheDir.getAbsolutePath() + File.separator +
                "image");
        File infoDir = new File(cacheDir.getAbsolutePath() + File.separator +
                "info");
        imageDir.mkdirs();
        infoDir.mkdirs();

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
        config.setProperty(Key.DERIVATIVE_CACHE, "FilesystemCache");
        config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                getCacheDir().getAbsolutePath());
        config.setProperty(Key.CACHE_SERVER_TTL, "1");

        // TODO: write this

        System.setProperty(ApplicationInitializer.CLEAN_CACHE_VM_ARGUMENT, "");
        StandaloneEntry.main(new String[] {});

        // Cause the Servlet to be loaded
        try {
            getClientResource().get();
        } catch (ResourceException e) {
            // This is expected, as the server has called System.exit() before
            // it could generate a response.
        }

        Thread.sleep(5000);
    }

    /**
     * Tests startup with the -Dcantaloupe.cache.purge VM option.
     */
    @Test
    public void mainWithPurgeCacheArg() throws Exception {
        exit.expectSystemExitWithStatus(0);
        redirectOutput();

        final File cacheDir = getCacheDir();
        final File imageDir = new File(cacheDir.getAbsolutePath() + "/image");
        final File infoDir = new File(cacheDir.getAbsolutePath() + "/info");

        // set up the cache
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
        config.setProperty(Key.DERIVATIVE_CACHE, "FilesystemCache");
        config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                cacheDir.getAbsolutePath());
        config.setProperty(Key.CACHE_SERVER_TTL, "10");

        // cache a dimension
        DerivativeCache cache = CacheFactory.getDerivativeCache();
        cache.put(new Identifier("cats"), new Info(500, 500));

        // cache an image
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("dogs"));
        ops.add(new Rotate(15));
        try (OutputStream wbc = cache.newDerivativeImageOutputStream(ops)) {
            Files.copy(TestUtil.getImage("jpg-rgb-64x56x8-baseline.jpg").toPath(), wbc);
        }

        // assert that they've been cached
        assertEquals(1, FileUtils.listFiles(imageDir, null, true).size());
        assertEquals(1, FileUtils.listFiles(infoDir, null, true).size());

        // purge the cache
        System.setProperty(ApplicationInitializer.PURGE_CACHE_VM_ARGUMENT, "");
        StandaloneEntry.main(new String[] {});

        // Cause the Servlet to be loaded
        try {
            getClientResource().get();
        } catch (ResourceException e) {
            // This is expected, as the server has called System.exit() before
            // it could generate a response.
        }

        Thread.sleep(5000);
        assertTrue(redirectedOutput.toString().contains("Purging the derivative cache"));

        // assert that they've been purged
        assertEquals(0, TestUtil.countFiles(imageDir));
        assertEquals(0, TestUtil.countFiles(infoDir));
    }

    /**
     * Tests startup with the -Dcantaloupe.cache.purge=identifier VM option.
     */
    @Test
    public void mainWithPurgeIdentifierArg() throws Exception {
        exit.expectSystemExitWithStatus(0);

        final File cacheDir = getCacheDir();
        final File imageDir = new File(cacheDir.getAbsolutePath() + "/image");
        final File infoDir = new File(cacheDir.getAbsolutePath() + "/info");

        // set up the cache
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
        config.setProperty(Key.DERIVATIVE_CACHE, "FilesystemCache");
        config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                cacheDir.getAbsolutePath());
        config.setProperty(Key.CACHE_SERVER_TTL, "10");

        // cache a couple of dimensions
        DerivativeCache cache = CacheFactory.getDerivativeCache();
        cache.put(new Identifier("cats"), new Info(500, 500));
        cache.put(new Identifier("dogs"), new Info(500, 500));

        // cache a couple of images
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("cats"));
        ops.add(new Rotate(15));
        try (OutputStream wbc = cache.newDerivativeImageOutputStream(ops)) {
            Files.copy(TestUtil.getImage("jpg").toPath(), wbc);
        }

        ops.setIdentifier(new Identifier("dogs"));
        try (OutputStream wbc = cache.newDerivativeImageOutputStream(ops)) {
            Files.copy(TestUtil.getImage("jpg").toPath(), wbc);
        }

        // assert that they've been cached
        assertEquals(2, FileUtils.listFiles(imageDir, null, true).size());
        assertEquals(2, FileUtils.listFiles(infoDir, null, true).size());

        // purge one identifier
        System.setProperty(ApplicationInitializer.PURGE_CACHE_VM_ARGUMENT, "dogs");
        StandaloneEntry.main(new String[] {});

        // Cause the Servlet to be loaded
        try {
            getClientResource().get();
        } catch (ResourceException e) {
            // This is expected, as the server has called System.exit() before
            // it could generate a response.
        }

        Thread.sleep(2000);

        // assert that they've been purged
        assertEquals(1, TestUtil.countFiles(imageDir));
        assertEquals(1, TestUtil.countFiles(infoDir));
    }

    /**
     * Tests startup with the -Dcantaloupe.cache.purge_expired VM option.
     */
    @Test
    public void mainWithPurgeExpiredCacheArg() throws Exception {
        exit.expectSystemExitWithStatus(0);

        File cacheDir = getCacheDir();
        File imageDir = new File(cacheDir.getAbsolutePath() + "/image");
        File infoDir = new File(cacheDir.getAbsolutePath() + "/info");
        imageDir.mkdirs();
        infoDir.mkdirs();

        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
        config.setProperty(Key.DERIVATIVE_CACHE, "FilesystemCache");
        config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                cacheDir.getAbsolutePath());
        config.setProperty(Key.CACHE_SERVER_TTL, "1");

        File.createTempFile("bla1", "tmp", imageDir);
        File.createTempFile("bla1", "tmp", infoDir);
        Thread.sleep(2500);
        File.createTempFile("bla2", "tmp", imageDir);
        File.createTempFile("bla2", "tmp", infoDir);

        System.setProperty(ApplicationInitializer.PURGE_EXPIRED_FROM_CACHE_VM_ARGUMENT, "");
        StandaloneEntry.main(new String[] {});

        // Cause the Servlet to be loaded
        try {
            getClientResource().get();
        } catch (ResourceException e) {
            // This is expected, as the server has called System.exit() before
            // it could generate a response.
        }

        Thread.sleep(5000);

        assertEquals(1, TestUtil.countFiles(imageDir));
        assertEquals(1, TestUtil.countFiles(infoDir));
    }

}
