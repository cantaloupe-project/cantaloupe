package edu.illinois.library.cantaloupe.resource.iiif.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.restlet.data.CacheDirective;
import org.restlet.data.Header;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static edu.illinois.library.cantaloupe.test.Assert.PathAssert.*;
import static org.junit.Assert.*;

/**
 * Functional test of the non-IIIF features of InformationResource.
 */
public class InformationResourceTest extends ResourceTest {

    @Override
    protected ClientResource getClientForUriPath(String path) {
        return super.getClientForUriPath(RestletApplication.IIIF_2_PATH + path);
    }

    @Test
    public void testCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable()
            throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.CLIENT_CACHE_ENABLED, "true");
        config.setProperty(Key.CLIENT_CACHE_MAX_AGE, "1234");
        config.setProperty(Key.CLIENT_CACHE_SHARED_MAX_AGE, "4567");
        config.setProperty(Key.CLIENT_CACHE_PUBLIC, "false");
        config.setProperty(Key.CLIENT_CACHE_PRIVATE, "true");
        config.setProperty(Key.CLIENT_CACHE_NO_CACHE, "true");
        config.setProperty(Key.CLIENT_CACHE_NO_STORE, "true");
        config.setProperty(Key.CLIENT_CACHE_MUST_REVALIDATE, "false");
        config.setProperty(Key.CLIENT_CACHE_PROXY_REVALIDATE, "false");
        config.setProperty(Key.CLIENT_CACHE_NO_TRANSFORM, "true");

        Map<String, String> expectedDirectives = new HashMap<>();
        expectedDirectives.put("max-age", "1234");
        expectedDirectives.put("s-maxage", "4567");
        expectedDirectives.put("private", null);
        expectedDirectives.put("no-cache", null);
        expectedDirectives.put("no-store", null);
        expectedDirectives.put("proxy-revalidate", null);
        expectedDirectives.put("no-transform", null);

        ClientResource client = getClientForUriPath("/" + IMAGE + "/info.json");
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
        config.setProperty(Key.CLIENT_CACHE_ENABLED, "true");
        config.setProperty(Key.CLIENT_CACHE_MAX_AGE, "1234");
        config.setProperty(Key.CLIENT_CACHE_SHARED_MAX_AGE, "4567");
        config.setProperty(Key.CLIENT_CACHE_PUBLIC, "false");
        config.setProperty(Key.CLIENT_CACHE_PRIVATE, "true");
        config.setProperty(Key.CLIENT_CACHE_NO_CACHE, "true");
        config.setProperty(Key.CLIENT_CACHE_NO_STORE, "true");
        config.setProperty(Key.CLIENT_CACHE_MUST_REVALIDATE, "false");
        config.setProperty(Key.CLIENT_CACHE_PROXY_REVALIDATE, "true");
        config.setProperty(Key.CLIENT_CACHE_NO_TRANSFORM, "false");

        ClientResource client = getClientForUriPath("/bogus/info.json");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(0, client.getResponse().getCacheDirectives().size());
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
                "/" + IMAGE + "/info.json?cache=false");
        client.get();
        assertEquals(0, client.getResponse().getCacheDirectives().size());
    }

    @Test
    public void testCacheHeadersWhenClientCachingIsDisabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.CLIENT_CACHE_ENABLED, "false");

        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/default.jpg");
        client.get();
        assertEquals(0, client.getResponse().getCacheDirectives().size());
    }

    @Test
    public void testCacheWhenDerviativeCachingIsEnabled() throws Exception {
        File cacheFolder = TestUtil.getTempFolder();
        cacheFolder = new File(cacheFolder.getAbsolutePath() + "/cache");
        File infoCacheFolder = new File(cacheFolder.getAbsolutePath() + "/info");
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

        assertRecursiveFileCount(cacheFolder.toPath(), 0);

        // request an info to cache it
        getClientForUriPath("/" + IMAGE + "/info.json").get();

        Thread.sleep(1000);

        // assert that it has been cached
        assertRecursiveFileCount(infoCacheFolder.toPath(), 1);
    }

    @Test
    public void testCacheWhenDerviativeCachingIsEnabledButNegativeCacheQueryArgumentIsSupplied()
            throws Exception {
        File cacheFolder = TestUtil.getTempFolder();
        cacheFolder = new File(cacheFolder.getAbsolutePath() + "/cache");
        File infoCacheFolder = new File(cacheFolder.getAbsolutePath() + "/info");
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

        assertRecursiveFileCount(cacheFolder.toPath(), 0);

        // request an info
        getClientForUriPath("/" + IMAGE + "/info.json?cache=false").get();

        Thread.sleep(1000);

        // assert that it has NOT been cached
        assertFalse(infoCacheFolder.exists());
    }

    @Test
    public void testEndpointDisabled() throws Exception {
        Configuration config = Configuration.getInstance();
        ClientResource client = getClientForUriPath("/" + IMAGE + "/info.json");

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
            Identifier identifier = new Identifier(IMAGE);

            assertRecursiveFileCount(cacheDir.toPath(), 0);

            // Request an image to cache its info.
            getClientForUriPath("/" + IMAGE + "/info.json").get();

            // The info may write asynchronously, so wait.
            Thread.sleep(1000);

            // Assert that it's been cached.
            assertRecursiveFileCount(cacheDir.toPath(), 1);
            DerivativeCache cache = CacheFactory.getDerivativeCache();
            assertNotNull(cache.getImageInfo(identifier));

            // Delete the source image.
            assertTrue(sourceImage.delete());

            // Request the same image which is now cached but underlying is
            // gone.
            try {
                getClientForUriPath("/" + IMAGE + "/info.json").get();
            } catch (ResourceException e) {
                // noop
            }

            // Stuff may be deleted asynchronously, so wait.
            Thread.sleep(1000);

            if (purgeMissing) {
                assertNull(cache.getImageInfo(identifier));
            } else {
                assertNotNull(cache.getImageInfo(identifier));
            }
        } finally {
            FileUtils.deleteDirectory(sourceDir);
            FileUtils.deleteDirectory(cacheDir);
        }
    }

    @Test
    public void testNotFound() throws Exception {
        ClientResource client = getClientForUriPath("/invalid/info.json");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.CLIENT_ERROR_NOT_FOUND, client.getStatus());
        }
    }

    @Test
    public void testRedirectToInfoJSON() {
        ClientResource client = getClientForUriPath("/" + IMAGE);
        client.setFollowingRedirects(false);
        client.get();
        assertEquals(Status.REDIRECTION_SEE_OTHER,
                client.getResponse().getStatus());
        assertEquals("info.json",
                client.getResponse().getLocationRef().getLastSegment());
    }

    @Test
    public void testRedirectToInfoJSONWithDifferentPublicIdentifier() {
        ClientResource client = getClientForUriPath("/" + IMAGE);
        client.setFollowingRedirects(false);
        client.getRequest().getHeaders().
                add(AbstractResource.PUBLIC_IDENTIFIER_HEADER, "foxes");
        client.get();
        assertEquals(Status.REDIRECTION_SEE_OTHER,
                client.getResponse().getStatus());
        assertTrue(client.getResponse().getLocationRef().toString().endsWith("/foxes/info.json"));
    }

    /**
     * Checks that the server responds with HTTP 500 when a non-FileResolver is
     * used with a non-StreamProcessor.
     */
    @Test
    public void testResolverProcessorCompatibility() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.RESOLVER_STATIC, "HttpResolver");
        config.setProperty(Key.HTTPRESOLVER_LOOKUP_STRATEGY,
                "BasicLookupStrategy");
        config.setProperty(Key.HTTPRESOLVER_URL_PREFIX,
                webServer.getHTTPHost() + ":" + webServer.getHTTPPort() + "/");
        config.setProperty("processor.jp2", "KakaduProcessor");
        config.setProperty(Key.PROCESSOR_FALLBACK, "KakaduProcessor");

        ClientResource client = getClientForUriPath("/jp2/info.json");
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

        ClientResource client = getClientForUriPath("/subfolderCATSjpg/info.json");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getResponse().getStatus());
    }

    @Test
    public void testUnavailableSourceFormat() throws Exception {
        ClientResource client = getClientForUriPath("/text.txt/info.json");
        try {
            client.get();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(Status.SERVER_ERROR_INTERNAL, client.getStatus());
        }
    }

    @Test
    public void testUrisInJson() throws Exception {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/info.json");
        client.get();
        String json = client.getResponse().getEntityAsText();
        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String,Object> info =
                (Map<String,Object>) mapper.readValue(json, TreeMap.class);
        assertEquals("http://localhost:" + PORT +
                RestletApplication.IIIF_2_PATH + "/" + IMAGE, info.get("@id"));
    }

    @Test
    public void testUrisInJsonWithBaseUriOverride() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.BASE_URI, "http://example.org/");

        ClientResource client = getClientForUriPath("/" + IMAGE + "/info.json");
        client.get();
        String json = client.getResponse().getEntityAsText();
        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String,Object> info =
                (Map<String,Object>) mapper.readValue(json, TreeMap.class);
        assertEquals("http://example.org" +
                RestletApplication.IIIF_2_PATH + "/" + IMAGE, info.get("@id"));
    }

    @Test
    public void testUrisInJsonWithProxyHeaders() throws Exception {
        ClientResource client = getClientForUriPath("/" + IMAGE + "/info.json");
        client.getRequest().getHeaders().add("X-Forwarded-Proto", "HTTP");
        client.getRequest().getHeaders().add("X-Forwarded-Host", "example.org");
        client.getRequest().getHeaders().add("X-Forwarded-Port", "8080");
        client.getRequest().getHeaders().add("X-Forwarded-Path", "/cats/");
        client.getRequest().getHeaders().add(
                AbstractResource.PUBLIC_IDENTIFIER_HEADER, "originalID");
        client.get();
        String json = client.getResponse().getEntityAsText();
        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String,Object> info =
                (Map<String,Object>) mapper.readValue(json, TreeMap.class);
        assertEquals("http://example.org:8080/cats" +
                RestletApplication.IIIF_2_PATH + "/originalID", info.get("@id"));
    }

    @Test
    public void testBaseUriOverridesProxyHeaders() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.BASE_URI, "https://example.net/");

        ClientResource client = getClientForUriPath("/" + IMAGE + "/info.json");
        client.getRequest().getHeaders().add("X-Forwarded-Proto", "HTTP");
        client.getRequest().getHeaders().add("X-Forwarded-Host", "example.org");
        client.getRequest().getHeaders().add("X-Forwarded-Port", "8080");
        client.getRequest().getHeaders().add("X-Forwarded-Path", "/cats");
        client.get();
        String json = client.getResponse().getEntityAsText();
        ObjectMapper mapper = new ObjectMapper();
        @SuppressWarnings("unchecked")
        Map<String,Object> info =
                (Map<String,Object>) mapper.readValue(json, TreeMap.class);
        assertEquals("https://example.net" +
                RestletApplication.IIIF_2_PATH + "/" + IMAGE, info.get("@id"));
    }

    @Test
    public void testXPoweredByHeader() throws Exception {
        ClientResource resource = getClientForUriPath(
                "/" + IMAGE + "/info.json");
        resource.get();
        Header header = resource.getResponse().getHeaders().
                getFirst("X-Powered-By");
        assertEquals("Cantaloupe/Unknown", header.getValue());
    }

}
