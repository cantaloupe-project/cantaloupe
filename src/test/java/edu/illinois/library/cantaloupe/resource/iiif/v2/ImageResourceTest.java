package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
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

import java.awt.Dimension;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class ImageResourceTest extends ResourceTest {

    @Override
    protected ClientResource getClientForUriPath(String path) {
        return super.getClientForUriPath(RestletApplication.IIIF_2_PATH + path);
    }

    @Test
    public void testAuthorizationWhenAuthorized() throws Exception {
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
    public void testAuthorizationWhenNotAuthorized() throws Exception {
        ClientResource client = getClientForUriPath("/forbidden.jpg/full/full/0/default.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_FORBIDDEN, client.getStatus());
        }
    }

    @Test
    public void testAuthorizationWhenRedirecting() throws Exception {
        ClientResource client = getClientForUriPath("/redirect.jpg/full/full/0/default.jpg");
        client.setFollowingRedirects(false);
        client.get();
        assertEquals(Status.REDIRECTION_SEE_OTHER, client.getStatus());
        assertEquals("http://example.org/", client.getLocationRef().toString());
    }

    @Test
    public void testBasicAuthentication() throws Exception {
        final String username = "user";
        final String secret = "secret";

        Configuration config = ConfigurationFactory.getInstance();
        try {
            // To enable auth, the web server needs to be restarted.
            // It will need to be restarted again to disable it.
            config.setProperty(Key.BASIC_AUTH_ENABLED, true);
            config.setProperty(Key.BASIC_AUTH_USERNAME, username);
            config.setProperty(Key.BASIC_AUTH_SECRET, secret);
            webServer.stop();
            webServer.start();

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
        } finally {
            config.setProperty(Key.BASIC_AUTH_ENABLED, false);
            webServer.stop();
            webServer.start();
        }
    }

    @Test
    public void testCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Key.CLIENT_CACHE_ENABLED, "true");
        config.setProperty(Key.CLIENT_CACHE_MAX_AGE, "1234");
        config.setProperty(Key.CLIENT_CACHE_SHARED_MAX_AGE, "4567");
        config.setProperty(Key.CLIENT_CACHE_PUBLIC, "true");
        config.setProperty(Key.CLIENT_CACHE_PRIVATE, "false");
        config.setProperty(Key.CLIENT_CACHE_NO_CACHE, "false");
        config.setProperty(Key.CLIENT_CACHE_NO_STORE, "false");
        config.setProperty(Key.CLIENT_CACHE_MUST_REVALIDATE, "false");
        config.setProperty(Key.CLIENT_CACHE_PROXY_REVALIDATE, "false");
        config.setProperty(Key.CLIENT_CACHE_NO_TRANSFORM, "true");

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
    public void testCacheHeadersWhenClientCachingIsEnabledAndResponseIsNotCacheable()
            throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Key.CLIENT_CACHE_ENABLED, "true");
        config.setProperty(Key.CLIENT_CACHE_MAX_AGE, "1234");
        config.setProperty(Key.CLIENT_CACHE_SHARED_MAX_AGE, "4567");
        config.setProperty(Key.CLIENT_CACHE_PUBLIC, "true");
        config.setProperty(Key.CLIENT_CACHE_PRIVATE, "false");
        config.setProperty(Key.CLIENT_CACHE_NO_CACHE, "false");
        config.setProperty(Key.CLIENT_CACHE_NO_STORE, "false");
        config.setProperty(Key.CLIENT_CACHE_MUST_REVALIDATE, "false");
        config.setProperty(Key.CLIENT_CACHE_PROXY_REVALIDATE, "false");
        config.setProperty(Key.CLIENT_CACHE_NO_TRANSFORM, "true");

        ClientResource client = getClientForUriPath(
                "/bogus/full/full/0/default.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(0, client.getResponseCacheDirectives().size());
        }
    }

    /**
     * Tests that there is no Cache-Control header returned when
     * cache.client.enabled = true but a cache=false argument is present in the
     * URL query.
     *
     * @throws Exception
     */
    @Test
    public void testCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInUrl()
            throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Key.CLIENT_CACHE_ENABLED, "true");
        config.setProperty(Key.CLIENT_CACHE_MAX_AGE, "1234");
        config.setProperty(Key.CLIENT_CACHE_SHARED_MAX_AGE, "4567");
        config.setProperty(Key.CLIENT_CACHE_PUBLIC, "true");
        config.setProperty(Key.CLIENT_CACHE_PRIVATE, "false");
        config.setProperty(Key.CLIENT_CACHE_NO_CACHE, "false");
        config.setProperty(Key.CLIENT_CACHE_NO_STORE, "false");
        config.setProperty(Key.CLIENT_CACHE_MUST_REVALIDATE, "false");
        config.setProperty(Key.CLIENT_CACHE_PROXY_REVALIDATE, "false");
        config.setProperty(Key.CLIENT_CACHE_NO_TRANSFORM, "true");

        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/default.jpg?cache=false");
        client.get();
        assertEquals(0, client.getResponse().getCacheDirectives().size());
    }

    @Test
    public void testCacheHeadersWhenClientCachingIsDisabled() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Key.CLIENT_CACHE_ENABLED, false);

        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/default.jpg");
        client.get();
        assertEquals(0, client.getResponse().getCacheDirectives().size());
    }

    @Test
    public void testCacheWhenDerviativeCachingIsEnabled() throws Exception {
        File cacheFolder = TestUtil.getTempFolder();
        cacheFolder = new File(cacheFolder.getAbsolutePath() + "/cache");
        File imageCacheFolder = new File(cacheFolder.getAbsolutePath() + "/image");
        if (cacheFolder.exists()) {
            FileUtils.cleanDirectory(cacheFolder);
        } else {
            cacheFolder.mkdir();
        }

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
        config.setProperty(Key.DERIVATIVE_CACHE, "FilesystemCache");
        config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                cacheFolder.getAbsolutePath());
        config.setProperty(Key.CACHE_SERVER_TTL, 10);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);

        assertEquals(0, FileUtils.listFiles(cacheFolder, null, true).size());

        // request an image to cache it
        getClientForUriPath("/" + IMAGE + "/full/full/0/default.png").get();

        // assert that it has been cached
        assertEquals(1, FileUtils.listFiles(imageCacheFolder, null, true).size());
    }

    @Test
    public void testCacheWhenDerviativeCachingIsEnabledButNegativeCacheQueryArgumentIsSupplied()
            throws Exception {
        File cacheFolder = TestUtil.getTempFolder();
        cacheFolder = new File(cacheFolder.getAbsolutePath() + "/cache");
        File imageCacheFolder = new File(cacheFolder.getAbsolutePath() + "/image");
        if (cacheFolder.exists()) {
            FileUtils.cleanDirectory(cacheFolder);
        } else {
            cacheFolder.mkdir();
        }

        Configuration config = Configuration.getInstance();
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
        config.setProperty(Key.DERIVATIVE_CACHE, "FilesystemCache");
        config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                cacheFolder.getAbsolutePath());
        config.setProperty(Key.CACHE_SERVER_TTL, 10);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);

        assertEquals(0, FileUtils.listFiles(cacheFolder, null, true).size());

        // request an image
        getClientForUriPath("/" + IMAGE + "/full/full/0/default.png?cache=false").get();

        // assert that it has NOT been cached
        assertFalse(imageCacheFolder.exists());
    }

    @Test
    public void testContentDispositionHeader() throws Exception {
        // no header
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/default.jpg");
        client.get();
        assertNull(client.getResponseEntity().getDisposition());

        // inline
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Key.IIIF_CONTENT_DISPOSITION, "inline");
        client.get();
        assertEquals(Disposition.TYPE_INLINE,
                client.getResponseEntity().getDisposition().getType());

        // attachment
        config.setProperty(Key.IIIF_CONTENT_DISPOSITION, "attachment");
        client.get();
        assertEquals(Disposition.TYPE_ATTACHMENT,
                client.getResponseEntity().getDisposition().getType());
        assertEquals(IMAGE + ".jpg",
                client.getResponseEntity().getDisposition().getFilename());
    }

    @Test
    public void testEndpointDisabled() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/default.jpg");

        config.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, true);
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        config.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, false);
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_FORBIDDEN, client.getStatus());
        }
    }

    /**
     * Tests that the Link header respects the <code>base_uri</code>
     * key in the configuration.
     */
    @Test
    public void testLinkHeader() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/default.jpg");

        config.setProperty(Key.BASE_URI, "");
        client.get();
        Header header = client.getResponse().getHeaders().getFirst("Link");
        assertTrue(header.getValue().startsWith("<http://localhost"));

        config.setProperty(Key.BASE_URI, "https://example.org/");
        client.get();
        header = client.getResponse().getHeaders().getFirst("Link");
        assertTrue(header.getValue().startsWith("<https://example.org/"));

        client.getRequest().getHeaders().add("X-IIIF-ID", "originalID");
        client.get();
        header = client.getResponse().getHeaders().getFirst("Link");
        assertTrue(header.getValue().contains("/originalID/"));
    }

    @Test
    public void testMinPixels() throws Exception {
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/0,0,0,0/full/0/default.png"); // zero area
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }
    }

    @Test
    public void testMaxPixels() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/default.png");

        config.setProperty(Key.MAX_PIXELS, 100000000);
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        config.setProperty(Key.MAX_PIXELS, 1000);
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_FORBIDDEN, client.getStatus());
        }
    }

    @Test
    public void testMaxPixelsIgnoredWhenStreamingSource() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/default.jpg");
        config.setProperty(Key.MAX_PIXELS, 1000);
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    @Test
    public void testNotFound() throws Exception {
        ClientResource client = getClientForUriPath("/invalid");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_NOT_FOUND, client.getStatus());
        }
    }

    @Test
    public void testProcessorValidationFailure() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty("processor.pdf", "PdfBoxProcessor");
        ClientResource client = getClientForUriPath(
                "/pdf-multipage.pdf/full/full/0/default.jpg?page=999999");
        try {
            client.get();
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
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
        // Create a directory that will contain a source image. Don't want to
        // use the image fixtures dir because we'll need to delete one.
        File sourceDir = TestUtil.getTempFolder();
        sourceDir = new File(sourceDir.getAbsolutePath() + "/source");
        if (sourceDir.exists()) {
            FileUtils.cleanDirectory(sourceDir);
        } else {
            sourceDir.mkdir();
        }

        // Populate the source directory with an image.
        File imageFixture = TestUtil.getImage(IMAGE);
        File sourceImage = new File(sourceDir.getAbsolutePath() + "/" +
                imageFixture.getName());
        FileUtils.copyFile(imageFixture, sourceImage);

        // Create the cache directory.
        File cacheDir = TestUtil.getTempFolder();
        cacheDir = new File(cacheDir.getAbsolutePath() + "/cache");
        if (cacheDir.exists()) {
            FileUtils.cleanDirectory(cacheDir);
        } else {
            cacheDir.mkdir();
        }

        final Configuration config = Configuration.getInstance();
        config.setProperty(Key.FILESYSTEMRESOLVER_PATH_PREFIX,
                sourceDir.getAbsolutePath() + "/");
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
        config.setProperty(Key.DERIVATIVE_CACHE, "FilesystemCache");
        config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                cacheDir.getAbsolutePath());
        config.setProperty(Key.CACHE_SERVER_TTL, 60);
        config.setProperty(Key.CACHE_SERVER_RESOLVE_FIRST, true);
        config.setProperty(Key.CACHE_SERVER_PURGE_MISSING, purgeMissing);

        try {
            final String imagePath = "/" + IMAGE + "/full/full/0/default.jpg";
            final OperationList ops = Parameters.fromUri(imagePath).
                    toOperationList();
            ops.applyNonEndpointMutations(new Dimension(64, 56), "",
                    new URL("http://example.org/"), new HashMap<>(),
                    new HashMap<>());

            assertEquals(0, FileUtils.listFiles(cacheDir, null, true).size());

            // request an image to cache it
            getClientForUriPath(imagePath).get();
            getClientForUriPath("/" + IMAGE + "/info.json").get();

            // assert that it has been cached (there should be both an image
            // and an info)
            assertEquals(2, FileUtils.listFiles(cacheDir, null, true).size());

            // Delete the source image.
            sourceImage.delete();

            // request the same image which is now cached but underlying is 404
            try {
                getClientForUriPath("/" + IMAGE + "/full/full/0/default.jpg").get();
                fail("Expected exception");
            } catch (ResourceException e) {
                // noop
            }

            if (purgeMissing) {
                assertEquals(0, FileUtils.listFiles(cacheDir, null, true).size());
            } else {
                assertEquals(2, FileUtils.listFiles(cacheDir, null, true).size());
            }
        } finally {
            FileUtils.deleteDirectory(sourceDir);
            FileUtils.deleteDirectory(cacheDir);
        }
    }

    /**
     * Checks that the server responds with HTTP 500 when a non-FileResolver is
     * used with a non-StreamProcessor.
     *
     * @throws Exception
     */
    @Test
    public void testResolverProcessorCompatibility() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(Key.RESOLVER_STATIC, "HttpResolver");
        config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
        config.setProperty(Key.HTTPRESOLVER_URL_PREFIX,
                webServer.getHTTPHost() + ":" + webServer.getHTTPPort() + "/");
        config.setProperty("processor.jp2", "KakaduProcessor");
        config.setProperty(Key.PROCESSOR_FALLBACK, "KakaduProcessor");

        ClientResource client = getClientForUriPath("/jp2/full/full/0/default.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.SERVER_ERROR_INTERNAL, e.getStatus());
        }
    }

    @Test
    public void testRestrictToSizes() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();

        // not restricted
        config.setProperty(Key.IIIF_2_RESTRICT_TO_SIZES, false);
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/53,37/0/default.jpg");

        client.get();
        assertEquals(Status.SUCCESS_OK, client.getResponse().getStatus());

        // restricted
        config.setProperty(Key.IIIF_2_RESTRICT_TO_SIZES, true);
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
        ConfigurationFactory.getInstance().setProperty("slash_substitute", "CATS");

        ClientResource client = getClientForUriPath("/subfolderCATSjpg/full/full/0/default.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getResponse().getStatus());
    }

    @Test
    public void testUnavailableSourceFormat() throws Exception {
        ClientResource client = getClientForUriPath("/text.txt/full/full/0/default.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.SERVER_ERROR_INTERNAL, client.getStatus());
        }
    }

    @Test
    public void testInvalidOutputFormat() throws Exception {
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/default.bogus");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }
    }

    @Test
    public void testUnavailableOutputFormat() throws Exception {
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/default.webp");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_UNSUPPORTED_MEDIA_TYPE,
                    client.getStatus());
        }
    }

    @Test
    public void testXPoweredByHeader() throws Exception {
        ClientResource resource = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/default.jpg");
        resource.get();
        Header header = resource.getResponse().getHeaders().
                getFirst("X-Powered-By");
        assertEquals("Cantaloupe/Unknown", header.getValue());
    }

    /**
     * Tests that an X-Sendfile header is added to the response when
     * FilesystemCache registers a hit, and not added otherwise.
     */
    @Test
    public void testXSendfileHeaderIsSentWhenAllConditionsAreMet()
            throws Exception {
        File cacheFolder = TestUtil.getTempFolder();
        cacheFolder = new File(cacheFolder.getAbsolutePath() + "/cache");
        if (cacheFolder.exists()) {
            FileUtils.cleanDirectory(cacheFolder);
        } else {
            cacheFolder.mkdir();
        }

        final Configuration config = Configuration.getInstance();

        // Set up the cache. We must use FilesystemCache because X-Sendfile
        // will work only with that.
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
        config.setProperty(Key.DERIVATIVE_CACHE, "FilesystemCache");
        config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                cacheFolder.getAbsolutePath());
        config.setProperty(Key.CACHE_SERVER_TTL, 10);

        // Request an image. Since it hasn't yet been cached, the response
        // shouldn't include the X-Sendfile header.
        ClientResource resource =
                getClientForUriPath("/" + IMAGE + "/full/full/0/default.png");
        resource.getRequest().getHeaders().add("X-Sendfile-Type", "X-Sendfile");
        resource.get();
        Header header = resource.getResponse().getHeaders().getFirst("X-Sendfile");
        assertNull(header);

        // Now it should be cached, so the next response should include the
        // X-Sendfile header.
        resource.get();
        header = resource.getResponse().getHeaders().getFirst("X-Sendfile");

        assertTrue(header.getValue().startsWith(config.getString(Key.FILESYSTEMCACHE_PATHNAME)));
        assertTrue(header.getValue().endsWith(".png"));
    }

    @Test
    public void testXSendfileHeaderIsNotSentWhenXSendfileTypeIsNotSupplied()
            throws Exception {
        File cacheFolder = TestUtil.getTempFolder();
        cacheFolder = new File(cacheFolder.getAbsolutePath() + "/cache");
        if (cacheFolder.exists()) {
            FileUtils.cleanDirectory(cacheFolder);
        } else {
            cacheFolder.mkdir();
        }

        final Configuration config = Configuration.getInstance();

        // Set up the cache. We must use FilesystemCache because X-Sendfile
        // will work only with that.
        config.setProperty(Key.DERIVATIVE_CACHE_ENABLED, true);
        config.setProperty(Key.DERIVATIVE_CACHE, "FilesystemCache");
        config.setProperty(Key.FILESYSTEMCACHE_PATHNAME,
                cacheFolder.getAbsolutePath());
        config.setProperty(Key.CACHE_SERVER_TTL, 10);

        // Request an image. Since it hasn't yet been cached, the response
        // shouldn't include the X-Sendfile header.
        ClientResource resource =
                getClientForUriPath("/" + IMAGE + "/full/full/0/default.png");
        resource.get();
        Header header = resource.getResponse().getHeaders().getFirst("X-Sendfile");
        assertNull(header);

        // Since the header is disabled, the next response shouldn't include it,
        // either.
        resource.get();
        header = resource.getResponse().getHeaders().getFirst("X-Sendfile");
        assertNull(header);
    }

}
