package edu.illinois.library.cantaloupe.image;

import edu.illinois.library.cantaloupe.config.Configuration;
import edu.illinois.library.cantaloupe.config.Key;
import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

public class ScaleConstraintTest extends BaseTest {

    private static final double DELTA = 0.00000001;

    private ScaleConstraint instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new ScaleConstraint(2, 3);
    }

    @Test
    void testFromIdentifierPathComponentWithNoConstraintPresent() {
        assertNull(ScaleConstraint.fromIdentifierPathComponent("cats"));
    }

    @Test
    void testFromIdentifierPathComponentWithConstraintPresent() {
        assertEquals(instance,
                ScaleConstraint.fromIdentifierPathComponent("cats-2:3"));
    }

    @Test
    void testFromIdentifierPathComponentWithNullArgument() {
        assertNull(ScaleConstraint.fromIdentifierPathComponent(null));
    }

    @Test
    void testGetIdentifierSuffixPattern() {
        var config = Configuration.getInstance();
        config.setProperty(Key.SCALE_CONSTRAINT_DELIMITER, "cats");
        Pattern expected = Pattern.compile("cats(\\d+):(\\d+)\\b");
        Pattern actual   = ScaleConstraint.getIdentifierSuffixPattern();
        assertEquals(expected.toString(), actual.toString());
    }

    @Test
    void testGetIdentifierSuffixPatternWithoutConfigKeySet() {
        var config = Configuration.getInstance();
        config.clearProperty(Key.SCALE_CONSTRAINT_DELIMITER);
        Pattern expected = Pattern.compile("-(\\d+):(\\d+)\\b");
        Pattern actual   = ScaleConstraint.getIdentifierSuffixPattern();
        assertEquals(expected.toString(), actual.toString());
    }

    @Test
    void testConstructorWithLargerNumeratorThanDenominator() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScaleConstraint(3, 2));
    }

    @Test
    void testConstructorWithNegativeNumerator() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScaleConstraint(-1, 2));
    }

    @Test
    void testConstructorWithNegativeDenominator() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScaleConstraint(1, -2));
    }

    @Test
    void testConstructorWithZeroNumerator() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScaleConstraint(0, 2));
    }

    @Test
    void testConstructorWithZeroDenominator() {
        assertThrows(IllegalArgumentException.class,
                () -> new ScaleConstraint(1, 0));
    }

    @Test
    void testEquals() {
        assertEquals(instance, new ScaleConstraint(2, 3));
        assertNotEquals(instance, new ScaleConstraint(3, 4));
    }

    @Test
    void testGetConstrainedSize() {
        Dimension fullSize = new Dimension(900, 600);
        Dimension actual = instance.getConstrainedSize(fullSize);
        assertEquals(600, actual.width(), DELTA);
        assertEquals(400, actual.height(), DELTA);
    }

    @Test
    void testGetReduced() {
        assertEquals(instance, instance.getReduced());
        assertEquals(new ScaleConstraint(23, 27),
                new ScaleConstraint(92, 108).getReduced());
    }

    @Test
    void testGetResultingSize() {
        Dimension fullSize = new Dimension(1500, 1200);
        Dimension actual = instance.getResultingSize(fullSize);
        assertEquals(new Dimension(1000, 800), actual);
    }

    @Test
    void testHasEffect() {
        assertTrue(instance.hasEffect());
        instance = new ScaleConstraint(2, 2);
        assertFalse(instance.hasEffect());
    }

    @Test
    void testHashCode() {
        double[] codes = { Long.hashCode(2), Long.hashCode(3) };
        int expected = Arrays.hashCode(codes);
        assertEquals(expected, instance.hashCode());
    }

    @Test
    void testToIdentifierSuffixWithScaleConstraintDelimiterSet() {
        var config = Configuration.getInstance();
        config.setProperty(Key.SCALE_CONSTRAINT_DELIMITER, "cats");
        assertEquals("cats2:3", instance.toIdentifierSuffix());
    }

    @Test
    void testToIdentifierSuffixWithoutScaleConstraintDelimiterSet() {
        var config = Configuration.getInstance();
        config.clearProperty(Key.SCALE_CONSTRAINT_DELIMITER);
        assertEquals("-2:3", instance.toIdentifierSuffix());
    }

    @Test
    void testToMap() {
        Map<String,Long> actual = instance.toMap();
        assertEquals(2, actual.size());
        assertEquals(2, (long) actual.get("numerator"));
        assertEquals(3, (long) actual.get("denominator"));
    }

    @Test
    void testToString() {
        assertEquals("2:3", instance.toString());
    }

}