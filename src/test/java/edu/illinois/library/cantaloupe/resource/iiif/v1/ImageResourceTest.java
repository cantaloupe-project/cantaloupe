package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.StandaloneEntry;
import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.operation.Format;
import edu.illinois.library.cantaloupe.operation.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.resource.*;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.restlet.data.CacheDirective;
import org.restlet.data.ChallengeResponse;
import org.restlet.data.ChallengeScheme;
import org.restlet.data.Disposition;
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
        return super.getClientForUriPath(WebApplication.IIIF_1_PATH + path);
    }

    @Test
    public void testAuthorizationDelegateWithBooleanReturnValue() throws Exception {
        webServer.start();
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
        webServer.start();
        ClientResource client = getClientForUriPath("/redirect.jpg/full/full/0/native.jpg");
        client.setFollowingRedirects(false);
        client.get();
        assertEquals(Status.REDIRECTION_SEE_OTHER, client.getStatus());
        assertEquals("http://example.org/", client.getLocationRef().toString());
    }

    @Test
    public void testBasicAuthentication() throws Exception {
        webServer.start();
        final String username = "user";
        final String secret = "secret";
        StandaloneEntry.getWebServer().stop();
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(WebApplication.BASIC_AUTH_ENABLED_CONFIG_KEY, "true");
        config.setProperty(WebApplication.BASIC_AUTH_USERNAME_CONFIG_KEY, username);
        config.setProperty(WebApplication.BASIC_AUTH_SECRET_CONFIG_KEY, secret);
        StandaloneEntry.getWebServer().start();

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
    }

    @Test
    public void testCacheHeadersWhenClientCachingEnabled() throws Exception {
        webServer.start();
        Configuration config = ConfigurationFactory.getInstance();
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
    public void testCacheHeadersWhenClientCachingDisabled() throws Exception {
        webServer.start();
        Configuration config = ConfigurationFactory.getInstance();
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
                "/" + IMAGE + "/full/full/0/native.jpg");
        client.get();
        assertEquals(0, client.getResponse().getCacheDirectives().size());
    }

    @Test
    public void testCacheWhenDerviativeCachingIsEnabled() throws Exception {
        webServer.start();
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
        getClientForUriPath("/" + IMAGE + "/full/full/0/native.png").get();

        // assert that it has been cached
        assertEquals(1, FileUtils.listFiles(imageCacheFolder, null, true).size());
    }

    @Test
    public void testCacheWhenDerviativeCachingIsEnabledButNegativeCacheQueryArgumentIsSupplied()
            throws Exception {
        webServer.start();
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
        getClientForUriPath("/" + IMAGE + "/full/full/0/native.png?cache=false").get();

        // assert that it has NOT been cached
        assertFalse(imageCacheFolder.exists());
    }

    @Test
    public void testContentDispositionHeader() throws Exception {
        webServer.start();
        // no header
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/native.jpg");
        client.get();
        assertNull(client.getResponseEntity().getDisposition());

        // inline
        Configuration config = ConfigurationFactory.getInstance();
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
        webServer.start();
        Configuration config = ConfigurationFactory.getInstance();
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
        webServer.start();
        Configuration config = ConfigurationFactory.getInstance();
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
        webServer.start();
        Configuration config = ConfigurationFactory.getInstance();
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/native.jpg");
        config.setProperty("max_pixels", 1000);
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getStatus());
    }

    @Test
    public void testNotFound() throws Exception {
        webServer.start();
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
        webServer.start();
        File cacheFolder = TestUtil.getTempFolder();
        cacheFolder = new File(cacheFolder.getAbsolutePath() + "/cache");
        if (cacheFolder.exists()) {
            FileUtils.cleanDirectory(cacheFolder);
        } else {
            cacheFolder.mkdir();
        }

        final Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(CacheFactory.DERIVATIVE_CACHE_CONFIG_KEY,
                "FilesystemCache");
        config.setProperty("FilesystemCache.pathname",
                cacheFolder.getAbsolutePath());
        config.setProperty(Cache.TTL_CONFIG_KEY, 10);
        config.setProperty(Cache.RESOLVE_FIRST_CONFIG_KEY, true);
        config.setProperty(Cache.PURGE_MISSING_CONFIG_KEY, purgeMissing);

        File tempImage = File.createTempFile("temp", ".jpg");
        tempImage.delete();
        File image = TestUtil.getImage(IMAGE);
        try {
            OperationList ops = TestUtil.newOperationList();
            ops.setIdentifier(new Identifier(IMAGE));
            ops.setOutputFormat(Format.JPG);

            assertEquals(0, FileUtils.listFiles(cacheFolder, null, true).size());

            // request an image to cache it
            getClientForUriPath("/" + IMAGE + "/full/full/0/native.jpg").get();
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
            assertFalse(image.exists());

            // request the same image which is now cached but underlying is 404
            try {
                getClientForUriPath("/" + IMAGE + "/full/full/0/native.jpg").get();
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
     * Checks that the server responds with HTTP 500 when a non-FileResolver is
     * used with a non-StreamProcessor.
     *
     * @throws Exception
     */
    @Test
    public void testResolverProcessorCompatibility() throws Exception {
        webServer.start();
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty("resolver.static", "HttpResolver");
        config.setProperty("HttpResolver.lookup_strategy", "BasicLookupStrategy");
        config.setProperty("HttpResolver.BasicLookupStrategy.url_prefix",
                webServer.getHttpHost() + ":" + webServer.getHttpPort() + "/");
        config.setProperty("processor.jp2", "KakaduProcessor");
        config.setProperty("processor.fallback", "KakaduProcessor");

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
        webServer.start();
        ConfigurationFactory.getInstance().setProperty("slash_substitute", "CATS");

        ClientResource client = getClientForUriPath("/subfolderCATSjpg/full/full/0/native.jpg");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getResponse().getStatus());
    }

    @Test
    public void testUnavailableSourceFormat() throws Exception {
        webServer.start();
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
        webServer.start();
        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/native.bogus");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_BAD_REQUEST, client.getStatus());
        }
    }

}
