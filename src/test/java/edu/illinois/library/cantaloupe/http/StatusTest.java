package edu.illinois.library.cantaloupe.http;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class StatusTest {

    private Status instance;

    @Before
    public void setUp() throws Exception {
        instance = new Status(200);
    }

    @Test
    public void testEqualsWithEqualInstances() {
        assertEquals(new Status(200), instance);
    }

    @Test
    public void testEqualsWithUnequalCodes() {
        assertNotEquals(new Status(201), instance);
    }

    @Test
    public void testEqualsWithUnequalDescriptions() {
        assertEquals(new Status(200, "Okey-Dokey"), instance);
    }

    @Test
    public void testHashCode() {
        assertEquals(Integer.hashCode(200), instance.hashCode());
    }

    @Test
    public void testToString() {
        assertEquals("200 OK", instance.toString());
    }

}