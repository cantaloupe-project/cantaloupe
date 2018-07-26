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
