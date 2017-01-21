package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class IdentifierTest extends BaseTest {

    private Identifier instance;

    @Before
    public void setUp() {
        instance = new Identifier("cats");
    }

    @Test
    public void testConstructor() {
        assertEquals("cats", instance.toString());

        try {
            new Identifier(null);
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
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
    public void testToFilename() {
        assertEquals("0832c1202da8d382318e329a7c133ea0", instance.toFilename());
    }

    @Test
    public void testToString() {
        assertEquals("cats", instance.toString());
    }

}
