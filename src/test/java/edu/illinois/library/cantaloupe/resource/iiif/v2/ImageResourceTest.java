package edu.illinois.library.cantaloupe.resource.iiif.v2;

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
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Disposition;
import org.restlet.data.Header;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageResourceTest extends ResourceTest {

    @Override
    protected ClientResource getClientForUriPath(String path) {
        return super.getClientForUriPath(WebApplication.IIIF_2_PATH + path);
    }

    public void testBasicAuth() throws Exception {
        final String username = "user";
        final String secret = "secret";
        Application.stopServer();
        Configuration config = Application.getConfiguration();
        config.setProperty(WebApplication.BASIC_AUTH_ENABLED_CONFIG_KEY, "true");
        config.setProperty(WebApplication.BASIC_AUTH_USERNAME_CONFIG_KEY, username);
        config.setProperty(WebApplication.BASIC_AUTH_SECRET_CONFIG_KEY, secret);
        Application.startServer();

        // no credentials
        ClientResource client = getClientForUriPath("/jpg/full/full/0/default.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_UNAUTHORIZED, client.getStatus());
        }

        // invalid credentials
        client.setChallengeResponse(
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, "invalid", "invalid"));
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_UNAUTHORIZED, client.getStatus());
        }

        // valid credentials
        client.setChallengeResponse(
                new ChallengeResponse(ChallengeScheme.HTTP_BASIC, username, secret));
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    public void testCacheHeadersWhenCachingEnabled() {
        Configuration config = Application.getConfiguration();
        config.setProperty("cache.client.enabled", "true");
        config.setProperty("cache.client.max_age", "1234");
        config.setProperty("cache.client.shared_max_age", "4567");
        config.setProperty("cache.client.public", "true");
        config.setProperty("cache.client.private", "false");
        config.setProperty("cache.client.no_cache", "false");
        config.setProperty("cache.client.no_store", "false");
        config.setProperty("cache.client.must_revalidate", "false");
        config.setProperty("cache.client.proxy_revalidate", "false");
        config.setProperty("cache.client.no_transform", "true");

        Map<String, String> expectedDirectives = new HashMap<>();
        expectedDirectives.put("max-age", "1234");
        expectedDirectives.put("s-maxage", "4567");
        expectedDirectives.put("public", null);
        expectedDirectives.put("no-transform", null);

        ClientResource client = getClientForUriPath("/jpg/full/full/0/default.jpg");
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

    public void testCacheHeadersWhenCachingDisabled() {
        Configuration config = Application.getConfiguration();
        config.setProperty("cache.client.enabled", "false");
        config.setProperty("cache.client.max_age", "1234");
        config.setProperty("cache.client.shared_max_age", "4567");
        config.setProperty("cache.client.public", "true");
        config.setProperty("cache.client.private", "false");
        config.setProperty("cache.client.no_cache", "false");
        config.setProperty("cache.client.no_store", "false");
        config.setProperty("cache.client.must_revalidate", "false");
        config.setProperty("cache.client.proxy_revalidate", "false");
        config.setProperty("cache.client.no_transform", "true");
        Application.setConfiguration(config);

        ClientResource client = getClientForUriPath("/jpg/full/full/0/default.jpg");
        client.get();
        assertEquals(0, client.getResponse().getCacheDirectives().size());
    }

    public void testContentDispositionHeader() {
        // no header
        ClientResource client = getClientForUriPath("/jpg/full/full/0/default.jpg");
        client.get();
        assertNull(client.getResponseEntity().getDisposition());

        // inline
        Configuration config = Application.getConfiguration();
        config.setProperty(ImageResource.CONTENT_DISPOSITION_CONFIG_KEY,
                "inline");
        client.get();
        assertEquals(Disposition.TYPE_INLINE,
                client.getResponseEntity().getDisposition().getType());

        // attachment
        config.setProperty(ImageResource.CONTENT_DISPOSITION_CONFIG_KEY,
                "attachment");
        client.get();
        assertEquals(Disposition.TYPE_ATTACHMENT,
                client.getResponseEntity().getDisposition().getType());
        assertEquals("jpg.jpg",
                client.getResponseEntity().getDisposition().getFilename());
    }

    public void testEndpointDisabled() {
        Configuration config = Application.getConfiguration();
        ClientResource client = getClientForUriPath("/jpg/full/full/0/default.jpg");

        config.setProperty("endpoint.iiif.2.enabled", true);
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        config.setProperty("endpoint.iiif.2.enabled", false);
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
            getClientForUriPath("/jpg/full/full/0/default.jpg").get();
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
                getClientForUriPath("/jpg/full/full/0/default.jpg").get();
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

    /**
     * Tests that the Link header respects the <code>base_uri</code>
     * key in the configuration.
     */
    public void testLinkHeader() {
        Configuration config = Application.getConfiguration();
        ClientResource client = getClientForUriPath("/jpg/full/full/0/default.jpg");

        config.setProperty("base_uri", null);
        client.get();
        Header header = client.getResponse().getHeaders().getFirst("Link");
        assertTrue(header.getValue().startsWith("<http://localhost"));

        config.setProperty("base_uri", "https://example.org/");
        client.get();
        header = client.getResponse().getHeaders().getFirst("Link");
        System.out.println(header.getValue());
        assertTrue(header.getValue().startsWith("<https://example.org/"));
    }

    public void testMaxPixels() {
        Configuration config = Application.getConfiguration();
        ClientResource client = getClientForUriPath("/png/full/full/0/default.jpg");

        config.setProperty("max_pixels", 100000000);
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        config.setProperty("max_pixels", 1000);
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.SERVER_ERROR_INTERNAL, client.getStatus());
        }
    }

    public void testMaxPixelsIgnoredWhenStreamingSource() {
        Configuration config = Application.getConfiguration();
        ClientResource client = getClientForUriPath("/jpg/full/full/0/default.jpg");
        config.setProperty("max_pixels", 1000);
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
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
            ClientResource client = getClientForUriPath(
                    "/escher_lego.jp2/full/full/0/default.jpg");
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
        ClientResource client = getClientForUriPath("/text.txt/full/full/0/default.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE,
                    client.getStatus());
        }
    }

    public void testUnavailableOutputFormat() throws IOException {
        ClientResource client = getClientForUriPath("/escher_logo.jpg/full/full/0/default.bogus");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }
    }

}
