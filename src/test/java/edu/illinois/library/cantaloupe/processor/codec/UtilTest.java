package edu.illinois.library.cantaloupe.processor.codec;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class UtilTest extends BaseTest {

    @Test
    public void testTrimXMPWithTrimmableXMP() {
        String xmp = "<?xpacket id=\"cats\"?>" +
                "<x:xmpmeta bla=\"dogs\">" +
                "<rdf:RDF foxes=\"bugs\">" +
                "</rdf:RDF>" +
                "</x:xmpmeta>";
        String result = Util.trimXMP(xmp);
        assertTrue(result.startsWith("<rdf:RDF"));
        assertTrue(result.endsWith("</rdf:RDF>"));
    }

    @Test
    public void testTrimXMPWithNonTrimmableXMP() {
        String xmp = "<rdf:RDF foxes=\"bugs\">" +
                "</rdf:RDF>";
        String result = Util.trimXMP(xmp);
        assertSame(xmp, result);
    }

    @Test(expected = NullPointerException.class)
    public void testTrimXMPWithNullArgument() {
        Util.trimXMP(null);
    }

}
