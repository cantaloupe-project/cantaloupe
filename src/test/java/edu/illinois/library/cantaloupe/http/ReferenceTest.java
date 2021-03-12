package edu.illinois.library.cantaloupe.http;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ReferenceTest extends BaseTest {

    private Reference instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Reference("http://user:secret@example.org:81/p1/p2.jpg?q1=cats&q2=dogs#35");
    }

    @Test
    void testDecode() {
        String uri      = "http://example.org/cats%2Fdogs?cats=dogs";
        String expected = "http://example.org/cats/dogs?cats=dogs";
        assertEquals(expected, Reference.decode(uri));
    }

    @Test
    void testEncode() {
        String str      = "dogs?cats=dogs";
        String expected = "dogs%3Fcats%3Ddogs";
        assertEquals(expected, Reference.encode(str));
    }

    @Test
    void testCopyConstructor() {
        Reference copy = new Reference(instance);
        assertEquals(copy, instance);
        assertNotSame(copy.getQuery(), instance.getQuery());
    }

    @Test
    void testCopyConstructorClonesQuery() {
        Reference copy = new Reference(instance);
        assertNotSame(instance.getQuery(), copy.getQuery());
    }

    @Test
    void testStringConstructor() {
        String uri = "http://example.org/cats/dogs?cats=dogs";
        Reference ref = new Reference(uri);
        assertEquals(uri, ref.toString());
    }

    @Test
    void testStringConstructorWithUnencodedCharacters() {
        String uri = "http://example.org/cats`/`dogs?cats=dogs`";
        Reference ref = new Reference(uri);
        assertEquals("http://example.org/cats%60/%60dogs?cats=dogs%60",
                ref.toString());
    }

    @Test
    void testStringConstructorWithEncodedCharacters() {
        String uri = "http://example.org/cats%2Fdogs";
        Reference ref = new Reference(uri);
        assertEquals("http://example.org/cats%2Fdogs", ref.toString());
    }

    @Test
    void testURIConstructor() throws Exception {
        URI uri = new URI("http://example.org/cats/dogs?cats=dogs");
        Reference ref = new Reference(uri);
        assertEquals(uri.toString(), ref.toString());
    }

    @Test
    void testApplyProxyHeadersWithNoProxyHeaders() {
        final String expected = instance.toString();
        instance.applyProxyHeaders(new Headers());
        assertEquals(expected, instance.toString());
    }

    @Test
    void testApplyProxyHeadersWithHTTPSchemeAndXForwardedProtoHTTP() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Proto", "HTTP");
        Reference ref = new Reference("http://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("http://bogus/cats", ref.toString());
    }

    @Test
    void testApplyProxyHeadersWithHTTPSchemeAndXForwardedProtoHTTPS() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Proto", "HTTPS");
        Reference ref = new Reference("http://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("https://bogus/cats", ref.toString());
    }

    @Test
    void testApplyProxyHeadersWithHTTPSSchemeAndXForwardedProtoHTTP() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Proto", "HTTP");
        Reference ref = new Reference("https://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("http://bogus/cats", ref.toString());
    }

    @Test
    void testApplyProxyHeadersWithHTTPSSchemeAndXForwardedProtoHTTPS() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Proto", "HTTPS");
        Reference ref = new Reference("https://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("https://bogus/cats", ref.toString());
    }

    @Test
    void testApplyProxyHeadersWithXForwardedHost() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Host", "example.org");
        Reference ref = new Reference("http://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("http://example.org/cats", ref.toString());
    }

    @Test
    void testApplyProxyHeadersWithHTTPSchemeAndXForwardedHostContainingDefaultPort() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Host", "example.org:80");
        Reference ref = new Reference("http://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("http://example.org/cats", ref.toString());
    }

    @Test
    void testApplyProxyHeadersWithHTTPSSchemeAndXForwardedHostContainingDefaultPort() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Host", "example.org:443");
        Reference ref = new Reference("https://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("https://example.org/cats", ref.toString());
    }

    @Test
    void testApplyProxyHeadersWithXForwardedHostContainingCustomPort() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Host", "example.org:8080");
        Reference ref = new Reference("http://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("http://example.org:8080/cats", ref.toString());
    }

    @Test
    void testApplyProxyHeadersWithXForwardedHostContainingCustomPortAndXForwardedPort() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Host", "example.org:8080");
        headers.set("X-Forwarded-Port", "8283");
        Reference ref = new Reference("http://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("http://example.org:8283/cats", ref.toString());
    }

    @Test
    void testApplyProxyHeadersWithXForwardedHostContainingCustomPortAndXForwardedProto() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Host", "example.org:8080");
        headers.set("X-Forwarded-Proto", "HTTP");
        Reference ref = new Reference("http://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("http://example.org:8080/cats", ref.toString());
    }

    @Test
    void testApplyProxyHeadersWithXForwardedPortMatchingDefaultHTTPPort() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Port", "80");
        Reference ref = new Reference("http://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("http://bogus/cats", ref.toString());
    }

    @Test
    void testApplyProxyHeadersWithXForwardedPortMatchingDefaultHTTPSPort() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Port", "443");
        Reference ref = new Reference("https://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("https://bogus/cats", ref.toString());
    }

    @Test
    void testApplyProxyHeadersWithXForwardedPort() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Port", "569");
        Reference ref = new Reference("http://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("http://bogus:569/cats", ref.toString());
    }

    @Test
    void testApplyProxyHeadersWithXForwardedPath1() {
        Headers headers = new Headers();
        headers.set("X-Forwarded-Path", "/");
        Reference ref = new Reference("http://bogus/cats");
        ref.applyProxyHeaders(headers);
        assertEquals("http://bogus", ref.toString());
    }

    @Test
    void testApplyProxyHeadersWithXForwardedPath2() {
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
    void testApplyProxyHeadersUsingChainedXForwardedHeaders() {
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
    void testEqualsWithEqualObjects() {
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
    void testEqualsWithUnequalObjects() {
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
    void testGetAuthorityWithUserInfo() {
        assertEquals("user:secret@example.org:81", instance.getAuthority());
    }

    @Test
    void testGetAuthorityWithoutUserInfo() {
        instance.setUser(null);
        instance.setSecret(null);
        assertEquals("example.org:81", instance.getAuthority());
    }

    @Test
    void testGetAuthorityWithHTTPSchemeAndStandardPort() {
        instance.setScheme("http");
        instance.setPort(80);
        assertEquals("user:secret@example.org", instance.getAuthority());
    }

    @Test
    void testGetAuthorityWithHTTPSchemeAndNonStandardPort() {
        instance.setScheme("http");
        instance.setPort(81);
        assertEquals("user:secret@example.org:81", instance.getAuthority());
    }

    @Test
    void testGetAuthorityWithHTTPSSchemeAndStandardPort() {
        instance.setScheme("httpS");
        instance.setPort(443);
        assertEquals("user:secret@example.org", instance.getAuthority());
    }

    @Test
    void testGetAuthorityWithHTTPSSchemeAndNonStandardPort() {
        instance.setScheme("httpS");
        instance.setPort(444);
        assertEquals("user:secret@example.org:444", instance.getAuthority());
    }

    @Test
    void testGetFragment() {
        assertEquals("35", instance.getFragment());
    }

    @Test
    void testGetHost() {
        assertEquals("example.org", instance.getHost());
    }

    @Test
    void testGetPath() {
        assertEquals("/p1/p2.jpg", instance.getPath());
    }

    @Test
    void testGetPathComponents() {
        List<String> components = instance.getPathComponents();
        assertEquals(2, components.size());
        assertEquals("p1", components.get(0));
        assertEquals("p2.jpg", components.get(1));
    }

    @Test
    void testGetPathExtension() {
        assertEquals("jpg", instance.getPathExtension());
    }

    @Test
    void testGetPort() {
        assertEquals(81, instance.getPort());
    }

    @Test
    void testGetQuery() {
        Query expected = new Query();
        expected.set("q1", "cats");
        expected.set("q2", "dogs");
        Query actual = instance.getQuery();
        assertEquals(expected, actual);
    }

    @Test
    void testGetRelativePath() {
        instance.setPath("/p1/p2/p3");
        assertEquals("/p2/p3", instance.getRelativePath("/p1"));
    }

    @Test
    void testGetScheme() {
        assertEquals("http", instance.getScheme());
    }

    @Test
    void testHashCode() {
        assertEquals(instance.toString().hashCode(), instance.hashCode());
    }

    @Test
    void testSetPathComponent() {
        instance.setPathComponent(0, "new");
        assertEquals("/new/p2.jpg", instance.getPath());
    }

    @Test
    void testSetPathComponentWithIndexOutOfBounds() {
        assertThrows(IndexOutOfBoundsException.class,
                () -> instance.setPathComponent(2, "new"));
    }

    @Test
    void testSetQueryWithNullArgument() {
        assertThrows(IllegalArgumentException.class,
                () -> instance.setQuery(null));
    }

    @Test
    void testToString1() {
        String expected = "http://user:secret@example.org:81/p1/p2.jpg?q1=cats&q2=dogs#35";
        String actual = instance.toString();
        assertEquals(expected, actual);
    }

    @Test
    void testToString1Encoding() {
        instance        = new Reference("http://user`:secret`@example.org:81/p`/p2.jpg?q1=cats`");
        String expected = "http://user%60:secret%60@example.org:81/p%60/p2.jpg?q1=cats%60";
        String actual   = instance.toString();
        assertEquals(expected, actual);
    }

    @Test
    void testToString2() {
        String expected = "http://user:secret@example.org:81/p1/p2.jpg?q1=cats&q2=dogs#35";
        String actual = instance.toString();
        assertEquals(expected, actual);
    }

    @Test
    void testToString2WithEncodingPath() {
        instance        = new Reference("http://user`:secret`@example.org:81/p`/p2.jpg?q1=cats`");
        String expected = "http://user%60:secret%60@example.org:81%2Fp%2560%2Fp2.jpg?q1=cats%60";
        String actual   = instance.toString(true);
        assertEquals(expected, actual);
    }

    @Test
    void testToString2WithNotEncodingPath() {
        instance        = new Reference("http://user`:secret`@example.org:81/pp%2Fpp2.jpg?q1=cats`");
        String expected = "http://user%60:secret%60@example.org:81/pp%2Fpp2.jpg?q1=cats%60";
        String actual   = instance.toString(false);
        assertEquals(expected, actual);
    }

}
