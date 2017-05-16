package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.resource.*;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
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
        return super.getClientForUriPath(WebApplication.IIIF_1_PATH + path);
    }

    @Test
    public void testAuthorizationDelegateWithBooleanReturnValue() throws Exception {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/full/full/0/native.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());

        try {
            client = getClientForUriPath("/forbidden.jpg/full/full/0/native.jpg");
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_FORBIDDEN, client.getStatus());
        }
    }

    @Test
    public void testAuthorizationDelegateWithHashReturnValue() throws Exception {
        ClientResource client = getClientForUriPath("/redirect.jpg/full/full/0/native.jpg");
        client.setFollowingRedirects(false);
        client.get();
        assertEquals(Status.REDIRECTION_SEE_OTHER, client.getStatus());
        assertEquals("http://example.org/", client.getLocationRef().toString());
    }

    @Test
    public void testBasicAuthentication() throws Exception {
        final String username = "user";
        final String secret = "secret";

        Configuration config = Configuration.getInstance();
        try {
            // To enable auth, the web server needs to be restarted.
            // It will need to be restarted again to disable it.
            config.setProperty(WebApplication.BASIC_AUTH_ENABLED_CONFIG_KEY, true);
            config.setProperty(WebApplication.BASIC_AUTH_USERNAME_CONFIG_KEY, username);
            config.setProperty(WebApplication.BASIC_AUTH_SECRET_CONFIG_KEY, secret);
            webServer.stop();
            webServer.start();

            // no credentials
            ClientResource client = getClientForUriPath(
                    "/" + IMAGE + "/full/full/0/native.jpg");
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
            config.setProperty(WebApplication.BASIC_AUTH_ENABLED_CONFIG_KEY, false);
            webServer.stop();
            webServer.start();
        }
    }

    @Test
    public void testCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(AbstractResource.CLIENT_CACHE_ENABLED_CONFIG_KEY, "true");
        config.setProperty(AbstractResource.CLIENT_CACHE_MAX_AGE_CONFIG_KEY, "1234");
        config.setProperty(AbstractResource.CLIENT_CACHE_SHARED_MAX_AGE_CONFIG_KEY, "4567");
        config.setProperty(AbstractResource.CLIENT_CACHE_PUBLIC_CONFIG_KEY, "true");
        config.setProperty(AbstractResource.CLIENT_CACHE_PRIVATE_CONFIG_KEY, "false");
        config.setProperty(AbstractResource.CLIENT_CACHE_NO_CACHE_CONFIG_KEY, "false");
        config.setProperty(AbstractResource.CLIENT_CACHE_NO_STORE_CONFIG_KEY, "false");
        config.setProperty(AbstractResource.CLIENT_CACHE_MUST_REVALIDATE_CONFIG_KEY, "false");
        config.setProperty(AbstractResource.CLIENT_CACHE_PROXY_REVALIDATE_CONFIG_KEY, "false");
        config.setProperty(AbstractResource.CLIENT_CACHE_NO_TRANSFORM_CONFIG_KEY, "true");

        Map<String, String> expectedDirectives = new HashMap<>();
        expectedDirectives.put("max-age", "1234");
        expectedDirectives.put("s-maxage", "4567");
        expectedDirectives.put("public", null);
        expectedDirectives.put("no-transform", null);

        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/native.jpg");
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
        Configuration config = Configuration.getInstance();
        config.setProperty(AbstractResource.CLIENT_CACHE_ENABLED_CONFIG_KEY, "true");
        config.setProperty(AbstractResource.CLIENT_CACHE_MAX_AGE_CONFIG_KEY, "1234");
        config.setProperty(AbstractResource.CLIENT_CACHE_SHARED_MAX_AGE_CONFIG_KEY, "4567");
        config.setProperty(AbstractResource.CLIENT_CACHE_PUBLIC_CONFIG_KEY, "true");
        config.setProperty(AbstractResource.CLIENT_CACHE_PRIVATE_CONFIG_KEY, "false");
        config.setProperty(AbstractResource.CLIENT_CACHE_NO_CACHE_CONFIG_KEY, "false");
        config.setProperty(AbstractResource.CLIENT_CACHE_NO_STORE_CONFIG_KEY, "false");
        config.setProperty(AbstractResource.CLIENT_CACHE_MUST_REVALIDATE_CONFIG_KEY, "false");
        config.setProperty(AbstractResource.CLIENT_CACHE_PROXY_REVALIDATE_CONFIG_KEY, "false");
        config.setProperty(AbstractResource.CLIENT_CACHE_NO_TRANSFORM_CONFIG_KEY, "true");

        ClientResource client = getClientForUriPath(
                "/bogus/full/full/0/native.jpg");
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
        Configuration config = Configuration.getInstance();
        config.setProperty(AbstractResource.CLIENT_CACHE_ENABLED_CONFIG_KEY, "true");
        config.setProperty(AbstractResource.CLIENT_CACHE_MAX_AGE_CONFIG_KEY, "1234");
        config.setProperty(AbstractResource.CLIENT_CACHE_SHARED_MAX_AGE_CONFIG_KEY, "4567");
        config.setProperty(AbstractResource.CLIENT_CACHE_PUBLIC_CONFIG_KEY, "true");
        config.setProperty(AbstractResource.CLIENT_CACHE_PRIVATE_CONFIG_KEY, "false");
        config.setProperty(AbstractResource.CLIENT_CACHE_NO_CACHE_CONFIG_KEY, "false");
        config.setProperty(AbstractResource.CLIENT_CACHE_NO_STORE_CONFIG_KEY, "false");
        config.setProperty(AbstractResource.CLIENT_CACHE_MUST_REVALIDATE_CONFIG_KEY, "false");
        config.setProperty(AbstractResource.CLIENT_CACHE_PROXY_REVALIDATE_CONFIG_KEY, "false");
        config.setProperty(AbstractResource.CLIENT_CACHE_NO_TRANSFORM_CONFIG_KEY, "true");

        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/native.jpg?cache=false");
        client.get();
        assertEquals(0, client.getResponse().getCacheDirectives().size());
    }

    @Test
    public void testCacheHeadersWhenClientCachingIsDisabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(AbstractResource.CLIENT_CACHE_ENABLED_CONFIG_KEY, "false");

        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/native.jpg");
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
        config.setProperty(CacheFactory.DERIVATIVE_CACHE_ENABLED_CONFIG_KEY,
                true);
        config.setProperty(CacheFactory.DERIVATIVE_CACHE_CONFIG_KEY,
                "FilesystemCache");
        config.setProperty("FilesystemCache.pathname",
                cacheFolder.getAbsolutePath());
        config.setProperty(Cache.TTL_CONFIG_KEY, 10);
        config.setProperty(Cache.RESOLVE_FIRST_CONFIG_KEY, true);

        assertEquals(0, FileUtils.listFiles(cacheFolder, null, true).size());

        // request an image to cache it
        getClientForUriPath("/" + IMAGE + "/full/full/0/native.png").get();

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
        config.setProperty(CacheFactory.DERIVATIVE_CACHE_ENABLED_CONFIG_KEY,
                true);
        config.setProperty(CacheFactory.DERIVATIVE_CACHE_CONFIG_KEY,
                "FilesystemCache");
        config.setProperty("FilesystemCache.pathname",
                cacheFolder.getAbsolutePath());
        config.setProperty(Cache.TTL_CONFIG_KEY, 10);
        config.setProperty(Cache.RESOLVE_FIRST_CONFIG_KEY, true);

        assertEquals(0, FileUtils.listFiles(cacheFolder, null, true).size());

        // request an image
        getClientForUriPath("/" + IMAGE + "/full/full/0/native.png?cache=false").get();

        // assert that it has NOT been cached
        assertFalse(imageCacheFolder.exists());
    }

    @Test
    public void testContentDispositionHeader() throws Exception {
        // no header
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/native.jpg");
        client.get();
        assertNull(client.getResponseEntity().getDisposition());

        // inline
        Configuration config = Configuration.getInstance();
        config.setProperty(edu.illinois.library.cantaloupe.resource.AbstractResource.CONTENT_DISPOSITION_CONFIG_KEY,
                "inline");
        client.get();
        assertEquals(Disposition.TYPE_INLINE,
                client.getResponseEntity().getDisposition().getType());

        // attachment
        config.setProperty(AbstractResource.CONTENT_DISPOSITION_CONFIG_KEY,
                "attachment");
        client.get();
        assertEquals(Disposition.TYPE_ATTACHMENT,
                client.getResponseEntity().getDisposition().getType());
        assertEquals(IMAGE + ".jpg",
                client.getResponseEntity().getDisposition().getFilename());
    }

    @Test
    public void testEndpointDisabled() throws Exception {
        Configuration config = Configuration.getInstance();
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/native.jpg");

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

    public void testIsAuthorized() {
        // will be tested in the v2 endpoint counterpart
    }

    @Test
    public void testMaxPixels() throws Exception {
        Configuration config = Configuration.getInstance();
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/native.png");

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
    public void testMaxPixelsIgnoredWhenStreamingSource() throws Exception {
        Configuration config = Configuration.getInstance();
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/native.jpg");
        config.setProperty("max_pixels", 1000);
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
        Configuration config = Configuration.getInstance();
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
        config.setProperty("FilesystemResolver.BasicLookupStrategy.path_prefix",
                sourceDir.getAbsolutePath() + "/");
        config.setProperty(CacheFactory.DERIVATIVE_CACHE_ENABLED_CONFIG_KEY,
                true);
        config.setProperty(CacheFactory.DERIVATIVE_CACHE_CONFIG_KEY,
                "FilesystemCache");
        config.setProperty("FilesystemCache.pathname",
                cacheDir.getAbsolutePath());
        config.setProperty(Cache.TTL_CONFIG_KEY, 60);
        config.setProperty(Cache.RESOLVE_FIRST_CONFIG_KEY, true);
        config.setProperty(Cache.PURGE_MISSING_CONFIG_KEY, purgeMissing);

        try {
            final String imagePath = "/" + IMAGE + "/full/full/0/native.jpg";
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
                getClientForUriPath("/" + IMAGE + "/full/full/0/native.jpg").get();
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
        Configuration config = Configuration.getInstance();
        config.setProperty(ResolverFactory.STATIC_RESOLVER_CONFIG_KEY,
                "HttpResolver");
        config.setProperty("HttpResolver.lookup_strategy", "BasicLookupStrategy");
        config.setProperty("HttpResolver.BasicLookupStrategy.url_prefix",
                webServer.getHttpHost() + ":" + webServer.getHttpPort() + "/");
        config.setProperty("processor.jp2", "KakaduProcessor");
        config.setProperty(ProcessorFactory.FALLBACK_PROCESSOR_CONFIG_KEY,
                "KakaduProcessor");

        ClientResource client = getClientForUriPath(
                "/jp2/full/full/0/native.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.SERVER_ERROR_INTERNAL, e.getStatus());
        }
    }

    @Test
    public void testSlashSubstitution() throws Exception {
        Configuration.getInstance().setProperty("slash_substitute", "CATS");

        ClientResource client = getClientForUriPath("/subfolderCATSjpg/full/full/0/native.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getResponse().getStatus());
    }

    @Test
    public void testUnavailableSourceFormat() throws Exception {
        ClientResource client = getClientForUriPath("/text.txt/full/full/0/native.jpg");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.SERVER_ERROR_INTERNAL, client.getStatus());
        }
    }

    @Test
    public void testUnavailableOutputFormat() throws Exception {
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/native.bogus");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }
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
        config.setProperty(CacheFactory.DERIVATIVE_CACHE_ENABLED_CONFIG_KEY,
                true);
        config.setProperty(CacheFactory.DERIVATIVE_CACHE_CONFIG_KEY,
                "FilesystemCache");
        config.setProperty("FilesystemCache.pathname",
                cacheFolder.getAbsolutePath());
        config.setProperty(Cache.TTL_CONFIG_KEY, 10);

        // Configure the X-Sendfile header
        config.setProperty(AbstractResource.FILESYSTEMCACHE_XSENDFILE_ENABLED_CONFIG_KEY,
                true);
        config.setProperty(AbstractResource.FILESYSTEMCACHE_XSENDFILE_HEADER_CONFIG_KEY,
                "X-Sendfile");

        // Request an image. Since it hasn't yet been cached, the response
        // shouldn't include the X-Sendfile header.
        ClientResource resource =
                getClientForUriPath("/" + IMAGE + "/full/full/0/native.png");
        resource.get();
        Header header = resource.getResponse().getHeaders().getFirst("X-Sendfile");
        assertNull(header);

        // Now it should be cached, so the next response should include the
        // X-Sendfile header.
        resource.get();
        header = resource.getResponse().getHeaders().getFirst("X-Sendfile");

        // /image/08/34/25/083425bc68eece64753ec83a25f87230_540586ed73955b63fd3c8d510a32fcac.png
        assertTrue(header.getValue().matches("^\\/image\\/[0-9a-f_/]*\\.png"));
    }

    @Test
    public void testXSendfileHeaderIsNotSentWhenDisabled() throws Exception {
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
        config.setProperty(CacheFactory.DERIVATIVE_CACHE_ENABLED_CONFIG_KEY,
                true);
        config.setProperty(CacheFactory.DERIVATIVE_CACHE_CONFIG_KEY,
                "FilesystemCache");
        config.setProperty("FilesystemCache.pathname",
                cacheFolder.getAbsolutePath());
        config.setProperty(Cache.TTL_CONFIG_KEY, 10);

        // Configure the X-Sendfile header
        config.setProperty(AbstractResource.FILESYSTEMCACHE_XSENDFILE_ENABLED_CONFIG_KEY,
                false);
        config.setProperty(AbstractResource.FILESYSTEMCACHE_XSENDFILE_HEADER_CONFIG_KEY,
                "X-Sendfile");

        // Request an image. Since it hasn't yet been cached, the response
        // shouldn't include the X-Sendfile header.
        ClientResource resource =
                getClientForUriPath("/" + IMAGE + "/full/full/0/native.png");
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
