package edu.illinois.library.cantaloupe.image;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class MetadataTest {

    private Metadata instance;

    @Before
    public void setUp() {
        instance = new Metadata();
    }

    @Test
    public void testEqualsWithEqualInstances() {
        Metadata m1 = new Metadata();
        m1.setEXIF("a");
        m1.setIPTC("b");
        m1.setXMP("<rdf:RDF>cats</rdf:RDF>");

        Metadata m2 = new Metadata();
        m2.setEXIF("a");
        m2.setIPTC("b");
        m2.setXMP("<rdf:RDF>cats</rdf:RDF>");

        assertEquals(m1, m2);
    }

    @Test
    public void testEqualsWithUnequalInstances() {
        Metadata m1 = new Metadata();
        m1.setEXIF("a");
        m1.setIPTC("b");
        m1.setXMP("<rdf:RDF>cats</rdf:RDF>");

        // different EXIF
        Metadata m2 = new Metadata();
        m2.setEXIF("z");
        m2.setIPTC("b");
        m2.setXMP("<rdf:RDF>cats</rdf:RDF>");
        assertNotEquals(m1, m2);

        // different IPTC
        m2 = new Metadata();
        m2.setEXIF("a");
        m2.setIPTC("z");
        m2.setXMP("<rdf:RDF>cats</rdf:RDF>");
        assertNotEquals(m1, m2);

        // different XMP
        m2 = new Metadata();
        m2.setEXIF("a");
        m2.setIPTC("b");
        m2.setXMP("<rdf:RDF>dogs</rdf:RDF>");
        assertNotEquals(m1, m2);
    }

    @Test
    public void testSetXMPTrimsData() {
        instance.setXMP("<??><rdf:RDF></rdf:RDF> <??>");
        assertTrue(instance.getXMP().startsWith("<rdf:RDF"));
        assertTrue(instance.getXMP().endsWith("</rdf:RDF>"));
    }

}
