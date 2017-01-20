package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class OrientationTest extends BaseTest {

    @Test
    public void testForEXIFOrientation() {
        System.out.println(Integer.valueOf(""));

        assertEquals(Orientation.ROTATE_0, Orientation.forEXIFOrientation(1));
        assertEquals(Orientation.ROTATE_180, Orientation.forEXIFOrientation(3));
        assertEquals(Orientation.ROTATE_90, Orientation.forEXIFOrientation(6));
        assertEquals(Orientation.ROTATE_270, Orientation.forEXIFOrientation(8));

        for (int i : new int[] { 0, 2, 4, 5, 7, 9 }) {
            try {
                Orientation.forEXIFOrientation(i);
                fail("Expected exception");
            } catch (IllegalArgumentException e) {
                // pass
            }
        }
    }

    @Test
    public void testGetDegrees() {
        assertEquals(0, Orientation.ROTATE_0.getDegrees());
        assertEquals(90, Orientation.ROTATE_90.getDegrees());
        assertEquals(180, Orientation.ROTATE_180.getDegrees());
        assertEquals(270, Orientation.ROTATE_270.getDegrees());
    }

}
