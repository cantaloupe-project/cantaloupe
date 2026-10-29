package edu.illinois.library.cantaloupe.image.xmp;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RiotException;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

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

    /* readRDFXML() */

    @Test
    void testReadRDFXML() {
        String xmp = "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" " +
                "xmlns:dc=\"http://purl.org/dc/elements/1.1/\">" +
                "<rdf:Description rdf:about=\"\" dc:rights=\"Smith &amp; Jones\"/>" +
                "</rdf:RDF>";
        Model model = ModelFactory.createDefaultModel();
        Utils.readRDFXML(model, xmp, "http://example.com");
        assertEquals(1, model.size());
    }

    @Test
    void testReadRDFXMLWithUnescapedAmpersandThrowsWithoutLoggingErrors() {
        String xmp = "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" " +
                "xmlns:dc=\"http://purl.org/dc/elements/1.1/\">" +
                "<rdf:Description rdf:about=\"\" dc:rights=\"Smith & Jones\"/>" +
                "</rdf:RDF>";
        Logger logger = (Logger) LoggerFactory.getLogger("org.apache.jena.riot");
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            assertThrows(RiotException.class, () -> Utils.readRDFXML(
                    ModelFactory.createDefaultModel(), xmp, "http://example.com"));
            assertTrue(appender.list.stream()
                    .noneMatch(e -> e.getLevel().isGreaterOrEqual(Level.ERROR)));
        } finally {
            logger.detachAppender(appender);
        }
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
    void testTrimXMPWithSelfClosingRDFElement() {
        String xmp = "<?xpacket id=\"cats\"?>" +
                "<x:xmpmeta bla=\"dogs\">" +
                "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"/>" +
                "</x:xmpmeta>";
        String result = Utils.trimXMP(xmp);
        assertEquals("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"/>",
                result);
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