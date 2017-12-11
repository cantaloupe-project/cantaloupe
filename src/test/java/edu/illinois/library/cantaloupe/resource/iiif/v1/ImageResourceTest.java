package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.RestletApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Client;
import edu.illinois.library.cantaloupe.http.Response;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.resource.ResourceTest;
import edu.illinois.library.cantaloupe.resource.iiif.ImageResourceTester;
import edu.illinois.library.cantaloupe.test.TestUtil;
import edu.illinois.library.cantaloupe.util.SystemUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static edu.illinois.library.cantaloupe.test.Assert.HTTPAssert.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

public class ImageResourceTest extends ResourceTest {

    private static final String IMAGE = "jpg-rgb-64x56x8-baseline.jpg";

    private ImageResourceTester tester = new ImageResourceTester();

    @Override
    protected String getEndpointPath() {
        return RestletApplication.IIIF_1_PATH;
    }

    @Test
    public void testAuthorizationWhenAuthorized() {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg");
        tester.testAuthorizationWhenAuthorized(uri);
    }

    @Test
    public void testAuthorizationWhenNotAuthorized() {
        URI uri = getHTTPURI("/forbidden.jpg/full/full/0/color.jpg");
        tester.testAuthorizationWhenNotAuthorized(uri);
    }

    @Test
    public void testAuthorizationWhenRedirecting() throws Exception {
        URI uri = getHTTPURI("/redirect.jpg/full/full/0/color.jpg");
        tester.testAuthorizationWhenRedirecting(uri);
    }

    @Test
    public void testBasicAuthenticationWithNoCredentials() throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg");
        tester.testBasicAuthWithNoCredentials(appServer, uri);
    }

    @Test
    public void testBasicAuthenticationWithInvalidCredentials()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg");
        tester.testBasicAuthWithInvalidCredentials(appServer, uri);
    }

    @Test
    public void testBasicAuthenticationWithValidCredentials()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg");
        tester.testBasicAuthWithValidCredentials(appServer, uri);
    }

    @Test
    public void testCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg");
        tester.testCacheHeadersWhenClientCachingIsEnabledAndResponseIsCacheable(uri);
    }

    @Test
    public void testCacheHeadersWhenClientCachingIsEnabledAndResponseIsNotCacheable()
            throws Exception {
        URI uri = getHTTPURI("/bogus/full/full/0/color.jpg");
        tester.testCacheHeadersWhenClientCachingIsEnabledAndResponseIsNotCacheable(uri);
    }

    /**
     * Tests that there is no Cache-Control header returned when
     * cache.client.enabled = true but a cache=false argument is present
     * in the URL query.
     */
    @Test
    public void testCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg?cache=false");
        tester.testCacheHeadersWhenClientCachingIsEnabledButCachingIsDisabledInURL(uri);
    }

    @Test
    public void testCacheHeadersWhenClientCachingIsDisabled() throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg");
        tester.testCacheHeadersWhenClientCachingIsDisabled(uri);
    }

    @Test
    public void testCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied()
            throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.png?cache=false");
        tester.testCachingWhenCachesAreEnabledButNegativeCacheQueryArgumentIsSupplied(uri);
    }

    @Test
    public void testCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/1/color.jpg");
        tester.testCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstEnabled(
                uri, TestUtil.getImage(IMAGE));
    }

    /**
     * This endpoint doesn't respect {@link Key#CACHE_SERVER_RESOLVE_FIRST}
     * (it's always effectively true), so this test is redundant.
     */
    @Test
    public void testCacheWithDerivativeCacheEnabledAndInfoCacheEnabledAndResolveFirstDisabled()
            throws Exception {
        // nnop
    }

    @Test
    public void testCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/1/color.jpg");
        tester.testCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstEnabled(
                uri, TestUtil.getImage(IMAGE));
    }

    /**
     * This endpoint doesn't respect {@link Key#CACHE_SERVER_RESOLVE_FIRST}
     * (it's always effectively true), so this test is redundant.
     */
    @Test
    public void testCacheWithDerivativeCacheEnabledAndInfoCacheDisabledAndResolveFirstDisabled()
            throws Exception {
        // noop
    }

    @Test
    public void testCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/1/color.jpg");
        tester.testCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstEnabled(
                uri, TestUtil.getImage(IMAGE));
    }

    /**
     * This endpoint doesn't respect {@link Key#CACHE_SERVER_RESOLVE_FIRST}
     * (it's always effectively true), so this test is redundant.
     */
    @Test
    public void testCacheWithDerivativeCacheDisabledAndInfoCacheEnabledAndResolveFirstDisabled()
            throws Exception {
        // noop
    }

    @Test
    public void testCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstEnabled()
            throws Exception {
        // The image must be modified as unmodified images aren't cached.
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/1/color.jpg");
        tester.testCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstEnabled(
                uri, TestUtil.getImage(IMAGE));
    }

    /**
     * This endpoint doesn't respect {@link Key#CACHE_SERVER_RESOLVE_FIRST}
     * (it's always effectively true), so this test is redundant.
     */
    @Test
    public void testCacheWithDerivativeCacheDisabledAndInfoCacheDisabledAndResolveFirstDisabled()
            throws Exception {
        // noop
    }

    @Test
    public void testContentDispositionHeaderWithNoHeader() throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg");
        tester.testContentDispositionHeaderWithNoHeader(uri);
    }

    @Test
    public void testContentDispositionHeaderSetToInline() throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg");
        tester.testContentDispositionHeaderSetToInline(uri);
    }

    @Test
    public void testContentDispositionHeaderSetToAttachment() throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg");
        tester.testContentDispositionHeaderSetToAttachment(uri);
    }

    @Test
    public void testEndpointEnabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.IIIF_1_ENDPOINT_ENABLED, true);

        assertStatus(200, getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg"));
    }

    @Test
    public void testEndpointDisabled() {
        Configuration config = Configuration.getInstance();
        config.setProperty(Key.IIIF_1_ENDPOINT_ENABLED, false);

        assertStatus(403, getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg"));
    }

    @Test
    public void testHTTP2() throws Exception {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg");
        tester.testHTTP2(uri);
    }

    @Test
    public void testHTTPS1_1() throws Exception {
        URI uri = getHTTPSURI("/" + IMAGE + "/full/full/0/color.jpg");
        tester.testHTTPS1_1(uri);
    }

    @Test
    public void testHTTPS2() throws Exception {
        assumeTrue(SystemUtils.isALPNAvailable());
        URI uri = getHTTPSURI("/" + IMAGE + "/full/full/0/color.jpg");
        tester.testHTTPS2(uri);
    }

    @Test
    public void testMinPixels() {
        URI uri = getHTTPURI("/" + IMAGE + "/0,0,0,0/full/0/color.png");
        tester.testMinPixels(uri);
    }

    @Test
    public void testLessThanMaxPixels() {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.png");
        tester.testLessThanMaxPixels(uri);
    }

    @Test
    public void testMoreThanMaxPixels() {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.png");
        tester.testMoreThanMaxPixels(uri);
    }

    @Test
    public void testMaxPixelsIgnoredWhenStreamingSource() {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.jpg");
        tester.testMaxPixelsIgnoredWhenStreamingSource(uri);
    }

    @Test
    public void testNotFound() {
        URI uri = getHTTPURI("/invalid/full/full/0/color.jpg");
        tester.testNotFound(uri);
    }

    @Test
    public void testProcessorValidationFailure() throws Exception {
        URI uri = getHTTPURI("/pdf-multipage.pdf/full/full/0/color.jpg?page=999999");
        tester.testProcessorValidationFailure(uri);
    }

    @Test
    public void testPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse()
            throws Exception {
        final String imagePath = "/" + IMAGE + "/full/full/0/color.jpg";
        final URI uri = getHTTPURI(imagePath);
        final OperationList opList = Parameters.fromUri(imagePath).toOperationList();
        tester.testPurgeFromCacheWhenSourceIsMissingAndOptionIsFalse(
                uri, opList);
    }

    @Test
    public void testPurgeFromCacheWhenSourceIsMissingAndOptionIsTrue()
            throws Exception {
        final String imagePath = "/" + IMAGE + "/full/full/0/color.jpg";
        final URI uri = getHTTPURI(imagePath);
        final OperationList opList = Parameters.fromUri(imagePath).toOperationList();
        tester.testPurgeFromCacheWhenSourceIsMissingAndOptionIsTrue(
                uri, opList);
    }

    /**
     * Checks that the server responds with HTTP 500 when a non-FileResolver is
     * used with a non-StreamProcessor.
     */
    @Test
    public void testResolverProcessorCompatibility() {
        URI uri = getHTTPURI("/jp2/full/full/0/color.jpg");
        tester.testResolverProcessorCompatibility(
                uri, appServer.getHTTPHost(), appServer.getHTTPPort());
    }

    @Test
    public void testSlashSubstitution() {
        URI uri = getHTTPURI("/subfolderCATSjpg/full/full/0/color.jpg");
        tester.testSlashSubstitution(uri);
    }

    @Test
    public void testUnavailableSourceFormat() {
        URI uri = getHTTPURI("/text.txt/full/full/0/color.jpg");
        tester.testUnavailableSourceFormat(uri);
    }

    @Test
    public void testInvalidOutputFormat() {
        URI uri = getHTTPURI("/" + IMAGE + "/full/full/0/color.bogus");
        tester.testInvalidOutputFormat(uri);
    }

    /**
     * Tests the default response headers. Individual headers may be tested
     * more thoroughly elsewhere.
     */
    @Test
    public void testResponseHeaders() throws Exception {
        client = newClient("/" + IMAGE + "/full/full/0/color.jpg");
        Response response = client.send();
        Map<String,String> headers = response.getHeaders();
        assertEquals(8, headers.size());

        // Accept-Ranges TODO: remove this
        assertEquals("bytes", headers.get("Accept-Ranges"));
        // Content-Type
        assertEquals("image/jpeg", headers.get("Content-Type"));
        // Date
        assertNotNull(headers.get("Date"));
        // Link
        assertTrue(headers.get("Link").contains("://"));
        // Server
        assertTrue(headers.get("Server").contains("Restlet"));
        // Transfer-Encoding
        assertEquals("chunked", headers.get("Transfer-Encoding"));
        // Vary
        List<String> parts = Arrays.asList(StringUtils.split(headers.get("Vary"), ", "));
        assertEquals(5, parts.size());
        assertTrue(parts.contains("Accept"));
        assertTrue(parts.contains("Accept-Charset"));
        assertTrue(parts.contains("Accept-Encoding"));
        assertTrue(parts.contains("Accept-Language"));
        assertTrue(parts.contains("Origin"));
        // X-Powered-By
        assertEquals("Cantaloupe/Unknown", headers.get("X-Powered-By"));
    }

}
