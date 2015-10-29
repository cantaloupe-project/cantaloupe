package edu.illinois.library.cantaloupe.request;

import edu.illinois.library.cantaloupe.CantaloupeTestCase;
import edu.illinois.library.cantaloupe.image.Identifier;
import org.restlet.data.Reference;

public class IdentifierTest extends CantaloupeTestCase {

    public void testEquals() {
        Identifier id1 = new Identifier("cats");
        Identifier id2 = new Identifier("cats");
        assertEquals(id1, id2);
        assertTrue(id1.equals(id2));
    }

    public void testFromUri() {
        Identifier id = Identifier.fromUri("cats");
        assertEquals("cats", id.getValue());

        String encodedId = Reference.encode("cats~`!@#$%^&*():;'<>,.?/\\");
        id = Identifier.fromUri(encodedId);
        assertEquals(Reference.decode(encodedId), id.getValue());
    }

    public void testHashCode() {
        Identifier id1 = new Identifier("cats");
        Identifier id2 = new Identifier("cats");
        assertEquals(id1.hashCode(), id2.hashCode());
    }

}
