package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.util.Map;

import static org.junit.Assert.*;

public class RotateTest extends BaseTest {

    private Rotate rotate;

    @Before
    public void setUp() throws Exception {
        super.setUp();

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
    public void testEquals() {
        assertTrue(rotate.equals(new Rotate()));
        assertFalse(rotate.equals(new Rotate(1)));
        assertFalse(rotate.equals(new Object()));
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
    public void testHasEffect() {
        assertFalse(rotate.hasEffect());
        rotate.setDegrees(30);
        assertTrue(rotate.hasEffect());
        rotate.setDegrees(0.001f);
        assertTrue(rotate.hasEffect());
        rotate.setDegrees(0.00001f);
        assertFalse(rotate.hasEffect());
    }

    @Test
    public void testHasEffectWithArguments() {
        Dimension fullSize = new Dimension(600, 400);
        OperationList opList = new OperationList();
        opList.add(new Crop(0, 0, 300, 200));
        assertFalse(rotate.hasEffect(fullSize, opList));
        rotate.setDegrees(30);
        assertTrue(rotate.hasEffect(fullSize, opList));
        rotate.setDegrees(0.001f);
        assertTrue(rotate.hasEffect(fullSize, opList));
        rotate.setDegrees(0.00001f);
        assertFalse(rotate.hasEffect(fullSize, opList));
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
        this.rotate.setFillColor(Color.fromString("#FFFFFF"));
        Map<String,Object> map = this.rotate.toMap(new Dimension(0, 0));
        assertEquals(rotate.getClass().getSimpleName(), map.get("class"));
        assertEquals(15f, map.get("degrees"));
        assertEquals("#FFFFFF", map.get("fill_color"));
    }

    @Test
    public void testToString() {
        Rotate r = new Rotate(50f, Color.fromString("#FFFFFF"));
        assertEquals("50_#FFFFFF", r.toString());
        r = new Rotate(50.5f, Color.fromString("#FFFFFF"));
        assertEquals("50.5_#FFFFFF", r.toString());
        r = new Rotate(50.5000f);
        assertEquals("50.5_null", r.toString());
    }

}
