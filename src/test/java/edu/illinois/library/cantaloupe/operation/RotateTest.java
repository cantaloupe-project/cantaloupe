package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Dimension;
import edu.illinois.library.cantaloupe.image.ScaleConstraint;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

public class RotateTest extends BaseTest {

    private static final double DELTA = 0.00000001;

    private Rotate instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        this.instance = new Rotate();
        assertEquals(0.0, this.instance.getDegrees(), DELTA);
    }

    @Test
    public void addDegrees() {
        instance.addDegrees(45);
        assertEquals(45, instance.getDegrees(), DELTA);
        instance.addDegrees(340.5);
        assertEquals(25.5, instance.getDegrees(), DELTA);
        instance.addDegrees(720);
        assertEquals(25.5, instance.getDegrees(), DELTA);
    }

    @Test(expected = IllegalStateException.class)
    public void addDegreesWhenFrozenThrowsException() {
        instance.freeze();
        instance.addDegrees(15);
    }

    @Test
    public void equals() {
        assertEquals(instance, new Rotate());
        assertNotEquals(instance, new Rotate(1));
        assertNotEquals(instance, new Object());
    }

    @Test
    public void getResultingSizeWithNoRotation() {
        Dimension fullSize = new Dimension(300, 200);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        assertEquals(fullSize,
                instance.getResultingSize(fullSize, scaleConstraint));
    }

    @Test
    public void getResultingSizeWithRotation() {
        Dimension fullSize = new Dimension(300, 200);
        final int degrees = 30;
        instance.setDegrees(degrees);

        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Dimension actualSize = instance.getResultingSize(fullSize, scaleConstraint);

        assertEquals(360, actualSize.intWidth());
        assertEquals(323, actualSize.intHeight());
    }

    @Test
    public void getResultingSizeWithScaleConstraint() {
        Dimension fullSize = new Dimension(300, 200);
        final int degrees = 30;
        instance.setDegrees(degrees);

        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 2);
        Dimension actual = instance.getResultingSize(fullSize, scaleConstraint);

        assertEquals(360, actual.intWidth());
        assertEquals(323, actual.intHeight());
    }

    @Test
    public void hasEffect() {
        assertFalse(instance.hasEffect());
        instance.setDegrees(30);
        assertTrue(instance.hasEffect());
        instance.setDegrees(0.001);
        assertTrue(instance.hasEffect());
        instance.setDegrees(0.00001);
        assertFalse(instance.hasEffect());
    }

    @Test
    public void hasEffectWithArguments() {
        Dimension fullSize = new Dimension(600, 400);
        OperationList opList = new OperationList();
        opList.add(new CropByPixels(0, 0, 300, 200));

        assertFalse(instance.hasEffect(fullSize, opList));
        instance.setDegrees(30);
        assertTrue(instance.hasEffect(fullSize, opList));
        instance.setDegrees(0.001);
        assertTrue(instance.hasEffect(fullSize, opList));
        instance.setDegrees(0.00001);
        assertFalse(instance.hasEffect(fullSize, opList));
    }

    @Test
    public void setDegrees() {
        double degrees = 50;
        instance.setDegrees(degrees);
        assertEquals(degrees, instance.getDegrees(), 0.000001);
    }

    @Test
    public void setDegreesWith360Degrees() {
        double degrees = 360;
        try {
            instance.setDegrees(degrees);
        } catch (IllegalArgumentException e) {
            assertEquals("Degrees must be between 0 and 360", e.getMessage());
        }
    }

    @Test
    public void setDegreesWithLargeDegrees() {
        double degrees = 530;
        try {
            instance.setDegrees(degrees);
        } catch (IllegalArgumentException e) {
            assertEquals("Degrees must be between 0 and 360", e.getMessage());
        }
    }

    @Test
    public void setDegreesWithNegativeDegrees() {
        double degrees = -50;
        try {
            instance.setDegrees(degrees);
        } catch (IllegalArgumentException e) {
            assertEquals("Degrees must be between 0 and 360", e.getMessage());
        }
    }

    @Test(expected = IllegalStateException.class)
    public void setDegreesWhenFrozenThrowsException() {
        instance.freeze();
        instance.setDegrees(15);
    }

    @Test
    public void toMap() {
        instance.setDegrees(15);
        Dimension size = new Dimension(0, 0);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map = instance.toMap(size, scaleConstraint);
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
        assertEquals(15.0, map.get("degrees"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void toMapReturnsUnmodifiableMap() {
        Dimension fullSize = new Dimension(100, 100);
        ScaleConstraint scaleConstraint = new ScaleConstraint(1, 1);
        Map<String,Object> map = instance.toMap(fullSize, scaleConstraint);
        map.put("test", "test");
    }

    @Test
    public void testToString() {
        Rotate r = new Rotate(50);
        assertEquals("50", r.toString());
        r = new Rotate(50.5);
        assertEquals("50.5", r.toString());
    }

}
