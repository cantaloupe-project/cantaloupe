package edu.illinois.library.cantaloupe.http;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RangeTest extends BaseTest {

    private Range instance;

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        instance = new Range(0, 100, 500);
    }

    @Test
    void testEqualsWithEqualInstances() {
        Range other = new Range(0, 100, 500);
        assertEquals(instance, other);
    }

    @Test
    void testEqualsWithUnequalInstances() {
        Range other = new Range(1, 100, 500);
        assertNotEquals(instance, other);

        other = new Range(0, 101, 500);
        assertNotEquals(instance, other);

        other = new Range(0, 100, 501);
        assertNotEquals(instance, other);
    }

    @Test
    void testHashCodeWithEqualInstances() {
        Range other = new Range(0, 100, 500);
        assertEquals(instance.hashCode(), other.hashCode());
    }

    @Test
    void testHashCodeWithUnequalInstances() {
        Range other = new Range(1, 100, 500);
        assertNotEquals(instance.hashCode(), other.hashCode());

        other = new Range(0, 101, 500);
        assertNotEquals(instance.hashCode(), other.hashCode());

        other = new Range(0, 100, 501);
        assertNotEquals(instance.hashCode(), other.hashCode());
    }

}
