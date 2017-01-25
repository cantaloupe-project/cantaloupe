package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.operation.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.processor.ProcessorFactory;
import edu.illinois.library.cantaloupe.resolver.ResolverFactory;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
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

import java.io.File;
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
    public void testAuthorizationDelegateWithBooleanReturnValue() throws Exception {
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
    public void testAuthorizationDelegateWithHashReturnValue() throws Exception {
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
            config.setProperty(WebApplication.BASIC_AUTH_ENABLED_CONFIG_KEY, true);
            config.setProperty(WebApplication.BASIC_AUTH_USERNAME_CONFIG_KEY, username);
            config.setProperty(WebApplication.BASIC_AUTH_SECRET_CONFIG_KEY, secret);
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
            config.setProperty(WebApplication.BASIC_AUTH_ENABLED_CONFIG_KEY, false);
            webServer.stop();
            webServer.start();
        }
    }

    @Test
    public void testCacheHeadersWhenClientCachingIsEnabled() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
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
                "/" + IMAGE + "/full/full/0/default.jpg?cache=false");
        client.get();
        assertEquals(0, client.getResponse().getCacheDirectives().size());
    }

    @Test
    public void testCacheHeadersWhenClientCachingIsDisabled() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty("cache.client.enabled", "false");

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

        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(CacheFactory.DERIVATIVE_CACHE_CONFIG_KEY,
                "FilesystemCache");
        config.setProperty("FilesystemCache.pathname",
                cacheFolder.getAbsolutePath());
        config.setProperty(Cache.TTL_CONFIG_KEY, 10);
        config.setProperty(Cache.RESOLVE_FIRST_CONFIG_KEY, true);

        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier(IMAGE));
        ops.setOutputFormat(Format.JPG);

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

        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(CacheFactory.DERIVATIVE_CACHE_CONFIG_KEY,
                "FilesystemCache");
        config.setProperty("FilesystemCache.pathname",
                cacheFolder.getAbsolutePath());
        config.setProperty(Cache.TTL_CONFIG_KEY, 10);
        config.setProperty(Cache.RESOLVE_FIRST_CONFIG_KEY, true);

        OperationList ops = TestUtil.newOperationList();
        ops.setIdentifier(new Identifier(IMAGE));
        ops.setOutputFormat(Format.JPG);

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
    public void testEndpointDisabled() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
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

    /**
     * Tests that the Link header respects the <code>base_uri</code>
     * key in the configuration.
     */
    @Test
    public void testLinkHeader() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/default.jpg");

        config.setProperty(AbstractResource.BASE_URI_CONFIG_KEY, "");
        client.get();
        Header header = client.getResponse().getHeaders().getFirst("Link");
        assertTrue(header.getValue().startsWith("<http://localhost"));

        config.setProperty(AbstractResource.BASE_URI_CONFIG_KEY,
                "https://example.org/");
        client.get();
        header = client.getResponse().getHeaders().getFirst("Link");
        assertTrue(header.getValue().startsWith("<https://example.org/"));

        client.getRequest().getHeaders().add("X-IIIF-ID", "originalID");
        client.get();
        header = client.getResponse().getHeaders().getFirst("Link");
        assertTrue(header.getValue().contains("/originalID/"));
    }

    @Test
    public void testMaxPixels() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
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
    public void testMaxPixelsIgnoredWhenStreamingSource() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/default.jpg");
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

        final Configuration config = ConfigurationFactory.getInstance();
        config.setProperty("FilesystemResolver.BasicLookupStrategy.path_prefix",
                sourceDir.getAbsolutePath() + "/");
        config.setProperty(CacheFactory.DERIVATIVE_CACHE_CONFIG_KEY,
                "FilesystemCache");
        config.setProperty("FilesystemCache.pathname",
                cacheDir.getAbsolutePath());
        config.setProperty(Cache.TTL_CONFIG_KEY, 60);
        config.setProperty(Cache.RESOLVE_FIRST_CONFIG_KEY, true);
        config.setProperty(Cache.PURGE_MISSING_CONFIG_KEY, purgeMissing);

        try {
            OperationList ops = TestUtil.newOperationList();
            ops.setIdentifier(new Identifier(IMAGE));
            ops.setOutputFormat(Format.JPG);

            assertEquals(0, FileUtils.listFiles(cacheDir, null, true).size());

            // request an image to cache it
            getClientForUriPath("/" + IMAGE + "/full/full/0/default.jpg").get();
            getClientForUriPath("/" + IMAGE + "/info.json").get();

            // assert that it has been cached
            assertEquals(2, FileUtils.listFiles(cacheDir, null, true).size());
            DerivativeCache cache = CacheFactory.getDerivativeCache();
            assertNotNull(cache.newDerivativeImageInputStream(ops));
            assertNotNull(cache.getImageInfo(ops.getIdentifier()));

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
                assertNull(cache.newDerivativeImageInputStream(ops));
                assertNull(cache.getImageInfo(ops.getIdentifier()));
            } else {
                assertNotNull(cache.newDerivativeImageInputStream(ops));
                assertNotNull(cache.getImageInfo(ops.getIdentifier()));
            }
        } finally {
            FileUtils.deleteDirectory(sourceDir);
            FileUtils.deleteDirectory(cacheDir);
        }
    }

    @Test
    public void testGetRepresentationDisposition() throws Exception {
        Configuration config = ConfigurationFactory.getInstance();

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
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(ResolverFactory.STATIC_RESOLVER_CONFIG_KEY,
                "HttpResolver");
        config.setProperty("HttpResolver.lookup_strategy", "BasicLookupStrategy");
        config.setProperty("HttpResolver.BasicLookupStrategy.url_prefix",
                webServer.getHttpHost() + ":" + webServer.getHttpPort() + "/");
        config.setProperty("processor.jp2", "KakaduProcessor");
        config.setProperty(ProcessorFactory.FALLBACK_PROCESSOR_CONFIG_KEY,
                "KakaduProcessor");

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

}
