package edu.illinois.library.cantaloupe.resource;

import edu.illinois.library.cantaloupe.CantalouperApplication;
import edu.illinois.library.cantaloupe.http.Cookies;
import edu.illinois.library.cantaloupe.http.Headers;
import edu.illinois.library.cantaloupe.http.Method;
import edu.illinois.library.cantaloupe.http.Reference;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = CantalouperApplication.class, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
    "spring.main.banner-mode=off",
    "logging.level.root=WARN",
    "http.enabled=true",
    "http.port=0"
})
class RequestTest {

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
        sr.addHeader("Cookie", "fruit=apples; animal=cats");
        sr.addHeader("Cookie", "shape=cube; car=ford");
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
        sr.addHeader("Cookie", "cats=yes");
        sr.addHeader("Accept", "text/plain");
        instance = new Request(sr);

        Headers headers = instance.getHeaders();
        assertEquals(2, headers.size());
        assertEquals("cats=yes", headers.getFirstValue("Cookie"));
        assertEquals("text/plain", headers.getFirstValue("Accept"));
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
        String path = "/cats";
        String query = "query=yes";
        MockHttpServletRequest sr = new MockHttpServletRequest();
        sr.setRequestURI(path);
        sr.setQueryString(query);
        sr.setServerName("example.org");
        sr.setScheme("http");
        sr.setServerPort(80);
        instance = new Request(sr);

        Reference expected = new Reference("http://example.org" + path + "?" + query);
        assertEquals(expected, instance.getReference());
    }

    @Test
    void testGetReferenceWithCustomPort() {
        String path = "/test";
        MockHttpServletRequest sr = new MockHttpServletRequest();
        sr.setRequestURI(path);
        sr.setServerName("localhost");
        sr.setScheme("http");
        sr.setServerPort(8080);
        instance = new Request(sr);

        Reference expected = new Reference("http://localhost:8080" + path);
        assertEquals(expected, instance.getReference());
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

    @Test
    void testGetReferenceWithEncodedPath() {
        String path = "/test%20path";
        MockHttpServletRequest sr = new MockHttpServletRequest();
        sr.setRequestURI(path);
        sr.setServerName("localhost");
        sr.setScheme("http");
        sr.setServerPort(8080);
        instance = new Request(sr);

        Reference expected = new Reference("http://localhost:8080" + path);
        assertEquals(expected, instance.getReference());
    }
}
