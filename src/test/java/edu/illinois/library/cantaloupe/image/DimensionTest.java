package edu.illinois.library.cantaloupe.image;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class DimensionTest {

    private static final double DELTA = 0.00000001;

    private Dimension instance;

    @Before
    public void setUp() throws Exception {
        instance = new Dimension(1000, 800);
    }

    @Test
    public void testDoubleConstructor() {
        instance = new Dimension(5.5, 4.4);
        assertEquals(5.5, instance.width(), DELTA);
        assertEquals(4.4, instance.height(), DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDoubleConstructorWithNegativeArgument() {
        new Dimension(-5.5, -4.4);
    }

    @Test
    public void testIntegerConstructor() {
        instance = new Dimension(5, 4);
        assertEquals(5, instance.width(), DELTA);
        assertEquals(4, instance.height(), DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIntegerConstructorWithNegativeArgument() {
        new Dimension(-5, -4);
    }

    @Test
    public void testCopyConstructor() {
        Dimension other = new Dimension(instance);
        assertEquals(other, instance);
    }

    @Test
    public void testEqualsWithEqualInstances() {
        assertEquals(instance, new Dimension(1000, 800));
    }

    @Test
    public void testEqualsWithUnequalInstances() {
        assertNotEquals(instance, new Dimension(1001, 800));
        assertNotEquals(instance, new Dimension(1000, 801));
    }

    @Test
    public void testHashCode() {
        int expected = Long.hashCode(Double.hashCode(instance.width()) +
                Double.hashCode(instance.height()));
        assertEquals(expected, instance.hashCode());
    }

    @Test
    public void testWidth() {
        assertEquals(1000, instance.width(), DELTA);
    }

    @Test
    public void testHeight() {
        assertEquals(800, instance.height(), DELTA);
    }

    @Test
    public void testIntWidth() {
        instance.setWidth(45.2);
        assertEquals(45, instance.intWidth());

        instance.setWidth(45.6);
        assertEquals(46, instance.intWidth());
    }

    @Test
    public void testIntHeight() {
        instance.setHeight(45.2);
        assertEquals(45, instance.intHeight());

        instance.setHeight(45.6);
        assertEquals(46, instance.intHeight());
    }

    @Test
    public void testInvert() {
        instance.invert();
        assertEquals(800, instance.width(), DELTA);
        assertEquals(1000, instance.height(), DELTA);
    }

    @Test
    public void testIsEmpty() {
        // width > 0.5, height > 0.5
        assertFalse(instance.isEmpty());

        // width < 0.5, height > 0.5
        instance = new Dimension(0.4, 0.6);
        assertTrue(instance.isEmpty());

        // width > 0.5, height < 0.5
        instance = new Dimension(0.6, 0.4);
        assertTrue(instance.isEmpty());

        // width < 0.5, height < 0.5
        instance = new Dimension(0.4, 0.4);
        assertTrue(instance.isEmpty());
    }

    @Test
    public void testScaleBy() {
        instance.scaleBy(1.5);
        assertEquals(1500, instance.width(), DELTA);
        assertEquals(1200, instance.height(), DELTA);
    }

    @Test
    public void testSetWidthWithDouble() {
        instance.setWidth(2.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetWidthWithDoubleAndNegativeArgument() {
        instance.setWidth(-1.0);
    }

    @Test
    public void testSetWidthWithInt() {
        instance.setWidth(5);
        assertEquals(5, instance.width(), DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetWidthWithIntAndNegativeArgument() {
        instance.setWidth(-1);
    }

    @Test
    public void testSetHeightWithDouble() {
        instance.setHeight(5.0);
        assertEquals(5.0, instance.height(), DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetHeightWithDoubleAndNegativeArgument() {
        instance.setHeight(-2.0);
    }

    @Test
    public void testSetHeightWithInt() {
        instance.setHeight(5);
        assertEquals(5, instance.height(), DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetHeightWithIntAndNegativeArgument() {
        instance.setHeight(-1);
    }

    @Test
    public void testToString() {
        assertEquals("1000x800", instance.toString());
    }

}