package edu.illinois.library.cantaloupe.status;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class HealthTest {

    private Health instance;

    @Before
    public void setUp() {
        instance = new Health();
    }

    @Test
    public void testSetMinColor() {
        assertEquals(Health.Color.GREEN, instance.getColor());

        instance.setMinColor(Health.Color.YELLOW);
        assertEquals(Health.Color.YELLOW, instance.getColor());

        instance.setMinColor(Health.Color.RED);
        assertEquals(Health.Color.RED, instance.getColor());

        instance.setMinColor(Health.Color.GREEN);
        assertEquals(Health.Color.RED, instance.getColor());
    }

    @Test
    public void testToString() {
        assertEquals("GREEN", instance.toString());

        instance.setMinColor(Health.Color.RED);
        instance.setMessage("cats");
        assertEquals("RED: cats", instance.toString());
    }

}
