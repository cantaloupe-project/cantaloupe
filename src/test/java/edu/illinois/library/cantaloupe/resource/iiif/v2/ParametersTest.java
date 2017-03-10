package edu.illinois.library.cantaloupe.resource.iiif.v2;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.operation.Crop;
import edu.illinois.library.cantaloupe.operation.Operation;
import edu.illinois.library.cantaloupe.operation.OperationList;
import edu.illinois.library.cantaloupe.operation.Rotate;
import edu.illinois.library.cantaloupe.operation.Scale;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.*;

public class ParametersTest extends BaseTest {

    private static final float FUDGE = 0.00000001f;

    private Parameters instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new Parameters("identifier", "0,0,200,200", "pct:50", "5",
                "default", "jpg");
    }

    @Test
    public void testFromUri() {
        instance = Parameters.fromUri("bla/20,20,50,50/pct:90/15/bitonal.jpg");
        assertEquals("bla", instance.getIdentifier().toString());
        assertEquals("20,20,50,50", instance.getRegion().toString());
        assertEquals(90f, instance.getSize().getPercent(), FUDGE);
        assertEquals(15f, instance.getRotation().getDegrees(), FUDGE);
        assertEquals(Quality.BITONAL, instance.getQuality());
        assertEquals(Format.JPG, instance.getOutputFormat());

        try {
            Parameters.fromUri("bla/20,20,50,50/15/bitonal.jpg");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
        try {
            Parameters.fromUri("bla/20,20,50,50/pct:90/15/bitonal");
            fail("Expected exception");
        } catch (IllegalArgumentException e) {
            // pass
        }
    }

    @Test
    public void testCompareTo() {
        // TODO: write this
    }

    @Test
    public void testToOperationList() throws Exception {
        final OperationList opList = instance.toOperationList();
        Iterator<Operation> it = opList.iterator();
        assertTrue(it.next() instanceof Crop);
        assertTrue(it.next() instanceof Scale);
        assertTrue(it.next() instanceof Rotate);
    }

}
