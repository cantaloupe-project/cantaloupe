package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.processor.ImageInfo;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class StandaloneEntryTest {

    private static final int HTTP_PORT = TestUtil.getOpenPort();

    private static Client httpClient = new Client(new Context(), Protocol.HTTP);

    // http://stackoverflow.com/questions/6141252/dealing-with-system-exit0-in-junit-tests
    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    private static void resetConfiguration() throws IOException {
        Configuration config = ConfigurationFactory.getInstance();
        config.clear();
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT,
                TestUtil.getFixture("config.properties").getAbsolutePath());
        try {
            config.setProperty(WebServer.HTTP_ENABLED_CONFIG_KEY, true);
            config.setProperty(WebServer.HTTP_PORT_CONFIG_KEY, HTTP_PORT);
            config.setProperty(WebServer.HTTPS_ENABLED_CONFIG_KEY, false);
            config.setProperty(ResolverFactory.STATIC_RESOLVER_CONFIG_KEY,
                    "FilesystemResolver");
            config.setProperty(ProcessorFactory.FALLBACK_PROCESSOR_CONFIG_KEY,
                    "Java2dProcessor");
        } catch (Exception e) {
            fail("Failed to get the configuration");
        }
    }

    @Before
    public void setUp() throws IOException {
        resetConfiguration();
    }

    @After
    public void tearDown() throws IOException {
        deleteCacheDir();
        System.clearProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT);
        System.clearProperty(EntryServlet.CLEAN_CACHE_VM_ARGUMENT);
        System.clearProperty(EntryServlet.PURGE_CACHE_VM_ARGUMENT);
        System.clearProperty(EntryServlet.PURGE_EXPIRED_FROM_CACHE_VM_ARGUMENT);
    }

    @Test
    public void testMainWithInvalidConfigFileArgumentExits() throws Exception {
        exit.expectSystemExitWithStatus(-1);
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT, "/bla/bla/bla");
        StandaloneEntry.main(new String[] {});
    }

    @Test
    public void testMainWithDirectoryConfigFileArgumentExits() throws Exception {
        exit.expectSystemExitWithStatus(-1);
        System.setProperty(ConfigurationFactory.CONFIG_VM_ARGUMENT,
                TestUtil.getFixture("bla").getParentFile().getAbsolutePath());
        StandaloneEntry.main(new String[] {});
    }

    @Test
    public void testMainWithValidConfigFileArgumentStartsServer() throws Exception {
        StandaloneEntry.main(new String[] {});
        ClientResource resource = getHttpClientForUriPath("/");
        resource.get();
        assertEquals(Status.SUCCESS_OK, resource.getResponse().getStatus());
    }

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
        config.setProperty("FilesystemCache.ttl_seconds", "1");

        // TODO: write this

        System.setProperty(EntryServlet.CLEAN_CACHE_VM_ARGUMENT, "");
        StandaloneEntry.main(new String[] {});

        // Cause the Servlet to be loaded
        try {
            getHttpClientForUriPath("/").get();
        } catch (ResourceException e) {
            // This is expected, as the server has called System.exit() before
            // it could generate a response.
        }
    }

    @Test
    public void testMainWithPurgeCacheArg() throws Exception {
        exit.expectSystemExitWithStatus(0);

        final File cacheDir = getCacheDir();
        final File imageDir = new File(cacheDir.getAbsolutePath() + "/image");
        final File infoDir = new File(cacheDir.getAbsolutePath() + "/info");

        // set up the cache
        resetConfiguration();
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(CacheFactory.DERIVATIVE_CACHE_CONFIG_KEY,
                "FilesystemCache");
        config.setProperty("FilesystemCache.pathname",
                getCacheDir().getAbsolutePath());
        config.setProperty("FilesystemCache.ttl_seconds", "10");

        // cache a dimension
        DerivativeCache cache = CacheFactory.getDerivativeCache();
        cache.putImageInfo(new Identifier("cats"), new ImageInfo(500, 500));

        // cache an image
        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier("dogs"));
        ops.add(new Rotate(15));
        OutputStream wbc = cache.getImageOutputStream(ops);
        InputStream rbc = new FileInputStream(
                TestUtil.getImage("jpg-rgb-64x56x8-baseline.jpg"));
        IOUtils.copy(rbc, wbc);

        // assert that they've been cached
        assertEquals(1, FileUtils.listFiles(imageDir, null, true).size());
        assertEquals(1, FileUtils.listFiles(infoDir, null, true).size());

        // purge the cache
        System.setProperty(EntryServlet.PURGE_CACHE_VM_ARGUMENT, "");
        StandaloneEntry.main(new String[] {});

        // Cause the Servlet to be loaded
        try {
            getHttpClientForUriPath("/").get();
        } catch (ResourceException e) {
            // This is expected, as the server has called System.exit() before
            // it could generate a response.
        }

        // assert that they've been purged
        assertEquals(0, imageDir.listFiles().length);
        assertEquals(0, infoDir.listFiles().length);
    }

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
        config.setProperty("FilesystemCache.ttl_seconds", "1");

        File.createTempFile("bla1", "tmp", imageDir);
        File.createTempFile("bla1", "tmp", infoDir);
        Thread.sleep(2500);
        File.createTempFile("bla2", "tmp", imageDir);
        File.createTempFile("bla2", "tmp", infoDir);

        System.setProperty(EntryServlet.PURGE_EXPIRED_FROM_CACHE_VM_ARGUMENT, "");
        StandaloneEntry.main(new String[] {});

        // Cause the Servlet to be loaded
        try {
            getHttpClientForUriPath("/").get();
        } catch (ResourceException e) {
            // This is expected, as the server has called System.exit() before
            // it could generate a response.
        }

        assertEquals(1, imageDir.listFiles().length);
        assertEquals(1, infoDir.listFiles().length);
    }

    private ClientResource getHttpClientForUriPath(String path) {
        final Reference url = new Reference("http://localhost:" + HTTP_PORT + path);
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

}
