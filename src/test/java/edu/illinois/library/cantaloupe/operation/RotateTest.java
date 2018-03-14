package edu.illinois.library.cantaloupe.operation;

import edu.illinois.library.cantaloupe.image.Format;
import edu.illinois.library.cantaloupe.image.Identifier;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.util.Map;

import static org.junit.Assert.*;

public class RotateTest extends BaseTest {

    private Rotate instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        this.instance = new Rotate();
        assertEquals(0f, this.instance.getDegrees(), 0.0000001f);
    }

    @Test
    public void addDegrees() {
        instance.addDegrees(45f);
        assertEquals(45f, instance.getDegrees(), 0.00000001f);
        instance.addDegrees(340.5f);
        assertEquals(25.5f, instance.getDegrees(), 0.00000001f);
        instance.addDegrees(720f);
        assertEquals(25.5f, instance.getDegrees(), 0.00000001f);
    }

    @Test(expected = IllegalStateException.class)
    public void addDegreesWhenFrozenThrowsException() {
        instance.freeze();
        instance.addDegrees(15);
    }

    @Test
    public void equals() {
        assertTrue(instance.equals(new Rotate()));
        assertFalse(instance.equals(new Rotate(1)));
        assertFalse(instance.equals(new Object()));
    }

    @Test
    public void getResultingSize() {
        Dimension fullSize = new Dimension(300, 200);
        assertEquals(fullSize, instance.getResultingSize(fullSize));

        final int degrees = 30;
        instance.setDegrees(degrees);

        Dimension expectedSize = new Dimension(360, 323);
        assertEquals(expectedSize, instance.getResultingSize(fullSize));
    }

    @Test
    public void hasEffect() {
        assertFalse(instance.hasEffect());
        instance.setDegrees(30);
        assertTrue(instance.hasEffect());
        instance.setDegrees(0.001f);
        assertTrue(instance.hasEffect());
        instance.setDegrees(0.00001f);
        assertFalse(instance.hasEffect());
    }

    @Test
    public void hasEffectWithArguments() {
        Dimension fullSize = new Dimension(600, 400);
        OperationList opList = new OperationList();
        opList.add(new Crop(0, 0, 300, 200));

        assertFalse(instance.hasEffect(fullSize, opList));
        instance.setDegrees(30);
        assertTrue(instance.hasEffect(fullSize, opList));
        instance.setDegrees(0.001f);
        assertTrue(instance.hasEffect(fullSize, opList));
        instance.setDegrees(0.00001f);
        assertFalse(instance.hasEffect(fullSize, opList));
    }

    @Test
    public void setDegrees() {
        float degrees = 50f;
        instance.setDegrees(degrees);
        assertEquals(degrees, instance.getDegrees(), 0.000001f);
    }

    @Test
    public void setDegreesWithLargeDegrees() {
        float degrees = 530f;
        try {
            instance.setDegrees(degrees);
        } catch (IllegalArgumentException e) {
            assertEquals("Degrees must be between 0 and 360", e.getMessage());
        }
    }

    @Test
    public void setDegreesWithNegativeDegrees() {
        float degrees = -50f;
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
        Map<String,Object> map = instance.toMap(new Dimension(0, 0));
        assertEquals(instance.getClass().getSimpleName(), map.get("class"));
        assertEquals(15f, map.get("degrees"));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void toMapReturnsUnmodifiableMap() {
        Dimension fullSize = new Dimension(100, 100);
        Map<String,Object> map = instance.toMap(fullSize);
        map.put("test", "test");
    }

    @Test
    public void testToString() {
        Rotate r = new Rotate(50f);
        assertEquals("50", r.toString());
        r = new Rotate(50.5f);
        assertEquals("50.5", r.toString());
    }

}
