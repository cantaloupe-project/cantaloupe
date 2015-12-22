package edu.illinois.library.cantaloupe.resource.iiif.v1_1;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.image.OutputFormat;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.io.FileUtils;
import org.restlet.data.CacheDirective;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Functional test of the non-IIIF features of InformationResource.
 */
public class InformationResourceTest extends ResourceTest {

    @Override
    protected ClientResource getClientForUriPath(String path) {
        return super.getClientForUriPath(WebApplication.IIIF_1_1_PATH + path);
    }

    public void testCacheHeaders() {
        Configuration config = Application.getConfiguration();
        config.setProperty("cache.client.enabled", "true");
        config.setProperty("cache.client.max_age", "1234");
        config.setProperty("cache.client.shared_max_age", "4567");
        config.setProperty("cache.client.public", "false");
        config.setProperty("cache.client.private", "true");
        config.setProperty("cache.client.no_cache", "true");
        config.setProperty("cache.client.no_store", "true");
        config.setProperty("cache.client.must_revalidate", "false");
        config.setProperty("cache.client.proxy_revalidate", "true");
        config.setProperty("cache.client.no_transform", "false");

        Map<String, String> expectedDirectives = new HashMap<>();
        expectedDirectives.put("max-age", "1234");
        expectedDirectives.put("s-maxage", "4567");
        expectedDirectives.put("private", null);
        expectedDirectives.put("no-cache", null);
        expectedDirectives.put("no-store", null);
        expectedDirectives.put("proxy-revalidate", null);
        expectedDirectives.put("no-transform", null);

        ClientResource client = getClientForUriPath("/jpg/info.json");
        client.get();
        List<CacheDirective> actualDirectives = client.getResponse().getCacheDirectives();
        for (CacheDirective d : actualDirectives) {
            if (d.getName() != null) {
                assertTrue(expectedDirectives.keySet().contains(d.getName()));
                if (d.getValue() != null) {
                    assertTrue(expectedDirectives.get(d.getName()).equals(d.getValue()));
                } else {
                    assertNull(expectedDirectives.get(d.getName()));
                }
            }
        }
    }

    public void testEndpointDisabled() {
        Configuration config = Application.getConfiguration();
        ClientResource client = getClientForUriPath("/jpg/full/full/0/native.jpg");

        config.setProperty("endpoint.iiif.1.1.enabled", true);
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        config.setProperty("endpoint.iiif.1.1.enabled", false);
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_FORBIDDEN, client.getStatus());
        }
    }

    public void testPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse()
            throws Exception {
        doPurgeFromCacheWhenSourceIsMissing(false);
    }

    public void testPurgeFromCacheWhenSourceIsMissingAndOptionIsTrue()
            throws Exception {
        doPurgeFromCacheWhenSourceIsMissing(true);
    }

    private void doPurgeFromCacheWhenSourceIsMissing(boolean purgeMissing)
            throws Exception {
        File cacheFolder = TestUtil.getTempFolder();
        cacheFolder = new File(cacheFolder.getAbsolutePath() + "/cache");
        if (!cacheFolder.exists()) {
            cacheFolder.mkdir();
        }
        final File imageCacheFolder =
                new File(cacheFolder.getAbsolutePath() + "/image");
        final File infoCacheFolder =
                new File(cacheFolder.getAbsolutePath() + "/info");

        Configuration config = Application.getConfiguration();
        config.setProperty("cache.server", "FilesystemCache");
        config.setProperty("FilesystemCache.pathname",
                cacheFolder.getAbsolutePath());
        config.setProperty("FilesystemCache.ttl_seconds", 10);
        config.setProperty("cache.server.purge_missing", purgeMissing);

        File tempImage = File.createTempFile("temp", ".jpg");
        File image = TestUtil.getFixture("jpg");
        try {
            OperationList ops = TestUtil.newOperationList();
            ops.setIdentifier(new Identifier("jpg"));
            ops.setOutputFormat(OutputFormat.JPG);

            assertEquals(0, cacheFolder.listFiles().length);

            // request an image to cache it
            getClientForUriPath("/jpg/full/full/0/native.jpg").get();
            getClientForUriPath("/jpg/info.json").get();

            // assert that it has been cached
            assertEquals(1, imageCacheFolder.listFiles().length);
            assertEquals(1, infoCacheFolder.listFiles().length);
            Cache cache = CacheFactory.getInstance();
            assertNotNull(cache.getImageReadableChannel(ops));
            assertNotNull(cache.getDimension(ops.getIdentifier()));

            // move the source image out of the way
            if (tempImage.exists()) {
                tempImage.delete();
            }
            FileUtils.moveFile(image, tempImage);

            // request the same image which is now cached but underlying is 404
            try {
                getClientForUriPath("/jpg/info.json").get();
                fail("Expected exception");
            } catch (ResourceException e) {
                // noop
            }

            if (purgeMissing) {
                assertNull(cache.getImageReadableChannel(ops));
                assertNull(cache.getDimension(ops.getIdentifier()));
            } else {
                assertNotNull(cache.getImageReadableChannel(ops));
                assertNotNull(cache.getDimension(ops.getIdentifier()));
            }
        } finally {
            FileUtils.deleteDirectory(cacheFolder);
            if (tempImage.exists() && !image.exists()) {
                FileUtils.moveFile(tempImage, image);
            }
        }
    }

    public void testJson() throws IOException {
        // TODO: this could be a lot more thorough; but the aspects of the JSON
        // response defined in the Image API spec are tested in
        // ConformanceTest

        // test whether the @id property respects the base_uri configuration
        // option
        ClientResource client = getClientForUriPath("/escher_lego.jpg/info.json");
        client.get();
        String json = client.getResponse().getEntityAsText();
        ObjectMapper mapper = new ObjectMapper();
        ImageInfo info = mapper.readValue(json, ImageInfo.class);
        assertTrue(info.id.startsWith("http://") &&
                info.id.contains(WebApplication.IIIF_1_1_PATH + "/escher_lego.jpg"));

        Configuration config = Application.getConfiguration();
        config.setProperty("base_uri", "http://example.org/");
        client.get();
        json = client.getResponse().getEntityAsText();
        info = mapper.readValue(json, ImageInfo.class);
        assertTrue(info.id.startsWith("http://example.org/"));
    }

    public void testNotFound() throws IOException {
        ClientResource client = getClientForUriPath("/invalid/info.json");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_NOT_FOUND, client.getStatus());
        }
    }

    /**
     * Checks that the server responds with HTTP 500 when a non-FileResolver is
     * used with a non-ChannelProcessor.
     *
     * @throws Exception
     */
    public void testResolverProcessorCompatibility() throws Exception {
        WebServer server = new WebServer();

        Configuration config = newConfiguration();
        config.setProperty("resolver.static", "HttpResolver");
        config.setProperty("HttpResolver.lookup_strategy", "BasicLookupStrategy");
        config.setProperty("HttpResolver.BasicLookupStrategy.url_prefix",
                server.getUri() + "/");
        config.setProperty("processor.jp2", "KakaduProcessor");
        Application.setConfiguration(config);

        try {
            server.start();
            ClientResource client = getClientForUriPath("/escher_lego.jp2/info.json");
            try {
                client.get();
                fail("Expected exception");
            } catch (ResourceException e) {
                assertEquals(Status.SERVER_ERROR_INTERNAL, e.getStatus());
            }
        } finally {
            server.stop();
        }
    }

    public void testUnavailableSourceFormat() throws IOException {
        ClientResource client = getClientForUriPath("/text.txt/info.json");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE,
                    client.getStatus());
        }
    }

}
