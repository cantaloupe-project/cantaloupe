package edu.illinois.library.cantaloupe.status;

import edu.illinois.library.cantaloupe.test.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class HealthTest extends BaseTest {

    private Health instance;

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        instance = new Health();
    }

    @Test
    void testSetMinColor() {
        assertEquals(Health.Color.GREEN, instance.getColor());

        instance.setMinColor(Health.Color.YELLOW);
        assertEquals(Health.Color.YELLOW, instance.getColor());

        instance.setMinColor(Health.Color.RED);
        assertEquals(Health.Color.RED, instance.getColor());

        instance.setMinColor(Health.Color.GREEN);
        assertEquals(Health.Color.RED, instance.getColor());
    }

    @Test
    void testToString() {
        assertEquals("GREEN", instance.toString());

        instance.setMinColor(Health.Color.RED);
        instance.setMessage("cats");
        assertEquals("RED: cats", instance.toString());
    }

}
