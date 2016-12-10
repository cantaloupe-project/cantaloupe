package edu.illinois.library.cantaloupe.operation;

import org.junit.Test;

import static org.junit.Assert.*;

public class IdentifierTest {

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
        Identifier id = new Identifier("cats");
        assertTrue(id.equals("cats"));
        assertFalse(id.equals("dogs"));
    }

    @Test
    public void testHashCode() {
        Identifier id1 = new Identifier("cats");
        Identifier id2 = new Identifier("cats");
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("cats", new Identifier("cats").toString());
    }

}
