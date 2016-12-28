package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.operation.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.processor.ImageInfo;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.test.BaseTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class StandaloneEntryTest extends BaseTest {

    private static final int HTTP_PORT = TestUtil.getOpenPort();

    private Client httpClient;

    private final ByteArrayOutputStream systemOutput = new ByteArrayOutputStream();
    private final ByteArrayOutputStream systemError = new ByteArrayOutputStream();

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
        File directory = new File(".");
        String cwd = directory.getCanonicalPath();
        Path path = Paths.get(cwd, "src", "test", "resources", "cache");
        return path.toFile();
    }

    /**
     * Redirects stdout/stderr output to byte arrays for analysis.
     */
    private void redirectOutput() {
        System.setOut(new PrintStream(systemOutput));
        System.setErr(new PrintStream(systemError));
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();

        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT,
                TestUtil.getFixture("config.properties").getAbsolutePath());
        System.setProperty(StandaloneEntry.TEST_VM_OPTION, "true");

        ConfigurationFactory.clearInstance();
        final Configuration config = ConfigurationFactory.getInstance();

        config.setProperty(WebServer.HTTP_ENABLED_CONFIG_KEY, true);
        config.setProperty(WebServer.HTTP_PORT_CONFIG_KEY, HTTP_PORT);
        config.setProperty(WebServer.HTTPS_ENABLED_CONFIG_KEY, false);
        config.setProperty(ResolverFactory.STATIC_RESOLVER_CONFIG_KEY,
                "FilesystemResolver");
        config.setProperty(ProcessorFactory.FALLBACK_PROCESSOR_CONFIG_KEY,
                "Java2dProcessor");

        httpClient = new Client(new Context(), Protocol.HTTP);
        httpClient.start();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        httpClient.stop();
        deleteCacheDir();
        System.clearProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT);
        System.clearProperty(EntryServlet.CLEAN_CACHE_VM_ARGUMENT);
        System.clearProperty(EntryServlet.PURGE_CACHE_VM_ARGUMENT);
        System.clearProperty(EntryServlet.PURGE_EXPIRED_FROM_CACHE_VM_ARGUMENT);
        System.clearProperty(StandaloneEntry.LIST_FONTS_VM_OPTION);
    }

    // list fonts

    @Test
    public void testMainWithListFontsOption() throws Exception {
        redirectOutput();
        System.setProperty(StandaloneEntry.LIST_FONTS_VM_OPTION, "");
        StandaloneEntry.main(new String[] {});
        assertTrue(systemOutput.toString().contains("Times"));
    }

    // missing config

    @Test
    public void testMainWithMissingConfigOptionPrintsUsage() throws Exception {
        redirectOutput();
        System.clearProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT);
        StandaloneEntry.main(new String[] {});
        assertEquals(StandaloneEntry.usage(), systemOutput.toString().trim());
    }

    @Test
    public void testMainWithMissingConfigOptionExits() throws Exception {
        System.clearProperty(StandaloneEntry.TEST_VM_OPTION);
        exit.expectSystemExitWithStatus(-1);
        System.clearProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT);
        StandaloneEntry.main(new String[] {});
    }

    // empty config VM option

    @Test
    public void testMainWithEmptyConfigOptionPrintsUsage() throws Exception {
        redirectOutput();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "");
        StandaloneEntry.main(new String[] {});
    }

    @Test
    public void testMainWithEmptyConfigOptionExits() throws Exception {
        System.clearProperty(StandaloneEntry.TEST_VM_OPTION);
        exit.expectSystemExitWithStatus(-1);
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "");
        StandaloneEntry.main(new String[] {});
    }

    // missing config file

    @Test
    public void testMainWithInvalidConfigFileArgumentPrintsUsage() throws Exception {
        redirectOutput();
        String path = "/bla/bla/bla";
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, path);
        StandaloneEntry.main(new String[] {});
        assertEquals("Does not exist: " + path + "\n\n" + StandaloneEntry.usage(),
                systemOutput.toString().trim());
    }

    @Test
    public void testMainWithInvalidConfigFileArgumentExits() throws Exception {
        System.clearProperty(StandaloneEntry.TEST_VM_OPTION);
        exit.expectSystemExitWithStatus(-1);
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "/bla/bla/bla");
        StandaloneEntry.main(new String[] {});
    }

    // config file is a directory

    @Test
    public void testMainWithDirectoryConfigFileArgumentPrintsUsage() throws Exception {
        redirectOutput();
        String path = TestUtil.getFixture("bla").getParentFile().getAbsolutePath();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, path);
        StandaloneEntry.main(new String[] {});
        assertEquals("Not a file: " + path + "\n\n" + StandaloneEntry.usage(),
                systemOutput.toString().trim());
    }

    @Test
    public void testMainWithDirectoryConfigFileArgumentExits() throws Exception {
        System.clearProperty(StandaloneEntry.TEST_VM_OPTION);
        exit.expectSystemExitWithStatus(-1);
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT,
                TestUtil.getFixture("bla").getParentFile().getAbsolutePath());
        StandaloneEntry.main(new String[] {});
    }

    // valid config file

    @Test
    public void testMainWithValidConfigFileArgumentStartsServer() throws Exception {
        StandaloneEntry.main(new String[] {});
        ClientResource resource = getClientResource();
        resource.get();
        assertEquals(Status.SUCCESS_OK, resource.getResponse().getStatus());
    }

    /**
     * Tests startup with the -Dcantaloupe.cache.clean VM option.
     */
    @Test
    public void testMainWithCleanCacheArg() throws Exception {
        exit.expectSystemExitWithStatus(0);

        File cacheDir = getCacheDir();
        File imageDir = new File(cacheDir.getAbsolutePath() + File.separator +
                "image");
        File infoDir = new File(cacheDir.getAbsolutePath() + File.separator +
                "info");
        imageDir.mkdirs();
        infoDir.mkdirs();

        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(CacheFactory.DERIVATIVE_CACHE_CONFIG_KEY,
                "FilesystemCache");
        config.setProperty("FilesystemCache.pathname",
                getCacheDir().getAbsolutePath());
        config.setProperty(Cache.TTL_CONFIG_KEY, "1");

        // TODO: write this

        System.setProperty(EntryServlet.CLEAN_CACHE_VM_ARGUMENT, "");
        StandaloneEntry.main(new String[] {});

        // Cause the Servlet to be loaded
        try {
            getClientResource().get();
        } catch (ResourceException e) {
            // This is expected, as the server has called System.exit() before
            // it could generate a response.
        }
    }

    /**
     * Tests startup with the -Dcantaloupe.cache.purge VM option.
     */
    @Test
    public void testMainWithPurgeCacheArg() throws Exception {
        exit.expectSystemExitWithStatus(0);

        final File cacheDir = getCacheDir();
        final File imageDir = new File(cacheDir.getAbsolutePath() + "/image");
        final File infoDir = new File(cacheDir.getAbsolutePath() + "/info");

        // set up the cache
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(CacheFactory.DERIVATIVE_CACHE_CONFIG_KEY,
                "FilesystemCache");
        config.setProperty("FilesystemCache.pathname",
                getCacheDir().getAbsolutePath());
        config.setProperty(Cache.TTL_CONFIG_KEY, "10");

        // cache a dimension
        DerivativeCache cache = CacheFactory.getDerivativeCache();
        cache.putImageInfo(new Identifier("cats"), new ImageInfo(500, 500));

        // cache an image
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("dogs"));
        ops.add(new Rotate(15));
        try (OutputStream wbc = cache.getImageOutputStream(ops);
             InputStream rbc = new FileInputStream(
                     TestUtil.getImage("jpg-rgb-64x56x8-baseline.jpg"))) {
            IOUtils.copy(rbc, wbc);
        }

        // assert that they've been cached
        assertEquals(1, FileUtils.listFiles(imageDir, null, true).size());
        assertEquals(1, FileUtils.listFiles(infoDir, null, true).size());

        // purge the cache
        System.setProperty(EntryServlet.PURGE_CACHE_VM_ARGUMENT, "");
        StandaloneEntry.main(new String[] {});

        // Cause the Servlet to be loaded
        try {
            getClientResource().get();
        } catch (ResourceException e) {
            // This is expected, as the server has called System.exit() before
            // it could generate a response.
        }

        // assert that they've been purged
        assertEquals(0, TestUtil.countFiles(imageDir));
        assertEquals(0, TestUtil.countFiles(infoDir));
    }

    /**
     * Tests startup with the -Dcantaloupe.cache.purge=identifier VM option.
     */
    @Test
    public void testMainWithPurgeIdentifierArg() throws Exception {
        exit.expectSystemExitWithStatus(0);

        final File cacheDir = getCacheDir();
        final File imageDir = new File(cacheDir.getAbsolutePath() + "/image");
        final File infoDir = new File(cacheDir.getAbsolutePath() + "/info");

        // set up the cache
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(CacheFactory.DERIVATIVE_CACHE_CONFIG_KEY,
                "FilesystemCache");
        config.setProperty("FilesystemCache.pathname",
                getCacheDir().getAbsolutePath());
        config.setProperty(Cache.TTL_CONFIG_KEY, "10");

        // cache a couple of dimensions
        DerivativeCache cache = CacheFactory.getDerivativeCache();
        cache.putImageInfo(new Identifier("cats"), new ImageInfo(500, 500));
        cache.putImageInfo(new Identifier("dogs"), new ImageInfo(500, 500));

        // cache a couple of images
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("cats"));
        ops.add(new Rotate(15));
        try (OutputStream wbc = cache.getImageOutputStream(ops);
             InputStream rbc = new FileInputStream(TestUtil.getImage("jpg"))) {
            IOUtils.copy(rbc, wbc);
        }

        ops.setIdentifier(new Identifier("dogs"));
        try (OutputStream wbc = cache.getImageOutputStream(ops);
             InputStream rbc = new FileInputStream(TestUtil.getImage("jpg"))) {
            IOUtils.copy(rbc, wbc);
        }

        // assert that they've been cached
        assertEquals(2, FileUtils.listFiles(imageDir, null, true).size());
        assertEquals(2, FileUtils.listFiles(infoDir, null, true).size());

        // purge one identifier
        System.setProperty(EntryServlet.PURGE_CACHE_VM_ARGUMENT, "dogs");
        StandaloneEntry.main(new String[] {});

        // Cause the Servlet to be loaded
        try {
            getClientResource().get();
        } catch (ResourceException e) {
            // This is expected, as the server has called System.exit() before
            // it could generate a response.
        }

        // assert that they've been purged
        assertEquals(1, TestUtil.countFiles(imageDir));
        assertEquals(1, TestUtil.countFiles(infoDir));
    }

    /**
     * Tests startup with the -Dcantaloupe.cache.purge_expired VM option.
     */
    @Test
    public void testMainWithPurgeExpiredCacheArg() throws Exception {
        exit.expectSystemExitWithStatus(0);

        File cacheDir = getCacheDir();
        File imageDir = new File(cacheDir.getAbsolutePath() + File.separator +
                "image");
        File infoDir = new File(cacheDir.getAbsolutePath() + File.separator +
                "info");
        imageDir.mkdirs();
        infoDir.mkdirs();

        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(CacheFactory.DERIVATIVE_CACHE_CONFIG_KEY,
                "FilesystemCache");
        config.setProperty("FilesystemCache.pathname",
                getCacheDir().getAbsolutePath());
        config.setProperty(Cache.TTL_CONFIG_KEY, "1");

        File.createTempFile("bla1", "tmp", imageDir);
        File.createTempFile("bla1", "tmp", infoDir);
        Thread.sleep(2500);
        File.createTempFile("bla2", "tmp", imageDir);
        File.createTempFile("bla2", "tmp", infoDir);

        System.setProperty(EntryServlet.PURGE_EXPIRED_FROM_CACHE_VM_ARGUMENT, "");
        StandaloneEntry.main(new String[] {});

        // Cause the Servlet to be loaded
        try {
            getClientResource().get();
        } catch (ResourceException e) {
            // This is expected, as the server has called System.exit() before
            // it could generate a response.
        }

        assertEquals(1, TestUtil.countFiles(imageDir));
        assertEquals(1, TestUtil.countFiles(infoDir));
    }

}
