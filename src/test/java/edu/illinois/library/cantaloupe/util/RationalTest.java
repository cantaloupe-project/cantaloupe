package edu.illinois.library.cantaloupe.util;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class RationalTest extends BaseTest {

    private static final float FLOAT_DELTA   = 0.00001f;
    private static final double DOUBLE_DELTA = 0.00000000001;

    private Rational instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new Rational(2, 3);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithZeroDenominator() {
        new Rational(2, 0);
    }

    @Test
    public void testDoubleValue() {
        assertTrue(Math.abs((2 / 3.0) - instance.doubleValue()) < DOUBLE_DELTA);
    }

    @Test
    public void testEquals() {
        assertEquals(instance, new Rational(2, 3));
        assertNotEquals(instance, new Rational(3, 4));
    }

    @Test
    public void testFloatValue() {
        assertTrue(Math.abs((2 / 3.0) - instance.floatValue()) < FLOAT_DELTA);
    }

    @Test
    public void testGetReduced() {
        assertSame(instance, instance.getReduced());
        assertEquals(new Rational(23, 27), new Rational(92, 108).getReduced());
    }

    @Test
    public void testHashCode() {
        assertEquals(instance.hashCode(), new Rational(2, 3).hashCode());
        assertNotEquals(instance.hashCode(), new Rational(3, 4).hashCode());
    }

    @Test
    public void testToMap() {
        Map<String,Long> expected = new LinkedHashMap<>(2);
        expected.put("numerator", 2L);
        expected.put("denominator", 3L);
        assertEquals(expected, instance.toMap());
    }

    @Test
    public void testToString() {
        assertEquals("2:3", instance.toString());
    }

}
