package edu.illinois.library.cantaloupe.http;

import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.List;

import static org.junit.Assert.*;

public class ReferenceTest {

    private Reference instance;

    @Before
    public void setUp() {
        instance = new Reference("http://user:secret@example.org:81/p1/p2.jpg?q1=cats&q2=dogs#35");
    }

    @Test
    public void testDecode() {
        String uri = "http://example.org/cats%2Fdogs?cats=dogs";
        String expected = "http://example.org/cats/dogs?cats=dogs";
        assertEquals(expected, Reference.decode(uri));
    }

    @Test
    public void testCopyConstructor() {
        Reference copy = new Reference(instance);
        assertEquals(copy, instance);
    }

    @Test
    public void testStringConstructor() {
        String uri = "http://example.org/cats/dogs?cats=dogs";
        Reference ref = new Reference(uri);
        assertEquals(uri, ref.toString());
    }

    @Test
    public void testURIConstructor() throws Exception {
        URI uri = new URI("http://example.org/cats/dogs?cats=dogs");
        Reference ref = new Reference(uri);
        assertEquals(uri.toString(), ref.toString());
    }

    @Test
    public void testApplyProxyHeadersWithNoProxyHeaders() {
        final String expected = instance.toString();
        instance.applyProxyHeaders(new Headers());
        assertEquals(expected, instance.toString());
    }

    @Test
    public void testApplyProxyHeadersWithHTTPSchemeAndXForwardedProtoHTTP() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Proto", "HTTP");
        Reference ref = new Reference("http://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("http://bogus/cats", ref.toString());
    }

    @Test
    public void testApplyProxyHeadersWithHTTPSchemeAndXForwardedProtoHTTPS() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Proto", "HTTPS");
        Reference ref = new Reference("http://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("https://bogus/cats", ref.toString());
    }

    @Test
    public void testApplyProxyHeadersWithHTTPSSchemeAndXForwardedProtoHTTP() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Proto", "HTTP");
        Reference ref = new Reference("https://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("http://bogus/cats", ref.toString());
    }

    @Test
    public void testApplyProxyHeadersWithHTTPSSchemeAndXForwardedProtoHTTPS() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Proto", "HTTPS");
        Reference ref = new Reference("https://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("https://bogus/cats", ref.toString());
    }

    @Test
    public void testApplyProxyHeadersWithXForwardedHost() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Host", "example.org");
        Reference ref = new Reference("http://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("http://example.org/cats", ref.toString());
    }

    @Test
    public void testApplyProxyHeadersWithHTTPSchemeAndXForwardedHostContainingDefaultPort() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Host", "example.org:80");
        Reference ref = new Reference("http://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("http://example.org/cats", ref.toString());
    }

    @Test
    public void testApplyProxyHeadersWithHTTPSSchemeAndXForwardedHostContainingDefaultPort() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Host", "example.org:443");
        Reference ref = new Reference("https://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("https://example.org/cats", ref.toString());
    }

    @Test
    public void testApplyProxyHeadersWithXForwardedHostContainingCustomPort() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Host", "example.org:8080");
        Reference ref = new Reference("http://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("http://example.org:8080/cats", ref.toString());
    }

    @Test
    public void testApplyProxyHeadersWithXForwardedHostContainingCustomPortAndXForwardedPort() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Host", "example.org:8080");
        headers.set("X-Forwarded-Port", "8283");
        Reference ref = new Reference("http://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("http://example.org:8283/cats", ref.toString());
    }

    @Test
    public void testApplyProxyHeadersWithXForwardedHostContainingCustomPortAndXForwardedProto() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Host", "example.org:8080");
        headers.set("X-Forwarded-Proto", "HTTP");
        Reference ref = new Reference("http://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("http://example.org:8080/cats", ref.toString());
    }

    @Test
    public void testApplyProxyHeadersWithXForwardedPortMatchingDefaultHTTPPort() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Port", "80");
        Reference ref = new Reference("http://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("http://bogus/cats", ref.toString());
    }

    @Test
    public void testApplyProxyHeadersWithXForwardedPortMatchingDefaultHTTPSPort() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Port", "443");
        Reference ref = new Reference("https://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("https://bogus/cats", ref.toString());
    }

    @Test
    public void testApplyProxyHeadersWithXForwardedPort() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Port", "569");
        Reference ref = new Reference("http://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("http://bogus:569/cats", ref.toString());
    }

    @Test
    public void testApplyProxyHeadersWithXForwardedPath1() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Path", "/");
        Reference ref = new Reference("http://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("http://bogus", ref.toString());
    }

    @Test
    public void testApplyProxyHeadersWithXForwardedPath2() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Path", "/this/is/the/path");
        Reference ref = new Reference("http://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("http://bogus/this/is/the/path", ref.toString());
    }

    /**
     * Tests behavior when using chained {@literal X-Forwarded} headers.
     */
    @Test
    public void testApplyProxyHeadersUsingChainedXForwardedHeaders() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Proto", "http,https");
        headers.set("X-Forwarded-Host", "example.org,example.mil");
        headers.set("X-Forwarded-Port", "80,8080");
        headers.set("X-Forwarded-Path", "/animals/foxes,/animals/dogs");
        Reference ref = new Reference("http://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("http://example.org/animals/foxes", ref.toString());
    }

    @Test
    public void testEqualsWithEqualObjects() {
        // equal strings
        Reference ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        Reference ref2 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        assertEquals(ref1, ref2);

        // HTTP port expressed differently
        ref1 = new Reference("http://user:secret@example.org/cats/dogs?cats=dogs&foxes=hens#frag");
        ref2 = new Reference("http://user:secret@example.org:80/cats/dogs?cats=dogs&foxes=hens#frag");
        assertEquals(ref1, ref2);

        // HTTPS port expressed differently
        ref1 = new Reference("https://user:secret@example.org/cats/dogs?cats=dogs&foxes=hens#frag");
        ref2 = new Reference("https://user:secret@example.org:443/cats/dogs?cats=dogs&foxes=hens#frag");
        assertEquals(ref1, ref2);
    }

    @Test
    public void testEqualsWithUnequalObjects() {
        // different scheme
        Reference ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        Reference ref2 = new Reference("http://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        assertNotEquals(ref1, ref2);

        // different user
        ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        ref2 = new Reference("https://bob:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        assertNotEquals(ref1, ref2);

        // different secret
        ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        ref2 = new Reference("https://user:bla@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        assertNotEquals(ref1, ref2);

        // different host
        ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        ref2 = new Reference("https://user:secret@example.net:81/cats/dogs?cats=dogs&foxes=hens#frag");
        assertNotEquals(ref1, ref2);

        // different port
        ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        ref2 = new Reference("https://user:secret@example.org:82/cats/dogs?cats=dogs&foxes=hens#frag");
        assertNotEquals(ref1, ref2);

        // different path
        ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        ref2 = new Reference("https://user:secret@example.org:81/cats/wolves?cats=dogs&foxes=hens#frag");
        assertNotEquals(ref1, ref2);

        // different query
        ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        ref2 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=ants#frag");
        assertNotEquals(ref1, ref2);

        // different fragment
        ref1 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#frag");
        ref2 = new Reference("https://user:secret@example.org:81/cats/dogs?cats=dogs&foxes=hens#2");
        assertNotEquals(ref1, ref2);
    }

    @Test
    public void testGetFragment() {
        assertEquals("35", instance.getFragment());
    }

    @Test
    public void testGetHost() {
        assertEquals("example.org", instance.getHost());
    }

    @Test
    public void testGetPath() {
        assertEquals("/p1/p2.jpg", instance.getPath());
    }

    @Test
    public void testGetPathComponents() {
        List<String> components = instance.getPathComponents();
        assertEquals(2, components.size());
        assertEquals("p1", components.get(0));
        assertEquals("p2.jpg", components.get(1));
    }

    @Test
    public void testGetPathExtension() {
        assertEquals("jpg", instance.getPathExtension());
    }

    @Test
    public void testGetPort() {
        assertEquals(81, instance.getPort());
    }

    @Test
    public void testGetQuery() {
        Query expected = new Query();
        expected.set("q1", "cats");
        expected.set("q2", "dogs");
        Query actual = instance.getQuery();
        assertEquals(expected, actual);
    }

    @Test
    public void testGetRelativePath() {
        instance.setPath("/p1/p2/p3");
        assertEquals("/p2/p3", instance.getRelativePath("/p1"));
    }

    @Test
    public void testGetScheme() {
        assertEquals("http", instance.getScheme());
    }

    @Test
    public void testHashCode() {
        assertEquals(instance.toString().hashCode(), instance.hashCode());
    }

    @Test
    public void testSetPathComponent() {
        instance.setPathComponent(0, "new");
        assertEquals("/new/p2.jpg", instance.getPath());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void testSetPathComponentWithIndexOutOfBounds() {
        instance.setPathComponent(2, "new");
    }

    @Test(expected = NullPointerException.class)
    public void testSetQueryWithNullArgument() {
        instance.setQuery(null);
    }

    @Test
    public void testToURI() throws Exception {
        URI expected = new URI(instance.toString());
        URI actual = instance.toURI();
        assertEquals(expected, actual);
    }

    @Test
    public void testToString() {
        String expected = "http://user:secret@example.org:81/p1/p2.jpg?q1=cats&q2=dogs#35";
        String actual = instance.toString();
        assertEquals(expected, actual);
    }

}
