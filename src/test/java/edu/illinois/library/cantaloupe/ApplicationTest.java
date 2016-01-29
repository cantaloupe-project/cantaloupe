package edu.illinois.library.cantaloupe;

import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.Rotate;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.*;

public class ApplicationTest {

    private static final int HTTP_PORT = TestUtil.getOpenPort();
    private static final int HTTPS_PORT = TestUtil.getOpenPort();

    private static Client httpClient = new Client(new Context(), Protocol.HTTP);
    private static Client httpsClient = new Client(new Context(), Protocol.HTTPS);

    private static BaseConfiguration newConfiguration() {
        BaseConfiguration config = new BaseConfiguration();
        try {
            config.setProperty("print_stack_trace_on_error_pages", false);
            config.setProperty("http.enabled", true);
            config.setProperty("http.port", HTTP_PORT);
            config.setProperty("https.enabled", false);
            config.setProperty("https.port", HTTPS_PORT);
            config.setProperty("https.key_store_type", "JKS");
            config.setProperty("https.key_store_password","password");
            config.setProperty("https.key_store_path",
                    TestUtil.getFixture("keystore.jks").getAbsolutePath());
            config.setProperty("https.key_password", "password");
            config.setProperty("resolver.static", "FilesystemResolver");
            config.setProperty("processor.fallback", "Java2dProcessor");
        } catch (Exception e) {
            fail("Failed to get the configuration");
        }
        return config;
    }

    @Before
    public void setUp() {
        Application.setConfiguration(newConfiguration());
        System.getProperties().remove(Application.PURGE_CACHE_VM_ARGUMENT);
        System.getProperties().remove(Application.PURGE_EXPIRED_FROM_CACHE_VM_ARGUMENT);

    }

    @After
    public void tearDown() throws IOException {
        deleteCacheDir();
    }

    @Test
    public void testGetConfiguration() {
        try {
            File directory = new File(".");
            String cwd = directory.getCanonicalPath();
            Path testPath = Paths.get(cwd, "src", "test", "java", "edu",
                    "illinois", "library", "cantaloupe", "test");

            String goodProps = testPath + File.separator + "cantaloupe.properties";
            System.setProperty(Application.CONFIG_FILE_VM_ARGUMENT, goodProps);
            assertNotNull(Application.getConfiguration());
        } catch (IOException e) {
            fail("Failed to set " + Application.CONFIG_FILE_VM_ARGUMENT);
        }
    }

    @Test
    public void testGetConfigurationFile() {
        try {
            File directory = new File(".");
            String cwd = directory.getCanonicalPath();
            Path testPath = Paths.get(cwd, "src", "test", "java", "edu",
                    "illinois", "library", "cantaloupe", "test");

            String goodProps = testPath + File.separator + "cantaloupe.properties";
            System.setProperty(Application.CONFIG_FILE_VM_ARGUMENT, goodProps);
            assertEquals(new File(cwd + "/src/test/java/edu/illinois/library/cantaloupe/test/cantaloupe.properties"),
                    Application.getConfigurationFile());
        } catch (IOException e) {
            fail("Failed to set " + Application.CONFIG_FILE_VM_ARGUMENT);
        }
    }

    /**
     * getVersion() is only partially testable as it checks whether the app is
     * running from a jar
     */
    @Test
    public void testGetVersion() {
        assertEquals("Non-Release", Application.getVersion());
    }

    @Test
    public void testSetConfiguration() {
        Configuration newConfig = newConfiguration();
        Application.setConfiguration(newConfig);
        assertSame(newConfig, Application.getConfiguration());
    }

    @Test
    public void testMainWithInvalidConfigExits() throws Exception {
        /* TODO: implement this
        http://stackoverflow.com/questions/6141252/dealing-with-system-exit0-in-junit-tests
        exit.expectSystemExitWithStatus(-1);
        String[] args = {};
        Configuration config = newConfiguration();
        config.setProperty("resolver.static", null);
        Application.setConfiguration(config);
        try {
            Application.main(args);
            fail("Expected exception");
        } catch (Exception e) {
            // pass
        } */
    }

    @Test
    public void testMainWithNoArgsStartsServer() throws Exception {
        String[] args = {};
        Application.main(args);
        ClientResource resource = getHttpClientForUriPath("/");
        resource.get();
        assertEquals(Status.SUCCESS_OK, resource.getResponse().getStatus());
    }

    @Test
    public void testMainWithPurgeCacheArg() throws Exception {
        final File cacheDir = getCacheDir();
        final File imageDir = new File(cacheDir.getAbsolutePath() + "/image");
        final File infoDir = new File(cacheDir.getAbsolutePath() + "/info");

        // set up the cache
        final Configuration config = newConfiguration();
        config.setProperty("cache.server", "FilesystemCache");
        config.setProperty("FilesystemCache.pathname",
                getCacheDir().getAbsolutePath());
        config.setProperty("FilesystemCache.ttl_seconds", "10");
        Application.setConfiguration(config);

        // cache a dimension
        Cache cache = CacheFactory.getInstance();
        cache.putDimension(new Identifier("cats"), new Dimension(500, 500));

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
        System.setProperty(Application.PURGE_CACHE_VM_ARGUMENT, "");
        String[] args = {};
        Application.main(args);

        // assert that they've been purged
        assertFalse(imageDir.exists());
        assertFalse(infoDir.exists());
    }

    @Test
    public void testMainWithPurgeExpiredCacheArg() throws Exception {
        File cacheDir = getCacheDir();
        File imageDir = new File(cacheDir.getAbsolutePath() + File.separator +
                "image");
        File infoDir = new File(cacheDir.getAbsolutePath() + File.separator +
                "info");
        imageDir.mkdirs();
        infoDir.mkdirs();

        Configuration config = newConfiguration();
        config.setProperty("cache.server", "FilesystemCache");
        config.setProperty("FilesystemCache.pathname",
                getCacheDir().getAbsolutePath());
        config.setProperty("FilesystemCache.ttl_seconds", "1");
        Application.setConfiguration(config);

        File.createTempFile("bla1", "tmp", imageDir);
        File.createTempFile("bla1", "tmp", infoDir);
        Thread.sleep(2500);
        File.createTempFile("bla2", "tmp", imageDir);
        File.createTempFile("bla2", "tmp", infoDir);

        System.setProperty(Application.PURGE_EXPIRED_FROM_CACHE_VM_ARGUMENT, "");
        String[] args = {};
        Application.main(args);

        assertEquals(1, imageDir.listFiles().length);
        assertEquals(1, infoDir.listFiles().length);
    }

    @Test
    public void testStartServerWithHttp() throws Exception {
        try {
            Application.setConfiguration(newConfiguration());
            Application.startServer();
            ClientResource resource = getHttpClientForUriPath("/");
            resource.get();
            assertEquals(Status.SUCCESS_OK, resource.getResponse().getStatus());
        } finally {
            Application.stopServer();
        }
    }

    @Test
    public void testStartServerWithHttps() throws Exception {
        try {
            Configuration config = newConfiguration();
            config.setProperty("https.enabled", true);
            Application.setConfiguration(config);
            Application.startServer();
            ClientResource resource = getHttpsClientForUriPath("/");
            resource.get();
            assertEquals(Status.SUCCESS_OK, resource.getResponse().getStatus());
        } finally {
            Application.stopServer();
        }
    }

    @Test
    public void testStopServerStopsHttp() throws Exception {
        Application.stopServer();
        // test that the HTTP server is stopped
        ClientResource resource = getHttpClientForUriPath("/");
        resource.setRetryOnError(false);
        try {
            resource.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            // pass
        }
    }

    @Test
    public void testStopServerStopsHttps() throws Exception {
        Application.stopServer();
        ClientResource resource = getHttpsClientForUriPath("/");
        resource.setRetryOnError(false);
        try {
            resource.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            // pass
        }
    }

    private ClientResource getHttpClientForUriPath(String path) {
        final Reference url = new Reference("http://localhost:" + HTTP_PORT + path);
        final ClientResource resource = new ClientResource(url);
        resource.setNext(httpClient);
        return resource;
    }

    /**
     * Create a self-signed cert with keytool:
     * keytool -keystore keystore.jks -alias server -genkey -keyalg RSA \
     *     -keysize 2048 -dname \
     *     "CN=localhost,OU=Simpson family,O=The Simpsons,C=US" \
     *     -sigalg "SHA1withRSA"
     *
     * Export a .crt from the keystore:
     * keytool -exportcert -keystore keystore.jks -alias server -file key.crt
     *
     * Import the .crt into a new truststore:
     * keytool -import -keystore truststore.jks -trustcacerts -alias server \
     *     -file key.crt
     *
     * Note: the .crt in the truststore needs to have an CN of "localhost".
     *
     * @param path
     * @return
     * @throws IOException
     */
    private ClientResource getHttpsClientForUriPath(String path)
            throws IOException {
        final Series<Parameter> parameters = httpsClient.getContext().getParameters();
        parameters.add("truststorePath",
                TestUtil.getFixture("truststore.jks").getAbsolutePath());
        //parameters.add("truststorePath", "src/test/resources/truststore.jks");
        parameters.add("truststorePassword", "password");
        parameters.add("truststoreType", "JKS");
        final Reference url = new Reference("https://localhost:" + HTTPS_PORT + path);
        final ClientResource resource = new ClientResource(url);
        resource.setNext(httpsClient);
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
