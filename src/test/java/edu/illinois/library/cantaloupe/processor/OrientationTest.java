package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.image.Orientation;
import org.junit.Test;

import static org.junit.Assert.*;

public class OrientationTest {

    @Test
    public void testGetDegrees() {
        assertEquals(0, Orientation.ROTATE_0.getDegrees());
        assertEquals(90, Orientation.ROTATE_90.getDegrees());
        assertEquals(180, Orientation.ROTATE_180.getDegrees());
        assertEquals(270, Orientation.ROTATE_270.getDegrees());
    }

}
