package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.CantalouperApplication;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.image.Format;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spring Boot-enabled test for the AbstractResource class.
 *
 * This test demonstrates the conversion from traditional JUnit tests to
 * Spring Boot testing framework, using Spring's mock objects and proper
 * configuration management.
 */
@SpringBootTest(classes = CantalouperApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
    "spring.main.banner-mode=off",
    "logging.level.root=WARN",
    "logging.level.edu.illinois.library.cantaloupe=INFO",
    "http.enabled=true",
    "http.port=0",
    "endpoint.iiif.1.enabled=true",
    "endpoint.iiif.2.enabled=true",
    "endpoint.iiif.3.enabled=true"
})
public class AbstractResourceTest {

    private AbstractResource instance;

    @BeforeEach
    public void setUp() throws Exception {
        instance = new AbstractResource() {
            @Override
            protected Logger getLogger() {
                return LoggerFactory.getLogger(AbstractResourceTest.class);
            }
        };

        MockHttpServletRequest mockServletRequest = new MockHttpServletRequest();
        mockServletRequest.setMethod("GET");
        mockServletRequest.setRequestURI("/");
        mockServletRequest.setServerName("localhost");
        mockServletRequest.setScheme("http");
        mockServletRequest.setServerPort(8182);

        Request mockRequest = new Request(mockServletRequest);
        instance.setRequest(mockRequest);

        MockHttpServletResponse mockServletResponse = new MockHttpServletResponse();
        instance.setResponse(mockServletResponse);
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
        MockHttpServletRequest servletRequest =
            (MockHttpServletRequest) instance.getRequest().getServletRequest();
        servletRequest.addHeader("Accept",
            "text/html;q=0.9, application/xhtml+xml, */*;q=0.2, text/plain;q=0.5");

        List<String> types = instance.getPreferredMediaTypes();
        assertEquals(3, types.size());
        assertEquals("application/xhtml+xml", types.get(0));
        assertEquals("text/html", types.get(1));
        assertEquals("text/plain", types.get(2));
    }

    @Test
    void testGetPreferredMediaTypesWithAcceptHeaderNotSet() {
        // Spring's MockHttpServletRequest doesn't have an Accept header by default
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
        servletRequest.setRequestURI("/base/llamas");
        servletRequest.setServerName("example.org");
        servletRequest.setScheme("http");

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
        servletRequest.setRequestURI("/cats");
        servletRequest.setServerName("bogus");
        servletRequest.setScheme("http");

        servletRequest.addHeader("X-Forwarded-Proto", "HTTP");
        servletRequest.addHeader("X-Forwarded-Host", "example.org");
        servletRequest.addHeader("X-Forwarded-Port", "80");
        servletRequest.addHeader("X-Forwarded-Path", "/");

        Reference ref = instance.getPublicReference();
        assertEquals("http://example.org/cats", ref.toString());
    }

    /**
     * Tests behavior of {@link AbstractResource#getPublicReference()} when
     * using neither {@link Key#BASE_URI} nor {@literal X-Forwarded} headers.
     */
    @Test
    void testGetPublicReferenceFallsBackToHTTPRequest() {
        MockHttpServletRequest servletRequest =
                (MockHttpServletRequest) instance.getRequest().getServletRequest();
        servletRequest.setContextPath("/cats");
        servletRequest.setRequestURI("/cats/dogs");
        servletRequest.setServerName("example.net");
        servletRequest.setScheme("http");
        servletRequest.setServerPort(80);

        Reference ref = instance.getPublicReference();
        assertEquals("http://example.net/cats/dogs", ref.toString());
    }

    /**
     * Tests behavior of {@link AbstractResource#getPublicReference()} when
     * using neither {@link Key#BASE_URI} nor {@literal X-Forwarded} headers.
     */
    @Test
    void testGetPublicReferenceFallsBackToHTTPSRequest() {
        MockHttpServletRequest servletRequest =
                (MockHttpServletRequest) instance.getRequest().getServletRequest();
        servletRequest.setContextPath("/cats");
        servletRequest.setRequestURI("/cats/dogs");
        servletRequest.setServerName("example.net");
        servletRequest.setScheme("https");
        servletRequest.setServerPort(443);
        servletRequest.setSecure(true);

        Reference ref = instance.getPublicReference();
        assertEquals("https://example.net/cats/dogs", ref.toString());
    }

    @Test
    void testGetPublicReferenceOmitsQuery() {
        MockHttpServletRequest servletRequest =
                (MockHttpServletRequest) instance.getRequest().getServletRequest();
        servletRequest.setContextPath("/cats");
        servletRequest.setRequestURI("/cats/dogs");
        servletRequest.setQueryString("arg=value");
        servletRequest.setServerName("example.net");
        servletRequest.setScheme("https");
        servletRequest.setServerPort(443);
        servletRequest.setSecure(true);

        Reference ref = instance.getPublicReference();
        assertEquals("https://example.net/cats/dogs", ref.toString());
    }

    /* getRepresentationDisposition() */

    @Test
    void testGetRepresentationDispositionWithNoQueryArgument() {
        instance.getRequest().getReference().getQuery().remove(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG);
        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertNull(disposition);
    }

    @Test
    void testGetRepresentationDispositionWithInlineQueryArgument() {
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "inline");
        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("inline; filename=\"cats___dogs.jpg\"", disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgument() {
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment");
        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"cats___dogs.jpg\"", disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgumentWithASCIIFilename() {
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename=\"dogs.jpg\"");
        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"dogs.jpg\"", disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgumentWithUnsafeASCIIFilename() {
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename=\"unsafe_path../\\.jpg\"");
        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"unsafe_path.jpg\"",
                disposition);

        // attachment; filename="unsafe_injection_.....//./.jpg"
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename=\"unsafe_injection_.....//./.jpg\"");
        disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"unsafe_injection_.jpg\"",
                disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgumentWithUnicodeFilename() {
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename*= UTF-8''dogs.jpg");
        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"cats___dogs.jpg\"; filename*= UTF-8''dogs.jpg",
                disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgumentWithUnsafeUnicodeFilename() {
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename*=UTF-8''unsafe_path../\\.jpg");
        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"cats___dogs.jpg\"; filename*= UTF-8''unsafe_path.jpg",
                disposition);

        // attachment; filename*= utf-8''"unsafe_injection_.....//./.jpg"
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename*= utf-8''unsafe_injection_.....//./.jpg");
        disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"cats___dogs.jpg\"; filename*= UTF-8''unsafe_injection_.jpg",
                disposition);
    }

    @Test
    void testGetRepresentationDispositionWithAttachmentQueryArgumentWithASCIIAndUnicodeFilenames() {
        instance.getRequest().getReference().getQuery().set(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG,
                "attachment; filename=\"dogs.jpg\"; filename*= UTF-8''dogs.jpg");
        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertEquals("attachment; filename=\"dogs.jpg\"; filename*= UTF-8''dogs.jpg",
                disposition);
    }

    @Test
    void testGetRepresentationDispositionFallsBackToNone() {
        instance.getRequest().getReference().getQuery().remove(
                AbstractResource.RESPONSE_CONTENT_DISPOSITION_QUERY_ARG);
        String disposition = instance.getRepresentationDisposition(
                "cats?/\\dogs", Format.get("jpg"));
        assertNull(disposition);
    }

    @Test
    void testGetPreferredMediaTypesWithComplexAcceptHeader() {
        MockHttpServletRequest servletRequest =
            (MockHttpServletRequest) instance.getRequest().getServletRequest();
        servletRequest.addHeader("Accept",
            "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");

        List<String> types = instance.getPreferredMediaTypes();
        assertTrue(types.size() >= 3);
        assertTrue(types.contains("text/html"));
        assertTrue(types.contains("application/xhtml+xml"));
    }

    @Test
    void testGetPublicReferenceWithCustomPort() {
        MockHttpServletRequest servletRequest =
                (MockHttpServletRequest) instance.getRequest().getServletRequest();
        servletRequest.setContextPath("");
        servletRequest.setRequestURI("/api/test");
        servletRequest.setServerName("localhost");
        servletRequest.setScheme("http");
        servletRequest.setServerPort(8080);

        Reference ref = instance.getPublicReference();
        assertEquals("http://localhost:8080/api/test", ref.toString());
    }
}
