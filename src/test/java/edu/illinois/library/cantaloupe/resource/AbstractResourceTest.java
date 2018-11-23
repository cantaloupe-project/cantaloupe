package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class AbstractResourceTest extends BaseTest {

    private AbstractResource instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        instance = new AbstractResource() {
            @Override
            protected Logger getLogger() {
                return LoggerFactory.getLogger(AbstractResourceTest.class);
            }
        };

        Request mockRequest = new Request(new MockHttpServletRequest());
        instance.setRequest(mockRequest);
        instance.setResponse(new MockHttpServletResponse());
    }

    @Test
    public void testDoDELETE() throws Exception {
        instance.doDELETE();
        assertEquals(405, instance.getResponse().getStatus());
    }

    @Test
    public void testDoGET() throws Exception {
        instance.doGET();
        assertEquals(405, instance.getResponse().getStatus());
    }

    @Test
    public void testDoHEAD() throws Exception {
        instance.doHEAD();
        assertEquals(405, instance.getResponse().getStatus());
    }

    @Test
    public void testDoOPTIONS() throws Exception {
        instance.doOPTIONS();
        assertEquals(204, instance.getResponse().getStatus());
    }

    @Test
    public void testDoPOST() throws Exception {
        instance.doPOST();
        assertEquals(405, instance.getResponse().getStatus());
    }

    @Test
    public void testDoPUT() throws Exception {
        instance.doPUT();
        assertEquals(405, instance.getResponse().getStatus());
    }

    @Test
    public void testGetCommonTemplateVars() {
        Map<String,Object> vars = instance.getCommonTemplateVars();
        assertTrue(((String) vars.get("baseUri")).endsWith("/"));
        assertNotNull(vars.get("version"));
    }

    @Test
    public void testGetPreferredMediaTypesWithAcceptHeaderSet() {
        instance.getRequest().getHeaders().set("Accept",
                "text/html;q=0.9, application/xhtml+xml, */*;q=0.2, text/plain;q=0.5");

        List<String> types = instance.getPreferredMediaTypes();
        assertEquals(4, types.size());
        assertEquals("application/xhtml+xml", types.get(0));
        assertEquals("text/html", types.get(1));
        assertEquals("text/plain", types.get(2));
        assertEquals("*/*", types.get(3));
    }

    @Test
    public void testGetPreferredMediaTypesWithAcceptHeaderNotSet() {
        instance.getRequest().getHeaders().removeAll("Accept");

        List<String> types = instance.getPreferredMediaTypes();
        assertTrue(types.isEmpty());
    }

    /**
     * Tests behavior of {@link AbstractResource#getPublicReference()} when
     * using {@link Key#BASE_URI}.
     */
    @Test
    public void testGetPublicReferenceUsingConfiguration() {
        final String baseURI = "http://example.net/base";
        Configuration.getInstance().setProperty(Key.BASE_URI, baseURI);

        MockHttpServletRequest servletRequest =
                (MockHttpServletRequest) instance.getRequest().getServletRequest();
        servletRequest.setContextPath("/base");
        servletRequest.setRequestURL("http://example.org/base/llamas");

        Reference ref = instance.getPublicReference();
        assertEquals(baseURI + "/llamas", ref.toString());
    }

    /**
     * Tests behavior of {@link AbstractResource#getPublicReference()} when
     * using {@literal X-Forwarded} headers indicating HTTP port 80.
     */
    @Test
    public void testGetPublicReferenceUsingXForwardedHeaders1() {
        MockHttpServletRequest servletRequest =
                (MockHttpServletRequest) instance.getRequest().getServletRequest();
        servletRequest.setContextPath("");
        servletRequest.setRequestURL("http://bogus/cats");

        Headers headers = instance.getRequest().getHeaders();
        headers.set("X-Forwarded-Proto", "HTTP");
        headers.set("X-Forwarded-Host", "example.org");
        headers.set("X-Forwarded-Port", "80");
        headers.set("X-Forwarded-Path", "/");
        Reference ref = instance.getPublicReference();
        assertEquals("http://example.org/cats", ref.toString());
    }

    /**
     * Tests behavior of {@link AbstractResource#getPublicReference()} when
     * using {@literal X-Forwarded} headers indicating HTTP port != 80.
     */
    @Test
    public void testGetPublicReferenceUsingXForwardedHeaders2() {
        MockHttpServletRequest servletRequest =
                (MockHttpServletRequest) instance.getRequest().getServletRequest();
        servletRequest.setContextPath("");
        servletRequest.setRequestURL("http://bogus/cats");

        Headers headers = instance.getRequest().getHeaders();
        headers.set("X-Forwarded-Proto", "HTTP");
        headers.set("X-Forwarded-Host", "example.org");
        headers.set("X-Forwarded-Port", "85");
        headers.set("X-Forwarded-Path", "/");
        Reference ref = instance.getPublicReference();
        assertEquals("http://example.org:85/cats", ref.toString());
    }

    /**
     * Tests behavior of {@link AbstractResource#getPublicReference()} when
     * using {@literal X-Forwarded} headers indicating HTTPS port 443.
     */
    @Test
    public void testGetPublicReferenceUsingXForwardedHeaders3() {
        MockHttpServletRequest servletRequest =
                (MockHttpServletRequest) instance.getRequest().getServletRequest();
        servletRequest.setContextPath("");
        servletRequest.setRequestURL("http://bogus/cats");

        Headers headers = instance.getRequest().getHeaders();
        headers.set("X-Forwarded-Proto", "HTTPS");
        headers.set("X-Forwarded-Host", "example.org");
        headers.set("X-Forwarded-Port", "443");
        headers.set("X-Forwarded-Path", "/");
        Reference ref = instance.getPublicReference();
        assertEquals("https://example.org/cats", ref.toString());
    }

    /**
     * Tests behavior of {@link AbstractResource#getPublicReference()} when
     * using {@literal X-Forwarded} headers indicating HTTPS port != 443.
     */
    @Test
    public void testGetPublicReferenceUsingXForwardedHeaders4() {
        MockHttpServletRequest servletRequest =
                (MockHttpServletRequest) instance.getRequest().getServletRequest();
        servletRequest.setContextPath("");
        servletRequest.setRequestURL("http://bogus/cats");

        Headers headers = instance.getRequest().getHeaders();
        headers.set("X-Forwarded-Proto", "HTTPS");
        headers.set("X-Forwarded-Host", "example.org");
        headers.set("X-Forwarded-Port", "450");
        headers.set("X-Forwarded-Path", "/");
        Reference ref = instance.getPublicReference();
        assertEquals("https://example.org:450/cats", ref.toString());
    }

    /**
     * Tests behavior of {@link AbstractResource#getPublicReference()} when
     * using {@literal X-Forwarded} headers.
     */
    @Test
    public void testGetPublicReferenceUsingChainedXForwardedHeaders() {
        MockHttpServletRequest servletRequest =
                (MockHttpServletRequest) instance.getRequest().getServletRequest();
        servletRequest.setContextPath("");
        servletRequest.setRequestURL("http://bogus/cats");

        Headers headers = instance.getRequest().getHeaders();
        headers.set("X-Forwarded-Proto", "http,http");
        headers.set("X-Forwarded-Host", "example.org,example.mil");
        headers.set("X-Forwarded-Port", "80,8080");
        headers.set("X-Forwarded-Path", "/foxes,/dogs");
        Reference ref = instance.getPublicReference();
        assertEquals("http://example.org/foxes/cats", ref.toString());
    }

    /**
     * Tests behavior of {@link AbstractResource#getPublicReference()} when
     * using neither {@link Key#BASE_URI} nor {@literal X-Forwarded} headers.
     */
    @Test
    public void testGetPublicReferenceFallsBackToHTTPRequest() {
        String resourceURI = "http://example.net/cats/dogs";

        MockHttpServletRequest servletRequest =
                (MockHttpServletRequest) instance.getRequest().getServletRequest();
        servletRequest.setContextPath("/cats");
        servletRequest.setRequestURL(resourceURI);
        Reference ref = instance.getPublicReference();
        assertEquals(resourceURI, ref.toString());
    }

    /**
     * Tests behavior of {@link AbstractResource#getPublicReference()} when
     * using neither {@link Key#BASE_URI} nor {@literal X-Forwarded} headers.
     */
    @Test
    public void testGetPublicReferenceFallsBackToHTTPSRequest() {
        String resourceURI = "https://example.net/cats/dogs";

        MockHttpServletRequest servletRequest =
                (MockHttpServletRequest) instance.getRequest().getServletRequest();
        servletRequest.setContextPath("/cats");
        servletRequest.setRequestURL(resourceURI);
        Reference ref = instance.getPublicReference();
        assertEquals(resourceURI, ref.toString());
    }

    /* getRepresentationDisposition() */

    @Test
    public void testGetRepresentationDispositionWithQueryArg() {
        final Identifier identifier = new Identifier("cats?/\\dogs");
        final Format outputFormat = Format.JPG;

        // none
        String disposition = instance.getRepresentationDisposition(
                null, identifier, outputFormat);
        assertNull(disposition);

        // inline
        disposition = instance.getRepresentationDisposition(
                "inline", identifier, outputFormat);
        assertEquals("inline", disposition);

        // attachment
        disposition = instance.getRepresentationDisposition(
                "attachment", identifier, outputFormat);
        assertEquals("attachment; filename=cats___dogs.jpg", disposition);

        // attachment; filename="dogs.jpg"
        disposition = instance.getRepresentationDisposition(
                "attachment; filename=\"dogs.jpg\"", identifier, outputFormat);
        assertEquals("attachment; filename=dogs.jpg", disposition);

        // attachment; filename="unsafe_path../\.jpg"
        disposition = instance.getRepresentationDisposition(
                "attachment; filename=\"unsafe_path../\\.jpg\"", identifier, outputFormat);
        assertEquals("attachment; filename=unsafe_path.jpg", disposition);

        // attachment; filename="unsafe_injection_.....//./.jpg"
        disposition = instance.getRepresentationDisposition(
                "attachment; filename=\"unsafe_injection_.....//./.jpg\"", identifier, outputFormat);
        assertEquals("attachment; filename=unsafe_injection_.jpg", disposition);
    }

    @Test
    public void testGetRepresentationDispositionUsingConfiguration() {
        Configuration config = Configuration.getInstance();

        final Identifier identifier = new Identifier("cats?/\\dogs");
        final Format outputFormat = Format.JPG;

        // test with config key set to "inline"
        config.setProperty(Key.IIIF_CONTENT_DISPOSITION, "inline");
        String disposition = instance.getRepresentationDisposition(
                null, identifier, outputFormat);
        assertEquals("inline; filename=cats___dogs.jpg", disposition);

        // test with config key set to "attachment"
        config.setProperty(Key.IIIF_CONTENT_DISPOSITION, "attachment");
        disposition = instance.getRepresentationDisposition(
                null, identifier, outputFormat);
        assertEquals("attachment; filename=cats___dogs.jpg", disposition);
    }

    @Test
    public void testGetRepresentationDispositionFallsBackToNone() {
        Configuration config = Configuration.getInstance();

        final Identifier identifier = new Identifier("cats?/\\dogs");
        final Format outputFormat = Format.JPG;

        // undefined config key
        config.clearProperty(Key.IIIF_CONTENT_DISPOSITION);
        String disposition = instance.getRepresentationDisposition(
                null, identifier, outputFormat);
        assertNull(disposition);

        // empty config key
        config.setProperty(Key.IIIF_CONTENT_DISPOSITION, "");
        disposition = instance.getRepresentationDisposition(
                null, identifier, outputFormat);
        assertNull(disposition);
    }

}
