package edu.illinois.library.cantaloupe.image.xmp;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UtilsTest extends BaseTest {

    /* encapsulateXMP() */

    @Test
    void testEncapsulateXMP() {
        final String xmp = "<rdf:RDF></rdf:RDF>";
        String actual    = Utils.encapsulateXMP(xmp);
        assertTrue(actual.startsWith("<?xpacket"));
        assertTrue(actual.endsWith("<?xpacket end=\"r\"?>"));
    }

}