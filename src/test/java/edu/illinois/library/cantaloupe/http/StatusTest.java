package edu.illinois.library.cantaloupe.http;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class StatusTest extends BaseTest {

    private Status instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Status(200);
    }

    @Test
    void testEqualsWithEqualInstances() {
        assertEquals(new Status(200), instance);
    }

    @Test
    void testEqualsWithUnequalCodes() {
        assertNotEquals(new Status(201), instance);
    }

    @Test
    void testEqualsWithUnequalDescriptions() {
        assertEquals(new Status(200, "Okey-Dokey"), instance);
    }

    @Test
    void testHashCode() {
        assertEquals(Integer.hashCode(200), instance.hashCode());
    }

    @Test
    void testToString() {
        assertEquals("200 OK", instance.toString());
    }

}