package edu.illinois.library.cantaloupe.resource.iiif.v1;

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
import org.junit.Test;
import org.restlet.data.CacheDirective;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Functional test of the non-IIIF features of InformationResource.
 */
public class InformationResourceTest extends ResourceTest {

    @Override
    protected ClientResource getClientForUriPath(String path) {
        return super.getClientForUriPath(WebApplication.IIIF_1_PATH + path);
    }

    @Test
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

    @Test
    public void testEndpointDisabled() {
        Configuration config = Application.getConfiguration();
        ClientResource client = getClientForUriPath("/jpg/full/full/0/native.jpg");

        config.setProperty("endpoint.iiif.1.enabled", true);
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        config.setProperty("endpoint.iiif.1.enabled", false);
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_FORBIDDEN, client.getStatus());
        }
    }

    @Test
    public void testPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse()
            throws Exception {
        doPurgeFromCacheWhenSourceIsMissing(false);
    }

    @Test
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

    @Test
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
    @Test
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

    @Test
    public void testSlashSubstitution() throws Exception {
        WebServer server = new WebServer();
        Application.getConfiguration().setProperty("slash_substitute", "CATS");
        try {
            server.start();
            ClientResource client = getClientForUriPath("/subfolderCATSjpg/info.json");
            client.get();
            assertEquals(Status.SUCCESS_OK, client.getResponse().getStatus());
        } finally {
            server.stop();
        }
    }

    @Test
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

    @Test
    public void testUrisInJson() throws IOException {
        ClientResource client = getClientForUriPath("/escher_lego.jpg/info.json");
        client.get();
        String json = client.getResponse().getEntityAsText();
        ObjectMapper mapper = new ObjectMapper();
        ImageInfo info = mapper.readValue(json, ImageInfo.class);
        assertEquals("http://localhost:" + PORT +
                WebApplication.IIIF_1_PATH + "/escher_lego.jpg", info.id);
    }

    @Test
    public void testUrisInJsonWithBaseUriOverride() throws IOException {
        Configuration config = Application.getConfiguration();
        config.setProperty(AbstractResource.BASE_URI_CONFIG_KEY,
                "http://example.org/");

        ClientResource client = getClientForUriPath("/escher_lego.jpg/info.json");
        client.get();
        String json = client.getResponse().getEntityAsText();
        ObjectMapper mapper = new ObjectMapper();
        ImageInfo info = mapper.readValue(json, ImageInfo.class);
        assertEquals("http://example.org" +
                WebApplication.IIIF_1_PATH + "/escher_lego.jpg", info.id);
    }

    @Test
    public void testUrisInJsonWithProxyHeaders() throws IOException {
        ClientResource client = getClientForUriPath("/escher_lego.jpg/info.json");
        client.getRequest().getHeaders().add("X-Forwarded-Proto", "HTTP");
        client.getRequest().getHeaders().add("X-Forwarded-Host", "example.org");
        client.getRequest().getHeaders().add("X-Forwarded-Port", "8080");
        client.getRequest().getHeaders().add("X-Forwarded-Path", "/cats");
        client.get();
        String json = client.getResponse().getEntityAsText();
        ObjectMapper mapper = new ObjectMapper();
        ImageInfo info = mapper.readValue(json, ImageInfo.class);
        assertEquals("http://example.org:8080/cats" +
                WebApplication.IIIF_1_PATH + "/escher_lego.jpg", info.id);
    }

    @Test
    public void testBaseUriOverridesProxyHeaders() throws IOException {
        Configuration config = Application.getConfiguration();
        config.setProperty(AbstractResource.BASE_URI_CONFIG_KEY,
                "https://example.net/");

        ClientResource client = getClientForUriPath("/escher_lego.jpg/info.json");
        client.getRequest().getHeaders().add("X-Forwarded-Proto", "HTTP");
        client.getRequest().getHeaders().add("X-Forwarded-Host", "example.org");
        client.getRequest().getHeaders().add("X-Forwarded-Port", "8080");
        client.getRequest().getHeaders().add("X-Forwarded-Path", "/cats");
        client.get();
        String json = client.getResponse().getEntityAsText();
        ObjectMapper mapper = new ObjectMapper();
        ImageInfo info = mapper.readValue(json, ImageInfo.class);
        assertEquals("https://example.net" +
                WebApplication.IIIF_1_PATH + "/escher_lego.jpg", info.id);
    }

}
