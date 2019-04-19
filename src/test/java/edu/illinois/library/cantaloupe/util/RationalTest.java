package edu.illinois.library.cantaloupe.util;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RationalTest extends BaseTest {

    private static final float FLOAT_DELTA   = 0.00001f;
    private static final double DOUBLE_DELTA = 0.00000000001;

    private Rational instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Rational(2, 3);
    }

    @Test
    void testConstructorWithZeroDenominator() {
        assertThrows(IllegalArgumentException.class, () -> new Rational(2, 0));
    }

    @Test
    void testDoubleValue() {
        assertTrue(Math.abs((2 / 3.0) - instance.doubleValue()) < DOUBLE_DELTA);
    }

    @Test
    void testEquals() {
        assertEquals(instance, new Rational(2, 3));
        assertNotEquals(instance, new Rational(3, 4));
    }

    @Test
    void testFloatValue() {
        assertTrue(Math.abs((2 / 3.0) - instance.floatValue()) < FLOAT_DELTA);
    }

    @Test
    void testGetReduced() {
        assertSame(instance, instance.getReduced());
        assertEquals(new Rational(23, 27), new Rational(92, 108).getReduced());
    }

    @Test
    void testHashCode() {
        assertEquals(instance.hashCode(), new Rational(2, 3).hashCode());
        assertNotEquals(instance.hashCode(), new Rational(3, 4).hashCode());
    }

    @Test
    void testToMap() {
        Map<String,Long> expected = new LinkedHashMap<>(2);
        expected.put("numerator", 2L);
        expected.put("denominator", 3L);
        assertEquals(expected, instance.toMap());
    }

    @Test
    void testToString() {
        assertEquals("2:3", instance.toString());
    }

}
