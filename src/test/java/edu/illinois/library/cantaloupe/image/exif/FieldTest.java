package edu.illinois.library.cantaloupe.image.exif;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class FieldTest extends BaseTest {

    private Field instance;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        instance = new Field(Tag.DATE_TIME_ORIGINAL, DataType.ASCII);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullTagArgument() {
        new Field(null, DataType.SHORT);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullDataTypeArgument() {
        new Field(Tag.MAKE, null);
    }

    @Test
    public void testCompareTo() {
        Field other = new Field(Tag.DATE_TIME_ORIGINAL, DataType.SHORT);
        assertEquals(0, instance.compareTo(other));

        other = new Field(Tag.DATE_TIME_DIGITIZED, DataType.SHORT);
        assertEquals(-1, instance.compareTo(other));
        assertEquals(1, other.compareTo(instance));
    }

    @Test
    public void testEqualsWithEqualInstances() {
        Field other = new Field(Tag.DATE_TIME_ORIGINAL, DataType.SHORT);
        assertEquals(instance, other);
    }

    @Test
    public void testEqualsWithUnequalInstances() {
        Field other = new Field(Tag.DATE_TIME, DataType.SHORT);
        assertNotEquals(instance, other);
    }

    @Test
    public void testHashCodeWithEqualInstances() {
        Field other = new Field(Tag.DATE_TIME_ORIGINAL, DataType.SHORT);
        assertEquals(instance.hashCode(), other.hashCode());
    }

    @Test
    public void testHashCodeWithUnequalInstances() {
        Field other = new Field(Tag.DATE_TIME, DataType.SHORT);
        assertNotEquals(instance.hashCode(), other.hashCode());
    }

}