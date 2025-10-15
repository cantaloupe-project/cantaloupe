package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.http.Cookies;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Reference;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RequestTest extends BaseTest {

    private Request instance;

    @Test
    void testGetContextPath() {
        final String path = "/the-new-path";
        MockHttpServletRequest sr = new MockHttpServletRequest();
        sr.setContextPath(path);
        instance = new Request(sr);
        assertEquals(path, instance.getContextPath());
    }

    @Test
    void testGetHeaders() {
        MockHttpServletRequest sr = new MockHttpServletRequest();
        sr.getHeaders().put("Cookie", List.of("cats=yes"));
        sr.getHeaders().put("Accept", List.of("text/plain"));
        instance = new Request(sr);

        Headers headers = instance.getHeaders();
        assertEquals(2, headers.size());
        assertEquals("cats=yes", headers.getFirstValue("Cookie"));
        assertEquals("text/plain", headers.getFirstValue("Accept"));
    }

    @Disabled // TODO: write this
    @Test
    void testGetInputStream() {
    }

    @Test
    void testGetMethod() {
        MockHttpServletRequest sr = new MockHttpServletRequest();
        sr.setMethod("PUT");
        instance = new Request(sr);

        assertEquals(Method.PUT, instance.getMethod());
    }

    @Test
    void testGetReference() {
        String url = "http://example.org/cats?query=yes";
        MockHttpServletRequest sr = new MockHttpServletRequest();
        sr.setRequestURL(url);
        instance = new Request(sr);

        assertEquals(new Reference(url), instance.getReference());
    }

    @Test
    void testGetRemoteAddr() {
        String addr = "10.2.5.3";
        MockHttpServletRequest sr = new MockHttpServletRequest();
        sr.setRemoteAddr(addr);
        instance = new Request(sr);

        assertEquals(addr, instance.getRemoteAddr());
    }

    @Test
    void testGetServletRequest() {
        MockHttpServletRequest sr = new MockHttpServletRequest();
        instance = new Request(sr);

        assertSame(sr, instance.getServletRequest());
    }


    /**
     * Tests behavior of {@link AbstractResource#getPublicReference()} when
     * using {@link Key#BASE_URI}.
     */
    @Test
    void testGetPublicReferenceUsingConfiguration() {
        final String baseURI = "http://example.net/base";
        Configuration.getInstance().setProperty(Key.BASE_URI, baseURI);

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setContextPath("/base");
        servletRequest.setRequestURL("http://example.org/base/llamas");

        instance = new Request(servletRequest);
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
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();

        servletRequest.setContextPath("");
        servletRequest.setRequestURL("http://bogus/cats");

        instance = new Request(servletRequest);
        Headers headers = instance.getHeaders();
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
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setContextPath("/cats");
        servletRequest.setRequestURL(resourceURI);

        instance = new Request(servletRequest);
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
        MockHttpServletRequest servletRequest = new MockHttpServletRequest();

        servletRequest.setContextPath("/cats");
        servletRequest.setRequestURL(resourceURI);

        instance = new Request(servletRequest);
        Reference ref = instance.getPublicReference();
        assertEquals(resourceURI, ref.toString());
    }

    @Test
    void testGetPublicReferenceOmitsQuery() {
        String resourceURI = "https://example.net/cats/dogs?arg=value";
        String expected = "https://example.net/cats/dogs";

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        servletRequest.setContextPath("/cats");
        servletRequest.setRequestURL(resourceURI);

        instance = new Request(servletRequest);
        Reference ref = instance.getPublicReference();
        assertEquals(expected, ref.toString());
    }
}
