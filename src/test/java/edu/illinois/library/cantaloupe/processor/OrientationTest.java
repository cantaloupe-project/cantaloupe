package edu.illinois.library.cantaloupe.processor;

import edu.illinois.library.cantaloupe.operation.Orientation;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Test;

import static org.junit.Assert.*;

public class OrientationTest extends BaseTest {

    @Test
    public void testGetDegrees() {
        assertEquals(0, Orientation.ROTATE_0.getDegrees());
        assertEquals(90, Orientation.ROTATE_90.getDegrees());
        assertEquals(180, Orientation.ROTATE_180.getDegrees());
        assertEquals(270, Orientation.ROTATE_270.getDegrees());
    }

}
