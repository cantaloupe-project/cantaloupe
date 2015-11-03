package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;

public class IdentifierTest extends CantaloupeTestCase {

    public void testCompareTo() {
        Identifier id1 = new Identifier("cats");
        Identifier id2 = new Identifier("dogs");
        Identifier id3 = new Identifier("cats");
        assertTrue(id1.compareTo(id2) < 0);
        assertEquals(0, id1.compareTo(id3));
    }

    public void testEquals() {
        Identifier id1 = new Identifier("cats");
        Identifier id2 = new Identifier("cats");
        Identifier id3 = new Identifier("dogs");
        assertTrue(id1.equals(id2));
        assertFalse(id2.equals(id3));
    }

    public void testHashCode() {
        Identifier id1 = new Identifier("cats");
        Identifier id2 = new Identifier("cats");
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    public void testToString() {
        assertEquals("cats", new Identifier("cats").toString());
    }

}
