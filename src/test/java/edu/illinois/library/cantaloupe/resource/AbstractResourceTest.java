package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AbstractResourceTest extends BaseTest {

    private AbstractResource instance;

    @BeforeEach
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
    void testDoDELETE() throws Exception {
        instance.doDELETE();
        assertEquals(405, instance.getResponse().getStatus());
    }

    @Test
    void testDoGET() throws Exception {
        instance.doGET();
        assertEquals(405, instance.getResponse().getStatus());
    }

    @Test
    void testDoHEAD() throws Exception {
        instance.doHEAD();
        assertEquals(405, instance.getResponse().getStatus());
    }

    @Test
    void testDoOPTIONS() {
        instance.doOPTIONS();
        assertEquals(204, instance.getResponse().getStatus());
    }

    @Test
    void testDoPOST() throws Exception {
        instance.doPOST();
        assertEquals(405, instance.getResponse().getStatus());
    }

    @Test
    void testDoPUT() throws Exception {
        instance.doPUT();
        assertEquals(405, instance.getResponse().getStatus());
    }

    @Test
    void testGetCommonTemplateVars() {
        Map<String,Object> vars = instance.getCommonTemplateVars();
        assertFalse(((String) vars.get("baseUri")).endsWith("/"));
        assertNotNull(vars.get("version"));
    }

    @Test
    void testGetPreferredMediaTypesWithAcceptHeaderSet() {
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
    void testGetPreferredMediaTypesWithAcceptHeaderNotSet() {
        instance.getRequest().getHeaders().removeAll("Accept");

        List<String> types = instance.getPreferredMediaTypes();
        assertTrue(types.isEmpty());
    }

    /**
     * Tests behavior of {@link AbstractResource#getPublicReference()} when
     * using {@link Key#BASE_URI}.
     */
    @Test
    void testGetPublicReferenceUsingConfiguration() {
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
     * using {@literal X-Forwarded} headers.
     *
     * This isn't a thorough test of every possible header/URI combination.
     * See {@link Reference#applyProxyHeaders(Headers)} for those.
     */
    @Test
    void testGetPublicReferenceUsingXForwardedHeaders() {
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
     * using neither {@link Key#BASE_URI} nor {@literal X-Forwarded} headers.
     */
    @Test
    void testGetPublicReferenceFallsBackToHTTPRequest() {
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
    void testGetPublicReferenceFallsBackToHTTPSRequest() {
        String resourceURI = "https://example.net/cats/dogs";

        MockHttpServletRequest servletRequest =
                (MockHttpServletRequest) instance.getRequest().getServletRequest();
        servletRequest.setContextPath("/cats");
        servletRequest.setRequestURL(resourceURI);
        Reference ref = instance.getPublicReference();
        assertEquals(resourceURI, ref.toString());
    }

    @Test
    void testGetPublicReferenceOmitsQuery() {
        String resourceURI = "https://example.net/cats/dogs?arg=value";
        String expected = "https://example.net/cats/dogs";

        MockHttpServletRequest servletRequest =
                (MockHttpServletRequest) instance.getRequest().getServletRequest();
        servletRequest.setContextPath("/cats");
        servletRequest.setRequestURL(resourceURI);
        Reference ref = instance.getPublicReference();
        assertEquals(expected, ref.toString());
    }

    /* getRepresentationDisposition() */

    @Test
    void testGetRepresentationDispositionWithNoQueryArgument() {
        instance.getRequest().getReference().getQuery().remove(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG);
        String disposition = instance.getRepresentationDisposition(
                new Identifier("cats?/\\dogs"), Format.JPG);
        assertNull(disposition);
    }

    @Test
    void testGetRepresentationDispositionWithInlineQueryArgument() {
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "inline");
        String disposition = instance.getRepresentationDisposition(
                new Identifier("cats?/\\dogs"), Format.JPG);
        assertEquals("inline; filename=\"cats___dogs.jpg\"", disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgument() {
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment");
        String disposition = instance.getRepresentationDisposition(
                new Identifier("cats?/\\dogs"), Format.JPG);
        assertEquals("attachment; filename=\"cats___dogs.jpg\"", disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgumentWithASCIIFilename() {
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename=\"dogs.jpg\"");
        String disposition = instance.getRepresentationDisposition(
                new Identifier("cats?/\\dogs"), Format.JPG);
        assertEquals("attachment; filename=\"dogs.jpg\"", disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgumentWithUnsafeASCIIFilename() {
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename=\"unsafe_path../\\.jpg\"");
        String disposition = instance.getRepresentationDisposition(
                new Identifier("cats?/\\dogs"), Format.JPG);
        assertEquals("attachment; filename=\"unsafe_path.jpg\"",
                disposition);

        // attachment; filename="unsafe_injection_.....//./.jpg"
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename=\"unsafe_injection_.....//./.jpg\"");
        disposition = instance.getRepresentationDisposition(
                new Identifier("cats?/\\dogs"), Format.JPG);
        assertEquals("attachment; filename=\"unsafe_injection_.jpg\"",
                disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgumentWithUnicodeFilename() {
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename*= UTF-8''dogs.jpg");
        String disposition = instance.getRepresentationDisposition(
                new Identifier("cats?/\\dogs"), Format.JPG);
        assertEquals("attachment; filename=\"cats___dogs.jpg\"; filename*= UTF-8''dogs.jpg",
                disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgumentWithUnsafeUnicodeFilename() {
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename*=UTF-8''unsafe_path../\\.jpg");
        String disposition = instance.getRepresentationDisposition(
                new Identifier("cats?/\\dogs"), Format.JPG);
        assertEquals("attachment; filename=\"cats___dogs.jpg\"; filename*= UTF-8''unsafe_path.jpg",
                disposition);

        // attachment; filename*= utf-8''"unsafe_injection_.....//./.jpg"
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename*= utf-8''unsafe_injection_.....//./.jpg");
        disposition = instance.getRepresentationDisposition(
                new Identifier("cats?/\\dogs"), Format.JPG);
        assertEquals("attachment; filename=\"cats___dogs.jpg\"; filename*= UTF-8''unsafe_injection_.jpg",
                disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgumentWithASCIIAndUnicodeFilenames() {
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename=\"dogs.jpg\"; filename*= UTF-8''dogs.jpg");
        String disposition = instance.getRepresentationDisposition(
                new Identifier("cats?/\\dogs"), Format.JPG);
        assertEquals("attachment; filename=\"dogs.jpg\"; filename*= UTF-8''dogs.jpg",
                disposition);
    }

    @Test
    void testGetRepresentationDispositionFallsBackToNone() {
        instance.getRequest().getReference().getQuery().remove(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG);
        String disposition = instance.getRepresentationDisposition(
                new Identifier("cats?/\\dogs"), Format.JPG);
        assertNull(disposition);
    }

}
