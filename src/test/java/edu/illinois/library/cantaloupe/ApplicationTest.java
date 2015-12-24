package edu.illinois.library.cantaloupe;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ApplicationTest extends CantaloupeTestCase {

    private static final Integer PORT = 34852;

    private static Client client = new Client(new Context(), Protocol.HTTP);

    private static BaseConfiguration newConfiguration() {
        BaseConfiguration config = new BaseConfiguration();
        try {
            config.setProperty("print_stack_trace_on_error_pages", false);
            config.setProperty("http.port", PORT);
            config.setProperty("resolver.static", "FilesystemResolver");
            config.setProperty("processor.fallback", "Java2dProcessor");
        } catch (Exception e) {
            fail("Failed to get the configuration");
        }
        return config;
    }

    public void setUp() {
        Application.setConfiguration(newConfiguration());
        System.getProperties().remove("cantaloupe.cache.purge");
        System.getProperties().remove("cantaloupe.cache.purge_expired");

    }

    public void tearDown() throws IOException {
        deleteCacheDir();
    }

    public void testGetConfiguration() {
        try {
            File directory = new File(".");
            String cwd = directory.getCanonicalPath();
            Path testPath = Paths.get(cwd, "src", "test", "java", "edu",
                    "illinois", "library", "cantaloupe", "test");

            String goodProps = testPath + File.separator + "cantaloupe.properties";
            System.setProperty("cantaloupe.config", goodProps);
            assertNotNull(Application.getConfiguration());
        } catch (IOException e) {
            fail("Failed to set cantaloupe.config");
        }
    }

    public void testGetConfigurationFile() {
        try {
            File directory = new File(".");
            String cwd = directory.getCanonicalPath();
            Path testPath = Paths.get(cwd, "src", "test", "java", "edu",
                    "illinois", "library", "cantaloupe", "test");

            String goodProps = testPath + File.separator + "cantaloupe.properties";
            System.setProperty("cantaloupe.config", goodProps);
            assertEquals(new File(cwd + "/src/test/java/edu/illinois/library/cantaloupe/test/cantaloupe.properties"),
                    Application.getConfigurationFile());
        } catch (IOException e) {
            fail("Failed to set cantaloupe.config");
        }
    }

    /**
     * getVersion() is only partially testable as it checks whether the app is
     * running from a jar
     */
    public void testGetVersion() {
        assertEquals("Non-Release", Application.getVersion());
    }

    public void testSetConfiguration() {
        Configuration newConfig = newConfiguration();
        Application.setConfiguration(newConfig);
        assertSame(newConfig, Application.getConfiguration());
    }

    public void testMainWithInvalidConfigExits() throws Exception {
        /* TODO: implement this once migrated to junit 4
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

    public void testMainWithNoArgsStartsServer() throws Exception {
        String[] args = {};
        Application.main(args);
        ClientResource resource = getClientForUriPath("/");
        resource.get();
        assertEquals(Status.SUCCESS_OK, resource.getResponse().getStatus());
    }

    public void testMainWithPurgeCacheArg() throws Exception {
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
        File.createTempFile("bla2", "tmp", infoDir);

        System.setProperty("cantaloupe.cache.purge", "");
        String[] args = {};
        Application.main(args);

        assertEquals(0, imageDir.listFiles().length);
        assertEquals(0, infoDir.listFiles().length);
    }

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

        System.setProperty("cantaloupe.cache.purge_expired", "");
        String[] args = {};
        Application.main(args);

        assertEquals(1, imageDir.listFiles().length);
        assertEquals(1, infoDir.listFiles().length);
    }

    public void testStart() throws Exception {
        try {
            Application.setConfiguration(newConfiguration());
            Application.startServer();
            ClientResource resource = getClientForUriPath("/");
            resource.get();
            assertEquals(Status.SUCCESS_OK, resource.getResponse().getStatus());
        } finally {
            Application.stopServer();
        }
    }

    public void testStop() throws Exception {
        Application.stopServer();
        ClientResource resource = getClientForUriPath("/iiif");
        resource.setRetryOnError(false);
        try {
            resource.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            // pass
        }
    }

    private ClientResource getClientForUriPath(String path) {
        Reference url = new Reference(getBaseUri() + path);
        ClientResource resource = new ClientResource(url);
        resource.setNext(client);
        return resource;
    }

    private void deleteCacheDir() throws IOException {
        FileUtils.deleteDirectory(getCacheDir());
    }

    private String getBaseUri() {
        return "http://localhost:" + PORT;
    }

    private File getCacheDir() throws IOException {
        File directory = new File(".");
        String cwd = directory.getCanonicalPath();
        Path path = Paths.get(cwd, "src", "test", "resources", "cache");
        return path.toFile();
    }

}
