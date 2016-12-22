package edu.illinois.library.cantaloupe.resource.iiif.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.WebApplication;
import edu.illinois.library.cantaloupe.cache.Cache;
import edu.illinois.library.cantaloupe.cache.CacheFactory;
import edu.illinois.library.cantaloupe.cache.DerivativeCache;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.ConfigurationFactory;
import edu.illinois.library.cantaloupe.operation.Format;
import edu.illinois.library.cantaloupe.operation.Identifier;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.restlet.data.CacheDirective;
import org.restlet.data.Status;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.Assert.*;

/**
 * Functional test of the non-IIIF features of InformationResource.
 */
public class InformationResourceTest extends ResourceTest {

    @Override
    protected ClientResource getClientForUriPath(String path) {
        return super.getClientForUriPath(WebApplication.IIIF_2_PATH + path);
    }

    @Test
    public void testCacheHeadersWhenClientCachingIsEnabled() throws Exception {
        webServer.start();
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(AbstractResource.CLIENT_CACHE_ENABLED_CONFIG_KEY, "true");
        config.setProperty(AbstractResource.CLIENT_CACHE_MAX_AGE_CONFIG_KEY, "1234");
        config.setProperty(AbstractResource.CLIENT_CACHE_SHARED_MAX_AGE_CONFIG_KEY, "4567");
        config.setProperty(AbstractResource.CLIENT_CACHE_PUBLIC_CONFIG_KEY, "false");
        config.setProperty(AbstractResource.CLIENT_CACHE_PRIVATE_CONFIG_KEY, "true");
        config.setProperty(AbstractResource.CLIENT_CACHE_NO_CACHE_CONFIG_KEY, "true");
        config.setProperty(AbstractResource.CLIENT_CACHE_NO_STORE_CONFIG_KEY, "true");
        config.setProperty(AbstractResource.CLIENT_CACHE_MUST_REVALIDATE_CONFIG_KEY, "false");
        config.setProperty(AbstractResource.CLIENT_CACHE_PROXY_REVALIDATE_CONFIG_KEY, "false");
        config.setProperty(AbstractResource.CLIENT_CACHE_NO_TRANSFORM_CONFIG_KEY, "true");

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
        webServer.start();
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
                "/" + IMAGE + "/info.json?cache=false");
        client.get();
        assertEquals(0, client.getResponse().getCacheDirectives().size());
    }

    @Test
    public void testCacheHeadersWhenClientCachingIsDisabled() throws Exception {
        webServer.start();
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(AbstractResource.CLIENT_CACHE_ENABLED_CONFIG_KEY, "false");

        ClientResource client = getClientForUriPath(
                "/" + IMAGE + "/full/full/0/default.jpg");
        client.get();
        assertEquals(0, client.getResponse().getCacheDirectives().size());
    }

    @Test
    public void testCacheWhenDerviativeCachingIsEnabled() throws Exception {
        webServer.start();
        File cacheFolder = TestUtil.getTempFolder();
        cacheFolder = new File(cacheFolder.getAbsolutePath() + "/cache");
        File infoCacheFolder = new File(cacheFolder.getAbsolutePath() + "/info");
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

        // request an info to cache it
        getClientForUriPath("/" + IMAGE + "/info.json").get();

        // assert that it has been cached
        assertEquals(1, FileUtils.listFiles(infoCacheFolder, null, true).size());
    }

    @Test
    public void testCacheWhenDerviativeCachingIsEnabledButNegativeCacheQueryArgumentIsSupplied()
            throws Exception {
        webServer.start();
        File cacheFolder = TestUtil.getTempFolder();
        cacheFolder = new File(cacheFolder.getAbsolutePath() + "/cache");
        File infoCacheFolder = new File(cacheFolder.getAbsolutePath() + "/info");
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

        // request an info
        getClientForUriPath("/" + IMAGE + "/info.json?cache=false").get();

        // assert that it has NOT been cached
        assertFalse(infoCacheFolder.exists());
    }

    @Test
    public void testEndpointDisabled() throws Exception {
        webServer.start();
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

        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(CacheFactory.DERIVATIVE_CACHE_CONFIG_KEY,
                "FilesystemCache");
        config.setProperty("FilesystemCache.pathname",
                cacheFolder.getAbsolutePath());
        config.setProperty(Cache.TTL_CONFIG_KEY, 10);
        config.setProperty(Cache.RESOLVE_FIRST_CONFIG_KEY, true);
        config.setProperty(Cache.PURGE_MISSING_CONFIG_KEY, purgeMissing);

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

            // request the same image which is now cached but underlying is gone
            try {
                getClientForUriPath("/" + IMAGE + "/info.json").get();
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

    @Test
    public void testNotFound() throws Exception {
        webServer.start();
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
     * used with a non-StreamProcessor.
     *
     * @throws Exception
     */
    @Test
    public void testResolverProcessorCompatibility() throws Exception {
        webServer.start();
        Configuration config = ConfigurationFactory.getInstance();
        resetConfiguration();
        config.setProperty("resolver.static", "HttpResolver");
        config.setProperty("HttpResolver.lookup_strategy", "BasicLookupStrategy");
        config.setProperty("HttpResolver.BasicLookupStrategy.url_prefix",
                webServer.getHttpHost() + ":" + webServer.getHttpPort() + "/");
        config.setProperty("processor.jp2", "KakaduProcessor");
        config.setProperty("processor.fallback", "KakaduProcessor");

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
        webServer.start();
        ConfigurationFactory.getInstance().setProperty("slash_substitute", "CATS");

        ClientResource client = getClientForUriPath("/subfolderCATSjpg/info.json");
        client.get();
        assertEquals(Status.SUCCESS_OK, client.getResponse().getStatus());
    }

    @Test
    public void testUnavailableSourceFormat() throws Exception {
        webServer.start();
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
        webServer.start();
        ClientResource client = getClientForUriPath("/" + IMAGE + "/info.json");
        client.get();
        String json = client.getResponse().getEntityAsText();
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> info =
                (Map<String,Object>) mapper.readValue(json, TreeMap.class);
        assertEquals("http://localhost:" + port +
                WebApplication.IIIF_2_PATH + "/" + IMAGE, info.get("@id"));
    }

    @Test
    public void testUrisInJsonWithBaseUriOverride() throws Exception {
        webServer.start();
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(AbstractResource.BASE_URI_CONFIG_KEY,
                "http://example.org/");

        ClientResource client = getClientForUriPath("/" + IMAGE + "/info.json");
        client.get();
        String json = client.getResponse().getEntityAsText();
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> info =
                (Map<String,Object>) mapper.readValue(json, TreeMap.class);
        assertEquals("http://example.org" +
                WebApplication.IIIF_2_PATH + "/" + IMAGE, info.get("@id"));
    }

    @Test
    public void testUrisInJsonWithProxyHeaders() throws Exception {
        webServer.start();
        ClientResource client = getClientForUriPath("/" + IMAGE + "/info.json");
        client.getRequest().getHeaders().add("X-Forwarded-Proto", "HTTP");
        client.getRequest().getHeaders().add("X-Forwarded-Host", "example.org");
        client.getRequest().getHeaders().add("X-Forwarded-Port", "8080");
        client.getRequest().getHeaders().add("X-Forwarded-Path", "/cats/");
        client.getRequest().getHeaders().add("X-IIIF-ID", "originalID");
        client.get();
        String json = client.getResponse().getEntityAsText();
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> info =
                (Map<String,Object>) mapper.readValue(json, TreeMap.class);
        assertEquals("http://example.org:8080/cats" +
                WebApplication.IIIF_2_PATH + "/originalID", info.get("@id"));
    }

    @Test
    public void testBaseUriOverridesProxyHeaders() throws Exception {
        webServer.start();
        Configuration config = ConfigurationFactory.getInstance();
        config.setProperty(AbstractResource.BASE_URI_CONFIG_KEY,
                "https://example.net/");

        ClientResource client = getClientForUriPath("/" + IMAGE + "/info.json");
        client.getRequest().getHeaders().add("X-Forwarded-Proto", "HTTP");
        client.getRequest().getHeaders().add("X-Forwarded-Host", "example.org");
        client.getRequest().getHeaders().add("X-Forwarded-Port", "8080");
        client.getRequest().getHeaders().add("X-Forwarded-Path", "/cats");
        client.get();
        String json = client.getResponse().getEntityAsText();
        ObjectMapper mapper = new ObjectMapper();
        Map<String,Object> info =
                (Map<String,Object>) mapper.readValue(json, TreeMap.class);
        assertEquals("https://example.net" +
                WebApplication.IIIF_2_PATH + "/" + IMAGE, info.get("@id"));
    }

}
