package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class IIIFRequestTest extends BaseTest {

    private MockHttpServletRequest sr;
    private IIIFRequest instance;
    
    @BeforeEach
    void beforeEach() {
        sr = new MockHttpServletRequest();
        instance = new IIIFRequest(sr, Collections.emptyList(), Configuration.getInstance());
    }

    @Test
    void testGetContextPath() {
        final String path = "/the-new-path";
        sr.setContextPath(path);
        assertEquals(path, instance.getContextPath());
    }

    @Test
    void testGetHeaders() {
        sr.getHeaders().put("Cookie", List.of("cats=yes"));
        sr.getHeaders().put("Accept", List.of("text/plain"));

        Headers headers = instance.getHeaders();
        assertEquals(2, headers.size());
        assertEquals("cats=yes", headers.getFirstValue("Cookie"));
        assertEquals("text/plain", headers.getFirstValue("Accept"));
    }

    @Test
    void testGetReference() {
        String url = "http://example.org/cats?query=yes";
        sr.setRequestURL(url);

        assertEquals(new Reference(url), instance.getReference());
    }

    @Test
    void testGetRemoteAddr() {
        String addr = "10.2.5.3";
        sr.setRemoteAddr(addr);

        assertEquals(addr, instance.getRemoteAddr());
    }

    @Test
    void testGetServletRequest() {
        assertSame(sr, instance.getServletRequest());
    }


    /**
     * Tests behavior of {@link IIIFRequest#getPublicReference()} when
     * using {@link Key#BASE_URI}.
     */
    @Test
    void testGetPublicReferenceUsingConfiguration() {
        final String baseURI = "http://example.net/base";
        Configuration.getInstance().setProperty(Key.BASE_URI, baseURI);

        sr.setContextPath("/base");
        sr.setRequestURL("http://example.org/base/llamas");

        Reference ref = instance.getPublicReference();
        assertEquals(baseURI + "/llamas", ref.toString());
    }

    /**
     * Tests behavior of {@link IIIFRequest#getPublicReference()} when
     * using {@literal X-Forwarded} headers.
     *
     * This isn't a thorough test of every possible header/URI combination.
     * See {@link Reference#applyProxyHeaders(Headers)} for those.
     */
    @Test
    void testGetPublicReferenceUsingXForwardedHeaders() {
        sr.setContextPath("");
        sr.setRequestURL("http://bogus/cats");

        Headers headers = instance.getHeaders();
        headers.set("X-Forwarded-Proto", "HTTP");
        headers.set("X-Forwarded-Host", "example.org");
        headers.set("X-Forwarded-Port", "80");
        headers.set("X-Forwarded-Path", "/");

        Reference ref = instance.getPublicReference();
        assertEquals("http://example.org/cats", ref.toString());
    }

    /**
     * Tests behavior of {@link IIIFRequest#getPublicReference()} when
     * using neither {@link Key#BASE_URI} nor {@literal X-Forwarded} headers.
     */
    @Test
    void testGetPublicReferenceFallsBackToHTTPRequest() {
        String resourceURI = "http://example.net/cats/dogs";
        sr.setContextPath("/cats");
        sr.setRequestURL(resourceURI);

        Reference ref = instance.getPublicReference();
        assertEquals(resourceURI, ref.toString());
    }

    /**
     * Tests behavior of {@link IIIFRequest#getPublicReference()} when
     * using neither {@link Key#BASE_URI} nor {@literal X-Forwarded} headers.
     */
    @Test
    void testGetPublicReferenceFallsBackToHTTPSRequest() {
        String resourceURI = "https://example.net/cats/dogs";

        sr.setContextPath("/cats");
        sr.setRequestURL(resourceURI);

        Reference ref = instance.getPublicReference();
        assertEquals(resourceURI, ref.toString());
    }

    @Test
    void testGetPublicReferenceOmitsQuery() {
        String resourceURI = "https://example.net/cats/dogs?arg=value";
        String expected = "https://example.net/cats/dogs";

        sr.setContextPath("/cats");
        sr.setRequestURL(resourceURI);

        Reference ref = instance.getPublicReference();
        assertEquals(expected, ref.toString());
    }
}
