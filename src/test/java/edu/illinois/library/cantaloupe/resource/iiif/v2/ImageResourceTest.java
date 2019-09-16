package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.Application;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.ResourceException;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import edu.illinois.library.cantaloupe.resource.Route;
import edu.illinois.library.cantaloupe.resource.iiif.ImageResourceTester;
import edu.illinois.library.cantaloupe.test.TestUtil;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static edu.illinois.library.cantaloupe.test.Assert.HTTPAssert.*;
import static org.junit.jupiter.api.Assertions.*;

public class ImageResourceTest extends ResourceTest {

    private static final String IMAGE = "jpg-rgb-64x56x8-baseline.jpg";

    private ImageResourceTester tester = new ImageResourceTester();

    @Override
    protected String getEndpointPath() {
        return Route.IIIF_2_PATH;
    }

    @Test
    void testGETAuthorizationWhenAuthorized() {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg");
        tester.testAuthorizationWhenAuthorized(uri);
    }

    @Test
    void testGETAuthorizationWhenNotAuthorized() {
        URI uri = getHTTPURI("/forbidden.jpg/full/full/0/color.jpg");
        tester.testAuthorizationWhenNotAuthorized(uri);
    }

    @Test
    void testGETAuthorizationWhenRedirecting() throws Exception {
        URI uri = getHTTPURI("/redirect.jpg/full/full/0/color.jpg");
        tester.testAuthorizationWhenRedirecting(uri);
    }

    @Test
    void testGETAuthorizationWhenScaleConstraining() throws Exception {
        URI uri = getHTTPURI("/reduce.jpg/full/full/0/color.jpg");
        tester.testAuthorizationWhenScaleConstraining(uri);
    }

    @Test
    void testGETCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg");
        tester.testCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable(uri);
    }

    @Test
    void testGETCacheHeadersWhenClientCachingIsEnabledAndResponseIsNotCacheable()
            throws Exception {
        URI uri = getHTTPURI("/bogus/full/full/0/color.jpg");
        tester.testCacheHeadersWhenClientCachingIsEnabledAndResponseIsNotCacheable(uri);
    }

    /**
     * Tests that there is no {@code Cache-Control} header returned when
     * {@code cache.client.enabled = true} but a {@code cache=nocache} argument
     * is present in the URL query.
     */
    @Test
    void testGETCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL1()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg?cache=nocache");
        tester.testCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL(uri);
    }

    /**
     * Tests that there is no {@code Cache-Control} header returned when
     * {@code cache.client.enabled = true} but a {@code cache=false} argument
     * is present in the URL query.
     */
    @Test
    void testGETCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL2()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg?cache=false");
        tester.testCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL(uri);
    }

    /**
     * Tests that there is a {@code Cache-Control} header returned when
     * {@code cache.client.enabled = true} and a {@code cache=recache} argument
     * is present in the URL query.
     */
    @Test
    void testGETCacheHeadersWhenClientCachingIsEnabledAndRecachingIsEnabledInURL()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg?cache=recache");
        tester.testCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable(uri);
    }

    @Test
    void testGETCacheHeadersWhenClientCachingIsDisabled() throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg");
        tester.testCacheHeadersWhenClientCachingIsDisabled(uri);
    }

    @Test
    void testGETCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied1()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.png?cache=nocache");
        tester.testCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied(uri);
    }

    @Test
    void testGETCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied2()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.png?cache=false");
        tester.testCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied(uri);
    }

    @Test
    void testGETCachingWhenCachesAreEnabledAndRecacheQueryArgumentIsSupplied()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.png?cache=recache");
        tester.testCachingWhenCachesAreEnabledAndRecacheQueryArgumentIsSupplied(uri);
    }

    @Test
    void testGETCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/1/color.jpg");
        tester.testCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstEnabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    void testGETCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstDisabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/1/color.jpg");
        tester.testCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstDisabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    void testGETCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/1/color.jpg");
        tester.testCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstEnabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    void testGETCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstDisabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/1/color.jpg");
        tester.testCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstDisabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    void testGETCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/1/color.jpg");
        tester.testCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstEnabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    void testGETCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstDisabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/1/color.jpg");
        tester.testCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstDisabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    void testGETCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/1/color.jpg");
        tester.testCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstEnabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    void testGETCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstDisabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/1/color.jpg");
        tester.testCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstDisabled(
                uri, TestUtil.getImage(IMAGE));
    }

    @Test
    void testGETContentDispositionHeaderWithNoHeader() throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg");
        tester.testContentDispositionHeaderWithNoHeader(uri);
    }

    @Test
    void testGETContentDispositionHeaderSetToInline() throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg?response-content-disposition=inline");
        tester.testContentDispositionHeaderSetToInline(uri);
    }

    @Test
    void testGETContentDispositionHeaderSetToAttachment() throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg?response-content-disposition=attachment");
        tester.testContentDispositionHeaderSetToAttachment(uri);
    }

    @Test
    void testGETContentDispositionHeaderSetToAttachmentWithFilename()
            throws Exception {
        final String filename = "cats%20dogs.jpg";
        final String expected = "cats_dogs.jpg";

        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg?response-content-disposition=attachment;filename%3D%22" + filename + "%22");

        tester.testContentDispositionHeaderSetToAttachmentWithFilename(uri, expected);

        uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg?response-content-disposition=attachment;%20filename%3D%22" + filename + "%22");
        tester.testContentDispositionHeaderSetToAttachmentWithFilename(uri, expected);

        uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg?response-content-disposition=attachment;filename%3D" + filename);
        tester.testContentDispositionHeaderSetToAttachmentWithFilename(uri, expected);

        uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg?response-content-disposition=attachment;%20filename%3D" + filename);
        tester.testContentDispositionHeaderSetToAttachmentWithFilename(uri, expected);
    }

    @Test
    void testGETEndpointEnabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.IIIF_1_ENDPOINT_ENABLED, true);

        assertStatus(200, getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg"));
    }

    @Test
    void testGETEndpointDisabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, false);

        assertStatus(403, getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg"));
    }

    @Test
    void testGETHTTP2() throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg");
        tester.testHTTP2(uri);
    }

    @Test
    void testGETHTTPS1_1() throws Exception {
        URI uri = getHTTPSURI("/" + IMAGE + "/full/full/0/color.jpg");
        tester.testHTTPS1_1(uri);
    }

    @Test
    void testGETHTTPS2() throws Exception {
        URI uri = getHTTPSURI("/" + IMAGE + "/full/full/0/color.jpg");
        tester.testHTTPS2(uri);
    }

    @Test
    void testGETLinkHeader() throws Exception {
        client = newClient("/" + IMAGE + "/full/full/0/color.jpg");
        Response response = client.send();

        String value = response.getHeaders().getFirstValue("Link");
        assertTrue(value.startsWith("<http://localhost"));
    }

    @Test
    void testGETLinkHeaderWithSlashSubstitution() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SLASH_SUBSTITUTE, "CATS");

        client = newClient("/subfolderCATSjpg/full/full/0/color.jpg");
        Response response = client.send();

        String value = response.getHeaders().getFirstValue("Link");
        assertTrue(value.contains("subfolderCATSjpg"));
    }

    @Test
    void testGETLinkHeaderWithEncodedCharacters() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.SLASH_SUBSTITUTE, ":");

        client = newClient("/subfolder%3Ajpg/full/full/0/color.jpg");
        Response response = client.send();

        String value = response.getHeaders().getFirstValue("Link");
        assertTrue(value.contains("subfolder%3Ajpg"));
    }

    @Test
    void testGETLinkHeaderWithBaseURIOverride() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.BASE_URI, "http://example.org/");

        client = newClient("/" + IMAGE + "/full/full/0/color.jpg");
        Response response = client.send();

        String value = response.getHeaders().getFirstValue("Link");
        assertTrue(value.startsWith("<http://example.org/"));
    }

    @Test
    void testGETLinkHeaderWithProxyHeaders() throws Exception {
        client = newClient("/" + IMAGE + "/pct:50,50,50,50/,35/0/color.jpg");
        client.getHeaders().set("X-Forwarded-Proto", "HTTP");
        client.getHeaders().set("X-Forwarded-Host", "example.org");
        client.getHeaders().set("X-Forwarded-Port", "8080");
        client.getHeaders().set("X-Forwarded-Path", "/cats");
        Response response = client.send();

        assertEquals("<http://example.org:8080/cats/iiif/2/jpg-rgb-64x56x8-baseline.jpg/32,28,32,28/40,/0/color.jpg>;rel=\"canonical\"",
                response.getHeaders().getFirstValue("Link"));
    }

    @Test
    void testGETLinkHeaderBaseURIOverridesProxyHeaders() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.BASE_URI, "https://example.net/");

        client = newClient("/" + IMAGE + "/full/full/0/color.jpg");
        client.getHeaders().set("X-Forwarded-Proto", "HTTP");
        client.getHeaders().set("X-Forwarded-Host", "example.org");
        client.getHeaders().set("X-Forwarded-Port", "8080");
        client.getHeaders().set("X-Forwarded-Path", "/cats");
        Response response = client.send();

        assertEquals("<https://example.net/iiif/2/jpg-rgb-64x56x8-baseline.jpg/full/full/0/color.jpg>;rel=\"canonical\"",
                response.getHeaders().getFirstValue("Link"));
    }

    @Test
    void testGETLessThanOrEqualToFullScale() {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.png");
        tester.testLessThanOrEqualToMaxScale(uri);
    }

    @Test
    void testGETGreaterThanFullScale() {
        URI uri = getHTTPURI("/" + IMAGE + "/full/pct:101/0/color.png");
        tester.testGreaterThanMaxScale(uri);
    }

    @Test
    void testGETMinPixels() {
        URI uri = getHTTPURI("/" + IMAGE + "/0,0,0,0/full/0/color.png");
        tester.testMinPixels(uri);
    }

    @Test
    void testGETLessThanMaxPixels() {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.png");
        tester.testLessThanMaxPixels(uri);
    }

    @Test
    void testGETMoreThanMaxPixels() {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.png");
        tester.testMoreThanMaxPixels(uri);
    }

    @Test
    void testGETMaxPixelsIgnoredWhenStreamingSource() {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg");
        tester.testMaxPixelsIgnoredWhenStreamingSource(uri);
    }

    @Test
    void testGETNotFound() {
        URI uri = getHTTPURI("/invalid/full/full/0/color.jpg");
        tester.testNotFound(uri);
    }

    @Test
    void testGETProcessorValidationFailure() {
        URI uri = getHTTPURI("/pdf-multipage.pdf/full/full/0/color.jpg?page=999999");
        tester.testProcessorValidationFailure(uri);
    }

    @Test
    void testGETPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse()
            throws Exception {
        final String imagePath = "/" + IMAGE + "/full/full/0/color.jpg";
        final URI uri = getHTTPURI(imagePath);
        final OperationList opList = Parameters.fromUri(imagePath).toOperationList();
        tester.testPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse(
                uri, opList);
    }

    @Test
    void testGETPurgeFromCacheWhenSourceIsMissingAndOptionIsTrue()
            throws Exception {
        final String imagePath = "/" + IMAGE + "/full/full/0/color.jpg";
        final URI uri = getHTTPURI(imagePath);
        final OperationList opList = Parameters.fromUri(imagePath).toOperationList();
        tester.testPurgeFromCacheWhenSourceIsMissingAndOptionIsTrue(
                uri, opList);
    }

    @Test
    void testGETRecoveryFromDerivativeCacheNewDerivativeImageInputStreamException()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg");
        tester.testRecoveryFromDerivativeCacheNewDerivativeImageInputStreamException(uri);
    }

    @Test
    void testGETRecoveryFromDerivativeCacheNewDerivativeImageOutputStreamException()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.png");
        tester.testRecoveryFromDerivativeCacheNewDerivativeImageOutputStreamException(uri);
    }

    @Test
    void testGETRecoveryFromIncorrectSourceFormat() throws Exception {
        URI uri = getHTTPURI("/jpg-incorrect-extension.png/full/full/0/color.jpg");
        tester.testRecoveryFromIncorrectSourceFormat(uri);
    }

    /**
     * Tests that a scale constraint of {@literal -1:1} is redirected to no
     * scale constraint.
     */
    @Test
    void testGETRedirectToNormalizedScaleConstraint1() {
        URI fromURI = getHTTPURI("/" + IMAGE +
                new ScaleConstraint(1, 1).toIdentifierSuffix() +
                "/full/full/0/color.png");
        URI toURI = getHTTPURI("/" + IMAGE + "/full/full/0/color.png");
        assertRedirect(fromURI, toURI, 301);
    }

    /**
     * Tests that a scale constraint of {@literal -2:2} is redirected to no
     * scale constraint.
     */
    @Test
    void testGETRedirectToNormalizedScaleConstraint2() {
        URI fromURI = getHTTPURI("/" + IMAGE +
                new ScaleConstraint(2, 2).toIdentifierSuffix() +
                "/full/full/0/color.png");
        URI toURI = getHTTPURI("/" + IMAGE + "/full/full/0/color.png");
        assertRedirect(fromURI, toURI, 301);
    }

    /**
     * Tests that a scale constraint of {@literal -2:4} is redirected to
     * {@literal -1:2}.
     */
    @Test
    void testGETRedirectToNormalizedScaleConstraint3() {
        URI fromURI = getHTTPURI("/" + IMAGE +
                new ScaleConstraint(2, 4).toIdentifierSuffix() +
                "/full/full/0/color.png");
        URI toURI = getHTTPURI("/" + IMAGE +
                new ScaleConstraint(1, 2).toIdentifierSuffix() +
                "/full/full/0/color.png");
        assertRedirect(fromURI, toURI, 301);
    }

    @Test
    void testGETResolverCheckAccessNotCalledWithSourceCacheHit()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg");
        tester.testSourceCheckAccessNotCalledWithSourceCacheHit(new Identifier(IMAGE), uri);
    }

    @Test
    void testGETResolverGetSourceFormatNotCalledWithSourceCacheHit()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg");
        tester.testSourceGetFormatNotCalledWithSourceCacheHit(new Identifier(IMAGE), uri);
    }

    /**
     * Checks that the server responds with HTTP 500 when a non-FileSource is
     * used with a non-StreamProcessor.
     */
    @Test
    void testGETResolverProcessorCompatibility() {
        URI uri = getHTTPURI("/jp2/full/full/0/color.jpg");
        tester.testSourceProcessorCompatibility(
                uri, appServer.getHTTPHost(), appServer.getHTTPPort());
    }

    @Test
    void testGETNotRestrictedToSizes() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.IIIF_RESTRICT_TO_SIZES, false);

        assertStatus(200, getHTTPURI("/" + IMAGE + "/full/53,37/0/color.jpg"));
    }

    @Test
    void testGETRestrictedToSizes() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.IIIF_RESTRICT_TO_SIZES, true);

        assertStatus(403, getHTTPURI("/" + IMAGE + "/full/53,37/0/color.jpg"));
    }

    @Test
    void testGETSlashSubstitution() {
        URI uri = getHTTPURI("/subfolderCATSjpg/full/full/0/color.jpg");
        tester.testSlashSubstitution(uri);
    }

    @Test
    void testGETUnavailableSourceFormat() {
        URI uri = getHTTPURI("/text.txt/full/full/0/color.jpg");
        tester.testUnavailableSourceFormat(uri);
    }

    @Test
    void testGETInvalidOutputFormat() {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.bogus");
        tester.testInvalidOutputFormat(uri);
    }

    /**
     * Tests the default response headers. Individual headers may be tested
     * more thoroughly elsewhere.
     */
    @Test
    void testGETResponseHeaders() throws Exception {
        client = newClient("/" + IMAGE + "/full/full/0/color.jpg");
        Response response = client.send();
        Headers headers = response.getHeaders();
        assertEquals(8, headers.size());

        // Access-Control-Allow-Origin
        assertEquals("*", headers.getFirstValue("Access-Control-Allow-Origin"));
        // Content-Length
        assertNotNull(headers.getFirstValue("Content-Length"));
        // Content-Type
        assertEquals("image/jpeg", headers.getFirstValue("Content-Type"));
        // Date
        assertNotNull(headers.getFirstValue("Date"));
        // Link
        assertTrue(headers.getFirstValue("Link").contains("://"));
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
    void testOPTIONSWhenEnabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, true);

        client = newClient("/" + IMAGE + "/full/full/0/color.jpg");
        client.setMethod(Method.OPTIONS);
        Response response = client.send();
        assertEquals(204, response.getStatus());

        Headers headers = response.getHeaders();
        List<String> methods =
                List.of(StringUtils.split(headers.getFirstValue("Allow"), ", "));
        assertEquals(2, methods.size());
        assertTrue(methods.contains("GET"));
        assertTrue(methods.contains("OPTIONS"));
    }

    @Test
    void testOPTIONSWhenDisabled() throws Exception {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.IIIF_2_ENDPOINT_ENABLED, false);
        try {
            client = newClient("/" + IMAGE + "/full/full/0/color.jpg");
            client.setMethod(Method.OPTIONS);
            client.send();
            fail("Expected exception");
        } catch (ResourceException e) {
            assertEquals(403, e.getStatusCode());
        }
    }

}
