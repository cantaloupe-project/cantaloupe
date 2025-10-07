package edu.illinois.library.cantaloupe.resource;

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
    void testGetCookies() {
        MockHttpServletRequest sr = new MockHttpServletRequest();
        sr.getHeaders().put("Cookie", List.of("fruit=apples; animal=cats",
                "shape=cube; car=ford"));
        instance = new Request(sr);

        Cookies cookies = instance.getCookies();
        assertEquals(4, cookies.size());
        assertEquals("apples", cookies.getFirstValue("fruit"));
        assertEquals("cats", cookies.getFirstValue("animal"));
        assertEquals("cube", cookies.getFirstValue("shape"));
        assertEquals("ford", cookies.getFirstValue("car"));
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

}
