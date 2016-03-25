package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.StandaloneEntry;
import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.OperationList;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.test.WebServer;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
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

import static org.junit.Assert.*;

public class ImageResourceTest extends ResourceTest {

    @Override
    protected ClientResource getClientForUriPath(String path) {
        return super.getClientForUriPath(WebApplication.IIIF_2_PATH + path);
    }

    @Test
    public void testAuthorizationDelegate() throws Exception {
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/default.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        try {
            client = getClientForUriPath("/forbidden.jpg/full/full/0/default.jpg");
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_FORBIDDEN, client.getStatus());
        }
    }

    @Test
    public void testBasicAuthentication() throws Exception {
        final String username = "user";
        final String secret = "secret";
        StandaloneEntry.getWebServer().stop();
        Configuration config = Configuration.getInstance();
        config.setProperty(WebApplication.BASIC_AUTH_ENABLED_CONFIG_KEY, "true");
        config.setProperty(WebApplication.BASIC_AUTH_USERNAME_CONFIG_KEY, username);
        config.setProperty(WebApplication.BASIC_AUTH_SECRET_CONFIG_KEY, secret);
        StandaloneEntry.getWebServer().start();

        // no credentials
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/default.jpg");
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

    @Test
    public void testCacheHeadersWhenCachingEnabled() {
        Configuration config = Configuration.getInstance();
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

        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/default.jpg");
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
    public void testCacheHeadersWhenCachingDisabled() {
        Configuration config = Configuration.getInstance();
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

        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/default.jpg");
        client.get();
        assertEquals(0, client.getResponse().getCacheDirectives().size());
    }

    @Test
    public void testContentDispositionHeader() {
        // no header
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/default.jpg");
        client.get();
        assertNull(client.getResponseEntity().getDisposition());

        // inline
        Configuration config = Configuration.getInstance();
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
        assertEquals(IMAGE + ".jpg",
                client.getResponseEntity().getDisposition().getFilename());
    }

    @Test
    public void testEndpointDisabled() {
        Configuration config = Configuration.getInstance();
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/default.jpg");

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

    @Test
    public void testIsAuthorized() {
        // TODO: write this
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

        Configuration config = Configuration.getInstance();
        config.setProperty(CacheFactory.DERIVATIVE_CACHE_CONFIG_KEY,
                "FilesystemCache");
        config.setProperty("FilesystemCache.pathname",
                cacheFolder.getAbsolutePath());
        config.setProperty("FilesystemCache.ttl_seconds", 10);
        config.setProperty("cache.server.resolve_first", true);
        config.setProperty("cache.server.purge_missing", purgeMissing);

        File tempImage = File.createTempFile("temp", ".jpg");
        File image = TestUtil.getImage(IMAGE);
        try {
            OperationList ops = TestUtil.newOperationList();
            ops.setIdentifier(new Identifier(IMAGE));
            ops.setOutputFormat(Format.JPG);

            assertEquals(0, FileUtils.listFiles(cacheFolder, null, true).size());

            // request an image to cache it
            getClientForUriPath("/" + IMAGE + "/full/full/0/default.jpg").get();
            getClientForUriPath("/" + IMAGE + "/info.json").get();

            // assert that it has been cached
            assertEquals(2, FileUtils.listFiles(cacheFolder, null, true).size());
            DerivativeCache cache = CacheFactory.getDerivativeCache();
            assertNotNull(cache.getImageInputStream(ops));
            assertNotNull(cache.getImageInfo(ops.getIdentifier()));

            // move the source image out of the way
            if (tempImage.exists()) {
                tempImage.delete();
            }
            FileUtils.moveFile(image, tempImage);

            // request the same image which is now cached but underlying is 404
            try {
                getClientForUriPath("/" + IMAGE + "/full/full/0/default.jpg").get();
                fail("Expected exception");
            } catch (ResourceException e) {
                // noop
            }

            if (purgeMissing) {
                assertNull(cache.getImageInputStream(ops));
                assertNull(cache.getImageInfo(ops.getIdentifier()));
            } else {
                assertNotNull(cache.getImageInputStream(ops));
                assertNotNull(cache.getImageInfo(ops.getIdentifier()));
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
    @Test
    public void testLinkHeader() {
        Configuration config = Configuration.getInstance();
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/default.jpg");

        config.setProperty(AbstractResource.BASE_URI_CONFIG_KEY, null);
        client.get();
        Header header = client.getResponse().getHeaders().getFirst("Link");
        assertTrue(header.getValue().startsWith("<http://localhost"));

        config.setProperty(AbstractResource.BASE_URI_CONFIG_KEY,
                "https://example.org/");
        client.get();
        header = client.getResponse().getHeaders().getFirst("Link");
        assertTrue(header.getValue().startsWith("<https://example.org/"));
    }

    @Test
    public void testMaxPixels() {
        Configuration config = Configuration.getInstance();
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/default.png");

        config.setProperty("max_pixels", 100000000);
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        config.setProperty("max_pixels", 1000);
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_FORBIDDEN, client.getStatus());
        }
    }

    @Test
    public void testMaxPixelsIgnoredWhenStreamingSource() {
        Configuration config = Configuration.getInstance();
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/default.jpg");
        config.setProperty("max_pixels", 1000);
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
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

    @Test
    public void testGetRepresentationDisposition() {
        Configuration config = Configuration.getInstance();
        config.clear();

        final Identifier identifier = new Identifier("cats?/\\dogs");
        final Format outputFormat = Format.JPG;

        // test with undefined config key
        Disposition disposition = AbstractResource.
                getRepresentationDisposition(identifier, outputFormat);
        assertEquals(Disposition.TYPE_NONE, disposition.getType());

        // test with empty config key
        config.setProperty(AbstractResource.CONTENT_DISPOSITION_CONFIG_KEY, "");
        disposition = AbstractResource.
                getRepresentationDisposition(identifier, outputFormat);
        assertEquals(Disposition.TYPE_NONE, disposition.getType());

        // test with config key set to "inline"
        config.setProperty(AbstractResource.CONTENT_DISPOSITION_CONFIG_KEY,
                "inline");
        disposition = AbstractResource.
                getRepresentationDisposition(identifier, outputFormat);
        assertEquals(Disposition.TYPE_INLINE, disposition.getType());

        // test with config key set to "attachment"
        config.setProperty(AbstractResource.CONTENT_DISPOSITION_CONFIG_KEY,
                "attachment");
        disposition = AbstractResource.
                getRepresentationDisposition(identifier, outputFormat);
        assertEquals(Disposition.TYPE_ATTACHMENT, disposition.getType());
        assertEquals("cats___dogs.jpg", disposition.getFilename());
    }

    /**
     * Checks that the server responds with HTTP 500 when a non-FileResolver is
     * used with a non-StreamProcessor.
     *
     * @throws Exception
     */
    @Test
    public void testResolverProcessorCompatibility() throws Exception {
        WebServer server = new WebServer();

        resetConfiguration();
        Configuration config = Configuration.getInstance();
        config.setProperty("resolver.static", "HttpResolver");
        config.setProperty("HttpResolver.lookup_strategy", "BasicLookupStrategy");
        config.setProperty("HttpResolver.BasicLookupStrategy.url_prefix",
                server.getUri() + "/");
        config.setProperty("processor.jp2", "KakaduProcessor");
        config.setProperty("processor.fallback", "KakaduProcessor");

        try {
            server.start();
            ClientResource client = getClientForUriPath(
                    "/jp2/full/full/0/default.jpg");
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
    public void testRestrictToSizes() {
        Configuration config = Configuration.getInstance();

        // not restricted
        config.setProperty(ImageResource.RESTRICT_TO_SIZES_CONFIG_KEY, false);
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/53,37/0/default.jpg");

        client.get();
        assertEquals(Status.SUCCESS_OK, client.getResponse().getStatus());

        // restricted
        config.setProperty(ImageResource.RESTRICT_TO_SIZES_CONFIG_KEY, true);
        client = getClientForUriPath("/" + IMAGE + "/full/53,37/0/default.jpg");
        try {
            client.get();
            fail();
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_FORBIDDEN, e.getStatus());
        }
    }

    @Test
    public void testSlashSubstitution() throws Exception {
        WebServer server = new WebServer();
        Configuration.getInstance().setProperty("slash_substitute", "CATS");
        try {
            server.start();
            ClientResource client = getClientForUriPath("/subfolderCATSjpg/full/full/0/default.jpg");
            client.get();
            assertEquals(Status.SUCCESS_OK, client.getResponse().getStatus());
        } finally {
            server.stop();
        }
    }

    @Test
    public void testUnavailableSourceFormat() throws IOException {
        ClientResource client = getClientForUriPath("/text.txt/full/full/0/default.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.SERVER_ERROR_INTERNAL, client.getStatus());
        }
    }

    @Test
    public void testUnavailableOutputFormat() throws IOException {
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/default.bogus");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }
    }

}
