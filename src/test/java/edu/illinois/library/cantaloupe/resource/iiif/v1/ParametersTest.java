package edu.illinois.library.cantaloupe.resource.iiif.v1;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.processor.UnsupportedOutputFormatException;
import edu.illinois.library.cantaloupe.resource.IllegalClientArgumentException;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

public class ParametersTest extends BaseTest {

    private Parameters instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new Parameters(new Identifier("identifier"),
                "0,0,200,200", "pct:50", "5", "native", "jpg");
    }

    @Test
    public void testFromUri() {
        Parameters params =
                Parameters.fromUri("bla/20,20,50,50/pct:90/15/native.jpg");
        assertEquals("bla", params.getIdentifier().toString());
        assertEquals("20,20,50,50", params.getRegion().toString());
        assertEquals(90f, params.getSize().getPercent(), 0.0000001f);
        assertEquals(15f, params.getRotation().getDegrees(), 0.0000001f);
        assertEquals(Quality.NATIVE, params.getQuality());
        assertEquals(Format.JPG, params.getOutputFormat());
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testFromUriWithInvalidURI1() {
        Parameters.fromUri("bla/20,20,50,50/15/bitonal.jpg");
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testFromUriWithInvalidURI2() {
        Parameters.fromUri("bla/20,20,50,50/pct:90/15/bitonal");
    }

    @Test(expected = IllegalClientArgumentException.class)
    public void testConstructorWithInvalidQuality()
            throws Exception {
        new Parameters(new Identifier("identifier"), "0,0,200,200", "pct:50",
                "5", "bogus", "jpg");
    }

    @Test(expected = UnsupportedOutputFormatException.class)
    public void testConstructorWithUnsupportedOutputFormat()
            throws Exception {
        new Parameters(new Identifier("identifier"), "0,0,200,200", "pct:50",
                "5", "native", "bogus");
    }

    @Test
    public void testCompareTo() {
        // TODO: write this
    }

    @Test
    public void testToOperationList() {
        final OperationList opList = instance.toOperationList();
        Iterator<Operation> it = opList.iterator();
        assertTrue(it.next() instanceof Crop);
        assertTrue(it.next() instanceof Scale);
        assertTrue(it.next() instanceof Rotate);
    }

}
