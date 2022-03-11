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

    /* trimXMP() */

    @Test
    void testTrimXMPWithTrimmableXMP() {
        String xmp = "<?xpacket id=\"cats\"?>" +
                "<x:xmpmeta bla=\"dogs\">" +
                "<rdf:RDF foxes=\"bugs\">" +
                "</rdf:RDF>" +
                "</x:xmpmeta>";
        String result = Utils.trimXMP(xmp);
        assertTrue(result.startsWith("<rdf:RDF"));
        assertTrue(result.endsWith("</rdf:RDF>"));
    }

    @Test
    void testTrimXMPWithNonTrimmableXMP() {
        String xmp = "<rdf:RDF foxes=\"bugs\">" +
                "</rdf:RDF>";
        String result = Utils.trimXMP(xmp);
        assertSame(xmp, result);
    }

    @Test
    void testTrimXMPWithNullArgument() {
        assertThrows(NullPointerException.class,
                () -> Utils.trimXMP(null));
    }

}