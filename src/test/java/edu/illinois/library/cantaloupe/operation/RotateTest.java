package edu.illinois.library.cantaloupe.operation;

import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.util.Map;

import static org.junit.Assert.*;

public class RotateTest {

    private Rotate rotate;

    @Before
    public void setUp() {
        this.rotate = new Rotate();
        assertEquals(0f, this.rotate.getDegrees(), 0.0000001f);
    }

    @Test
    public void testAddDegrees() {
        rotate.addDegrees(45f);
        assertEquals(45f, rotate.getDegrees(), 0.00000001f);
        rotate.addDegrees(340.5f);
        assertEquals(25.5f, rotate.getDegrees(), 0.00000001f);
        rotate.addDegrees(720f);
        assertEquals(25.5f, rotate.getDegrees(), 0.00000001f);
    }

    @Test
    public void testGetEffectiveSize() {
        Dimension fullSize = new Dimension(300, 200);
        assertEquals(fullSize, rotate.getResultingSize(fullSize));

        final int degrees = 45;
        rotate.setDegrees(degrees);

        final int expectedWidth = (int) Math.round(
                Math.abs(fullSize.width * Math.cos(degrees)) +
                        Math.abs(fullSize.height * Math.sin(degrees)));
        final int expectedHeight = (int) Math.round(
                Math.abs(fullSize.height * Math.cos(degrees)) +
                        Math.abs(fullSize.width * Math.sin(degrees)));
        Dimension expectedSize = new Dimension(expectedWidth, expectedHeight);
        assertEquals(expectedSize, rotate.getResultingSize(fullSize));
    }

    @Test
    public void testIsNoOp() {
        assertTrue(rotate.isNoOp());
        rotate.setDegrees(30);
        assertFalse(rotate.isNoOp());
        rotate.setDegrees(0.001f);
        assertFalse(rotate.isNoOp());
        rotate.setDegrees(0.00001f);
        assertTrue(rotate.isNoOp());
    }

    @Test
    public void testIsNoOpWithArguments() {
        Dimension fullSize = new Dimension(600, 400);
        OperationList opList = new OperationList();
        opList.add(new Crop(0, 0, 300, 200));
        assertTrue(rotate.isNoOp(fullSize, opList));
        rotate.setDegrees(30);
        assertFalse(rotate.isNoOp(fullSize, opList));
        rotate.setDegrees(0.001f);
        assertFalse(rotate.isNoOp(fullSize, opList));
        rotate.setDegrees(0.00001f);
        assertTrue(rotate.isNoOp(fullSize, opList));
    }

    @Test
    public void testSetDegrees() {
        float degrees = 50f;
        this.rotate.setDegrees(degrees);
        assertEquals(degrees, this.rotate.getDegrees(), 0.000001f);
    }

    @Test
    public void testSetLargeDegrees() {
        float degrees = 530f;
        try {
            this.rotate.setDegrees(degrees);
        } catch (IllegalArgumentException e) {
            assertEquals("Degrees must be between 0 and 360", e.getMessage());
        }
    }

    @Test
    public void testSetNegativeDegrees() {
        float degrees = -50f;
        try {
            this.rotate.setDegrees(degrees);
        } catch (IllegalArgumentException e) {
            assertEquals("Degrees must be between 0 and 360", e.getMessage());
        }
    }

    @Test
    public void testToMap() {
        this.rotate.setDegrees(15);
        Map<String,Object> map = this.rotate.toMap(new Dimension(0, 0));
        assertEquals(rotate.getClass().getSimpleName(), map.get("class"));
        assertEquals(15f, map.get("degrees"));
    }

    @Test
    public void testToString() {
        Rotate r = new Rotate(50f);
        assertEquals("50", r.toString());
        r = new Rotate(50.5f);
        assertEquals("50.5", r.toString());
        r = new Rotate(50.5000f);
        assertEquals("50.5", r.toString());
    }

}
