package edu.illinois.library.cantaloupe.resource.iiif.v3;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.MetaIdentifier;
import edu.illinois.library.cantaloupe.image.StandardMetaIdentifierTransformer;
import edu.illinois.library.cantaloupe.resource.AbstractResource;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import edu.illinois.library.cantaloupe.resource.Route;
import edu.illinois.library.cantaloupe.resource.iiif.InformationResourceTester;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.utils.SystemUtils;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static edu.illinois.library.cantaloupe.test.Assert.HTTPAssert.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Functional test of the features of InformationResource.
 */
public class InformationResourceTest extends ResourceTest {

    private static final String IMAGE = "jpg-rgb-64x56x8-baseline.jpg";

    private final InformationResourceTester tester =
            new InformationResourceTester();

    @Override
    protected String getEndpointPath() {
        return Route.IIIF_3_PATH;
    }

    @Test
    void testGETAuthorizationWhenAuthorized() {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testAuthorizationWhenAuthorized(uri);
    }

    @Test
    void testGETAuthorizationWhenUnauthorized() {
        URI uri = getHTTPURI("/unauthorized.jpg/info.json");
        // This may vary depending on the return value of a delegate method,
        // but the test delegate script returns 401.
        assertStatus(401, uri);
        assertRepresentationEquals("{\"@context\":\"http://iiif.io/api/image/3/context.json\","+
                "\"id\":\"" + uri.toString().replace("/info.json", "") + "\"," +
                "\"type\":\"ImageService3\"," +
                "\"protocol\":\"http://iiif.io/api/image\"," +
                "\"profile\":\"level2\"," +
                "\"status\":401," +
                "\"message\":\"Unauthorized\"," +
                "\"attribution\":\"Copyright My Great Organization. All rights reserved.\"," +
                "\"license\":\"http://example.org/license.html\"," +
                "\"service\":{" +
                    "\"@context\":\"http://iiif.io/api/annex/services/physdim/1/context.json\"," +
                    "\"profile\":\"http://iiif.io/api/annex/services/physdim\"," +
                    "\"physicalScale\":0.0025," +
                    "\"physicalUnits\":\"in\"}" +
                "}", uri);
    }

    @Test
    void testGETAuthorizationWhenForbidden() {
        URI uri = getHTTPURI("/forbidden.jpg/info.json");
        tester.testAuthorizationWhenForbidden(uri);
    }

    @Test
    void testGETAuthorizationWhenNotAuthorizedWhenAccessingCachedResource()
            throws Exception {
        URI uri = getHTTPURI("/forbidden.jpg/info.json");
        tester.testAuthorizationWhenNotAuthorizedWhenAccessingCachedResource(uri);
    }

    @Test
    void testGETAuthorizationWhenScaleConstraining() throws Exception {
        URI uri = getHTTPURI("/reduce.jpg/info.json");
        tester.testAuthorizationWhenScaleConstraining(uri);
    }

    @Test
    void testGETCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable(uri);
    }

    @Test
    void testGETCacheHeadersWhenClientCachingIsEnabledAndResponseIsNotCacheable()
            throws Exception {
        URI uri = getHTTPURI("/bogus/info.json");
        tester.testCacheHeadersWhenClientCachingIsEnabledAndResponseIsNotCacheable(uri);
    }

    /**
     * Tests that there is no Cache-Control header returned when
     * cache.client.enabled = true but a cache=false argument is present in the
     * URL query.
     */
    @Test
    void testGETCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json?cache=false");
        tester.testCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL(uri);
    }

    @Test
    void testGETCacheHeadersWhenClientCachingIsDisabled() throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheHeadersWhenClientCachingIsDisabled(uri);
    }

    @Test
    void testGETCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied1()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json?cache=nocache");
        tester.testCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied(uri);
    }

    @Test
    void testGETCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied2()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json?cache=false");
        tester.testCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied(uri);
    }

    @Test
    void testGETCachingWhenCachesAreEnabledAndRecacheQueryArgumentIsSupplied()
            throws Exception {
        assumeFalse(SystemUtils.IS_OS_WINDOWS); // TODO: why does this fail in Windows?

        URI uri = getHTTPURI("/" + IMAGE + "/info.json?cache=recache");
        tester.testCachingWhenCachesAreEnabledAndRecacheQueryArgumentIsSupplied(uri);
    }

    @Test
    void testGETCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstEnabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    void testGETCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstDisabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstDisabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    void testGETCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstEnabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    void testGETCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstDisabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstDisabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    void testGETCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstEnabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    void testGETCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstDisabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstDisabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    void testGETCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstEnabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    void testGETCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstDisabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstDisabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    void testGETEndpointEnabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.IIIF_3_ENDPOINT_ENABLED, true);

        assertStatus(200, getHTTPURI("/" + IMAGE + "/info.json"));
    }

    @Test
    void testGETEndpointDisabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.IIIF_3_ENDPOINT_ENABLED, false);

        assertStatus(403, getHTTPURI("/" + IMAGE + "/info.json"));
    }

    @Test
    void testGETWithForwardSlashInIdentifier() {
        URI uri = getHTTPURI("/subfolder%2Fjpg/info.json");
        tester.testForwardSlashInIdentifier(uri);
    }

    @Test
    void testGETWithBackslashInIdentifier() {
        URI uri = getHTTPURI("/subfolder%5Cjpg/info.json");
        tester.testBackslashInIdentifier(uri);
    }

    @Test
    void testGETWithIllegalCharactersInIdentifier() {
        String uri = getHTTPURIString("/[bogus]/info.json");
        tester.testIllegalCharactersInIdentifier(uri);
    }

    @Test
    void testGETHTTP2() throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testHTTP2(uri);
    }

    @Test
    void testGETHTTPS1_1() throws Exception {
        URI uri = getHTTPSURI("/" + IMAGE + "/info.json");
        tester.testHTTPS1_1(uri);
    }

    @Test
    void testGETHTTPS2() throws Exception {
        URI uri = getHTTPSURI("/" + IMAGE + "/info.json");
        tester.testHTTPS2(uri);
    }

    @Test
    void testGETForbidden() {
        URI uri = getHTTPURI("/forbidden/info.json");
        tester.testForbidden(uri);
    }

    @Test
    void testGETNotFound() {
        URI uri = getHTTPURI("/invalid/info.json");
        tester.testNotFound(uri);
    }

    @Test
    void testGETWithPageNumberInMetaIdentifier() {
        final String image = "pdf-multipage.pdf";
        URI uri1 = getHTTPURI("/" + image + "/info.json");
        URI uri2 = getHTTPURI("/" + image + ";2/info.json");
        assertRepresentationsNotSame(uri1, uri2);
    }

    @Test
    void testGETWithPageNumberInQuery() {
        final String image = "pdf-multipage.pdf";
        URI uri1 = getHTTPURI("/" + image + "/info.json");
        URI uri2 = getHTTPURI("/" + image + "/info.json?page=2");
        assertRepresentationsNotSame(uri1, uri2);
    }

    @Test
    void testGETPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse(uri);
    }

    @Test
    void testGETPurgeFromCacheWhenSourceIsMissingAndOptionIsTrue()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testPurgeFromCacheWhenSourceIsMissingAndOptionIsTrue(uri);
    }

    @Test
    void testGETRecoveryFromDerivativeCacheNewDerivativeImageInputStreamException()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testRecoveryFromDerivativeCacheNewDerivativeImageInputStreamException(uri);
    }

    @Test
    void testGETRecoveryFromDerivativeCacheNewDerivativeImageOutputStreamException()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testRecoveryFromDerivativeCacheNewDerivativeImageOutputStreamException(uri);
    }

    @Test
    void testGETRecoveryFromIncorrectSourceFormat() throws Exception {
        URI uri = getHTTPURI("/jpg-incorrect-extension.png/info.json");
        tester.testRecoveryFromIncorrectSourceFormat(uri);
    }

    /**
     * Tests that a scale constraint of {@literal 1:1} is redirected to no
     * scale constraint.
     */
    @Test
    void testGETRedirectToNormalizedScaleConstraint1() {
        MetaIdentifier metaIdentifier = MetaIdentifier.builder()
                .withIdentifier(IMAGE)
                .withScaleConstraint(1, 1)
                .build();
        String metaIdentifierString = new StandardMetaIdentifierTransformer()
                .serialize(metaIdentifier, false);

        URI fromURI = getHTTPURI("/" + metaIdentifierString + "/info.json");
        URI toURI   = getHTTPURI("/" + IMAGE + "/info.json");
        assertRedirect(fromURI, toURI, 301);
    }

    /**
     * Tests that a scale constraint of {@literal 2:2} is redirected to no
     * scale constraint.
     */
    @Test
    void testGETRedirectToNormalizedScaleConstraint2() {
        MetaIdentifier metaIdentifier = MetaIdentifier.builder()
                .withIdentifier(IMAGE)
                .withScaleConstraint(2, 2)
                .build();
        String metaIdentifierString = new StandardMetaIdentifierTransformer()
                .serialize(metaIdentifier, false);

        URI fromURI = getHTTPURI("/" + metaIdentifierString + "/info.json");
        URI toURI   = getHTTPURI("/" + IMAGE + "/info.json");
        assertRedirect(fromURI, toURI, 301);
    }

    /**
     * Tests that a scale constraint of {@literal 2:4} is redirected to
     * {@literal 1:2}.
     */
    @Test
    void testGETRedirectToNormalizedScaleConstraint3() {
        MetaIdentifier.Builder builder = MetaIdentifier.builder()
                .withIdentifier(IMAGE);
        // create the "from" URI
        MetaIdentifier metaIdentifier = builder
                .withScaleConstraint(2, 4)
                .build();
        String metaIdentifierString =
                new StandardMetaIdentifierTransformer().serialize(metaIdentifier);
        URI fromURI = getHTTPURI("/" + metaIdentifierString + "/info.json");

        // create the "to" URI
        metaIdentifier = builder.withScaleConstraint(1, 2).build();
        metaIdentifierString =
                new StandardMetaIdentifierTransformer().serialize(metaIdentifier);
        URI toURI = getHTTPURI("/" + metaIdentifierString + "/info.json");

        assertRedirect(fromURI, toURI, 301);
    }

    @Test
    void testGETScaleConstraintIsRespected() throws Exception {
        client = newClient("/" + IMAGE + ";1:2/info.json");
        Response response = client.send();

        String json = response.getBodyAsString();
        ObjectMapper mapper = new ObjectMapper();
        Information<?, ?> info = mapper.readValue(json, Information.class);
        assertEquals(32, info.get("width"));
        assertEquals(28, info.get("height"));
    }

    @Test
    void testGETSourceCheckAccessNotCalledWithSourceCacheHit()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testSourceCheckAccessNotCalledWithSourceCacheHit(new Identifier(IMAGE), uri);
    }

    @Test
    void testGETSourceGetSourceFormatNotCalledWithSourceCacheHit()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testSourceGetFormatNotCalledWithSourceCacheHit(new Identifier(IMAGE), uri);
    }

    /**
     * Checks that the server responds with HTTP 500 when a non-FileSource is
     * used with a non-StreamProcessor.
     */
    @Test
    void testGETSourceProcessorCompatibility() {
        URI uri = getHTTPURI("/jp2/info.json");
        tester.testSourceProcessorCompatibility(
                uri,
                appServer.getHTTPHost(),
                appServer.getHTTPPort());
    }

    @Test
    void testGETSlashSubstitution() {
        URI uri = getHTTPURI("/subfolderCATSjpg/info.json");
        tester.testSlashSubstitution(uri);
    }

    @Test
    void testGETUnavailableSourceFormat() {
        URI uri = getHTTPURI("/text.txt/info.json");
        tester.testUnavailableSourceFormat(uri);
    }

    @Test
    void testGETURIsInJSON() throws Exception {
        client = newClient("/" + IMAGE + "/info.json");
        Response response = client.send();

        String json = response.getBodyAsString();
        ObjectMapper mapper = new ObjectMapper();
        Information<?, ?> info = mapper.readValue(json, Information.class);
        assertEquals("http://localhost:" + getHTTPPort() +
                Route.IIIF_3_PATH + "/" + IMAGE, info.get("id"));
    }

    @Test
    void testGETURIsInJSONWithBaseURIOverride() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.BASE_URI, "http://example.org/");

        client = newClient("/" + IMAGE + "/info.json");
        Response response = client.send();

        String json = response.getBodyAsString();
        ObjectMapper mapper = new ObjectMapper();
        Information<?, ?> info = mapper.readValue(json, Information.class);
        assertEquals("http://example.org" +
                Route.IIIF_3_PATH + "/" + IMAGE, info.get("id"));
    }

    @Test
    void testGETURIsInJSONWithSlashSubstitution() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SLASH_SUBSTITUTE, "CATS");

        final String path = "/subfolderCATSjpg";
        client = newClient(path + "/info.json");
        Response response = client.send();

        String json = response.getBodyAsString();
        ObjectMapper mapper = new ObjectMapper();
        Information<?, ?> info = mapper.readValue(json, Information.class);
        assertEquals("http://localhost:" + getHTTPPort() +
                Route.IIIF_3_PATH + path, info.get("id"));
    }

    @Test
    void testGETURIsInJSONWithEncodedCharacters() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SLASH_SUBSTITUTE, "`");

        final String path = "/subfolder%60jpg";
        client = newClient(path + "/info.json");
        Response response = client.send();

        String json = response.getBodyAsString();
        ObjectMapper mapper = new ObjectMapper();
        Information<?, ?> info = mapper.readValue(json, Information.class);
        assertEquals("http://localhost:" + getHTTPPort() +
                Route.IIIF_3_PATH + path, info.get("id"));
    }

    @Test
    void testGETURIsInJSONWithProxyHeaders() throws Exception {
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
        Information<?, ?> info = mapper.readValue(json, Information.class);
        assertEquals("http://example.org:8080/cats" +
                Route.IIIF_3_PATH + "/originalID", info.get("id"));
    }

    @Test
    void testGETBaseURIOverridesProxyHeaders() throws Exception {
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
        Information<?, ?> info = mapper.readValue(json, Information.class);
        assertEquals("https://example.net" +
                Route.IIIF_3_PATH + "/" + IMAGE, info.get("id"));
    }

    /**
     * Tests the default response headers. Individual headers may be tested
     * more thoroughly elsewhere.
     */
    @Test
    void testGETResponseHeaders() throws Exception {
        client = newClient("/" + IMAGE + "/info.json");
        Response response = client.send();
        Headers headers = response.getHeaders();
        assertEquals(8, headers.size());

        // Access-Control-Allow-Origin
        assertEquals("*", headers.getFirstValue("Access-Control-Allow-Origin"));
        // Content-Length
        assertNotNull(headers.getFirstValue("Content-Length"));
        // Content-Type
        assertTrue("application/ld+json;charset=UTF-8;profile=\"http://iiif.io/api/image/3/context.json\"".equalsIgnoreCase(
                headers.getFirstValue("Content-Type")));
        // Date
        assertNotNull(headers.getFirstValue("Date"));
        // Last-Modified
        assertNotNull(headers.getFirstValue("Last-Modified"));
        // Server
        assertNotNull(headers.getFirstValue("Server"));
        // Vary
        List<String> parts =
                List.of(StringUtils.split(headers.getFirstValue("Vary"), ", "));
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
    void testGETLastModifiedResponseHeaderWhenDerivativeCacheIsEnabled()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/info.json");
        tester.testLastModifiedHeaderWhenDerivativeCacheIsEnabled(uri);
    }

    @Test
    void testOPTIONSWhenEnabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.IIIF_3_ENDPOINT_ENABLED, true);

        client = newClient("/" + IMAGE + "/info.json");
        client.setMethod(Method.OPTIONS);
        Response response = client.send();
        assertEquals(204, response.getStatus());

        Headers headers = response.getHeaders();
        List<String> methods =
                List.of(StringUtils.split(headers.getFirstValue("Allow"), ", "));
        assertEquals(2, methods.size());
        assertTrue(methods.contains("GET"));
        assertTrue(methods.contains("OPTIONS"));

        List<String> allowedHeaders =
                List.of(StringUtils.split(headers.getFirstValue("Access-Control-Allow-Headers"), ", "));
        assertEquals(1, allowedHeaders.size());
        assertTrue(allowedHeaders.contains("Authorization"));
    }

    @Test
    void testOPTIONSWhenDisabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.IIIF_3_ENDPOINT_ENABLED, false);
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
