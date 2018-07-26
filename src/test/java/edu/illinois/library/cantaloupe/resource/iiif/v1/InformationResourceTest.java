package edu.illinois.library.cantaloupe.resource.iiif.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import edu.illinois.library.cantaloupe.resource.Route;
import edu.illinois.library.cantaloupe.resource.iiif.InformationResourceTester;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.SystemUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

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
        return Route.IIIF_1_PATH;
    }

    @Test
    public void testGETBasicAuthenticationWithNoCredentials() throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testBasicAuthWithNoCredentials(appServer, uri);
    }

    @Test
    public void testGETBasicAuthenticationWithInvalidCredentials()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testBasicAuthWithInvalidCredentials(appServer, uri);
    }

    @Test
    public void testGETBasicAuthenticationWithValidCredentials()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testBasicAuthWithValidCredentials(appServer, uri);
    }

    @Test
    public void testGETCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable(uri);
    }

    @Test
    public void testGETCacheHeadersWhenClientCachingIsEnabledAndResponseIsNotCacheable()
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
    public void testGETCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json?cache=false");
        tester.testCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL(uri);
    }

    @Test
    public void testGETCacheHeadersWhenClientCachingIsDisabled() throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheHeadersWhenClientCachingIsDisabled(uri);
    }

    @Test
    public void testGETCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json?cache=false");
        tester.testCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied(uri);
    }

    @Test
    public void testGETCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstEnabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    public void testGETCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstDisabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstDisabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    public void testGETCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstEnabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    public void testGETCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstDisabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstDisabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    public void testGETCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstEnabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    public void testGETCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstDisabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstDisabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    public void testGETCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstEnabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    public void testGETCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstDisabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstDisabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    public void testGETEndpointEnabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.IIIF_1_ENDPOINT_ENABLED, true);

        assertStatus(200, getHTTPURI("/" + IMAGE + "/info.json"));
    }

    @Test
    public void testGETEndpointDisabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.IIIF_1_ENDPOINT_ENABLED, false);

        assertStatus(403, getHTTPURI("/" + IMAGE + "/info.json"));
    }

    @Test
    public void testGETHTTP2() throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testHTTP2(uri);
    }

    @Test
    public void testGETHTTPS1_1() throws Exception {
        URI uri = getHTTPSURI("/" + IMAGE + "/info.json");
        tester.testHTTPS1_1(uri);
    }

    @Test
    public void testGETHTTPS2() throws Exception {
        assumeTrue(SystemUtils.isALPNAvailable());
        URI uri = getHTTPSURI("/" + IMAGE + "/info.json");
        tester.testHTTPS2(uri);
    }

    @Test
    public void testGETNotFound() {
        URI uri = getHTTPURI("/invalid/info.json");
        tester.testNotFound(uri);
    }

    @Test
    public void testGETPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse(uri);
    }

    @Test
    public void testGETPurgeFromCacheWhenSourceIsMissingAndOptionIsTrue()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testPurgeFromCacheWhenSourceIsMissingAndOptionIsTrue(uri);
    }

    @Test
    public void testGETRecoveryFromDerivativeCacheNewDerivativeImageInputStreamException()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testRecoveryFromDerivativeCacheNewDerivativeImageInputStreamException(uri);
    }

    @Test
    public void testGETRecoveryFromDerivativeCacheNewDerivativeImageOutputStreamException()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testRecoveryFromDerivativeCacheNewDerivativeImageOutputStreamException(uri);
    }

    @Test
    public void testGETResolverCheckAccessNotCalledWithSourceCacheHit()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testSourceCheckAccessNotCalledWithSourceCacheHit(new Identifier(IMAGE), uri);
    }

    @Test
    public void testGETResolverGetSourceFormatNotCalledWithSourceCacheHit()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testSourceGetFormatNotCalledWithSourceCacheHit(new Identifier(IMAGE), uri);
    }

    /**
     * Checks that the server responds with HTTP 500 when a non-FileSource is
     * used with a non-StreamProcessor.
     */
    @Test
    public void testGETResolverProcessorCompatibility() {
        URI uri = getHTTPURI("/jp2/info.json");
        tester.testSourceProcessorCompatibility(
                uri,
                appServer.getHTTPHost(),
                appServer.getHTTPPort());
    }

    @Test
    public void testGETSlashSubstitution() {
        URI uri = getHTTPURI("/subfolderCATSjpg/info.json");
        tester.testSlashSubstitution(uri);
    }

    @Test
    public void testGETUnavailableSourceFormat() {
        URI uri = getHTTPURI("/text.txt/info.json");
        tester.testUnavailableSourceFormat(uri);
    }

    @Test
    public void testGETURIsInJSON() throws Exception {
        client = newClient("/" + IMAGE + "/info.json");
        Response response = client.send();

        String json = response.getBodyAsString();
        ObjectMapper mapper = new ObjectMapper();
        ImageInfo info = mapper.readValue(json, ImageInfo.class);
        assertEquals("http://localhost:" + HTTP_PORT +
                Route.IIIF_1_PATH + "/" + IMAGE, info.id);
    }

    @Test
    public void testGETURIsInJSONWithBaseURIOverride() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.BASE_URI, "http://example.org/");

        client = newClient("/" + IMAGE + "/info.json");
        Response response = client.send();

        String json = response.getBodyAsString();
        ObjectMapper mapper = new ObjectMapper();
        ImageInfo info = mapper.readValue(json, ImageInfo.class);
        assertEquals("http://example.org" + Route.IIIF_1_PATH + "/" + IMAGE,
                info.id);
    }

    @Test
    public void testGETURIsInJSONWithSlashSubstitution() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SLASH_SUBSTITUTE, "CATS");

        final String path = "/subfolderCATSjpg";
        client = newClient(path + "/info.json");
        Response response = client.send();

        String json = response.getBodyAsString();
        ObjectMapper mapper = new ObjectMapper();
        ImageInfo info = mapper.readValue(json, ImageInfo.class);
        assertEquals("http://localhost:" + HTTP_PORT + Route.IIIF_1_PATH + path,
                info.id);
    }

    @Test
    public void testGETURIsInJSONWithEncodedCharacters() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SLASH_SUBSTITUTE, ":");

        final String path = "/subfolder%3Ajpg";
        client = newClient(path + "/info.json");
        Response response = client.send();

        String json = response.getBodyAsString();
        ObjectMapper mapper = new ObjectMapper();
        ImageInfo info = mapper.readValue(json, ImageInfo.class);
        assertEquals("http://localhost:" + HTTP_PORT +
                Route.IIIF_1_PATH + path, info.id);
    }

    @Test
    public void testGETURIsInJSONWithProxyHeaders() throws Exception {
        client = newClient("/" + IMAGE + "/info.json");
        client.getHeaders().set("X-Forwarded-Proto", "HTTP");
        client.getHeaders().set("X-Forwarded-Host", "example.org");
        client.getHeaders().set("X-Forwarded-Port", "8080");
        client.getHeaders().set("X-Forwarded-Path", "/cats");
        client.getHeaders().set(
                AbstractResource.PUBLIC_IDENTIFIER_HEADER, "originalID");
        Response response = client.send();

        String json = response.getBodyAsString();
        ObjectMapper mapper = new ObjectMapper();
        ImageInfo info = mapper.readValue(json, ImageInfo.class);
        assertEquals("http://example.org:8080/cats" +
                Route.IIIF_1_PATH + "/originalID", info.id);
    }

    @Test
    public void testGETBaseURIOverridesProxyHeaders() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.BASE_URI, "https://example.net/");

        client = newClient("/" + IMAGE + "/info.json");
        client.getHeaders().set("X-Forwarded-Proto", "HTTP");
        client.getHeaders().set("X-Forwarded-Host", "example.org");
        client.getHeaders().set("X-Forwarded-Port", "8080");
        client.getHeaders().set("X-Forwarded-Path", "/cats");
        Response response = client.send();

        String json = response.getBodyAsString();
        ObjectMapper mapper = new ObjectMapper();
        ImageInfo info = mapper.readValue(json, ImageInfo.class);
        assertEquals("https://example.net" +
                Route.IIIF_1_PATH + "/" + IMAGE, info.id);
    }

    /**
     * Tests the default response headers. Individual headers may be tested
     * more thoroughly elsewhere.
     */
    @Test
    public void testGETResponseHeaders() throws Exception {
        client = newClient("/" + IMAGE + "/info.json");
        Response response = client.send();
        Headers headers = response.getHeaders();
        assertEquals(8, headers.size());

        // Access-Control-Allow-Origin
        assertEquals("*", headers.getFirstValue("Access-Control-Allow-Origin"));
        // Content-Length
        assertNotNull(headers.getFirstValue("Content-Length"));
        // Content-Type
        assertEquals("application/json;charset=UTF-8",
                headers.getFirstValue("Content-Type"));
        // Date
        assertNotNull(headers.getFirstValue("Date"));
        // Link
        assertTrue(headers.getFirstValue("Link").contains("://"));
        // Server
        assertNotNull(headers.getFirstValue("Server"));
        // Vary
        List<String> parts = Arrays.asList(StringUtils.split(headers.getFirstValue("Vary"), ", "));
        assertEquals(5, parts.size());
        assertTrue(parts.contains("Accept"));
        assertTrue(parts.contains("Accept-Charset"));
        assertTrue(parts.contains("Accept-Encoding"));
        assertTrue(parts.contains("Accept-Language"));
        assertTrue(parts.contains("Origin"));
        // X-Powered-By
        assertEquals(Application.getName() + "/" + Application.getVersion(),
                headers.getFirstValue("X-Powered-By"));
    }

    @Test
    public void testOPTIONSWhenEnabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.IIIF_1_ENDPOINT_ENABLED, true);

        client = newClient("/" + IMAGE + "/info.json");
        client.setMethod(Method.OPTIONS);
        Response response = client.send();
        assertEquals(204, response.getStatus());

        Headers headers = response.getHeaders();
        List<String> methods =
                Arrays.asList(StringUtils.split(headers.getFirstValue("Allow"), ", "));
        assertEquals(2, methods.size());
        assertTrue(methods.contains("GET"));
        assertTrue(methods.contains("OPTIONS"));
    }

    @Test
    public void testOPTIONSWhenDisabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.IIIF_1_ENDPOINT_ENABLED, false);
        try {
            client = newClient("/" + IMAGE + "/info.json");
            client.setMethod(Method.OPTIONS);
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(403, e.getStatusCode());
        }
    }

}
