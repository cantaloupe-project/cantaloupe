package edu.illinois.library.cantaloupe.image.exif;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RationalTest extends BaseTest {

    private Rational instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Rational(2, 3);
    }

    @Test
    void testEquals() {
        assertEquals(instance, new Rational(2, 3));
        assertNotEquals(instance, new Rational(3, 4));
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
