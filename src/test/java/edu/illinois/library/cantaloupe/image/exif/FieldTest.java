package edu.illinois.library.cantaloupe.image.exif;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class FieldTest extends BaseTest {

    private Field instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Field(Tag.DATE_TIME_ORIGINAL, DataType.ASCII);
    }

    @Test
    void testConstructorWithNullTagArgument() {
        assertThrows(NullPointerException.class, () -> new Field(null, DataType.SHORT));
    }

    @Test
    void testConstructorWithNullDataTypeArgument() {
        assertThrows(NullPointerException.class, () -> new Field(Tag.MAKE, null));
    }

    @Test
    void testCompareTo() {
        Field other = new Field(Tag.DATE_TIME_ORIGINAL, DataType.SHORT);
        assertEquals(0, instance.compareTo(other));

        other = new Field(Tag.DATE_TIME_DIGITIZED, DataType.SHORT);
        assertEquals(-1, instance.compareTo(other));
        assertEquals(1, other.compareTo(instance));
    }

    @Test
    void testEqualsWithEqualInstances() {
        Field other = new Field(Tag.DATE_TIME_ORIGINAL, DataType.SHORT);
        assertEquals(instance, other);
    }

    @Test
    void testEqualsWithUnequalInstances() {
        Field other = new Field(Tag.DATE_TIME, DataType.SHORT);
        assertNotEquals(instance, other);
    }

    @Test
    void testHashCodeWithEqualInstances() {
        Field other = new Field(Tag.DATE_TIME_ORIGINAL, DataType.SHORT);
        assertEquals(instance.hashCode(), other.hashCode());
    }

    @Test
    void testHashCodeWithUnequalInstances() {
        Field other = new Field(Tag.DATE_TIME, DataType.SHORT);
        assertNotEquals(instance.hashCode(), other.hashCode());
    }

}