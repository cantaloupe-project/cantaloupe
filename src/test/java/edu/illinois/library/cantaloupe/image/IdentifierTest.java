package edu.illinois.library.cantaloupe.image;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.io.StringWriter;

import static org.junit.Assert.*;

public class IdentifierTest extends BaseTest {

    private Identifier instance;

    @Before
    public void setUp() {
        instance = new Identifier("cats");
    }

    @Test
    public void testSerialization() throws Exception {
        Identifier identifier = new Identifier("cats");
        try (StringWriter writer = new StringWriter()) {
            new ObjectMapper().writeValue(writer, identifier);
            assertEquals("\"cats\"", writer.toString());
        }
    }

    @Test
    public void testDeserialization() throws Exception {
        Identifier identifier = new ObjectMapper().readValue("\"cats\"",
                Identifier.class);
        assertEquals("cats", identifier.toString());
    }

    @Test
    public void testFromURIPathComponent() {
        Configuration.getInstance().setProperty(Key.SLASH_SUBSTITUTE, "BUG");

        String pathComponent = "catsBUG%3Adogs" +
                new ScaleConstraint(3, 4).toIdentifierSuffix();
        Identifier actual = Identifier.fromURIPathComponent(pathComponent);
        Identifier expected = new Identifier("cats/:dogs");
        assertEquals(expected, actual);
    }

    @Test
    public void testConstructor() {
        assertEquals("cats", instance.toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNullArgument() {
        new Identifier(null);
    }

    @Test
    public void testCompareTo() {
        Identifier id1 = new Identifier("cats");
        Identifier id2 = new Identifier("dogs");
        Identifier id3 = new Identifier("cats");
        assertTrue(id1.compareTo(id2) < 0);
        assertEquals(0, id1.compareTo(id3));
    }

    @Test
    public void testEqualsWithIdentifier() {
        Identifier id1 = new Identifier("cats");
        Identifier id2 = new Identifier("cats");
        Identifier id3 = new Identifier("dogs");
        assertTrue(id1.equals(id2));
        assertFalse(id2.equals(id3));
    }

    @Test
    public void testEqualsWithString() {
        assertTrue(instance.equals("cats"));
        assertFalse(instance.equals("dogs"));
    }

    @Test
    public void testHashCode() {
        Identifier id1 = new Identifier("cats");
        Identifier id2 = new Identifier("cats");
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("cats", instance.toString());
    }

}
