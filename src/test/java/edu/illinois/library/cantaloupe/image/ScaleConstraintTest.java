package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import java.awt.Dimension;
import java.util.Map;

import static org.junit.Assert.*;

public class ScaleConstraintTest extends BaseTest {

    private ScaleConstraint instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new ScaleConstraint(2, 3);
    }

    @Test
    public void testFromIdentifierPathComponentWithNoConstraintPresent() {
        assertNull(ScaleConstraint.fromIdentifierPathComponent("cats"));
    }

    @Test
    public void testFromIdentifierPathComponentWithConstraintPresent() {
        assertEquals(instance,
                ScaleConstraint.fromIdentifierPathComponent("cats-2:3"));
    }

    @Test
    public void testFromIdentifierPathComponentWithNullArgument() {
        assertNull(ScaleConstraint.fromIdentifierPathComponent(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithLargerNumeratorThanDenominator() {
        new ScaleConstraint(3, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNegativeNumerator() {
        new ScaleConstraint(-1, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNegativeDenominator() {
        new ScaleConstraint(1, -2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithZeroNumerator() {
        new ScaleConstraint(0, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithZeroDenominator() {
        new ScaleConstraint(1, 0);
    }

    @Test
    public void testEquals() {
        assertEquals(instance, new ScaleConstraint(2, 3));
        assertNotEquals(instance, new ScaleConstraint(3, 4));
    }

    @Test
    public void testGetConstrainedSize() {
        Dimension fullSize = new Dimension(900, 600);
        Dimension actual = instance.getConstrainedSize(fullSize);
        assertEquals(600, actual.width);
        assertEquals(400, actual.height);
    }

    @Test
    public void testGetReduced() {
        assertSame(instance, instance.getReduced());
        assertEquals(new ScaleConstraint(23, 27),
                new ScaleConstraint(92, 108).getReduced());
    }

    @Test
    public void testGetResultingSize() {
        Dimension fullSize = new Dimension(1500, 1200);
        Dimension actual = instance.getResultingSize(fullSize);
        assertEquals(new Dimension(1000, 800), actual);
    }

    @Test
    public void testGetScale() {
        final double delta = 0.00000001;
        assertTrue(Math.abs((2 / 3.0) - instance.getScale()) < delta);
    }

    @Test
    public void testHasEffect() {
        assertTrue(instance.hasEffect());
        instance = new ScaleConstraint(2, 2);
        assertFalse(instance.hasEffect());
    }

    @Test
    public void testHashCode() {
        assertEquals(instance.hashCode(), new ScaleConstraint(2, 3).hashCode());
        assertNotEquals(instance.hashCode(), new ScaleConstraint(3, 4).hashCode());
    }

    @Test
    public void testToIdentifierSuffix() {
        assertEquals("-2:3", instance.toIdentifierSuffix());
    }

    @Test
    public void testToMap() {
        Map<String,Long> actual = instance.toMap();
        assertEquals(2, actual.size());
        assertEquals(2, (long) actual.get("numerator"));
        assertEquals(3, (long) actual.get("denominator"));
    }

    @Test
    public void testToString() {
        assertEquals("2:3", instance.toString());
    }

}