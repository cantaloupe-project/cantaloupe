package edu.illinois.library.cantaloupe.resource.iiif.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import edu.illinois.library.cantaloupe.resource.iiif.InformationResourceTester;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.SystemUtils;
import org.eclipse.jetty.client.api.ContentResponse;
import org.junit.Test;

import java.net.URI;

import static edu.illinois.library.cantaloupe.test.Assert.HTTPAssert.*;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/**
 * Functional test of the non-IIIF features of InformationResource.
 */
public class InformationResourceTest extends ResourceTest {

    private static final String IMAGE = "jpg-rgb-64x56x8-baseline.jpg";

    private InformationResourceTester tester = new InformationResourceTester();

    @Override
    protected String getEndpointPath() {
        return RestletApplication.IIIF_2_PATH;
    }

    @Test
    public void testBasicAuthenticationWithNoCredentials() throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testBasicAuthWithNoCredentials(appServer, uri);
    }

    @Test
    public void testBasicAuthenticationWithInvalidCredentials()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testBasicAuthWithInvalidCredentials(appServer, uri);
    }

    @Test
    public void testBasicAuthenticationWithValidCredentials()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testBasicAuthWithValidCredentials(appServer, uri);
    }

    @Test
    public void testCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable(uri);
    }

    @Test
    public void testCacheHeadersWhenClientCachingIsEnabledAndResponseIsNotCacheable()
            throws Exception {
        URI uri = getHTTPURI("/bogus/info.json");
        tester.testCacheHeadersWhenClientCachingIsEnabledAndResponseIsNotCacheable(uri);
    }

    /**
     * Tests that there is no Cache-Control header returned when
     * cache.httpClient.enabled = true but a cache=false argument is present in the
     * URL query.
     */
    @Test
    public void testCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json?cache=false");
        tester.testCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL(uri);
    }

    @Test
    public void testCacheHeadersWhenClientCachingIsDisabled() throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheHeadersWhenClientCachingIsDisabled(uri);
    }

    @Test
    public void testCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json?cache=false");
        tester.testCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied(uri);
    }

    @Test
    public void testCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstEnabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    public void testCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstDisabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstDisabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    public void testCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstEnabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    public void testCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstDisabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstDisabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    public void testCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstEnabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    public void testCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstDisabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstDisabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    public void testCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstEnabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    public void testCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstDisabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstDisabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    public void testEndpointEnabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, true);

        assertStatus(200, getHTTPURI("/" + IMAGE + "/info.json"));
    }

    @Test
    public void testEndpointDisabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, false);

        assertStatus(403, getHTTPURI("/" + IMAGE + "/info.json"));
    }

    @Test
    public void testHTTP2() throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testHTTP2(uri);
    }

    @Test
    public void testHTTPS1_1() throws Exception {
        URI uri = getHTTPSURI("/" + IMAGE + "/info.json");
        tester.testHTTPS1_1(uri);
    }

    @Test
    public void testHTTPS2() throws Exception {
        assumeTrue(SystemUtils.isALPNAvailable());
        URI uri = getHTTPSURI("/" + IMAGE + "/info.json");
        tester.testHTTPS2(uri);
    }

    @Test
    public void testNotFound() {
        URI uri = getHTTPURI("/invalid/info.json");
        tester.testNotFound(uri);
    }

    @Test
    public void testPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse(uri);
    }

    @Test
    public void testPurgeFromCacheWhenSourceIsMissingAndOptionIsTrue()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testPurgeFromCacheWhenSourceIsMissingAndOptionIsTrue(uri);
    }

    @Test
    public void testRedirectToInfoJSON() {
        URI fromURI = getHTTPURI("/" + IMAGE);
        URI toURI = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testRedirectToInfoJSON(fromURI, toURI);
    }

    @Test
    public void testRedirectToInfoJSONWithDifferentPublicIdentifier()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE);
        tester.testRedirectToInfoJSONWithDifferentPublicIdentifier(uri);
    }

    /**
     * Checks that the server responds with HTTP 500 when a non-FileResolver is
     * used with a non-StreamProcessor.
     */
    @Test
    public void testResolverProcessorCompatibility() throws Exception {
        URI uri = getHTTPURI("/jp2/info.json");
        tester.testResolverProcessorCompatibility(
                uri,
                appServer.getHTTPHost(),
                appServer.getHTTPPort());
    }

    @Test
    public void testSlashSubstitution() throws Exception {
        URI uri = getHTTPURI("/subfolderCATSjpg/info.json");
        tester.testSlashSubstitution(uri);
    }

    @Test
    public void testUnavailableSourceFormat() throws Exception {
        URI uri = getHTTPURI("/text.txt/info.json");
        tester.testUnavailableSourceFormat(uri);
    }

    @Test
    public void testURIsInJSON() throws Exception {
        client = newClient("/" + IMAGE + "/info.json");
        ContentResponse response = client.send();

        String json = response.getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        ImageInfo<?, ?> info = mapper.readValue(json, ImageInfo.class);
        assertEquals("http://localhost:" + HTTP_PORT +
                RestletApplication.IIIF_2_PATH + "/" + IMAGE, info.get("@id"));
    }

    @Test
    public void testURIsInJSONWithBaseURIOverride() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.BASE_URI, "http://example.org/");

        client = newClient("/" + IMAGE + "/info.json");
        ContentResponse response = client.send();

        String json = response.getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        ImageInfo<?, ?> info = mapper.readValue(json, ImageInfo.class);
        assertEquals("http://example.org" +
                RestletApplication.IIIF_2_PATH + "/" + IMAGE, info.get("@id"));
    }

    @Test
    public void testURIsInJSONWithProxyHeaders() throws Exception {
        client = newClient("/" + IMAGE + "/info.json");
        client.getHeaders().put("X-Forwarded-Proto", "HTTP");
        client.getHeaders().put("X-Forwarded-Host", "example.org");
        client.getHeaders().put("X-Forwarded-Port", "8080");
        client.getHeaders().put("X-Forwarded-Path", "/cats");
        client.getHeaders().put(
                AbstractResource.PUBLIC_IDENTIFIER_HEADER, "originalID");
        ContentResponse response = client.send();

        String json = response.getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        ImageInfo<?, ?> info = mapper.readValue(json, ImageInfo.class);
        assertEquals("http://example.org:8080/cats" +
                RestletApplication.IIIF_2_PATH + "/originalID", info.get("@id"));
    }

    @Test
    public void testBaseURIOverridesProxyHeaders() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.BASE_URI, "https://example.net/");

        client = newClient("/" + IMAGE + "/info.json");
        client.getHeaders().put("X-Forwarded-Proto", "HTTP");
        client.getHeaders().put("X-Forwarded-Host", "example.org");
        client.getHeaders().put("X-Forwarded-Port", "8080");
        client.getHeaders().put("X-Forwarded-Path", "/cats");
        ContentResponse response = client.send();

        String json = response.getContentAsString();
        ObjectMapper mapper = new ObjectMapper();
        ImageInfo<?, ?> info = mapper.readValue(json, ImageInfo.class);
        assertEquals("https://example.net" +
                RestletApplication.IIIF_2_PATH + "/" + IMAGE, info.get("@id"));
    }

    @Test
    public void testXPoweredByHeader() throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testXPoweredByHeader(uri);
    }

}
