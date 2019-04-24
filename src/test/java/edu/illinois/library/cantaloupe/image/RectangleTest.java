package edu.illinois.library.cantaloupe.image;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RectangleTest {

    private static final double DELTA = 0.00000001;

    private Rectangle instance;

    @Before
    public void setUp() throws Exception {
        instance = new Rectangle(10, 5, 1000, 800);
    }

    @Test
    public void testDoubleConstructor() {
        instance = new Rectangle(10.2, 5.2, 1000.2, 800.2);
        assertEquals(10.2, instance.x(), DELTA);
        assertEquals(5.2, instance.y(), DELTA);
        assertEquals(1000.2, instance.width(), DELTA);
        assertEquals(800.2, instance.height(), DELTA);
    }

    @Test
    public void testIntegerConstructor() {
        assertEquals(10, instance.x(), DELTA);
        assertEquals(5, instance.y(), DELTA);
        assertEquals(1000, instance.width(), DELTA);
        assertEquals(800, instance.height(), DELTA);
    }

    @Test
    public void testCopyConstructor() {
        Rectangle other = new Rectangle(instance);
        assertEquals(other, instance);
    }

    @Test
    public void testContainsAgainstSmallerInstance() {
        assertTrue(instance.contains(new Rectangle(11, 6, 998, 798)));
    }

    @Test
    public void testContainsAgainstEqualInstance() {
        assertTrue(instance.contains(new Rectangle(10, 5, 1000, 800)));
    }

    @Test
    public void testContainsAgainstOutOfBoundsOrigins() {
        // X
        assertFalse(instance.contains(new Rectangle(9, 5, 1000, 800)));
        // Y
        assertFalse(instance.contains(new Rectangle(10, 4, 1000, 800)));
    }

    @Test
    public void testContainsAgainstOutOfBoundsDimensions() {
        // X
        assertFalse(instance.contains(new Rectangle(10, 5, 1001, 800)));
        assertFalse(instance.contains(new Rectangle(500, 5, 511, 800)));
        // Y
        assertFalse(instance.contains(new Rectangle(10, 5, 1000, 801)));
        assertFalse(instance.contains(new Rectangle(10, 400, 1000, 406)));
    }

    @Test
    public void testEqualsWithEqualInstances() {
        assertEquals(instance, new Rectangle(10, 5, 1000, 800));
    }

    @Test
    public void testEqualsWithUnequalInstances() {
        assertNotEquals(instance, new Rectangle(11, 5, 1000, 800));
        assertNotEquals(instance, new Rectangle(10, 6, 1000, 800));
        assertNotEquals(instance, new Rectangle(10, 5, 1001, 800));
        assertNotEquals(instance, new Rectangle(10, 5, 1000, 801));
    }

    @Test
    public void testGrowWidth() {
        final double initialWidth = instance.width();
        instance.growWidth(3.2);
        Rectangle expected = new Rectangle(
                instance.x(), instance.y(),
                initialWidth + 3.2, instance.height());
        assertEquals(expected, instance);
    }

    @Test
    public void testGrowHeight() {
        final double initialHeight = instance.height();
        instance.growHeight(3.2);
        Rectangle expected = new Rectangle(
                instance.x(), instance.y(),
                instance.width(), initialHeight + 3.2);
        assertEquals(expected, instance);
    }

    @Test
    public void testHashCode() {
        int expected = Long.hashCode(
                Double.hashCode(instance.x()) +
                Double.hashCode(instance.y()) +
                Double.hashCode(instance.width()) +
                Double.hashCode(instance.height()));
        assertEquals(expected, instance.hashCode());
    }

    @Test
    public void testIntX() {
        instance.setX(5.4);
        assertEquals(5, instance.intX());
        instance.setX(5.6);
        assertEquals(6, instance.intX());
    }

    @Test
    public void testIntY() {
        instance.setY(5.4);
        assertEquals(5, instance.intY());
        instance.setY(5.6);
        assertEquals(6, instance.intY());
    }

    @Test
    public void testIntWidth() {
        instance.setWidth(5.4);
        assertEquals(5, instance.intWidth());
        instance.setWidth(5.6);
        assertEquals(6, instance.intWidth());
    }

    @Test
    public void testIntHeight() {
        instance.setHeight(5.4);
        assertEquals(5, instance.intHeight());
        instance.setHeight(5.6);
        assertEquals(6, instance.intHeight());
    }

    @Test
    public void testIntersectsWithIntersectingInstance() {
        Rectangle other = new Rectangle(instance);
        assertTrue(other.intersects(instance));

        other.setX(0);
        other.setY(0);
        assertTrue(other.intersects(instance));

        other.setX(500);
        other.setY(500);
        assertTrue(other.intersects(instance));
    }

    @Test
    public void testIntersectsWithNonIntersectingInstance() {
        // too far N
        Rectangle other = new Rectangle(10, 0, 1000, 5);
        assertFalse(other.intersects(instance));

        // too far E
        other = new Rectangle(1100, 0, 1000, 800);
        assertFalse(other.intersects(instance));

        // too far S
        other = new Rectangle(10, 900, 1000, 800);
        assertFalse(other.intersects(instance));

        // too far W
        other = new Rectangle(0, 0, 4, 800);
        assertFalse(other.intersects(instance));
    }

    @Test
    public void testIsEmptyWithNonEmptyInstance() {
        assertFalse(instance.isEmpty());
    }

    @Test
    public void testIsEmptyWithEmptyWidth() {
        instance.setWidth(0.4);
        assertTrue(instance.isEmpty());
    }

    @Test
    public void testIsEmptyWithEmptyHeight() {
        instance.setHeight(0.4);
        assertTrue(instance.isEmpty());
    }

    @Test
    public void testMoveLeft() {
        final double initialX = instance.x();
        instance.moveLeft(3.5);
        Rectangle expected = new Rectangle(
                initialX - 3.5, instance.y(),
                instance.width(), instance.height());
        assertEquals(expected, instance);
    }

    @Test
    public void testMoveRight() {
        final double initialX = instance.x();
        instance.moveRight(3.5);
        Rectangle expected = new Rectangle(
                initialX + 3.5, instance.y(),
                instance.width(), instance.height());
        assertEquals(expected, instance);
    }

    @Test
    public void testMoveUp() {
        final double initialY = instance.y();
        instance.moveUp(3.5);
        Rectangle expected = new Rectangle(
                instance.x(), initialY - 3.5,
                instance.width(), instance.height());
        assertEquals(expected, instance);
    }

    @Test
    public void testMoveDown() {
        final double initialY = instance.y();
        instance.moveDown(3.5);
        Rectangle expected = new Rectangle(
                instance.x(), initialY + 3.5,
                instance.width(), instance.height());
        assertEquals(expected, instance);
    }

    @Test
    public void testScaleX() {
        instance.scaleX(0.8);
        assertEquals(8, instance.x(), DELTA);
        assertEquals(5, instance.y(), DELTA);
        assertEquals(800, instance.width(), DELTA);
        assertEquals(800, instance.height(), DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testScaleXWithZeroArgument() {
        instance.scaleX(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testScaleXWithNegativeArgument() {
        instance.scaleX(-0.5);
    }

    @Test
    public void testScaleY() {
        instance.scaleY(0.8);
        assertEquals(10, instance.x(), DELTA);
        assertEquals(4, instance.y(), DELTA);
        assertEquals(1000, instance.width(), DELTA);
        assertEquals(640, instance.height(), DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testScaleYWithZeroArgument() {
        instance.scaleY(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testScaleYWithNegativeArgument() {
        instance.scaleY(-0.5);
    }

    @Test
    public void testSize() {
        assertEquals(new Dimension(1000, 800), instance.size());
    }

    @Test
    public void testSetDimension() {
        Dimension size = new Dimension(50, 40);
        instance.setDimension(size);
        assertEquals(size, instance.size());
    }

    @Test
    public void testSetDoubleX() {
        instance.setX(5.2);
        assertEquals(5.2, instance.x(), DELTA);
    }

    @Test
    public void testSetIntegerX() {
        instance.setX(5);
        assertEquals(5, instance.intX());
    }

    @Test
    public void testSetDoubleY() {
        instance.setY(5.2);
        assertEquals(5.2, instance.y(), DELTA);
    }

    @Test
    public void testSetIntegerY() {
        instance.setY(5);
        assertEquals(5, instance.intY());
    }

    @Test
    public void testSetDoubleWidth() {
        instance.setWidth(5.2);
        assertEquals(5.2, instance.width(), DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetDoubleWidthWithNegativeArgument() {
        instance.setWidth(-5.2);
    }

    @Test
    public void testSetIntegerWidth() {
        instance.setWidth(5);
        assertEquals(5, instance.intWidth());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetIntegerWidthWithNegativeArgument() {
        instance.setWidth(-5);
    }

    @Test
    public void testSetDoubleHeight() {
        instance.setHeight(5.2);
        assertEquals(5.2, instance.height(), DELTA);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetDoubleHeightWithNegativeArgument() {
        instance.setHeight(-5.2);
    }

    @Test
    public void testSetIntegerHeight() {
        instance.setHeight(5);
        assertEquals(5, instance.intHeight());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetIntegerHeightWithNegativeArgument() {
        instance.setHeight(-5);
    }

    @Test
    public void testToAWTRectangle() {
        java.awt.Rectangle expected = new java.awt.Rectangle(10, 5, 1000, 800);
        assertEquals(expected, instance.toAWTRectangle());
    }

    @Test
    public void testToString() {
        String expected = String.format("%f,%f/%fx%f",
                instance.x(), instance.y(),
                instance.width(), instance.height());
        assertEquals(expected, instance.toString());
    }

}
