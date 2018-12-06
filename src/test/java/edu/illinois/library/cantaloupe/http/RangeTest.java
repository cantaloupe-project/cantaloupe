package edu.illinois.library.cantaloupe.http;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class RangeTest extends BaseTest {

    private Range instance;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        instance = new Range(0, 100, 500);
    }

    @Test
    public void testEqualsWithEqualInstances() {
        Range other = new Range(0, 100, 500);
        assertEquals(instance, other);
    }

    @Test
    public void testEqualsWithUnequalInstances() {
        Range other = new Range(1, 100, 500);
        assertNotEquals(instance, other);

        other = new Range(0, 101, 500);
        assertNotEquals(instance, other);

        other = new Range(0, 100, 501);
        assertNotEquals(instance, other);
    }

    @Test
    public void testHashCodeWithEqualInstances() {
        Range other = new Range(0, 100, 500);
        assertEquals(instance.hashCode(), other.hashCode());
    }

    @Test
    public void testHashCodeWithUnequalInstances() {
        Range other = new Range(1, 100, 500);
        assertNotEquals(instance.hashCode(), other.hashCode());

        other = new Range(0, 101, 500);
        assertNotEquals(instance.hashCode(), other.hashCode());

        other = new Range(0, 100, 501);
        assertNotEquals(instance.hashCode(), other.hashCode());
    }

}
